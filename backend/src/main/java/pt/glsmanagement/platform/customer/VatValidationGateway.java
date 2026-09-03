package pt.glsmanagement.platform.customer;

public interface VatValidationGateway {
    Result validate(String countryCode, String vatNumber);

    record Result(
            boolean valid,
            String registeredName,
            String registeredAddress,
            String registeredStreet,
            String registeredPostalCode,
            String registeredLocality
    ) {
        public Result(boolean valid) {
            this(valid, null, null, null, null, null);
        }
    }
}
