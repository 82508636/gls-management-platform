package pt.glsmanagement.platform.shipment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import pt.glsmanagement.platform.pricing.PricingQuote;
import pt.glsmanagement.platform.pricing.PricingQuoteRequest;
import pt.glsmanagement.platform.pricing.PricingQuoteService;

import java.time.Duration;

@Component
class RemotePricingQuoteService implements PricingQuoteService {
    private final RestClient pricing;

    RemotePricingQuoteService(
            @Value("${ltft.integrations.pricing.base-url}") String baseUrl,
            @Value("${ltft.integrations.pricing.connect-timeout:2s}") Duration connectTimeout,
            @Value("${ltft.integrations.pricing.read-timeout:5s}") Duration readTimeout) {
        var requests = new SimpleClientHttpRequestFactory();
        requests.setConnectTimeout(connectTimeout);
        requests.setReadTimeout(readTimeout);
        pricing = RestClient.builder().baseUrl(baseUrl).requestFactory(requests).build();
    }

    @Override
    public PricingQuote quote(PricingQuoteRequest request) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwt)) {
            throw ShipmentException.pricingUnavailable();
        }
        try {
            var quote = pricing.post().uri("/internal/v1/pricing/quotes")
                    .headers(headers -> headers.setBearerAuth(jwt.getToken().getTokenValue()))
                    .body(request).retrieve().body(PricingQuote.class);
            if (quote == null) throw ShipmentException.pricingUnavailable();
            return quote;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().is4xxClientError()
                    && exception.getStatusCode().value() != 401
                    && exception.getStatusCode().value() != 403) {
                throw ShipmentException.invalidData();
            }
            throw ShipmentException.pricingUnavailable();
        } catch (RestClientException exception) {
            throw ShipmentException.pricingUnavailable();
        }
    }
}
