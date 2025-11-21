package org.openhab.binding.rachio.internal;

import static org.openhab.binding.rachio.RachioBindingConstants.*;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds configuration values for the Rachio binding.
 * Field names must match config keys defined in thing-types.xml or config-description.xml.
 */
@NonNullByDefault
public class RachioConfiguration {

    private final Logger logger = LoggerFactory.getLogger(RachioConfiguration.class);

    public String apikey = "";
    public int pollingInterval = DEFAULT_POLLING_INTERVAL;
    public int defaultRuntime = DEFAULT_ZONE_RUNTIME;
    public String callbackUrl = "";
    public boolean clearAllCallbacks = false;
    public String ipFilter = "192.168.0.0/16;10.0.0.0/8;172.16.0.0/12";

    /**
     * Updates configuration fields from the raw config map.
     */
    public void updateConfig(Map<String, Object> config) {
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().toString().trim();

            if (key.equalsIgnoreCase("component.name") || key.equalsIgnoreCase("component.id")) {
                continue;
            }

            logger.debug("Rachio config: {} = {}", key, value);

            switch (key.toLowerCase()) {
                case PARAM_APIKEY:
                    apikey = value;
                    break;
                case PARAM_POLLING_INTERVAL:
                    pollingInterval = parseInt(value, DEFAULT_POLLING_INTERVAL);
                    break;
                case PARAM_DEF_RUNTIME:
                    defaultRuntime = parseInt(value, DEFAULT_ZONE_RUNTIME);
                    break;
                case PARAM_CALLBACK_URL:
                    callbackUrl = value;
                    break;
                case PARAM_IPFILTER:
                    ipFilter = value;
                    break;
                case PARAM_CLEAR_CALLBACK:
                    clearAllCallbacks = parseBoolean(value);
                    break;
                default:
                    logger.debug("Rachio: Unknown config key '{}'", key);
                    break;
            }
        }
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.warn("Rachio: Invalid integer '{}', using fallback {}", value, fallback);
            return fallback;
        }
    }

    private boolean parseBoolean(String value) {
        return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") || value.equals("1");
    }
}