package pt.glsmanagement.identity.domain;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
class IdentityAdminController {
    private final IdentityLifecycleService service;
    IdentityAdminController(IdentityLifecycleService service) { this.service = service; }

    @GetMapping List<IdentityUserResponse> list() { return service.list(); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    IdentityUserResponse join(@Valid @RequestBody JoinerRequest request, @AuthenticationPrincipal Jwt actor) {
        return service.join(request, actor);
    }

    @PutMapping("/{id}/role")
    IdentityUserResponse move(
            @PathVariable String id, @Valid @RequestBody MoverRequest request, @AuthenticationPrincipal Jwt actor) {
        if (id.equals(actor.getSubject())) throw new IllegalArgumentException("Self role changes are not allowed");
        return service.move(id, request, actor);
    }

    @PostMapping("/{id}/leave")
    IdentityUserResponse leave(@PathVariable String id, @AuthenticationPrincipal Jwt actor) {
        if (id.equals(actor.getSubject())) throw new IllegalArgumentException("Self deactivation is not allowed");
        return service.leave(id, actor);
    }
}
