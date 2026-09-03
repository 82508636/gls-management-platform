package pt.glsmanagement.platform.billing;

import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.*;

final class BillingZoneDtos {
    private BillingZoneDtos() {}

    record CreateRequest(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,40}") String code,
            @NotBlank @Size(max = 160) String designation,
            @NotNull BillingZone.ZoneType zoneType,
            @NotBlank @Pattern(regexp = "[A-Za-z]{2}") String country,
            @Size(max = 120) String groupName,
            @NotEmpty Set<@NotBlank @Size(max = 30) String> postalCodePatterns) {}

    record UpdateRequest(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,40}") String code,
            @NotBlank @Size(max = 160) String designation,
            @NotNull BillingZone.ZoneType zoneType,
            @NotBlank @Pattern(regexp = "[A-Za-z]{2}") String country,
            @Size(max = 120) String groupName,
            @NotEmpty Set<@NotBlank @Size(max = 30) String> postalCodePatterns) {}

    record StatusRequest(@NotNull Boolean active) {}

    record Response(UUID id, String code, String designation, BillingZone.ZoneType zoneType, String country,
                    String groupName, boolean active, Set<String> postalCodePatterns, OffsetDateTime createdAt,
                    String createdBy, OffsetDateTime updatedAt, String updatedBy, long version) {
        static Response from(BillingZone value) {
            return new Response(value.id(), value.code(), value.designation(), value.zoneType(), value.country(),
                    value.groupName(), value.active(), value.postalCodePatterns(), value.createdAt(), value.createdBy(),
                    value.updatedAt(), value.updatedBy(), value.version());
        }
    }
}

