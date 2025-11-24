package org.openhab.binding.rachio.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioNetwork} class handles network operations for the Rachio binding
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioNetwork {
    private final Logger logger = LoggerFactory.getLogger(RachioNetwork.class);

    private final RachioHttp http;

    public RachioNetwork(RachioHttp http) {
        this.http = http;
    }

    /**
     * Perform an HTTP GET request
     *
     * @param url The URL to request
     * @param contentType The content type header
     * @return The response body as string, or empty string on error
     */
    public @Nullable String httpGet(String url, String contentType) {
        try {
            // FIXED: Remove .resultString reference - directly return the string
            String response = http.httpGet(url, contentType);
            logger.debug("GET {} returned: {}", url, response != null ? response.substring(0, Math.min(response.length(), 100)) : "null");
            return response;
        } catch (Exception e) {
            logger.debug("GET {} failed: {}", url, e.getMessage());
            return null;
        }
    }

    /**
     * Perform an HTTP PUT request
     *
     * @param url The URL to request
     * @param jsonData The JSON data to send
     * @return The response body as string, or empty string on error
     */
    public @Nullable String httpPut(String url, String jsonData) {
        try {
            // FIXED: Remove .resultString reference - directly return the string
            String response = http.httpPut(url, jsonData);
            logger.debug("PUT {} returned: {}", url, response != null ? response.substring(0, Math.min(response.length(), 100)) : "null");
            return response;
        } catch (Exception e) {
            logger.debug("PUT {} failed: {}", url, e.getMessage());
            return null;
        }
    }

    /**
     * Perform an HTTP POST request
     *
     * @param url The URL to request
     * @param jsonData The JSON data to send
     * @return The response body as string, or empty string on error
     */
    public @Nullable String httpPost(String url, String jsonData) {
        try {
            // FIXED: Remove .resultString reference - directly return the string
            String response = http.httpPost(url, jsonData);
            logger.debug("POST {} returned: {}", url, response != null ? response.substring(0, Math.min(response.length(), 100)) : "null");
            return response;
        } catch (Exception e) {
            logger.debug("POST {} failed: {}", url, e.getMessage());
            return null;
        }
    }

    /**
     * Perform an HTTP DELETE request
     *
     * @param url The URL to request
     * @param contentType The content type header
     * @return The response body as string, or empty string on error
     */
    public @Nullable String httpDelete(String url, String contentType) {
        try {
            // FIXED: Remove .resultString reference - directly return the string
            String response = http.httpDelete(url, contentType);
            logger.debug("DELETE {} returned: {}", url, response != null ? response.substring(0, Math.min(response.length(), 100)) : "null");
            return response;
        } catch (Exception e) {
            logger.debug("DELETE {} failed: {}", url, e.getMessage());
            return null;
        }
    }

    /**
     * Test the connection to Rachio API
     *
     * @return true if connection is successful, false otherwise
     */
    public boolean testConnection() {
        try {
            String response = httpGet("https://api.rach.io/1/public/person/info", "application/json");
            return response != null && !response.isEmpty() && !response.contains("error");
        } catch (Exception e) {
            logger.debug("Connection test failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get the underlying HTTP client
     *
     * @return The RachioHttp instance
     */
    public RachioHttp getHttpClient() {
        return http;
    }
}
