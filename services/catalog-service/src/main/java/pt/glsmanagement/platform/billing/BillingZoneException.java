package pt.glsmanagement.platform.billing;

final class BillingZoneException extends RuntimeException {
    enum Reason { NOT_FOUND, DUPLICATE }
    private final Reason reason;
    BillingZoneException(Reason reason) { super(reason.name()); this.reason = reason; }
    Reason reason() { return reason; }
}

