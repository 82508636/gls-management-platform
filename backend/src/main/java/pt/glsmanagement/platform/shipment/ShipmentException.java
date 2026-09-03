package pt.glsmanagement.platform.shipment;

class ShipmentException extends RuntimeException {
    enum Reason { NOT_FOUND, INVALID_DATA, PRICING_UNAVAILABLE }
    private final Reason reason;
    private ShipmentException(Reason reason) { super(reason.name()); this.reason = reason; }
    static ShipmentException notFound() { return new ShipmentException(Reason.NOT_FOUND); }
    static ShipmentException invalidData() { return new ShipmentException(Reason.INVALID_DATA); }
    static ShipmentException pricingUnavailable() { return new ShipmentException(Reason.PRICING_UNAVAILABLE); }
    Reason reason() { return reason; }
}
