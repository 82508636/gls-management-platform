package pt.glsmanagement.platform.collaborator;

class ReferenceCatalogException extends RuntimeException {
    enum Reason { DUPLICATE, NOT_FOUND }
    private final Reason reason;

    ReferenceCatalogException(Reason reason) { this.reason = reason; }
    Reason reason() { return reason; }
}
