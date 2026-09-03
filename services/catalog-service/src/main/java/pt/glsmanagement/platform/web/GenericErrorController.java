package pt.glsmanagement.platform.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
class GenericErrorController implements ErrorController {
    @RequestMapping("/error")
    ResponseEntity<ApiError> error(HttpServletRequest request) {
        Object value = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int rawStatus = value instanceof Integer status ? status : 500;
        HttpStatusCode status = HttpStatusCode.valueOf(rawStatus);
        String message = rawStatus >= 500
                ? "Não foi possível concluir o pedido."
                : "Não foi possível concluir a operação solicitada.";
        return ResponseEntity.status(status).body(ApiError.of(rawStatus, message));
    }
}

