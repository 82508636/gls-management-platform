package pt.glsmanagement.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("ltft.security.identity")
public class IdentityDecisionProperties {
    private String baseUrl = "http://localhost:8084";

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
}
