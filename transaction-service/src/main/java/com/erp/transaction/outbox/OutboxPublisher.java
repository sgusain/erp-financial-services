package com.erp.transaction.outbox;

import com.erp.transaction.entity.OutboxEvent;
import com.erp.transaction.kafka.TransactionProducer;
import com.erp.transaction.repository.OutboxEventRepository;
import com.erp.common.event.TransactionEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxPublisher {

    private static final int BATCH_SIZE = 50;

    private final OutboxEventRepository outboxEventRepository;
    private final TransactionProducer producer;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findByPublishedFalseOrderByCreatedAtAsc(PageRequest.of(0, BATCH_SIZE));

        for (OutboxEvent outboxEvent : pending) {
            try {
                TransactionEvent event = objectMapper.readValue(outboxEvent.getPayload(), TransactionEvent.class);
                // Block until the broker actually acknowledges the send (or it fails/times
                // out) so a Kafka outage leaves published=false for the next poll to retry,
                // instead of a fire-and-forget call optimistically marking it delivered.
                producer.sendTransactionEvent(event).get(5, TimeUnit.SECONDS);
                outboxEvent.setPublished(true);
                outboxEvent.setPublishedAt(LocalDateTime.now());
            } catch (Exception e) {
                log.error("Failed to publish outbox event id={}, will retry next poll", outboxEvent.getId(), e);
                // leave published=false, next scheduled run retries it
            }
        }
    }
}
