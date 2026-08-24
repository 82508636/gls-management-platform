package pt.glsmanagement.platform.customer;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vat-validations")
class VatValidationController {
    private final VatValidationService service;

    VatValidationController(VatValidationService service) {
        this.service = service;
    }

    @PostMapping
    VatValidationResponse validate(@Valid @RequestBody VatValidationRequest request) {
        return service.validate(request);
    }
}
