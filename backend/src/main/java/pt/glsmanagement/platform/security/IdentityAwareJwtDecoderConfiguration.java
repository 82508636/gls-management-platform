package pt.glsmanagement.platform.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import pt.glsmanagement.platform.identity.IdentityAccessControl;

@Configuration
class IdentityAwareJwtDecoderConfiguration {
    @Bean
    JwtDecoder jwtDecoder(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
                          IdentityAccessControl accessControl) {
        var decoder = (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(issuer);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                new IdentityAccessTokenValidator(accessControl)));
        return decoder;
    }
}
