package pt.glsmanagement.platform.pricing;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.*;

import static pt.glsmanagement.platform.pricing.PricingDtos.*;

@RestController
@RequestMapping("/api/pricing")
class PricingController {
    private final PricingService service;
    PricingController(PricingService service) { this.service = service; }

    @GetMapping("/plans") List<PlanSummary> list() { return service.list(); }
    @GetMapping("/plans/{id}") PlanDetail detail(@PathVariable("id") UUID id) { return service.detail(id); }
    @PostMapping("/plans") @ResponseStatus(HttpStatus.CREATED)
    PlanDetail create(@Valid @RequestBody PlanRequest request, @AuthenticationPrincipal Jwt jwt) { return service.create(request, actor(jwt)); }
    @PutMapping("/plans/{id}")
    PlanDetail update(@PathVariable("id") UUID id, @Valid @RequestBody PlanUpdateRequest request, @AuthenticationPrincipal Jwt jwt) {
        return service.update(id, request, actor(jwt));
    }
    @PostMapping("/plans/{id}/routes") @ResponseStatus(HttpStatus.CREATED)
    RouteResponse addRoute(@PathVariable("id") UUID id, @Valid @RequestBody RouteCreateRequest request, @AuthenticationPrincipal Jwt jwt) {
        return service.addRoute(id, request, actor(jwt));
    }
    @PutMapping("/plans/{planId}/routes/{routeId}")
    RouteResponse updateRoute(@PathVariable("planId") UUID planId, @PathVariable("routeId") UUID routeId,
                              @Valid @RequestBody RouteUpdateRequest request, @AuthenticationPrincipal Jwt jwt) {
        return service.updateRoute(planId, routeId, request, actor(jwt));
    }
    @DeleteMapping("/plans/{planId}/routes/{routeId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteRoute(@PathVariable("planId") UUID planId, @PathVariable("routeId") UUID routeId, @AuthenticationPrincipal Jwt jwt) {
        service.deleteRoute(planId, routeId, actor(jwt));
    }
    @PatchMapping("/plans/{id}/status")
    PlanDetail status(@PathVariable("id") UUID id, @Valid @RequestBody StatusRequest request, @AuthenticationPrincipal Jwt jwt) {
        return service.setStatus(id, request, actor(jwt));
    }
    @PostMapping("/simulations") SimulationResponse simulate(@Valid @RequestBody SimulationRequest request) {
        return service.simulate(request);
    }

    private static String actor(Jwt jwt) {
        var username = jwt == null ? null : jwt.getClaimAsString("preferred_username");
        return username == null || username.isBlank() ? "Sistema" : username;
    }
}
