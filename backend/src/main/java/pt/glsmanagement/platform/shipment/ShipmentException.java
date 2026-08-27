package pt.glsmanagement.platform.shipment;

class ShipmentException extends RuntimeException {
    enum Reason { NOT_FOUND, INVALID_DATA }
    private final Reason reason;
    private ShipmentException(Reason reason) { super(reason.name()); this.reason = reason; }
    static ShipmentException notFound() { return new ShipmentException(Reason.NOT_FOUND); }
    static ShipmentException invalidData() { return new ShipmentException(Reason.INVALID_DATA); }
    Reason reason() { return reason; }
}
