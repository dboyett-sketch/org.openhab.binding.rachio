package org.openhab.binding.rachio.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.Configuration;

/**
 * The {@link RachioConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Markus Michels - Initial contribution
 */
@NonNullByDefault
public class RachioConfiguration {
    
    // Bridge configuration parameters
    public @Nullable String apikey;
    public int pollingInterval = 120;
    public int defaultRuntime = 300;
    
    // Webhook configuration
    public @Nullable String callbackUrl;
    public boolean clearAllCallbacks = true;
    public @Nullable String ipFilter;

    // Thing configuration (for devices/zones)
    public @Nullable String deviceId;
    public @Nullable String zoneId;
    public @Nullable String personId;

    /**
     * Update configuration from properties
     */
    public void updateConfig(@Nullable Configuration config) {
        if (config != null) {
            apikey = (String) config.get("apikey");
            pollingInterval = config.get("pollingInterval") != null ? ((Number) config.get("pollingInterval")).intValue() : 120;
            defaultRuntime = config.get("defaultRuntime") != null ? ((Number) config.get("defaultRuntime")).intValue() : 300;
            callbackUrl = (String) config.get("callbackUrl");
            clearAllCallbacks = config.get("clearAllCallbacks") != null ? (Boolean) config.get("clearAllCallbacks") : true;
            ipFilter = (String) config.get("ipFilter");
            deviceId = (String) config.get("deviceId");
            zoneId = (String) config.get("zoneId");
            personId = (String) config.get("personId");
        }
    }

    /**
     * Validate configuration
     */
    public boolean isValid() {
        return apikey != null && !apikey.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "RachioConfiguration{" +
                "apikey='" + (apikey != null ? apikey.substring(0, Math.min(8, apikey.length())) + "..." : "null") + '\'' +
                ", pollingInterval=" + pollingInterval +
                ", defaultRuntime=" + defaultRuntime +
                ", callbackUrl='" + callbackUrl + '\'' +
                ", clearAllCallbacks=" + clearAllCallbacks +
                ", ipFilter='" + ipFilter + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", zoneId='" + zoneId + '\'' +
                ", personId='" + personId + '\'' +
                '}';
    }
}
