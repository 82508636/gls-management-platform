package pt.glsmanagement.platform.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        var securityErrors = new ApiSecurityErrorWriter(objectMapper);
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(securityErrors)
                        .accessDeniedHandler(securityErrors))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error", "/actuator/health", "/actuator/info", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/customers", "/api/customers/**").hasAnyRole("ADMIN", "OPERATOR", "ACCOUNTING", "FRONT_DESK")
                        .requestMatchers(HttpMethod.HEAD, "/api/customers", "/api/customers/**").hasAnyRole("ADMIN", "OPERATOR", "ACCOUNTING", "FRONT_DESK")
                        .requestMatchers(HttpMethod.GET, "/api/recipients", "/api/pickup-points", "/api/pickup-points/**").hasAnyRole("ADMIN", "OPERATOR", "ACCOUNTING", "FRONT_DESK")
                        .requestMatchers(HttpMethod.HEAD, "/api/recipients", "/api/pickup-points", "/api/pickup-points/**").hasAnyRole("ADMIN", "OPERATOR", "ACCOUNTING", "FRONT_DESK")
                        .requestMatchers(HttpMethod.GET, "/api/reference-data/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.HEAD, "/api/reference-data/**").hasRole("ADMIN")
                        .requestMatchers("/api/reference-data/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/pricing/**").hasAnyRole("ADMIN", "ACCOUNTING")
                        .requestMatchers(HttpMethod.HEAD, "/api/pricing/**").hasAnyRole("ADMIN", "ACCOUNTING")
                        .requestMatchers(HttpMethod.POST, "/api/pricing/simulations").hasAnyRole("ADMIN", "ACCOUNTING")
                        .requestMatchers("/api/pricing/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/operational-services", "/api/operational-services/**",
                                "/api/service-groups", "/api/service-groups/**", "/api/billing/zones", "/api/billing/zones/**")
                                .hasAnyRole("ADMIN", "OPERATOR", "ACCOUNTING", "FRONT_DESK")
                        .requestMatchers(HttpMethod.HEAD, "/api/operational-services", "/api/operational-services/**",
                                "/api/service-groups", "/api/service-groups/**", "/api/billing/zones", "/api/billing/zones/**")
                                .hasAnyRole("ADMIN", "OPERATOR", "ACCOUNTING", "FRONT_DESK")
                        .requestMatchers("/api/operational-services", "/api/operational-services/**",
                                "/api/service-groups", "/api/service-groups/**",
                                "/api/billing/zones", "/api/billing/zones/**")
                                .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/shipments", "/api/shipments/**").hasAnyRole("ADMIN", "OPERATOR", "ACCOUNTING", "FRONT_DESK")
                        .requestMatchers(HttpMethod.HEAD, "/api/shipments", "/api/shipments/**").hasAnyRole("ADMIN", "OPERATOR", "ACCOUNTING", "FRONT_DESK")
                        .requestMatchers(HttpMethod.POST, "/api/shipments").hasAnyRole("ADMIN", "OPERATOR", "FRONT_DESK")
                        .requestMatchers("/api/recipients", "/api/recipients/**").denyAll()
                        .requestMatchers(HttpMethod.POST, "/api/vat-validations").hasAnyRole("ADMIN", "OPERATOR", "FRONT_DESK")
                        .requestMatchers(HttpMethod.POST, "/api/customers", "/api/customers/**").hasAnyRole("ADMIN", "OPERATOR", "FRONT_DESK")
                        .requestMatchers(HttpMethod.POST, "/api/pickup-points").hasAnyRole("ADMIN", "OPERATOR", "FRONT_DESK")
                        .requestMatchers(HttpMethod.PATCH, "/api/customers/*/status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/pickup-points/*/status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/customers/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/pickup-points/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/customers/**").hasAnyRole("ADMIN", "OPERATOR", "FRONT_DESK")
                        .requestMatchers(HttpMethod.PUT, "/api/pickup-points/**").hasAnyRole("ADMIN", "OPERATOR", "FRONT_DESK")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").denyAll()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
        return converter;
    }

    static final class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            Object realmAccessClaim = jwt.getClaim("realm_access");
            if (!(realmAccessClaim instanceof Map<?, ?> realmAccess)
                    || !(realmAccess.get("roles") instanceof Collection<?> roles)) {
                return List.of();
            }
            return roles.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(String::toUpperCase)
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .map(GrantedAuthority.class::cast)
                    .toList();
        }
    }
}
