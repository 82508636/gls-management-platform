package pt.glsmanagement.platform.customer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Component
class RemoteCustomerReferenceLookup implements CustomerReferenceLookup {
    private final RestClient customer;

    RemoteCustomerReferenceLookup(
            @Value("${ltft.integrations.customer.base-url}") String baseUrl,
            @Value("${ltft.integrations.customer.connect-timeout:2s}") Duration connectTimeout,
            @Value("${ltft.integrations.customer.read-timeout:5s}") Duration readTimeout) {
        var requests = new SimpleClientHttpRequestFactory();
        requests.setConnectTimeout(connectTimeout);
        requests.setReadTimeout(readTimeout);
        customer = RestClient.builder().baseUrl(baseUrl).requestFactory(requests).build();
    }

    @Override
    public boolean allExist(Set<UUID> ids) {
        if (ids.isEmpty()) return true;
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwt)) {
            throw new CustomerReferenceUnavailableException();
        }
        try {
            var response = customer.post().uri("/internal/v1/customers/existence")
                    .headers(headers -> headers.setBearerAuth(jwt.getToken().getTokenValue()))
                    .body(new ExistenceRequest(ids)).retrieve().body(ExistenceResponse.class);
            return response != null && response.allExist();
        } catch (RestClientException exception) {
            throw new CustomerReferenceUnavailableException(exception);
        }
    }

    record ExistenceRequest(Set<UUID> ids) {}
    record ExistenceResponse(boolean allExist) {}
}
