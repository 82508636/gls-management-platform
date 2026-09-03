package pt.glsmanagement.platform.servicecatalog;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.*;

import static pt.glsmanagement.platform.servicecatalog.ServiceGroupDtos.*;

@RestController
@RequestMapping("/api/service-groups")
class ServiceGroupController {
    private final ServiceGroupService service;
    ServiceGroupController(ServiceGroupService service) { this.service = service; }

    @GetMapping List<Response> list(@RequestParam(required = false) Boolean active) { return service.list(active); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    Response create(@Valid @RequestBody CreateRequest request, @AuthenticationPrincipal Jwt jwt) {
        return service.create(request, actor(jwt));
    }

    @PutMapping("/{id}")
    Response update(@PathVariable String id, @Valid @RequestBody UpdateRequest request,
                    @AuthenticationPrincipal Jwt jwt) {
        return service.update(id, request, actor(jwt));
    }

    @PatchMapping("/{id}/status")
    Response status(@PathVariable String id, @Valid @RequestBody StatusRequest request,
                    @AuthenticationPrincipal Jwt jwt) {
        return service.status(id, request.active(), actor(jwt));
    }

    private static String actor(Jwt jwt) {
        var value = jwt == null ? null : jwt.getClaimAsString("preferred_username");
        return value == null || value.isBlank() ? "Sistema" : value;
    }
}
