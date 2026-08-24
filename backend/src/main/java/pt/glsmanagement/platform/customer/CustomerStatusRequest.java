package pt.glsmanagement.platform.customer;

import jakarta.validation.constraints.NotNull;

public record CustomerStatusRequest(@NotNull Boolean active) {
}
