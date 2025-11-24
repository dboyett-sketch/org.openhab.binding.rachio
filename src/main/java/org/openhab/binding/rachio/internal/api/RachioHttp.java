package org.openhab.binding.rachio.internal.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.handler.RachioBridgeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * The {@link RachioHttp} class is responsible for HTTP communications with the Rachio Cloud API
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioHttp {
    private final Logger logger = LoggerFactory.getLogger(RachioHttp.class);

    private static final String API_URL = "https://api.rach.io/1/public";
    private final String apiKey;
    private final Gson gson;

    public RachioHttp(String apiKey) {
        this.apiKey = apiKey;
        this.gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").create();
    }

    public String httpRequest(String method, String urlString, @Nullable String data, String contentType) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Content-Type", contentType);
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            if (data != null && !data.isEmpty()) {
                connection.setDoOutput(true);
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = data.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }

            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    return response.toString();
                }
            } else {
                logger.debug("HTTP {} request failed with status code: {}", method, responseCode);
                // Read error stream if available
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        errorResponse.append(responseLine.trim());
                    }
                    logger.debug("Error response: {}", errorResponse.toString());
                }
            }
        } catch (IOException e) {
            logger.debug("HTTP {} request failed: {}", method, e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return "";
    }

    // ADDED: Missing HTTP methods
    public String httpGet(String url, String contentType) {
        return httpRequest("GET", url, null, contentType);
    }

    public String httpPut(String url, String jsonData) {
        return httpRequest("PUT", url, jsonData, "application/json");
    }

    public String httpPost(String url, String jsonData) {
        return httpRequest("POST", url, jsonData, "application/json");
    }

    public String httpDelete(String url, String contentType) {
        return httpRequest("DELETE", url, null, contentType);
    }

    // ADDED: Missing getPerson method
    public RachioPerson getPerson() {
        String response = httpGet(API_URL + "/person/info", "application/json");
        return gson.fromJson(response, RachioPerson.class);
    }

    public RachioDevice getDevice(String deviceId) {
        String response = httpGet(API_URL + "/device/" + deviceId, "application/json");
        return gson.fromJson(response, RachioDevice.class);
    }

    public RachioZone getZone(String zoneId) {
        String response = httpGet(API_URL + "/zone/" + zoneId, "application/json");
        return gson.fromJson(response, RachioZone.class);
    }

    public String startZone(String zoneId, int duration) {
        String jsonData = "{\"id\":\"" + zoneId + "\",\"duration\":" + duration + "}";
        return httpPut(API_URL + "/zone/start", jsonData);
    }

    public String stopWatering(String deviceId) {
        String jsonData = "{\"id\":\"" + deviceId + "\"}";
        return httpPut(API_URL + "/device/stop_water", jsonData);
    }

    public String rainDelay(String deviceId, int duration) {
        String jsonData = "{\"id\":\"" + deviceId + "\",\"duration\":" + duration + "}";
        return httpPut(API_URL + "/device/rain_delay", jsonData);
    }

    public String createWebhook(String url, String externalId) {
        String jsonData = "{\"url\":\"" + url + "\",\"externalId\":\"" + externalId + "\",\"eventTypes\":[\"DEVICE_STATUS_EVENT\",\"ZONE_STATUS_EVENT\",\"SCHEDULE_STATUS_EVENT\"]}";
        return httpPost(API_URL + "/webhook", jsonData);
    }

    public String deleteWebhook(String webhookId) {
        return httpDelete(API_URL + "/webhook/" + webhookId, "application/json");
    }

    public RachioWebhook getWebhook(String webhookId) {
        String response = httpGet(API_URL + "/webhook/" + webhookId, "application/json");
        return gson.fromJson(response, RachioWebhook.class);
    }

    public String getDeviceState(String deviceId) {
        return httpGet(API_URL + "/device/" + deviceId + "/state", "application/json");
    }

    public String getDeviceSchedule(String deviceId) {
        return httpGet(API_URL + "/device/" + deviceId + "/schedule", "application/json");
    }

    public String getDeviceForecast(String deviceId) {
        return httpGet(API_URL + "/device/" + deviceId + "/forecast", "application/json");
    }

    public String getZoneSchedule(String zoneId) {
        return httpGet(API_URL + "/zone/" + zoneId + "/schedule", "application/json");
    }

    public String getZoneForecast(String zoneId) {
        return httpGet(API_URL + "/zone/" + zoneId + "/forecast", "application/json");
    }

    public String setZoneSchedule(String zoneId, String scheduleJson) {
        return httpPut(API_URL + "/zone/" + zoneId + "/schedule", scheduleJson);
    }

    public String setZoneForecast(String zoneId, String forecastJson) {
        return httpPut(API_URL + "/zone/" + zoneId + "/forecast", forecastJson);
    }

    public String setDeviceSchedule(String deviceId, String scheduleJson) {
        return httpPut(API_URL + "/device/" + deviceId + "/schedule", scheduleJson);
    }

    public String setDeviceForecast(String deviceId, String forecastJson) {
        return httpPut(API_URL + "/device/" + deviceId + "/forecast", forecastJson);
    }

    public String setDeviceState(String deviceId, String stateJson) {
        return httpPut(API_URL + "/device/" + deviceId + "/state", stateJson);
    }

    public String setZoneState(String zoneId, String stateJson) {
        return httpPut(API_URL + "/zone/" + zoneId + "/state", stateJson);
    }
}
