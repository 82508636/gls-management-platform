package pt.glsmanagement.platform.billing;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import pt.glsmanagement.platform.web.ApiError;

@RestControllerAdvice(assignableTypes = BillingZoneController.class)
class BillingZoneExceptionHandler {
    @ExceptionHandler(BillingZoneException.class)
    ResponseEntity<ApiError> handle(BillingZoneException exception) {
        var status = exception.reason() == BillingZoneException.Reason.NOT_FOUND ? HttpStatus.NOT_FOUND : HttpStatus.CONFLICT;
        return ResponseEntity.status(status).body(ApiError.of(status.value(),
                status == HttpStatus.NOT_FOUND ? "Não foi possível encontrar a zona solicitada." : "Não foi possível guardar a zona."));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> conflict(DataIntegrityViolationException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(HttpStatus.CONFLICT.value(), "Não foi possível guardar a zona."));
    }
}
