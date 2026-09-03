package pt.glsmanagement.platform.customer;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@RestControllerAdvice(assignableTypes = VatValidationController.class)
class VatValidationExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiError invalidRequest(MethodArgumentNotValidException ignored) {
        return new ApiError(OffsetDateTime.now(ZoneOffset.UTC), 400,
                "Não foi possível validar os dados enviados.");
    }
    record ApiError(OffsetDateTime timestamp, int status, String message) {}
}
