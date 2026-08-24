package pt.glsmanagement.platform.customer;

public class VatValidationProviderUnavailableException extends RuntimeException {
    public VatValidationProviderUnavailableException(Throwable cause) {
        super("VAT validation provider is unavailable", cause);
    }

    public VatValidationProviderUnavailableException() {
        super("VAT validation provider returned an invalid response");
    }
}
