package pt.glsmanagement.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

final class RemoteIdentityAccessTokenValidator implements OAuth2TokenValidator<Jwt> {
    private static final OAuth2Error REVOKED = new OAuth2Error(
            "invalid_token", "The platform access was revoked or changed", null);
    private static final OAuth2Error UNAVAILABLE = new OAuth2Error(
            "invalid_token", "The platform access decision is unavailable", null);

    private final RestClient identity;

    RemoteIdentityAccessTokenValidator(RestClient identity) { this.identity = identity; }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        try {
            var decision = identity.get().uri("/internal/v1/access-decisions/self")
                    .headers(headers -> headers.setBearerAuth(jwt.getTokenValue()))
                    .retrieve().body(AccessDecision.class);
            return decision != null && decision.allowed()
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(REVOKED);
        } catch (RestClientException exception) {
            return OAuth2TokenValidatorResult.failure(UNAVAILABLE);
        }
    }

    record AccessDecision(boolean allowed) {}
}
