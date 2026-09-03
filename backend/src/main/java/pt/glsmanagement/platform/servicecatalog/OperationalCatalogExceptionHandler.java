package pt.glsmanagement.platform.servicecatalog;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import pt.glsmanagement.platform.web.ApiError;

@RestControllerAdvice(assignableTypes = {OperationalServiceController.class, ServiceGroupController.class})
class OperationalCatalogExceptionHandler {
    @ExceptionHandler(OperationalCatalogException.class)
    ResponseEntity<ApiError> handle(OperationalCatalogException exception) {
        var status = switch (exception.reason()) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case DUPLICATE -> HttpStatus.CONFLICT;
            case INVALID_CONFIGURATION -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        var message = switch (status) {
            case NOT_FOUND -> "Não foi possível encontrar a configuração solicitada.";
            case CONFLICT -> "Já existe uma configuração com os mesmos dados.";
            default -> "A configuração indicada não é válida.";
        };
        return ResponseEntity.status(status).body(ApiError.of(status.value(), message));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> conflict(DataIntegrityViolationException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(HttpStatus.CONFLICT.value(), "Não foi possível guardar a configuração."));
    }
}
