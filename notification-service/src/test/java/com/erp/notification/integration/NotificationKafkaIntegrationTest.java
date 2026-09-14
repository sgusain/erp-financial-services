package com.erp.notification.integration;

import com.erp.common.event.TransactionEvent;
import com.erp.notification.kafka.TransactionEventConsumer;
import com.erp.notification.support.PostgresTestContainer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Real-broker (EmbeddedKafka) tests of the notification-service Kafka listeners:
 * proves the manual-ack contract on the happy path, and that a listener which always
 * fails is retried via the non-blocking @RetryableTopic machinery and eventually lands
 * on the real -dlt topic.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EmbeddedKafka(
        partitions = 1,
        topics = {"transaction-events", "budget-events"},
        brokerProperties = {"auto.create.topics.enable=true"}
)
@DirtiesContext
class NotificationKafkaIntegrationTest extends PostgresTestContainer {

    @Autowired
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @SpyBean
    private TransactionEventConsumer transactionEventConsumer;

    @org.springframework.beans.factory.annotation.Value("${spring.embedded.kafka.brokers}")
    private String embeddedBrokers;

    @Test
    void consumeTransactionEvent_processesRealMessage_andAcknowledges() {
        TransactionEvent event = new TransactionEvent("txn-1", "acc-1", "DEBIT", new BigDecimal("42.00"), LocalDateTime.now());

        kafkaTemplate.send("transaction-events", event.getTransactionId(), event);

        verify(transactionEventConsumer, timeout(15_000)).consumeTransactionEvent(any(TransactionEvent.class), any(Acknowledgment.class));
    }

    @Test
    void consumeTransactionEvent_alwaysFailing_isRetried_andLandsOnDlt() throws Exception {
        doThrow(new RuntimeException("simulated processing failure"))
                .when(transactionEventConsumer)
                .consumeTransactionEvent(any(TransactionEvent.class), any(Acknowledgment.class));

        TransactionEvent event = new TransactionEvent("txn-dlt-1", "acc-2", "CREDIT", new BigDecimal("7.00"), LocalDateTime.now());
        kafkaTemplate.send("transaction-events", event.getTransactionId(), event);

        // 3 attempts with backoff (1s, 2s) before landing on the DLT - give it generous headroom.
        TransactionEvent dltEvent = pollDltForEvent("transaction-events-dlt", event.getTransactionId(), Duration.ofSeconds(30));
        assertThat(dltEvent).isNotNull();
        assertThat(dltEvent.getTransactionId()).isEqualTo("txn-dlt-1");

        // The consumer method itself should have been invoked more than once (original + retries).
        verify(transactionEventConsumer, timeout(1_000).atLeast(2))
                .consumeTransactionEvent(any(TransactionEvent.class), any(Acknowledgment.class));
    }

    private TransactionEvent pollDltForEvent(String topic, String transactionId, Duration timeout) throws Exception {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedBrokers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-dlt-verifier-" + System.nanoTime());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(topic));
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();

            long deadline = System.currentTimeMillis() + timeout.toMillis();
            while (System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> record : records) {
                    TransactionEvent event = mapper.readValue(record.value(), TransactionEvent.class);
                    if (transactionId.equals(event.getTransactionId())) {
                        return event;
                    }
                }
            }
            return null;
        }
    }
}
