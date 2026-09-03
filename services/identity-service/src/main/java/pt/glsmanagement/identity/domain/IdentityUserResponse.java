package pt.glsmanagement.identity.domain;

import java.util.Set;

public record IdentityUserResponse(
        String id, String username, String email, String firstName, String lastName,
        boolean enabled, Set<PlatformRole> roles) {}
