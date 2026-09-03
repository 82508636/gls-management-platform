package pt.glsmanagement.platform.customer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VatNumberNormalizerTest {
    @Test
    void createsOneStableKeyForEquivalentPortugueseNifs() {
        assertThat(VatNumberNormalizer.normalize("pt", "PT 501.234.567").key())
                .isEqualTo("PT:501234567");
        assertThat(VatNumberNormalizer.normalize("PT", "501-234-567").key())
                .isEqualTo("PT:501234567");
    }

    @Test
    void mapsGreekCountryCodeToViesCode() {
        assertThat(VatNumberNormalizer.normalize("GR", "GR123456789").key())
                .isEqualTo("EL:123456789");
    }

    @Test
    void rejectsPunctuationOutsideTheAcceptedSeparators() {
        assertThatThrownBy(() -> VatNumberNormalizer.normalize("PT", "501_234_567"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
