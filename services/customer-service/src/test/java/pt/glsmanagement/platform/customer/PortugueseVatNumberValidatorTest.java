package pt.glsmanagement.platform.customer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PortugueseVatNumberValidatorTest {
    private final PortugueseVatNumberValidator validator = new PortugueseVatNumberValidator();

    @Test
    void acceptsSyntheticNumbersWithAValidChecksum() {
        assertThat(validator.isStructurallyValid("500000000")).isTrue();
        assertThat(validator.isStructurallyValid("600000001")).isTrue();
        assertThat(validator.isStructurallyValid("720000009")).isTrue();
        assertThat(validator.isStructurallyValid("800000005")).isTrue();
    }

    @Test
    void rejectsInvalidFormatAndChecksum() {
        assertThat(validator.isStructurallyValid(null)).isFalse();
        assertThat(validator.isStructurallyValid("")).isFalse();
        assertThat(validator.isStructurallyValid("50000000")).isFalse();
        assertThat(validator.isStructurallyValid("PT500000000")).isFalse();
        assertThat(validator.isStructurallyValid("500000001")).isFalse();
        assertThat(validator.isStructurallyValid("000000000")).isFalse();
    }

    @Test
    void rejectsTheConsumerFinalSentinel() {
        assertThat(validator.isStructurallyValid("999999990")).isFalse();
    }
}
