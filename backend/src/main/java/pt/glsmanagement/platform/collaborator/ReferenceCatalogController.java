package pt.glsmanagement.platform.collaborator;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import static pt.glsmanagement.platform.collaborator.ReferenceCatalogDtos.*;

@RestController
@RequestMapping("/api/reference-data")
class ReferenceCatalogController {
    private final ReferenceCatalogService service;

    ReferenceCatalogController(ReferenceCatalogService service) { this.service = service; }

    @GetMapping("/account-profiles") List<Response> profiles() { return service.profiles(); }
    @PostMapping("/account-profiles") @ResponseStatus(HttpStatus.CREATED)
    Response createProfile(@Valid @RequestBody CreateRequest request, @AuthenticationPrincipal Jwt jwt) {
        return service.createProfile(request, actor(jwt));
    }
    @PutMapping("/account-profiles/{id}")
    Response updateProfile(@PathVariable String id, @Valid @RequestBody UpdateRequest request, @AuthenticationPrincipal Jwt jwt) {
        return service.updateProfile(id, request, actor(jwt));
    }
    @PatchMapping("/account-profiles/{id}/status")
    Response profileStatus(@PathVariable String id, @Valid @RequestBody StatusRequest request, @AuthenticationPrincipal Jwt jwt) {
        return service.setProfileStatus(id, request.active(), actor(jwt));
    }

    @GetMapping("/professional-categories") List<Response> categories() { return service.categories(); }
    @PostMapping("/professional-categories") @ResponseStatus(HttpStatus.CREATED)
    Response createCategory(@Valid @RequestBody CreateRequest request, @AuthenticationPrincipal Jwt jwt) {
        return service.createCategory(request, actor(jwt));
    }
    @PutMapping("/professional-categories/{id}")
    Response updateCategory(@PathVariable String id, @Valid @RequestBody UpdateRequest request, @AuthenticationPrincipal Jwt jwt) {
        return service.updateCategory(id, request, actor(jwt));
    }
    @PatchMapping("/professional-categories/{id}/status")
    Response categoryStatus(@PathVariable String id, @Valid @RequestBody StatusRequest request, @AuthenticationPrincipal Jwt jwt) {
        return service.setCategoryStatus(id, request.active(), actor(jwt));
    }

    private static String actor(Jwt jwt) {
        var username = jwt == null ? null : jwt.getClaimAsString("preferred_username");
        return username == null || username.isBlank() ? "Sistema" : username;
    }
}
