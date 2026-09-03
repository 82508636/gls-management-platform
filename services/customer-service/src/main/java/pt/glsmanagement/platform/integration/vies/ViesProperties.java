package pt.glsmanagement.platform.integration.vies;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "ltft.integrations.vies")
record ViesProperties(
        @NotNull URI baseUrl,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout,
        @Min(1) int maxConcurrentRequests,
        @Min(1) int maxAttempts,
        @NotNull Duration retryDelay
) {
    ViesProperties {
        if (connectTimeout != null && (connectTimeout.isZero() || connectTimeout.isNegative())) {
            throw new IllegalArgumentException("VIES connect timeout must be positive");
        }
        if (readTimeout != null && (readTimeout.isZero() || readTimeout.isNegative())) {
            throw new IllegalArgumentException("VIES read timeout must be positive");
        }
        if (retryDelay != null && retryDelay.isNegative()) {
            throw new IllegalArgumentException("VIES retry delay cannot be negative");
        }
    }
}
