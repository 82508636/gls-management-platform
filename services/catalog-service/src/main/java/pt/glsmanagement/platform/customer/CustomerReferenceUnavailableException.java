package pt.glsmanagement.platform.customer;

public final class CustomerReferenceUnavailableException extends RuntimeException {
    public CustomerReferenceUnavailableException(Throwable cause) {
        super("Customer reference validation is unavailable", cause);
    }

    public CustomerReferenceUnavailableException() {
        super("Customer reference validation requires an authenticated JWT");
    }
}
