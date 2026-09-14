package com.erp.budget.kafka;

import com.erp.common.event.BudgetEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class BudgetProducer {

    private static final String TOPIC = "budget-events";

    private final KafkaTemplate<String, BudgetEvent> kafkaTemplate;

    // Returns the send future so callers (the outbox poller) can confirm actual
    // broker acknowledgement before treating the event as delivered - a plain
    // fire-and-forget call here would let the poller mark a row "published" even
    // when the broker never received it (e.g. Kafka is down but the client-side
    // send() call itself doesn't throw synchronously).
    public CompletableFuture<SendResult<String, BudgetEvent>> sendBudgetEvent(BudgetEvent event) {
        log.info("Sending budget event to Kafka: {}", event);
        return kafkaTemplate.send(TOPIC, event.getBudgetId(), event);
    }
}
