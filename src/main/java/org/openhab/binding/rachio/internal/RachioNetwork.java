package org.openhab.binding.rachio.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.RachioApiException;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Rachio Network Utilities
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioNetwork {
    private final Logger logger = LoggerFactory.getLogger(RachioNetwork.class);

    private final RachioHttp http;
    private final Gson gson = new Gson();

    public RachioNetwork(RachioHttp http) {
        this.http = http;
    }

    public @Nullable JsonObject httpGet(String endpoint, String accept) {
        try {
            return http.httpGet(endpoint);
        } catch (RachioApiException e) {
            logger.debug("HTTP GET failed for {}: {}", endpoint, e.getMessage());
            return null;
        }
    }

    public @Nullable JsonObject httpPut(String endpoint, String body) {
        try {
            return http.httpPut(endpoint, body);
        } catch (RachioApiException e) {
            logger.debug("HTTP PUT failed for {}: {}", endpoint, e.getMessage());
            return null;
        }
    }

    public @Nullable JsonObject httpPost(String endpoint, String body) {
        try {
            return http.httpPost(endpoint, body);
        } catch (RachioApiException e) {
            logger.debug("HTTP POST failed for {}: {}", endpoint, e.getMessage());
            return null;
        }
    }

    public boolean httpDelete(String endpoint, String body) {
        try {
            http.httpDelete(endpoint, body);
            return true;
        } catch (RachioApiException e) {
            logger.debug("HTTP DELETE failed for {}: {}", endpoint, e.getMessage());
            return false;
        }
    }
}
