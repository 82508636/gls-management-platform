package pt.glsmanagement.platform.integration.vies;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import pt.glsmanagement.platform.customer.VatValidationProviderUnavailableException;

import java.time.Duration;
import java.util.concurrent.Semaphore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ViesVatValidationClientTest {
    private MockRestServiceServer server;
    private ViesVatValidationClient client;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl("https://vies.test");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ViesVatValidationClient(builder.build(), new Semaphore(1));
    }

    @Test
    void usesTheOfficialEndpointAndMapsAValidResponse() {
        server.expect(once(), requestTo("https://vies.test/check-vat-number"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string(equalTo("{\"countryCode\":\"PT\",\"vatNumber\":\"500000000\"}")))
                .andRespond(withSuccess("""
                        {"countryCode":"PT","vatNumber":"500000000","valid":true,
                         "name":"CP - Comboios de Portugal, E.P.E.",
                         "address":"CALÇADA DO DUQUE, 20\\n1249-109 LISBOA",
                         "futureField":"ignored"}
                        """, MediaType.APPLICATION_JSON));
        var result = client.validate("PT", "500000000");
        assertThat(result.valid()).isTrue();
        assertThat(result.registeredName()).isEqualTo("CP - Comboios de Portugal, E.P.E.");
        assertThat(result.registeredAddress()).isEqualTo("CALÇADA DO DUQUE, 20\n1249-109 LISBOA");
        server.verify();
    }

    @Test
    void mapsAWellFormedNegativeResponse() {
        server.expect(requestTo("https://vies.test/check-vat-number"))
                .andRespond(withSuccess("{\"valid\":false}", MediaType.APPLICATION_JSON));
        assertThat(client.validate("PT", "500000000").valid()).isFalse();
    }

    @Test
    void rejectsApplicationErrorsReturnedWithHttp200() {
        server.expect(requestTo("https://vies.test/check-vat-number"))
                .andRespond(withSuccess("""
                        {"actionSucceed":false,"errorWrappers":[{"error":"TIMEOUT","message":"Upstream detail"}]}
                        """, MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.validate("PT", "500000000"))
                .isInstanceOf(VatValidationProviderUnavailableException.class)
                .hasMessageNotContaining("Upstream detail");
    }

    @Test
    void rejectsMissingDecisionAndServerErrors() {
        server.expect(requestTo("https://vies.test/check-vat-number"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.validate("PT", "500000000"))
                .isInstanceOf(VatValidationProviderUnavailableException.class);
        server.reset();
        server.expect(requestTo("https://vies.test/check-vat-number")).andRespond(withServerError());
        assertThatThrownBy(() -> client.validate("PT", "500000000"))
                .isInstanceOf(VatValidationProviderUnavailableException.class);
        server.verify();
    }

    @Test
    void rejectsClientErrorsWithoutExposingTheViesMessage() {
        server.expect(requestTo("https://vies.test/check-vat-number"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"actionSucceed":false,"errorWrappers":[
                                  {"error":"VOW-ERR-2600","message":"Sensitive upstream detail"}
                                ]}
                                """));
        assertThatThrownBy(() -> client.validate("PT", "500000000"))
                .isInstanceOf(VatValidationProviderUnavailableException.class)
                .hasMessageNotContaining("Sensitive upstream detail");
    }

    @Test
    void doesNotRetryAClientErrorEvenWhenRetriesAreEnabled() {
        var builder = RestClient.builder().baseUrl("https://vies.test");
        var retryServer = MockRestServiceServer.bindTo(builder).build();
        var retryingClient = new ViesVatValidationClient(
                builder.build(), new Semaphore(1), 3, Duration.ZERO);
        retryServer.expect(once(), requestTo("https://vies.test/check-vat-number"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> retryingClient.validate("PT", "500225680"))
                .isInstanceOf(VatValidationProviderUnavailableException.class);
        retryServer.verify();
    }

    @Test
    void rejectsMalformedJson() {
        server.expect(requestTo("https://vies.test/check-vat-number"))
                .andRespond(withSuccess("{", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.validate("PT", "500000000"))
                .isInstanceOf(VatValidationProviderUnavailableException.class);
    }

    @Test
    void retriesATransientServerFailureAndReturnsTheFollowingDecision() {
        var builder = RestClient.builder().baseUrl("https://vies.test");
        var retryServer = MockRestServiceServer.bindTo(builder).build();
        var retryingClient = new ViesVatValidationClient(
                builder.build(), new Semaphore(1), 3, Duration.ZERO);
        retryServer.expect(once(), requestTo("https://vies.test/check-vat-number"))
                .andRespond(withServerError());
        retryServer.expect(once(), requestTo("https://vies.test/check-vat-number"))
                .andRespond(withSuccess("{\"valid\":true}", MediaType.APPLICATION_JSON));

        assertThat(retryingClient.validate("PT", "500225680").valid()).isTrue();
        retryServer.verify();
    }

    @Test
    void rejectsImmediatelyWhenTheOutboundConcurrencyLimitIsExhausted() {
        var limitedClient = new ViesVatValidationClient(
                RestClient.builder().baseUrl("https://vies.test").build(), new Semaphore(0));
        assertThatThrownBy(() -> limitedClient.validate("PT", "500000000"))
                .isInstanceOf(VatValidationProviderUnavailableException.class);
    }
}
