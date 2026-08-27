package pt.glsmanagement.platform.shipment;

import jakarta.persistence.*;
import pt.glsmanagement.platform.entity.RecipientResponse;
import pt.glsmanagement.platform.pricing.PricingQuote;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "shipments")
class Shipment {
    @Id private UUID id;
    @Column(name = "shipment_number", nullable = false, unique = true, length = 40) private String shipmentNumber;
    @Column(name = "external_reference", nullable = false, unique = true, length = 80) private String externalReference;
    @Column(name = "customer_id", nullable = false) private UUID customerId;
    @Column(name = "customer_code", nullable = false, length = 6) private String customerCode;
    @Column(name = "customer_name", nullable = false, length = 200) private String customerName;
    @Column(name = "recipient_id", nullable = false) private UUID recipientId;
    @Column(name = "recipient_code", length = 30) private String recipientCode;
    @Column(name = "recipient_designation", nullable = false, length = 200) private String recipientDesignation;
    @Column(name = "recipient_address", nullable = false, length = 500) private String recipientAddress;
    @Column(name = "recipient_postal_code", nullable = false, length = 20) private String recipientPostalCode;
    @Column(name = "recipient_locality", nullable = false, length = 120) private String recipientLocality;
    @Column(name = "recipient_country", nullable = false, length = 2) private String recipientCountry;
    @Column(name = "pricing_plan_id", nullable = false) private UUID pricingPlanId;
    @Column(name = "pricing_plan_code", nullable = false, length = 40) private String pricingPlanCode;
    @Column(name = "pricing_plan_version", nullable = false) private int pricingPlanVersion;
    @Column(name = "pricing_route_id", nullable = false) private UUID pricingRouteId;
    @Column(name = "route_code", nullable = false, length = 60) private String routeCode;
    @Column(name = "route_designation", nullable = false, length = 160) private String routeDesignation;
    @Column(name = "service_code", nullable = false, length = 30) private String serviceCode;
    @Column(name = "shipment_date", nullable = false) private LocalDate shipmentDate;
    @Column(name = "due_date", nullable = false) private LocalDate dueDate;
    @Column(name = "parcel_count", nullable = false) private int parcelCount;
    @Column(name = "actual_weight_kg", nullable = false, precision = 10, scale = 3) private BigDecimal actualWeightKg;
    @Column(name = "volumetric_weight_kg", nullable = false, precision = 10, scale = 3) private BigDecimal volumetricWeightKg;
    @Column(name = "chargeable_weight_kg", nullable = false, precision = 10, scale = 3) private BigDecimal chargeableWeightKg;
    @Column(name = "length_cm", precision = 10, scale = 2) private BigDecimal lengthCm;
    @Column(name = "width_cm", precision = 10, scale = 2) private BigDecimal widthCm;
    @Column(name = "height_cm", precision = 10, scale = 2) private BigDecimal heightCm;
    @Column(name = "base_price", nullable = false, precision = 12, scale = 2) private BigDecimal basePrice;
    @Column(name = "fuel_surcharge", nullable = false, precision = 12, scale = 2) private BigDecimal fuelSurcharge;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal subtotal;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal vat;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal total;
    @Column(nullable = false, length = 3) private String currency;
    @Enumerated(EnumType.STRING) @Column(name = "shipment_status", nullable = false, length = 20) private ShipmentStatus shipmentStatus;
    @Enumerated(EnumType.STRING) @Column(name = "payment_status", nullable = false, length = 20) private PaymentStatus paymentStatus;
    @Column(name = "paid_at") private OffsetDateTime paidAt;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "created_by", nullable = false, length = 120) private String createdBy;

    protected Shipment() {}

    static Shipment create(String shipmentNumber, CustomerSnapshot customer, RecipientResponse recipient,
                           ShipmentRequest request, PricingQuote quote, String actor) {
        var shipment = new Shipment();
        shipment.id = UUID.randomUUID();
        shipment.shipmentNumber = shipmentNumber;
        shipment.externalReference = "LOCAL-" + shipmentNumber;
        shipment.customerId = request.customerId();
        shipment.customerCode = customer.code();
        shipment.customerName = customer.name();
        shipment.recipientId = recipient.id();
        shipment.recipientCode = recipient.code();
        shipment.recipientDesignation = recipient.designation();
        shipment.recipientAddress = recipient.address();
        shipment.recipientPostalCode = recipient.postalCode();
        shipment.recipientLocality = recipient.locality();
        shipment.recipientCountry = recipient.country();
        shipment.pricingPlanId = quote.planId();
        shipment.pricingPlanCode = quote.planCode();
        shipment.pricingPlanVersion = quote.planVersion();
        shipment.pricingRouteId = quote.routeId();
        shipment.routeCode = quote.routeCode();
        shipment.routeDesignation = quote.routeDesignation();
        shipment.serviceCode = quote.serviceCode();
        shipment.shipmentDate = request.shipmentDate();
        shipment.dueDate = request.dueDate() == null ? request.shipmentDate().plusDays(30) : request.dueDate();
        shipment.parcelCount = request.parcelCount();
        shipment.actualWeightKg = quote.actualWeightKg();
        shipment.volumetricWeightKg = quote.volumetricWeightKg();
        shipment.chargeableWeightKg = quote.chargeableWeightKg();
        shipment.lengthCm = request.lengthCm();
        shipment.widthCm = request.widthCm();
        shipment.heightCm = request.heightCm();
        shipment.basePrice = quote.basePrice();
        shipment.fuelSurcharge = quote.fuelSurcharge();
        shipment.subtotal = quote.subtotal();
        shipment.vat = quote.vat();
        shipment.total = quote.total();
        shipment.currency = quote.currency();
        shipment.shipmentStatus = ShipmentStatus.CREATED;
        shipment.paymentStatus = request.paymentStatus();
        shipment.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        shipment.createdBy = actor;
        shipment.paidAt = request.paymentStatus() == PaymentStatus.PAID ? shipment.createdAt : null;
        return shipment;
    }

    record CustomerSnapshot(String code, String name) {}

    UUID id() { return id; }
    String shipmentNumber() { return shipmentNumber; }
    String externalReference() { return externalReference; }
    UUID customerId() { return customerId; }
    String customerCode() { return customerCode; }
    String customerName() { return customerName; }
    UUID recipientId() { return recipientId; }
    String recipientCode() { return recipientCode; }
    String recipientDesignation() { return recipientDesignation; }
    String recipientAddress() { return recipientAddress; }
    String recipientPostalCode() { return recipientPostalCode; }
    String recipientLocality() { return recipientLocality; }
    String recipientCountry() { return recipientCountry; }
    UUID pricingPlanId() { return pricingPlanId; }
    String pricingPlanCode() { return pricingPlanCode; }
    int pricingPlanVersion() { return pricingPlanVersion; }
    UUID pricingRouteId() { return pricingRouteId; }
    String routeCode() { return routeCode; }
    String routeDesignation() { return routeDesignation; }
    String serviceCode() { return serviceCode; }
    LocalDate shipmentDate() { return shipmentDate; }
    LocalDate dueDate() { return dueDate; }
    int parcelCount() { return parcelCount; }
    BigDecimal actualWeightKg() { return actualWeightKg; }
    BigDecimal volumetricWeightKg() { return volumetricWeightKg; }
    BigDecimal chargeableWeightKg() { return chargeableWeightKg; }
    BigDecimal basePrice() { return basePrice; }
    BigDecimal fuelSurcharge() { return fuelSurcharge; }
    BigDecimal subtotal() { return subtotal; }
    BigDecimal vat() { return vat; }
    BigDecimal total() { return total; }
    String currency() { return currency; }
    ShipmentStatus shipmentStatus() { return shipmentStatus; }
    PaymentStatus paymentStatus() { return paymentStatus; }
    OffsetDateTime paidAt() { return paidAt; }
    OffsetDateTime createdAt() { return createdAt; }
    String createdBy() { return createdBy; }
}
