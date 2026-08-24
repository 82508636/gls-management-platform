package pt.glsmanagement.platform.customer;

import java.time.OffsetDateTime;

public record VatValidationResponse(
        String countryCode,
        String vatNumber,
        boolean formatValid,
        VatValidationStatus viesStatus,
        OffsetDateTime checkedAt
) {
}
