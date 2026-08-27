package pt.glsmanagement.platform.collaborator;

import org.springframework.http.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.*;
import pt.glsmanagement.platform.web.ApiError;

@RestControllerAdvice(assignableTypes = ReferenceCatalogController.class)
class ReferenceCatalogExceptionHandler {
    @ExceptionHandler(ReferenceCatalogException.class)
    ResponseEntity<ApiError> handle(ReferenceCatalogException exception) {
        var status = exception.reason() == ReferenceCatalogException.Reason.NOT_FOUND
                ? HttpStatus.NOT_FOUND : HttpStatus.CONFLICT;
        var message = status == HttpStatus.NOT_FOUND
                ? "Não foi possível encontrar o recurso solicitado." : "Não foi possível guardar os dados.";
        return ResponseEntity.status(status).body(ApiError.of(status.value(), message));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> handleConflict(DataIntegrityViolationException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(HttpStatus.CONFLICT.value(), "Não foi possível guardar os dados."));
    }
}
