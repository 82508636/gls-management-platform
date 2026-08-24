package pt.glsmanagement.platform.customer;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity @Table(name = "customers")
class Customer {
    @Id private UUID id;
    @Column(name="customer_code", nullable=false, unique=true, length=6) private String customerCode;
    @Column(length=30) private String abbreviation;
    @Column(name="shipping_name", nullable=false, length=200) private String shippingName;
    @Column(nullable=false, length=10) private String agency;
    @Column(length=500) private String address;
    @Column(name="postal_code", length=20) private String postalCode;
    @Column(length=120) private String locality;
    @Column(length=2) private String country;
    @Column(name="contact_email", length=254) private String contactEmail;
    @Column(length=50) private String mobile;
    @Column(length=50) private String phone;
    @Column(name="billing_country", length=2) private String billingCountry;
    @Column(name="vat_number", nullable=false, unique=true, length=50) private String vatNumber;
    @Column(name="billing_legal_name", length=200) private String billingLegalName;
    @Column(name="billing_address", length=500) private String billingAddress;
    @Column(name="billing_postal_code", length=20) private String billingPostalCode;
    @Column(name="billing_locality", length=120) private String billingLocality;
    @Column(name="account_code", length=100) private String accountCode;
    @Column(name="billing_reference", length=100) private String billingReference;
    @Enumerated(EnumType.STRING) @Column(name="customer_type", nullable=false, length=20) private CustomerRequest.CustomerType customerType;
    @Column(name="responsible_name", length=200) private String responsibleName;
    @Column(name="billing_email", length=254) private String billingEmail;
    @Column(name="default_document", length=50) private String defaultDocument;
    @Column(name="exchange_rate", precision=18, scale=6) private BigDecimal exchangeRate;
    @Column(length=3) private String currency;
    @Column(name="invoice_by_post", nullable=false) private boolean invoiceByPost;
    @Column(name="documents_by_email", nullable=false) private boolean documentsByEmail;
    @Column(nullable=false) private boolean active;
    @Column(name="created_at", nullable=false) private OffsetDateTime createdAt;
    @Column(name="updated_at", nullable=false) private OffsetDateTime updatedAt;

    protected Customer() {}
    static Customer create(CustomerRequest request, String customerCode) { var c=new Customer(); c.id=UUID.randomUUID(); c.customerCode=customerCode; c.createdAt=OffsetDateTime.now(ZoneOffset.UTC); c.apply(request); return c; }
    void updateDetails(CustomerRequest request) { var previousActive=active; apply(request); active=previousActive; }
    void setActive(boolean active) { this.active=active; updatedAt=OffsetDateTime.now(ZoneOffset.UTC); }
    private void apply(CustomerRequest r) {
        abbreviation=n(r.abbreviation()); shippingName=r.shippingName().trim(); agency=r.agency();
        address=n(r.address()); postalCode=n(r.postalCode()); locality=n(r.locality()); country=upper(r.country()); contactEmail=n(r.contactEmail()); mobile=n(r.mobile()); phone=n(r.phone());
        billingCountry=upper(r.billingCountry()); vatNumber=r.vatNumber().trim(); billingLegalName=n(r.billingLegalName()); billingAddress=n(r.billingAddress()); billingPostalCode=n(r.billingPostalCode()); billingLocality=n(r.billingLocality());
        accountCode=n(r.accountCode()); billingReference=n(r.billingReference()); customerType=r.customerType(); responsibleName=n(r.responsibleName()); billingEmail=n(r.billingEmail()); defaultDocument=n(r.defaultDocument()); exchangeRate=r.exchangeRate(); currency=upper(r.currency());
        invoiceByPost=r.invoiceByPost(); documentsByEmail=r.documentsByEmail(); active=r.active(); updatedAt=OffsetDateTime.now(ZoneOffset.UTC);
    }
    private static String n(String v){return v==null||v.isBlank()?null:v.trim();} private static String upper(String v){var x=n(v);return x==null?null:x.toUpperCase();}
    UUID id(){return id;} String customerCode(){return customerCode;} String abbreviation(){return abbreviation;} String shippingName(){return shippingName;} String agency(){return agency;} String address(){return address;} String postalCode(){return postalCode;} String locality(){return locality;} String country(){return country;} String contactEmail(){return contactEmail;} String mobile(){return mobile;} String phone(){return phone;} String billingCountry(){return billingCountry;} String vatNumber(){return vatNumber;} String billingLegalName(){return billingLegalName;} String billingAddress(){return billingAddress;} String billingPostalCode(){return billingPostalCode;} String billingLocality(){return billingLocality;} String accountCode(){return accountCode;} String billingReference(){return billingReference;} CustomerRequest.CustomerType customerType(){return customerType;} String responsibleName(){return responsibleName;} String billingEmail(){return billingEmail;} String defaultDocument(){return defaultDocument;} BigDecimal exchangeRate(){return exchangeRate;} String currency(){return currency;} boolean invoiceByPost(){return invoiceByPost;} boolean documentsByEmail(){return documentsByEmail;} boolean active(){return active;} OffsetDateTime createdAt(){return createdAt;} OffsetDateTime updatedAt(){return updatedAt;}
}
