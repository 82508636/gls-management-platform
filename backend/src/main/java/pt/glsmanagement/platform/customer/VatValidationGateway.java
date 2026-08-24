package pt.glsmanagement.platform.customer;

public interface VatValidationGateway {
    Result validate(String countryCode, String vatNumber);

    record Result(boolean valid) {
    }
}
