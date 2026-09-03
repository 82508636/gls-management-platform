package pt.glsmanagement.pickup.domain;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record PickupPointRequest(
        @NotBlank @Size(max = 30) String code,
        @NotBlank @Size(max = 200) String designation,
        LocalTime morningOpen,
        LocalTime morningClose,
        LocalTime afternoonOpen,
        LocalTime afternoonClose,
        @NotBlank @Size(max = 500) String address,
        @NotBlank @Size(max = 20) String postalCode,
        @NotBlank @Size(max = 120) String locality,
        @NotBlank @Pattern(regexp = "[A-Za-z]{2}") String country,
        @Email @Size(max = 254) String email,
        @Size(max = 50) String phone,
        @Size(max = 50) String mobile,
        boolean openSaturday,
        boolean openSunday,
        @NotNull Boolean active
) {
    public PickupPointRequest {
        validatePeriod(morningOpen, morningClose, "morning");
        validatePeriod(afternoonOpen, afternoonClose, "afternoon");
        if (morningClose != null && afternoonOpen != null && morningClose.isAfter(afternoonOpen)) {
            throw new IllegalArgumentException("Pickup schedules cannot overlap");
        }
    }

    private static void validatePeriod(LocalTime start, LocalTime end, String period) {
        if ((start == null) != (end == null) || (start != null && !start.isBefore(end))) {
            throw new IllegalArgumentException("Invalid " + period + " schedule");
        }
    }
}
