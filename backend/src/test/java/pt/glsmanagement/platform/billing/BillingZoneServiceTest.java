package pt.glsmanagement.platform.billing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static pt.glsmanagement.platform.billing.BillingZoneDtos.*;

class BillingZoneServiceTest {
    private BillingZoneRepository repository;
    private BillingZoneService service;

    @BeforeEach void setUp() {
        repository = mock(BillingZoneRepository.class);
        service = new BillingZoneService(repository);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test void normalizesIdentityCountryAndPostalPatterns() {
        var response = service.create(new CreateRequest(" pt-norte ", " Norte   litoral ",
                BillingZone.ZoneType.DESTINATION_POSTAL_CODES, " pt ", "  Continente  ",
                Set.of(" 4000-* ", "4000-*", " 4100-000 ")), "admin");

        assertThat(response.code()).isEqualTo("PT-NORTE");
        assertThat(response.designation()).isEqualTo("Norte litoral");
        assertThat(response.country()).isEqualTo("PT");
        assertThat(response.groupName()).isEqualTo("Continente");
        assertThat(response.postalCodePatterns()).containsExactly("4000-*", "4100-000");
        assertThat(response.createdBy()).isEqualTo("admin");
    }

    @Test void rejectsDuplicateCodeOrDesignationBeforeWriting() {
        when(repository.existsByCodeIgnoreCase("PT-NORTE")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("pt-norte", "Norte"), "admin"))
                .isInstanceOf(BillingZoneException.class)
                .extracting(error -> ((BillingZoneException) error).reason())
                .isEqualTo(BillingZoneException.Reason.DUPLICATE);
        verify(repository, never()).save(any());

        reset(repository);
        when(repository.existsByDesignationIgnoreCase("Norte")).thenReturn(true);
        assertThatThrownBy(() -> service.create(request("PT-N", "Norte"), "admin"))
                .isInstanceOf(BillingZoneException.class);
        verify(repository, never()).save(any());
    }

    @Test void onlyReportsAllActiveWhenEveryRequestedZoneIsActive() {
        var ids = Set.of(UUID.randomUUID(), UUID.randomUUID());
        when(repository.countByIdInAndActiveTrue(ids)).thenReturn(1L);
        assertThat(service.allActive(ids)).isFalse();
        when(repository.countByIdInAndActiveTrue(ids)).thenReturn(2L);
        assertThat(service.allActive(ids)).isTrue();
        assertThat(service.allActive(Set.of())).isTrue();
    }

    @Test void missingZoneCannotBeUpdatedOrHaveItsStatusChanged() {
        var id = UUID.randomUUID();
        assertThatThrownBy(() -> service.update(id, new UpdateRequest("PT-N", "Norte",
                BillingZone.ZoneType.DESTINATION_POSTAL_CODES, "PT", null, Set.of("4000-*")), "admin"))
                .isInstanceOf(BillingZoneException.class)
                .extracting(error -> ((BillingZoneException) error).reason())
                .isEqualTo(BillingZoneException.Reason.NOT_FOUND);
        assertThatThrownBy(() -> service.status(id, false, "admin"))
                .isInstanceOf(BillingZoneException.class);
    }

    private static CreateRequest request(String code, String designation) {
        return new CreateRequest(code, designation, BillingZone.ZoneType.DESTINATION_POSTAL_CODES,
                "PT", null, Set.of("4000-*"));
    }
}
