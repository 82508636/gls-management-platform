package pt.glsmanagement.platform.customer;

import java.util.UUID;

final class CustomerNotFoundException extends RuntimeException {
    CustomerNotFoundException(UUID id) { super("Cliente não encontrado: " + id); }
}
