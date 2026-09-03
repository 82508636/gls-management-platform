package pt.glsmanagement.platform.integration.vies;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import pt.glsmanagement.platform.customer.VatValidationGateway;
import pt.glsmanagement.platform.customer.VatValidationProviderUnavailableException;

import java.time.Duration;
import java.util.concurrent.Semaphore;

@Component
class ViesVatValidationClient implements VatValidationGateway {
    private final RestClient restClient;
    private final Semaphore requestPermits;
    private final int maxAttempts;
    private final Duration retryDelay;

    @Autowired
    ViesVatValidationClient(
            @Qualifier("viesRestClient") RestClient restClient,
            @Qualifier("viesRequestPermits") Semaphore requestPermits,
            ViesProperties properties) {
        this(restClient, requestPermits, properties.maxAttempts(), properties.retryDelay());
    }

    ViesVatValidationClient(RestClient restClient, Semaphore requestPermits) {
        this(restClient, requestPermits, 1, Duration.ZERO);
    }

    ViesVatValidationClient(
            RestClient restClient, Semaphore requestPermits, int maxAttempts, Duration retryDelay) {
        this.restClient = restClient;
        this.requestPermits = requestPermits;
        this.maxAttempts = maxAttempts;
        this.retryDelay = retryDelay;
    }

    @Override
    public Result validate(String countryCode, String vatNumber) {
        if (!requestPermits.tryAcquire()) throw new VatValidationProviderUnavailableException();
        try {
            VatValidationProviderUnavailableException lastFailure = null;
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    return requestValidation(countryCode, vatNumber);
                } catch (RestClientResponseException exception) {
                    if (exception.getStatusCode().is4xxClientError()) {
                        throw new VatValidationProviderUnavailableException(exception);
                    }
                    lastFailure = new VatValidationProviderUnavailableException(exception);
                } catch (RestClientException exception) {
                    lastFailure = new VatValidationProviderUnavailableException(exception);
                } catch (VatValidationProviderUnavailableException exception) {
                    lastFailure = exception;
                }
                if (attempt < maxAttempts) pauseBeforeRetry();
            }
            throw lastFailure == null ? new VatValidationProviderUnavailableException() : lastFailure;
        } finally {
            requestPermits.release();
        }
    }

    private Result requestValidation(String countryCode, String vatNumber) {
        var response = restClient.post().uri("/check-vat-number")
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .body(new ViesRequest(countryCode, vatNumber)).retrieve().body(ViesResponse.class);
        if (response == null || response.valid() == null || Boolean.FALSE.equals(response.actionSucceed())) {
            throw new VatValidationProviderUnavailableException();
        }
        return new Result(
                response.valid(), response.name(), response.address(), response.traderStreet(),
                response.traderPostalCode(), response.traderCity());
    }

    private void pauseBeforeRetry() {
        try {
            Thread.sleep(retryDelay.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new VatValidationProviderUnavailableException(exception);
        }
    }

    private record ViesRequest(String countryCode, String vatNumber) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ViesResponse(
            Boolean valid, Boolean actionSucceed, String name, String address,
            String traderStreet, String traderPostalCode, String traderCity) {}
}
