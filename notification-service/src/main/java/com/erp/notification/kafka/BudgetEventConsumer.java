package com.erp.notification.kafka;

import com.erp.common.event.BudgetEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

/**
 * Consumes budget-events with non-blocking retry + DLT.
 * Kept in its own class (rather than combined with the transaction-event consumer)
 * because Spring Kafka only supports a single @DltHandler method per bean -
 * having one @DltHandler per listener class keeps DLT routing unambiguous.
 */
@Service
@Slf4j
public class BudgetEventConsumer {

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2),
            dltTopicSuffix = "-dlt",
            include = {Exception.class}
    )
    @KafkaListener(topics = "budget-events", groupId = "erp-notification-group")
    public void consumeBudgetEvent(BudgetEvent event, Acknowledgment ack) {
        log.info("Received budget event: {}", event);
        log.info("ALERT: Budget {} has been EXCEEDED. Total: {}, Spent: {} at {}",
                event.getBudgetName(),
                event.getTotalAmount(),
                event.getSpentAmount(),
                event.getTimestamp()
        );
        ack.acknowledge();
    }

    @DltHandler
    public void handleBudgetDlt(BudgetEvent event) {
        log.error("Budget event failed after retries, sent to DLT: {}", event);
    }
}
