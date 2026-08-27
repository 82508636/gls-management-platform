package pt.glsmanagement.platform.customer;

import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

@RestControllerAdvice(assignableTypes = CustomerController.class)
class CustomerExceptionHandler {
    @ExceptionHandler(CustomerNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ApiError notFound(CustomerNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "Não foi possível encontrar o recurso solicitado.", Map.of());
    }

    @ExceptionHandler({DuplicateCustomerVatNumberException.class, DataIntegrityViolationException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    ApiError duplicate(RuntimeException exception) {
        return error(HttpStatus.CONFLICT, "Não foi possível guardar os dados.", Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiError validation(MethodArgumentNotValidException exception) {
        return error(HttpStatus.BAD_REQUEST, "Não foi possível validar os dados enviados.", Map.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiError invalidOperation(IllegalArgumentException exception) {
        return error(HttpStatus.BAD_REQUEST, "Não foi possível concluir a operação solicitada.", Map.of());
    }

    private ApiError error(HttpStatus status, String message, Map<String, String> fields) {
        return new ApiError(OffsetDateTime.now(ZoneOffset.UTC), status.value(), message, fields);
    }

    record ApiError(OffsetDateTime timestamp, int status, String message, Map<String, String> fields) {
    }
}
