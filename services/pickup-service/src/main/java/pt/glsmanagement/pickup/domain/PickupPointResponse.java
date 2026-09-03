package pt.glsmanagement.pickup.domain;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PickupPointResponse(
        UUID id,
        String code,
        String designation,
        LocalTime morningOpen,
        LocalTime morningClose,
        LocalTime afternoonOpen,
        LocalTime afternoonClose,
        String address,
        String postalCode,
        String locality,
        String country,
        String email,
        String phone,
        String mobile,
        boolean openSaturday,
        boolean openSunday,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    static PickupPointResponse from(PickupPoint point) {
        return new PickupPointResponse(
                point.id(), point.code(), point.designation(),
                point.morningOpen(), point.morningClose(), point.afternoonOpen(), point.afternoonClose(),
                point.address(), point.postalCode(), point.locality(), point.country(),
                point.email(), point.phone(), point.mobile(), point.openSaturday(), point.openSunday(),
                point.active(), point.createdAt(), point.updatedAt());
    }
}
