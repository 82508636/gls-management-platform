package pt.glsmanagement.platform.servicecatalog;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.*;

import static pt.glsmanagement.platform.servicecatalog.OperationalServiceDtos.*;

@RestController
@RequestMapping("/api/operational-services")
class OperationalServiceController {
    private final OperationalServiceService service;
    OperationalServiceController(OperationalServiceService service) { this.service = service; }

    @GetMapping List<Summary> list(@RequestParam(required = false) Boolean active) { return service.list(active); }
    @GetMapping("/{id}") Response detail(@PathVariable UUID id) { return service.detail(id); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    Response create(@Valid @RequestBody Request request, @AuthenticationPrincipal Jwt jwt) {
        return service.create(request, actor(jwt));
    }

    @PutMapping("/{id}")
    Response update(@PathVariable UUID id, @Valid @RequestBody Request request, @AuthenticationPrincipal Jwt jwt) {
        return service.update(id, request, actor(jwt));
    }

    @PatchMapping("/{id}/status")
    Response status(@PathVariable UUID id, @Valid @RequestBody StatusRequest request,
                    @AuthenticationPrincipal Jwt jwt) {
        return service.status(id, request.active(), actor(jwt));
    }

    private static String actor(Jwt jwt) {
        var value = jwt == null ? null : jwt.getClaimAsString("preferred_username");
        return value == null || value.isBlank() ? "Sistema" : value;
    }
}

