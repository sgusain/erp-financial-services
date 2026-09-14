package com.erp.notification.kafka;

import com.erp.common.event.BudgetEvent;
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
class BudgetEventConsumerTest {

    @Mock
    private Acknowledgment acknowledgment;

    private final BudgetEventConsumer consumer = new BudgetEventConsumer();

    @Test
    void consumeBudgetEvent_acknowledgesAfterProcessing() {
        BudgetEvent event = new BudgetEvent("1", "Marketing", new BigDecimal("1000.00"),
                new BigDecimal("1000.00"), "EXCEEDED", LocalDateTime.now());

        consumer.consumeBudgetEvent(event, acknowledgment);

        verify(acknowledgment, times(1)).acknowledge();
        verifyNoMoreInteractions(acknowledgment);
    }

    @Test
    void dltHandler_doesNotThrow_forFailedEvent() {
        BudgetEvent event = new BudgetEvent("2", "Travel", new BigDecimal("500.00"),
                new BigDecimal("600.00"), "EXCEEDED", LocalDateTime.now());

        consumer.handleBudgetDlt(event);
    }
}
