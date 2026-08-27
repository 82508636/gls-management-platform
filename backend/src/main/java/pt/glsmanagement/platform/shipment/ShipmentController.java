package pt.glsmanagement.platform.shipment;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
class ShipmentController {
    private final ShipmentService service;

    ShipmentController(ShipmentService service) {
        this.service = service;
    }

    @GetMapping("/api/shipments")
    ShipmentPageResponse list(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "50") int size) {
        return service.list(page, size);
    }

    @PostMapping("/api/shipments")
    @ResponseStatus(HttpStatus.CREATED)
    ShipmentResponse create(@Valid @RequestBody ShipmentRequest request,
                            @AuthenticationPrincipal Jwt jwt) {
        return service.create(request, actor(jwt));
    }

    @GetMapping("/api/customers/{customerId}/services")
    List<CustomerAccountServiceResponse> customerServices(@PathVariable UUID customerId) {
        return service.customerServices(customerId);
    }

    private static String actor(Jwt jwt) {
        var username = jwt == null ? null : jwt.getClaimAsString("preferred_username");
        return username == null || username.isBlank() ? "Sistema" : username;
    }
}
