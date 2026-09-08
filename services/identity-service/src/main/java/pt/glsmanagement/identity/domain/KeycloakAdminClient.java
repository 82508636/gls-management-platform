package pt.glsmanagement.identity.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
class KeycloakAdminClient {
    private static final Set<String> PLATFORM_ROLES = Arrays.stream(PlatformRole.values())
            .map(Enum::name).collect(Collectors.toUnmodifiableSet());
    private final RestClient http;
    private final String realm;
    private final String clientId;
    private final String clientSecret;

    @Autowired
    KeycloakAdminClient(
            RestClient.Builder builder,
            @Value("${ltft.identity.keycloak.base-url}") String baseUrl,
            @Value("${ltft.identity.keycloak.realm}") String realm,
            @Value("${ltft.identity.keycloak.client-id}") String clientId,
            @Value("${ltft.identity.keycloak.client-secret}") String clientSecret,
            @Value("${ltft.identity.keycloak.connect-timeout}") Duration connectTimeout,
            @Value("${ltft.identity.keycloak.read-timeout}") Duration readTimeout) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        this.http = builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    KeycloakAdminClient(RestClient http, String realm, String clientId, String clientSecret) {
        this.http = http;
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    List<IdentityUserResponse> listUsers() {
        var users = http.get().uri("/admin/realms/{realm}/users?first=0&max=50&briefRepresentation=true", realm)
                .headers(headers -> headers.setBearerAuth(token())).retrieve()
                .body(new ParameterizedTypeReference<List<UserRepresentation>>() {});
        if (users == null) return List.of();
        return users.stream().filter(user -> user.serviceAccountClientId() == null)
                .map(user -> response(user, platformRoles(user.id()))).toList();
    }

    IdentityUserResponse create(JoinerRequest request) {
        var body = Map.of(
                "username", request.username().trim(), "email", request.email().trim(),
                "firstName", request.firstName().trim(), "lastName", request.lastName().trim(),
                "enabled", true, "emailVerified", false);
        var result = http.post().uri("/admin/realms/{realm}/users", realm)
                .headers(headers -> headers.setBearerAuth(token())).contentType(MediaType.APPLICATION_JSON)
                .body(body).retrieve().toBodilessEntity();
        URI location = result.getHeaders().getLocation();
        if (location == null) throw new IdentityOperationException("Keycloak did not return the user identifier");
        var userId = location.getPath().substring(location.getPath().lastIndexOf('/') + 1);
        try {
            resetPassword(userId, request.temporaryPassword());
            replacePlatformRoles(userId, request.role());
            return get(userId);
        } catch (RuntimeException failure) {
            compensateCreatedUser(userId, failure);
            throw failure;
        }
    }

    void delete(String userId) {
        http.delete().uri("/admin/realms/{realm}/users/{id}", realm, userId)
                .headers(headers -> headers.setBearerAuth(token())).retrieve().toBodilessEntity();
    }

    private void compensateCreatedUser(String userId, RuntimeException originalFailure) {
        try {
            delete(userId);
        } catch (RuntimeException compensationFailure) {
            originalFailure.addSuppressed(compensationFailure);
        }
    }

    IdentityUserResponse move(String userId, PlatformRole role) {
        replacePlatformRoles(userId, role);
        logout(userId);
        return get(userId);
    }

    IdentityUserResponse leave(String userId) {
        var current = getRepresentation(userId);
        var update = new LinkedHashMap<String, Object>();
        update.put("username", current.username());
        update.put("email", current.email());
        update.put("firstName", current.firstName());
        update.put("lastName", current.lastName());
        update.put("enabled", false);
        http.put().uri("/admin/realms/{realm}/users/{id}", realm, userId)
                .headers(headers -> headers.setBearerAuth(token())).contentType(MediaType.APPLICATION_JSON)
                .body(update).retrieve().toBodilessEntity();
        logout(userId);
        return get(userId);
    }

    IdentityUserResponse get(String userId) {
        var user = getRepresentation(userId);
        return response(user, platformRoles(userId));
    }

    private UserRepresentation getRepresentation(String userId) {
        var user = http.get().uri("/admin/realms/{realm}/users/{id}", realm, userId)
                .headers(headers -> headers.setBearerAuth(token())).retrieve().body(UserRepresentation.class);
        if (user == null) throw new IdentityOperationException("User was not returned by Keycloak");
        return user;
    }

    private void resetPassword(String userId, String temporaryPassword) {
        http.put().uri("/admin/realms/{realm}/users/{id}/reset-password", realm, userId)
                .headers(headers -> headers.setBearerAuth(token())).contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("type", "password", "value", temporaryPassword, "temporary", true))
                .retrieve().toBodilessEntity();
    }

    private void replacePlatformRoles(String userId, PlatformRole role) {
        var existing = realmRoleMappings(userId).stream()
                .filter(item -> PLATFORM_ROLES.contains(item.name())).toList();
        if (!existing.isEmpty()) {
            http.method(HttpMethod.DELETE).uri(
                            "/admin/realms/{realm}/users/{id}/role-mappings/realm", realm, userId)
                    .headers(headers -> headers.setBearerAuth(token())).contentType(MediaType.APPLICATION_JSON)
                    .body(existing).retrieve().toBodilessEntity();
        }
        var selected = http.get().uri("/admin/realms/{realm}/roles/{role}", realm, role.name())
                .headers(headers -> headers.setBearerAuth(token())).retrieve().body(RoleRepresentation.class);
        if (selected == null) throw new IdentityOperationException("Platform role was not found");
        http.post().uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", realm, userId)
                .headers(headers -> headers.setBearerAuth(token())).contentType(MediaType.APPLICATION_JSON)
                .body(List.of(selected)).retrieve().toBodilessEntity();
    }

    private void logout(String userId) {
        http.post().uri("/admin/realms/{realm}/users/{id}/logout", realm, userId)
                .headers(headers -> headers.setBearerAuth(token())).retrieve().toBodilessEntity();
    }

    private Set<PlatformRole> platformRoles(String userId) {
        var roles = EnumSet.noneOf(PlatformRole.class);
        realmRoleMappings(userId).stream().map(RoleRepresentation::name).filter(PLATFORM_ROLES::contains)
                .map(PlatformRole::valueOf).forEach(roles::add);
        return roles;
    }

    private List<RoleRepresentation> realmRoleMappings(String userId) {
        var roles = http.get().uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", realm, userId)
                .headers(headers -> headers.setBearerAuth(token())).retrieve()
                .body(new ParameterizedTypeReference<List<RoleRepresentation>>() {});
        return roles == null ? List.of() : roles;
    }

    private String token() {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        var response = http.post().uri("/realms/{realm}/protocol/openid-connect/token", realm)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(TokenResponse.class);
        if (response == null || response.accessToken() == null) {
            throw new IdentityOperationException("Keycloak token unavailable");
        }
        return response.accessToken();
    }

    private static IdentityUserResponse response(UserRepresentation user, Set<PlatformRole> roles) {
        return new IdentityUserResponse(user.id(), user.username(), user.email(), user.firstName(), user.lastName(),
                user.enabled(), roles);
    }

    private record TokenResponse(@JsonProperty("access_token") String accessToken) {}
    private record UserRepresentation(
            String id, String username, String email, String firstName, String lastName,
            boolean enabled, String serviceAccountClientId) {}
    private record RoleRepresentation(String id, String name) {}
}
