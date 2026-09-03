package pt.glsmanagement.workforce.reference;

class ReferenceCatalogException extends RuntimeException {
    enum Reason { DUPLICATE, NOT_FOUND }
    private final Reason reason;

    ReferenceCatalogException(Reason reason) { this.reason = reason; }
    Reason reason() { return reason; }
}
