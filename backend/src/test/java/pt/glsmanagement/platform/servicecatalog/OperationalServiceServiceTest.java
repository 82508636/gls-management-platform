package pt.glsmanagement.platform.servicecatalog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.glsmanagement.platform.billing.BillingZoneLookup;
import pt.glsmanagement.platform.customer.CustomerReferenceLookup;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static pt.glsmanagement.platform.servicecatalog.OperationalServiceDtos.*;

class OperationalServiceServiceTest {
    private OperationalServiceRepository repository;
    private ServiceGroupService groups;
    private BillingZoneLookup zones;
    private CustomerReferenceLookup customers;
    private OperationalServiceService service;

    @BeforeEach void setUp() {
        repository = mock(OperationalServiceRepository.class);
        groups = mock(ServiceGroupService.class);
        zones = mock(BillingZoneLookup.class);
        customers = mock(CustomerReferenceLookup.class);
        service = new OperationalServiceService(repository, groups, zones, customers);
        when(zones.allActive(any())).thenReturn(true);
        when(customers.allExist(any())).thenReturn(true);
    }

    @Test void rejectsAnEndTimeBeforeThePickupStart() {
        var request = validRequest();
        request = replaceSchedule(request, new ScheduleRequest(LocalTime.of(17, 0), LocalTime.of(9, 0), 60,
                Set.of(DayOfWeek.MONDAY), Set.of(DayOfWeek.TUESDAY)));
        assertInvalid(request);
    }

    @Test void rejectsDuplicateBillingZonesAndPackageTypes() {
        var zoneId = UUID.randomUUID();
        var request = validRequest();
        request = new Request(request.identity(), new TransitRequest(1, 2, null, 3, List.of(
                new ZoneRuleRequest(zoneId, 1, 2), new ZoneRuleRequest(zoneId, 2, 3))), request.pricing(),
                request.limits(), request.schedule(), request.pickupRules(), request.characteristics(),
                request.additionalServices(), request.definitions(), request.restrictions());
        assertInvalid(request);

        var duplicate = new PackageLimitRequest(OperationalService.PackageType.BOX, BigDecimal.ONE,
                null, null, null, null);
        request = validRequest();
        request = new Request(request.identity(), request.transit(), request.pricing(),
                new LimitsRequest(List.of(duplicate, duplicate), 1, 2, BigDecimal.ONE, BigDecimal.TEN),
                request.schedule(), request.pickupRules(), request.characteristics(), request.additionalServices(),
                request.definitions(), request.restrictions());
        assertInvalid(request);
    }

    @Test void rejectsUnknownCustomersAndInactiveZones() {
        when(customers.allExist(any())).thenReturn(false);
        assertInvalid(validRequestWithCustomer());
        when(customers.allExist(any())).thenReturn(true);
        when(zones.allActive(any())).thenReturn(false);
        assertInvalid(validRequestWithZone());
    }

    @Test void rejectsContradictoryNoPickupConfiguration() {
        var request = validRequest();
        var noPickup = new DefinitionsRequest(true, false, false, false, true, true, false, null);
        request = new Request(request.identity(), request.transit(), request.pricing(), request.limits(),
                request.schedule(), request.pickupRules(), request.characteristics(), request.additionalServices(),
                noPickup, request.restrictions());
        assertInvalid(request);

        request = validRequest();
        var emptySchedule = new ScheduleRequest(null, null, null, Set.of(), Set.of(DayOfWeek.MONDAY));
        var pickupOnly = new CharacteristicsRequest(true, false, false, false, false, false, false, false);
        request = new Request(request.identity(), request.transit(), request.pricing(), request.limits(), emptySchedule,
                request.pickupRules(), pickupOnly, request.additionalServices(), noPickup, request.restrictions());
        assertInvalid(request);
    }

    @Test void rejectsACombinedDimensionBelowAnIndividualMaximum() {
        var request = validRequest();
        var impossible = new PackageLimitRequest(OperationalService.PackageType.BOX, new BigDecimal("40"),
                new BigDecimal("120"), new BigDecimal("80"), new BigDecimal("80"), new BigDecimal("100"));
        request = new Request(request.identity(), request.transit(), request.pricing(),
                new LimitsRequest(List.of(impossible), 1, 20, BigDecimal.ONE, new BigDecimal("400")),
                request.schedule(), request.pickupRules(), request.characteristics(), request.additionalServices(),
                request.definitions(), request.restrictions());
        assertInvalid(request);
    }

    @Test void createsAValidServiceWithNormalizedIdentityAndAuditActor() {
        var group = ServiceGroup.create("PARCEL", "Parcel", "admin");
        when(groups.requireActive("PARCEL")).thenReturn(group);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(validRequest(), "admin");

        assertThat(response.identity().code()).isEqualTo("BP_24H");
        assertThat(response.identity().designation()).isEqualTo("Business Parcel");
        assertThat(response.createdBy()).isEqualTo("admin");
        verify(repository).save(any(OperationalService.class));
    }

    @Test void editingTheSameZoneAndPackageUpdatesInsteadOfDuplicatingChildren() {
        var group = ServiceGroup.create("PARCEL", "Parcel", "admin");
        var zoneId = UUID.randomUUID();
        var original = validRequestWithZone(zoneId, 2, 12);
        var entity = OperationalService.create(original, group, null, null, "admin");
        when(repository.findById(entity.id())).thenReturn(Optional.of(entity));
        when(groups.requireActive("PARCEL")).thenReturn(group);

        var changed = validRequestWithZone(zoneId, 4, 18);
        var response = service.update(entity.id(), changed, "editor");

        assertThat(response.transit().zones()).hasSize(1);
        assertThat(response.transit().zones().getFirst().transitMinHours()).isEqualTo(4);
        assertThat(response.limits().packageLimits()).hasSize(1);
        assertThat(response.updatedBy()).isEqualTo("editor");
    }

    private void assertInvalid(Request request) {
        assertThatThrownBy(() -> service.create(request, "admin"))
                .isInstanceOf(OperationalCatalogException.class)
                .extracting(error -> ((OperationalCatalogException) error).reason())
                .isEqualTo(OperationalCatalogException.Reason.INVALID_CONFIGURATION);
    }

    private static Request validRequestWithCustomer() {
        var request = validRequest();
        return new Request(request.identity(), request.transit(), request.pricing(), request.limits(), request.schedule(),
                request.pickupRules(), request.characteristics(), request.additionalServices(), request.definitions(),
                new RestrictionsRequest(Set.of(UUID.randomUUID()), Set.of()));
    }

    private static Request validRequestWithZone() {
        return validRequestWithZone(UUID.randomUUID(), 2, 12);
    }

    private static Request validRequestWithZone(UUID zoneId, int minimum, int maximum) {
        var request = validRequest();
        return new Request(request.identity(), new TransitRequest(1, 24, LocalTime.of(18, 0), 3,
                List.of(new ZoneRuleRequest(zoneId, minimum, maximum))), request.pricing(), request.limits(),
                request.schedule(), request.pickupRules(), request.characteristics(), request.additionalServices(),
                request.definitions(), request.restrictions());
    }

    private static Request replaceSchedule(Request request, ScheduleRequest schedule) {
        return new Request(request.identity(), request.transit(), request.pricing(), request.limits(), schedule,
                request.pickupRules(), request.characteristics(), request.additionalServices(), request.definitions(),
                request.restrictions());
    }

    private static Request validRequest() {
        return new Request(
                new IdentityRequest(" bp_24h ", " Business   Parcel ", "parcel",
                        OperationalService.TransportType.SMALL_VOLUMES, true),
                new TransitRequest(1, 24, LocalTime.of(18, 0), 3, List.of()),
                new PricingRequest(OperationalService.PriceCalculationRule.WEIGHT,
                        OperationalService.SalesCalculationRule.DEFAULT, null, OperationalService.VatMode.AUTO,
                        false, false, false, true),
                new LimitsRequest(List.of(new PackageLimitRequest(OperationalService.PackageType.BOX,
                        new BigDecimal("40"), new BigDecimal("120"), new BigDecimal("80"),
                        new BigDecimal("80"), new BigDecimal("280"))), 1, 20, BigDecimal.ONE,
                        new BigDecimal("400")),
                new ScheduleRequest(LocalTime.of(9, 0), LocalTime.of(18, 0), 60,
                        Set.of(DayOfWeek.MONDAY), Set.of(DayOfWeek.TUESDAY)),
                new PickupRulesRequest(null, null),
                new CharacteristicsRequest(false, false, false, false, false, false, false, false),
                new AdditionalServicesRequest(true, true, true, false),
                new DefinitionsRequest(true, false, false, false, false, true, false, null),
                new RestrictionsRequest(Set.of(), Set.of("LTFT02")));
    }
}
