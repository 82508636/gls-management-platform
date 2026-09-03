package pt.glsmanagement.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.client.RestClient;

@AutoConfiguration
@AutoConfigureBefore(OAuth2ResourceServerAutoConfiguration.class)
@EnableConfigurationProperties(IdentityDecisionProperties.class)
@ConditionalOnProperty(prefix = "ltft.security.identity", name = "enabled", havingValue = "true", matchIfMissing = true)
public class IdentityAwareJwtDecoderAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    JwtDecoder identityAwareJwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri,
            IdentityDecisionProperties properties) {
        var decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        var identity = RestClient.builder().baseUrl(properties.getBaseUrl()).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                new RemoteIdentityAccessTokenValidator(identity)));
        return decoder;
    }
}
