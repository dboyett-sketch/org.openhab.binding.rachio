package org.openhab.binding.rachio.internal.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.handler.RachioBridgeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * The {@link RachioHttp} is responsible for communicating with the Rachio cloud server.
 *
 * @author Michael Lobstein - Initial contribution
 */

@NonNullByDefault
public class RachioHttp {
    private static final String RACHIO_API_URL = "https://api.rach.io/1/public";
    private static final int TIMEOUT_MILLISECONDS = 10000;

    private final Logger logger = LoggerFactory.getLogger(RachioHttp.class);

    private final String apiKey;
    private @Nullable RachioBridgeHandler handler;
    private String webhookId = "";
    private @Nullable ScheduledFuture<?> pollingJob;

    public RachioHttp(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setHandler(RachioBridgeHandler handler) {
        this.handler = handler;
    }

    public void dispose() {
        stopPolling();
    }

    /**
     * Get a device by ID
     */
    public @Nullable RachioDevice getDevice(String deviceId) {
        try {
            String url = RACHIO_API_URL + "/device/" + deviceId;
            String response = executeGet(url);
            
            if (response != null && !response.isEmpty()) {
                Gson gson = new Gson();
                return gson.fromJson(response, RachioDevice.class);
            }
        } catch (Exception e) {
            logger.debug("Error getting device: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Run all zones for a device
     */
    public void runAllZones(String deviceId, int duration) {
        try {
            String url = RACHIO_API_URL + "/zone/start";
            String json = String.format("{\"id\":\"%s\",\"duration\":%d}", deviceId, duration);
            executePut(url, json);
            logger.debug("Started all zones for device: {}", deviceId);
        } catch (Exception e) {
            logger.debug("Error running all zones: {}", e.getMessage());
        }
    }

    /**
     * Run next zone for a device
     */
    public void runNextZone(String deviceId, int duration) {
        try {
            String url = RACHIO_API_URL + "/zone/start_next";
            String json = String.format("{\"id\":\"%s\",\"duration\":%d}", deviceId, duration);
            executePut(url, json);
            logger.debug("Started next zone for device: {}", deviceId);
        } catch (Exception e) {
            logger.debug("Error running next zone: {}", e.getMessage());
        }
    }

    /**
     * Run a specific zone
     */
    public void runZone(String zoneId, int duration) {
        try {
            String url = RACHIO_API_URL + "/zone/start";
            String json = String.format("{\"id\":\"%s\",\"duration\":%d}", zoneId, duration);
            executePut(url, json);
            logger.debug("Started zone: {} for {} seconds", zoneId, duration);
        } catch (Exception e) {
            logger.debug("Error running zone: {}", e.getMessage());
        }
    }

    /**
     * Stop watering for a device
     */
    public void stopWatering(String deviceId) {
        try {
            String url = RACHIO_API_URL + "/device/stop_water";
            String json = "{\"id\":\"" + deviceId + "\"}";
            executePut(url, json);
            logger.debug("Stopped watering for device: {}", deviceId);
        } catch (Exception e) {
            logger.debug("Error stopping watering: {}", e.getMessage());
        }
    }

    /**
     * Get the webhook ID
     */
    public String getWebhookId() {
        return webhookId;
    }

    /**
     * Create a webhook
     */
    public void createWebhook(String url, String deviceId) {
        try {
            String apiUrl = RACHIO_API_URL + "/webhook";
            String json = "{\"url\":\"" + url + "\",\"deviceId\":\"" + deviceId + "\",\"eventTypes\":[\"DEVICE_STATUS_EVENT\",\"ZONE_STATUS_EVENT\"]}";
            String response = executePost(apiUrl, json);
            
            if (response != null && !response.isEmpty()) {
                Gson gson = new Gson();
                RachioWebhook webhook = gson.fromJson(response, RachioWebhook.class);
                if (webhook != null) {
                    webhookId = webhook.getId();
                    logger.debug("Created webhook with ID: {}", webhookId);
                }
            }
        } catch (Exception e) {
            logger.debug("Error creating webhook: {}", e.getMessage());
        }
    }

    /**
     * Delete a webhook
     */
    public void deleteWebhook(String webhookId) {
        try {
            String url = RACHIO_API_URL + "/webhook/" + webhookId;
            executeDelete(url);
            logger.debug("Deleted webhook: {}", webhookId);
        } catch (Exception e) {
            logger.debug("Error deleting webhook: {}", e.getMessage());
        }
    }

    /**
     * Execute a GET request
     */
    private @Nullable String executeGet(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(TIMEOUT_MILLISECONDS);
            connection.setReadTimeout(TIMEOUT_MILLISECONDS);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return readResponse(connection);
            } else {
                logger.debug("GET request failed with response code: {}", responseCode);
            }
        } catch (Exception e) {
            logger.debug("Error executing GET request: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Execute a PUT request
     */
    private void executePut(String urlString, String json) {
        executeRequestWithBody(urlString, json, "PUT");
    }

    /**
     * Execute a POST request
     */
    private @Nullable String executePost(String urlString, String json) {
        return executeRequestWithBody(urlString, json, "POST");
    }

    /**
     * Execute a DELETE request
     */
    private void executeDelete(String urlString) {
        executeRequestWithBody(urlString, "", "DELETE");
    }

    /**
     * Execute a request with a body
     */
    private @Nullable String executeRequestWithBody(String urlString, String json, String method) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(TIMEOUT_MILLISECONDS);
            connection.setReadTimeout(TIMEOUT_MILLISECONDS);
            connection.setDoOutput(true);

            if (!json.isEmpty()) {
                connection.getOutputStream().write(json.getBytes());
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                return readResponse(connection);
            } else {
                logger.debug("{} request failed with response code: {}", method, responseCode);
            }
        } catch (Exception e) {
            logger.debug("Error executing {} request: {}", method, e.getMessage());
        }
        return null;
    }

    /**
     * Read the response from a connection
     */
    private String readResponse(HttpURLConnection connection) throws IOException {
        try (InputStream inputStream = connection.getInputStream()) {
            return new String(inputStream.readAllBytes());
        }
    }

    private void startPolling() {
        ScheduledFuture<?> localPollingJob = pollingJob;
        if (localPollingJob == null || localPollingJob.isCancelled()) {
            pollingJob = null; // You'll need to implement proper scheduling
        }
    }

    private void stopPolling() {
        ScheduledFuture<?> localPollingJob = pollingJob;
        if (localPollingJob != null && !localPollingJob.isCancelled()) {
            localPollingJob.cancel(true);
            pollingJob = null;
        }
    }
}
