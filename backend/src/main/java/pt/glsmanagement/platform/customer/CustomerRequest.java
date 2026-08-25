package pt.glsmanagement.platform.customer;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CustomerRequest(
        @Size(max = 6) String customerCode,
        @Size(max = 30) String abbreviation,
        @NotBlank @Size(max = 200) String shippingName,
        @NotBlank @Pattern(regexp = "LTFT01|LTFT02") String agency,
        @NotBlank @Size(max = 500) String address,
        @NotBlank @Size(max = 20) String postalCode,
        @NotBlank @Size(max = 120) String locality,
        @NotBlank @Pattern(regexp = "[A-Za-z]{2}") String country,
        @Email @Size(max = 254) String contactEmail,
        @Size(max = 50) String mobile,
        @Size(max = 50) String phone,
        @Size(max = 2) String billingCountry,
        @NotBlank @Size(max = 50) String vatNumber,
        @Size(max = 200) String billingLegalName,
        @Size(max = 500) String billingAddress,
        @Size(max = 20) String billingPostalCode,
        @Size(max = 120) String billingLocality,
        @Size(max = 100) String accountCode,
        @Size(max = 100) String billingReference,
        @NotNull CustomerType customerType,
        @Size(max = 200) String responsibleName,
        @Email @Size(max = 254) String billingEmail,
        @Size(max = 50) String defaultDocument,
        @PositiveOrZero BigDecimal exchangeRate,
        @Size(max = 3) String currency,
        boolean invoiceByPost,
        boolean documentsByEmail,
        boolean active
) {
    public enum CustomerType { COMPANY, PRIVATE }
}
