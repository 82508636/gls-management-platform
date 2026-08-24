package pt.glsmanagement.platform.entity;

import jakarta.validation.constraints.NotNull;

public record PickupPointStatusRequest(@NotNull Boolean active) {
}
