package com.erp.ai.advisor;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PiiMaskingAdvisorTest {

    @Test
    void masksEmailCardAndPhone() {
        String masked = PiiMaskingAdvisor.maskText(
                "Mail jane.doe@example.com, card 4111 1111 1111 1111, call +91 98765 43210");

        assertThat(masked).contains("[EMAIL]", "[CARD]", "[PHONE]");
        assertThat(masked).doesNotContain("jane.doe@example.com", "4111", "98765");
    }

    @Test
    void leavesNormalFinanceTextAlone() {
        String text = "What is the balance of account 12 for fiscal year 2026?";

        assertThat(PiiMaskingAdvisor.maskText(text)).isEqualTo(text);
    }

    @Test
    void nullStaysNull() {
        assertThat(PiiMaskingAdvisor.maskText(null)).isNull();
    }
}
