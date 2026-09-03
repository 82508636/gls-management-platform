package pt.glsmanagement.platform.servicecatalog;

final class OperationalCatalogException extends RuntimeException {
    enum Reason { NOT_FOUND, DUPLICATE, INVALID_CONFIGURATION }
    private final Reason reason;
    OperationalCatalogException(Reason reason) { super(reason.name()); this.reason = reason; }
    Reason reason() { return reason; }
}
