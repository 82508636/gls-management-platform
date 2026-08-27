package pt.glsmanagement.platform.customer;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class VatNumberNormalizerTest {
    @Test
    void createsOneCanonicalKeyForEquivalentPortugueseValues() {
        assertThat(VatNumberNormalizer.normalize("pt", "PT 501.234-567").key()).isEqualTo("PT:501234567");
        assertThat(VatNumberNormalizer.normalize("PT", "501234567").key()).isEqualTo("PT:501234567");
    }

    @Test
    void keepsEqualNumbersFromDifferentCountriesDistinct() {
        assertThat(VatNumberNormalizer.normalize("PT", "123456789").key())
                .isNotEqualTo(VatNumberNormalizer.normalize("ES", "123456789").key());
    }

    @Test
    void normalizesTheGreekViesCountryAlias() {
        assertThat(VatNumberNormalizer.normalize("GR", "GR123456789").key()).isEqualTo("EL:123456789");
    }
}
