package pt.glsmanagement.pickup.domain;

import jakarta.validation.constraints.NotNull;

public record PickupPointStatusRequest(@NotNull Boolean active) {}
