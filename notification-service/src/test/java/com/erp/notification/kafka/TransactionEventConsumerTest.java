package com.erp.notification.kafka;

import com.erp.common.event.TransactionEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class TransactionEventConsumerTest {

    @Mock
    private Acknowledgment acknowledgment;

    private final TransactionEventConsumer consumer = new TransactionEventConsumer();

    @Test
    void consumeTransactionEvent_acknowledgesAfterProcessing() {
        TransactionEvent event = new TransactionEvent("1", "10", "DEBIT", new BigDecimal("25.00"), LocalDateTime.now());

        consumer.consumeTransactionEvent(event, acknowledgment);

        verify(acknowledgment, times(1)).acknowledge();
        verifyNoMoreInteractions(acknowledgment);
    }

    @Test
    void dltHandler_doesNotThrow_forFailedEvent() {
        TransactionEvent event = new TransactionEvent("2", "20", "CREDIT", new BigDecimal("5.00"), LocalDateTime.now());

        // Should just log - must not throw so the DLT machinery doesn't itself fail.
        consumer.handleTransactionDlt(event);
    }
}
