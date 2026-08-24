package pt.glsmanagement.platform.customer;

class DuplicateCustomerVatNumberException extends RuntimeException {
    DuplicateCustomerVatNumberException(String vatNumber) {
        super("Já existe um cliente com o NIF " + vatNumber);
    }
}

