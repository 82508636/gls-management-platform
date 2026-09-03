package pt.glsmanagement.platform.pricing;

class PricingException extends RuntimeException {
    enum Reason { NOT_FOUND, DUPLICATE, INVALID_STATE, INVALID_CONFIGURATION, OUT_OF_RANGE }
    private final Reason reason;
    private PricingException(Reason reason) { this.reason = reason; }
    Reason reason() { return reason; }
    static PricingException notFound() { return new PricingException(Reason.NOT_FOUND); }
    static PricingException duplicate() { return new PricingException(Reason.DUPLICATE); }
    static PricingException invalidState() { return new PricingException(Reason.INVALID_STATE); }
    static PricingException invalidConfiguration() { return new PricingException(Reason.INVALID_CONFIGURATION); }
    static PricingException outOfRange() { return new PricingException(Reason.OUT_OF_RANGE); }
}

