package pt.glsmanagement.platform.identity;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withCreatedEntity;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KeycloakAdminClientTest {
    private static final String BASE_URL = "https://keycloak.test";

    @Test
    void deletesANewUserWhenRoleAssignmentFails() {
        var builder = RestClient.builder().baseUrl(BASE_URL);
        var server = MockRestServiceServer.bindTo(builder).build();
        var client = new KeycloakAdminClient(builder.build(), "ltft", "jml-client", "secret");

        expectToken(server);
        server.expect(requestTo(BASE_URL + "/admin/realms/ltft/users"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withCreatedEntity(URI.create(BASE_URL + "/admin/realms/ltft/users/user-1")));
        expectToken(server);
        server.expect(requestTo(BASE_URL + "/admin/realms/ltft/users/user-1/reset-password"))
                .andExpect(method(HttpMethod.PUT)).andRespond(withSuccess());
        expectToken(server);
        server.expect(requestTo(BASE_URL + "/admin/realms/ltft/users/user-1/role-mappings/realm"))
                .andExpect(method(HttpMethod.GET)).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        expectToken(server);
        server.expect(requestTo(BASE_URL + "/admin/realms/ltft/roles/OPERATOR"))
                .andExpect(method(HttpMethod.GET)).andRespond(withServerError());
        expectToken(server);
        server.expect(requestTo(BASE_URL + "/admin/realms/ltft/users/user-1"))
                .andExpect(method(HttpMethod.DELETE)).andRespond(withSuccess());

        var request = new JoinerRequest("new.user", "new.user@example.test", "New", "User",
                PlatformRole.OPERATOR, "Temporary-Password-123!");

        assertThatThrownBy(() -> client.create(request)).isInstanceOf(RuntimeException.class);
        server.verify();
    }

    private static void expectToken(MockRestServiceServer server) {
        server.expect(requestTo(BASE_URL + "/realms/ltft/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"token\"}", MediaType.APPLICATION_JSON));
    }
}
