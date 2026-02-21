package com.prog3360.product_service.flags;

import io.getunleash.DefaultUnleash;
import io.getunleash.Unleash;
import io.getunleash.util.UnleashConfig;
import org.springframework.stereotype.Service;

@Service
public class FeatureFlagService {

    private final Unleash unleash; // null if init fails

    public FeatureFlagService() {
        Unleash tmp;
        try {
            
            String apiUrl = getenvOrDefault("UNLEASH_API_URL", "http://unleash-server:4242/api");
            
            apiUrl = apiUrl.endsWith("/") ? apiUrl.substring(0, apiUrl.length() - 1) : apiUrl;

            String apiToken = getenvOrDefault("UNLEASH_API_TOKEN", "");
            if (apiToken.isBlank()) {
                throw new IllegalStateException("UNLEASH_API_TOKEN is blank");
            }

            UnleashConfig config = UnleashConfig.builder()
                    .appName("product-service")
                    .instanceId(getenvOrDefault("HOSTNAME", "local"))
                    .unleashAPI(apiUrl)
                    
                    .customHttpHeader("Authorization", apiToken)
                    .build();

            tmp = new DefaultUnleash(config);
            System.out.println("[FeatureFlagService] Unleash initialized. URL=" + apiUrl);
        } catch (Exception e) {
            tmp = null;
            System.err.println("[FeatureFlagService] Unleash init failed. All flags OFF. " + e.getMessage());
        }
        this.unleash = tmp;
    }

    public boolean isEnabled(String flagName) {
        try {
            if (unleash == null) return false;
            return unleash.isEnabled(flagName);
        } catch (Exception e) {
            System.err.println("[FeatureFlagService] Flag check failed for '" + flagName + "'. Defaulting OFF. " + e.getMessage());
            return false;
        }
    }

    private static String getenvOrDefault(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? def : v;
    }
}