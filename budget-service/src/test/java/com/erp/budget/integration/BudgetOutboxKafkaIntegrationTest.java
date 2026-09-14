package com.erp.budget.integration;

import com.erp.budget.repository.OutboxEventRepository;
import com.erp.budget.support.KafkaAndPostgresTestContainer;
import com.erp.budget.support.TestJwtUtil;
import com.erp.common.event.BudgetEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test of the outbox pattern: HTTP write -> outbox row -> @Scheduled
 * OutboxPublisher -> real Kafka broker (Testcontainers) -> a plain consumer reading
 * the budget-events topic. Proves actual broker delivery, not just that the DB
 * row's `published` flag flips.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class BudgetOutboxKafkaIntegrationTest extends KafkaAndPostgresTestContainer {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    private KafkaConsumer<String, String> consumer;

    @BeforeEach
    void setUpConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-verifier-" + System.nanoTime());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("budget-events"));
    }

    @AfterEach
    void tearDownConsumer() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    void exceededBudgetUpdate_isPublishedToKafka_viaOutboxPoller() throws Exception {
        String userAuth = "Bearer " + TestJwtUtil.userToken();

        MvcResult createResult = mockMvc.perform(post("/api/budgets")
                        .header(HttpHeaders.AUTHORIZATION, userAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"budgetName\":\"Kafka E2E\",\"totalAmount\":250.00,\"spentAmount\":0.00,\"fiscalYear\":2026,\"status\":\"ACTIVE\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        Long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/budgets/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, userAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"budgetName\":\"Kafka E2E\",\"totalAmount\":250.00,\"spentAmount\":250.00,\"fiscalYear\":2026,\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk());

        // 1. Confirm the outbox poller flips the row to published=true (DB-level proof).
        await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> assertThat(
                        outboxEventRepository.findAll().stream()
                                .anyMatch(e -> e.getAggregateId().equals(id.toString()) && e.isPublished())
                ).isTrue());

        // 2. Confirm the message actually landed on the real broker's budget-events topic
        // (broker-level proof, not just the DB flag).
        BudgetEvent found = pollForEvent(id.toString());
        assertThat(found).isNotNull();
        assertThat(found.getBudgetId()).isEqualTo(id.toString());
        assertThat(found.getStatus()).isEqualTo("EXCEEDED");
        assertThat(found.getSpentAmount()).isEqualByComparingTo("250.00");
    }

    private BudgetEvent pollForEvent(String budgetId) throws Exception {
        long deadline = System.currentTimeMillis() + Duration.ofSeconds(20).toMillis();
        while (System.currentTimeMillis() < deadline) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, String> record : records) {
                BudgetEvent event = objectMapper.readValue(record.value(), BudgetEvent.class);
                if (event.getBudgetId().equals(budgetId)) {
                    return event;
                }
            }
        }
        return null;
    }
}
