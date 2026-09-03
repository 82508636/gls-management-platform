package pt.glsmanagement.platform.integration.vies;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.concurrent.Semaphore;

@Configuration
@EnableConfigurationProperties(ViesProperties.class)
class ViesConfiguration {
    @Bean("viesRestClient")
    RestClient viesRestClient(RestClient.Builder builder, ViesProperties properties) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Math.toIntExact(properties.connectTimeout().toMillis()));
        requestFactory.setReadTimeout(Math.toIntExact(properties.readTimeout().toMillis()));
        return builder.baseUrl(properties.baseUrl().toString().replaceFirst("/+$", ""))
                .requestFactory(requestFactory).build();
    }

    @Bean("viesRequestPermits")
    Semaphore viesRequestPermits(ViesProperties properties) {
        return new Semaphore(properties.maxConcurrentRequests(), true);
    }
}
