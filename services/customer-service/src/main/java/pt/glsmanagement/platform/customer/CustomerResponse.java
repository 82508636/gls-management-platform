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
    static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.id(), customer.customerCode(), customer.abbreviation(), customer.shippingName(),
                customer.agency(), customer.address(), customer.postalCode(), customer.locality(), customer.country(),
                customer.contactEmail(), customer.mobile(), customer.phone(), customer.billingCountry(),
                customer.vatNumber(), customer.billingLegalName(), customer.billingAddress(),
                customer.billingPostalCode(), customer.billingLocality(), customer.accountCode(),
                customer.billingReference(), customer.customerType(), customer.responsibleName(),
                customer.billingEmail(), customer.defaultDocument(), customer.exchangeRate(), customer.currency(),
                customer.invoiceByPost(), customer.documentsByEmail(), customer.active(),
                customer.createdAt(), customer.updatedAt());
    }
}
