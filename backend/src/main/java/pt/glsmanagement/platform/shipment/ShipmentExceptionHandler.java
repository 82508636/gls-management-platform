package pt.glsmanagement.platform.shipment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import pt.glsmanagement.platform.web.ApiError;

@RestControllerAdvice(assignableTypes = ShipmentController.class)
class ShipmentExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ShipmentExceptionHandler.class);

    @ExceptionHandler(ShipmentException.class)
    ResponseEntity<ApiError> shipment(ShipmentException exception) {
        log.debug("Shipment operation rejected: {}", exception.reason());
        if (exception.reason() == ShipmentException.Reason.NOT_FOUND) {
            return response(HttpStatus.NOT_FOUND, "Não foi possível encontrar os dados necessários ao envio.");
        }
        return response(HttpStatus.BAD_REQUEST, "Não foi possível validar os dados do envio.");
    }

    private static ResponseEntity<ApiError> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiError.of(status.value(), message));
    }
}
