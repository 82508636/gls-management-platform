package pt.glsmanagement.platform.integration.vies;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import pt.glsmanagement.platform.customer.VatValidationGateway;
import pt.glsmanagement.platform.customer.VatValidationProviderUnavailableException;

import java.util.concurrent.Semaphore;

@Component
class ViesVatValidationClient implements VatValidationGateway {
    private final RestClient restClient;
    private final Semaphore requestPermits;

    ViesVatValidationClient(
            @Qualifier("viesRestClient") RestClient restClient,
            @Qualifier("viesRequestPermits") Semaphore requestPermits
    ) {
        this.restClient = restClient;
        this.requestPermits = requestPermits;
    }

    @Override
    public Result validate(String countryCode, String vatNumber) {
        if (!requestPermits.tryAcquire()) {
            throw new VatValidationProviderUnavailableException();
        }
        try {
            var response = restClient.post()
                    .uri("/check-vat-number")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(new ViesRequest(countryCode, vatNumber))
                    .retrieve()
                    .body(ViesResponse.class);

            if (response == null || response.valid() == null || Boolean.FALSE.equals(response.actionSucceed())) {
                throw new VatValidationProviderUnavailableException();
            }
            return new Result(response.valid());
        } catch (RestClientException exception) {
            throw new VatValidationProviderUnavailableException(exception);
        } finally {
            requestPermits.release();
        }
    }

    private record ViesRequest(String countryCode, String vatNumber) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ViesResponse(Boolean valid, Boolean actionSucceed) {
    }
}
