package pt.glsmanagement.platform.customer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/customers")
class CustomerReferenceController {
    private final CustomerService service;

    CustomerReferenceController(CustomerService service) { this.service = service; }

    @PostMapping("/existence")
    ExistenceResponse existence(@Valid @RequestBody ExistenceRequest request) {
        return new ExistenceResponse(service.allExist(request.ids()));
    }

    record ExistenceRequest(@NotNull @Size(max = 500) Set<UUID> ids) {}
    record ExistenceResponse(boolean allExist) {}
}
