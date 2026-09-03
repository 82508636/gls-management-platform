package pt.glsmanagement.workforce.reference;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static pt.glsmanagement.workforce.reference.ReferenceCatalogDtos.CreateRequest;
import static pt.glsmanagement.workforce.reference.ReferenceCatalogDtos.Response;
import static pt.glsmanagement.workforce.reference.ReferenceCatalogDtos.StatusRequest;
import static pt.glsmanagement.workforce.reference.ReferenceCatalogDtos.UpdateRequest;

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
    Response updateProfile(@PathVariable String id, @Valid @RequestBody UpdateRequest request,
                           @AuthenticationPrincipal Jwt jwt) {
        return service.updateProfile(id, request, actor(jwt));
    }
    @PatchMapping("/account-profiles/{id}/status")
    Response profileStatus(@PathVariable String id, @Valid @RequestBody StatusRequest request,
                           @AuthenticationPrincipal Jwt jwt) {
        return service.setProfileStatus(id, request.active(), actor(jwt));
    }

    @GetMapping("/professional-categories") List<Response> categories() { return service.categories(); }
    @PostMapping("/professional-categories") @ResponseStatus(HttpStatus.CREATED)
    Response createCategory(@Valid @RequestBody CreateRequest request, @AuthenticationPrincipal Jwt jwt) {
        return service.createCategory(request, actor(jwt));
    }
    @PutMapping("/professional-categories/{id}")
    Response updateCategory(@PathVariable String id, @Valid @RequestBody UpdateRequest request,
                            @AuthenticationPrincipal Jwt jwt) {
        return service.updateCategory(id, request, actor(jwt));
    }
    @PatchMapping("/professional-categories/{id}/status")
    Response categoryStatus(@PathVariable String id, @Valid @RequestBody StatusRequest request,
                            @AuthenticationPrincipal Jwt jwt) {
        return service.setCategoryStatus(id, request.active(), actor(jwt));
    }

    private static String actor(Jwt jwt) {
        var username = jwt == null ? null : jwt.getClaimAsString("preferred_username");
        if (username == null || username.isBlank()) return "Sistema";
        var normalized = username.trim();
        return normalized.length() <= 200 ? normalized : normalized.substring(0, 200);
    }
}
