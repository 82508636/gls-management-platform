package pt.glsmanagement.gateway;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.Principal;

@Configuration
class RateLimitConfig {
    @Bean
    KeyResolver authenticatedPrincipalKeyResolver() {
        return exchange -> exchange.getPrincipal().map(Principal::getName);
    }
}
