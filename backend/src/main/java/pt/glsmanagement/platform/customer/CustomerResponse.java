package pt.glsmanagement.platform.customer;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CustomerResponse(
        UUID id, String customerCode, String abbreviation, String shippingName, String agency,
        String address, String postalCode, String locality, String country, String contactEmail,
        String mobile, String phone, String billingCountry, String vatNumber, String billingLegalName,
        String billingAddress, String billingPostalCode, String billingLocality, String accountCode,
        String billingReference, CustomerRequest.CustomerType customerType, String responsibleName,
        String billingEmail, String defaultDocument, BigDecimal exchangeRate, String currency,
        boolean invoiceByPost, boolean documentsByEmail, boolean active,
        OffsetDateTime createdAt, OffsetDateTime updatedAt
) {
    static CustomerResponse from(Customer c) {
        return new CustomerResponse(c.id(), c.customerCode(), c.abbreviation(), c.shippingName(), c.agency(),
                c.address(), c.postalCode(), c.locality(), c.country(), c.contactEmail(), c.mobile(), c.phone(),
                c.billingCountry(), c.vatNumber(), c.billingLegalName(), c.billingAddress(), c.billingPostalCode(),
                c.billingLocality(), c.accountCode(), c.billingReference(), c.customerType(), c.responsibleName(),
                c.billingEmail(), c.defaultDocument(), c.exchangeRate(), c.currency(), c.invoiceByPost(),
                c.documentsByEmail(), c.active(), c.createdAt(), c.updatedAt());
    }
}
