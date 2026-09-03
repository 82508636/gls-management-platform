package pt.glsmanagement.platform.customer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VatValidationServiceTest {
    @Mock private VatValidationGateway gateway;
    private VatValidationService service;

    @BeforeEach
    void setUp() {
        service = new VatValidationService(new PortugueseVatNumberValidator(), gateway);
    }

    @Test
    void normalizesAndValidatesAPortugueseCompany() {
        when(gateway.validate("PT", "500000000")).thenReturn(new VatValidationGateway.Result(true));
        var result = service.validate(new VatValidationRequest(
                "pt", "PT 500-000-000", VatValidationSubjectType.COMPANY));
        assertThat(result.countryCode()).isEqualTo("PT");
        assertThat(result.vatNumber()).isEqualTo("500000000");
        assertThat(result.formatValid()).isTrue();
        assertThat(result.viesStatus()).isEqualTo(VatValidationStatus.VALID);
    }

    @Test
    void mapsAndSeparatesPortugueseRegistrationDetails() {
        when(gateway.validate("PT", "500498601")).thenReturn(new VatValidationGateway.Result(
                true, " CP - Comboios de Portugal, E.P.E. ",
                "CALÇADA DO DUQUE, 20\n1249-109 LISBOA", null, null, null));

        var result = service.validate(new VatValidationRequest(
                "PT", "500498601", VatValidationSubjectType.COMPANY));

        assertThat(result.registeredName()).isEqualTo("CP - Comboios de Portugal, E.P.E.");
        assertThat(result.registeredAddress()).isEqualTo("CALÇADA DO DUQUE, 20");
        assertThat(result.registeredPostalCode()).isEqualTo("1249-109");
        assertThat(result.registeredLocality()).isEqualTo("LISBOA");
    }

    @Test
    void ignoresViesPlaceholdersInsteadOfOverwritingBillingFields() {
        when(gateway.validate("PT", "500498601")).thenReturn(new VatValidationGateway.Result(
                true, "---", "---", null, null, null));

        var result = service.validate(new VatValidationRequest(
                "PT", "500498601", VatValidationSubjectType.COMPANY));

        assertThat(result.registeredName()).isNull();
        assertThat(result.registeredAddress()).isNull();
        assertThat(result.registeredPostalCode()).isNull();
        assertThat(result.registeredLocality()).isNull();
    }

    @Test
    void mapsANegativeViesResultWithoutClaimingTheNifDoesNotExist() {
        when(gateway.validate("PT", "500000000")).thenReturn(new VatValidationGateway.Result(false));
        var result = service.validate(new VatValidationRequest(
                "PT", "500000000", VatValidationSubjectType.COMPANY));
        assertThat(result.formatValid()).isTrue();
        assertThat(result.viesStatus()).isEqualTo(VatValidationStatus.NOT_VALID);
    }

    @Test
    void mapsTheGreekIsoCodeAndPrefixToTheViesCode() {
        when(gateway.validate("EL", "123456789")).thenReturn(new VatValidationGateway.Result(true));
        var result = service.validate(new VatValidationRequest(
                "GR", "GR 123-456-789", VatValidationSubjectType.COMPANY));
        assertThat(result.countryCode()).isEqualTo("EL");
        assertThat(result.vatNumber()).isEqualTo("123456789");
        assertThat(result.viesStatus()).isEqualTo(VatValidationStatus.VALID);
    }

    @Test
    void doesNotCallViesForAnInvalidPortugueseNumber() {
        var result = service.validate(new VatValidationRequest(
                "PT", "500000001", VatValidationSubjectType.COMPANY));
        assertThat(result.formatValid()).isFalse();
        assertThat(result.viesStatus()).isEqualTo(VatValidationStatus.NOT_CHECKED);
        verify(gateway, never()).validate(anyString(), anyString());
    }

    @Test
    void onlyPerformsStructuralValidationForPrivateCustomers() {
        var result = service.validate(new VatValidationRequest(
                "PT", "600000001", VatValidationSubjectType.PRIVATE));
        assertThat(result.formatValid()).isTrue();
        assertThat(result.viesStatus()).isEqualTo(VatValidationStatus.NOT_APPLICABLE);
        verify(gateway, never()).validate(anyString(), anyString());
    }

    @Test
    void mapsProviderFailuresToUnavailable() {
        when(gateway.validate("PT", "500000000"))
                .thenThrow(new VatValidationProviderUnavailableException());
        var result = service.validate(new VatValidationRequest(
                "PT", "500000000", VatValidationSubjectType.COMPANY));
        assertThat(result.formatValid()).isTrue();
        assertThat(result.viesStatus()).isEqualTo(VatValidationStatus.UNAVAILABLE);
    }

    @Test
    void doesNotCallViesForCountriesOutsideItsScope() {
        var result = service.validate(new VatValidationRequest(
                "US", "AB123456", VatValidationSubjectType.COMPANY));
        assertThat(result.formatValid()).isTrue();
        assertThat(result.viesStatus()).isEqualTo(VatValidationStatus.NOT_APPLICABLE);
        verify(gateway, never()).validate(anyString(), anyString());
    }
}
