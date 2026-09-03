package pt.glsmanagement.platform.customer;

final class DuplicateCustomerVatNumberException extends RuntimeException {
    DuplicateCustomerVatNumberException(String vatNumber) { super("Já existe um cliente com o NIF " + vatNumber); }
}
