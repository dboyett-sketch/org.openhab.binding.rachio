package org.openhab.binding.rachio.internal.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Rachio HTTP Client for API calls
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioHttp {
    private final Logger logger = LoggerFactory.getLogger(RachioHttp.class);

    private static final String RACHIO_API_URL = "https://api.rach.io/1/public/";
    private static final int TIMEOUT_SECONDS = 30;

    private final String apiKey;
    private final Gson gson;
    private final HttpClient httpClient;

    public RachioHttp(String apiKey, Gson gson) {
        this.apiKey = apiKey;
        this.gson = gson;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
    }

    public JsonObject httpGet(String endpoint) throws RachioApiException {
        return httpGet(endpoint, JsonObject.class);
    }

    public <T> T httpGet(String endpoint, Class<T> responseType) throws RachioApiException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(RACHIO_API_URL + endpoint))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                throw new RachioApiException("HTTP error " + response.statusCode() + ": " + response.body());
            }

            return gson.fromJson(response.body(), responseType);
        } catch (URISyntaxException e) {
            throw new RachioApiException("Invalid URI: " + e.getMessage(), e);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RachioApiException("Request failed: " + e.getMessage(), e);
        }
    }

    public JsonObject httpPut(String endpoint, String body) throws RachioApiException {
        return httpPut(endpoint, body, JsonObject.class);
    }

    public <T> T httpPut(String endpoint, String body, Class<T> responseType) throws RachioApiException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(RACHIO_API_URL + endpoint))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .PUT(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                throw new RachioApiException("HTTP error " + response.statusCode() + ": " + response.body());
            }

            return gson.fromJson(response.body(), responseType);
        } catch (URISyntaxException e) {
            throw new RachioApiException("Invalid URI: " + e.getMessage(), e);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RachioApiException("Request failed: " + e.getMessage(), e);
        }
    }

    public JsonObject httpPost(String endpoint, String body) throws RachioApiException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(RACHIO_API_URL + endpoint))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                throw new RachioApiException("HTTP error " + response.statusCode() + ": " + response.body());
            }

            return gson.fromJson(response.body(), JsonObject.class);
        } catch (URISyntaxException e) {
            throw new RachioApiException("Invalid URI: " + e.getMessage(), e);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RachioApiException("Request failed: " + e.getMessage(), e);
        }
    }

    public void httpDelete(String endpoint, String body) throws RachioApiException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(RACHIO_API_URL + endpoint))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .method("DELETE", HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                throw new RachioApiException("HTTP error " + response.statusCode() + ": " + response.body());
            }
        } catch (URISyntaxException e) {
            throw new RachioApiException("Invalid URI: " + e.getMessage(), e);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RachioApiException("Request failed: " + e.getMessage(), e);
        }
    }

    // API-specific methods
    public JsonObject getPerson() throws RachioApiException {
        return httpGet("person/info");
    }

    public JsonObject getDevice(String deviceId) throws RachioApiException {
        return httpGet("device/" + deviceId);
    }

    public JsonObject getZone(String zoneId) throws RachioApiException {
        return httpGet("zone/" + zoneId);
    }

    public void runZone(String deviceId, int duration) throws RachioApiException {
        String body = String.format("{\"id\":\"%s\",\"duration\":%d}", deviceId, duration);
        httpPut("zone/start", body);
    }

    public void stopWatering(String deviceId) throws RachioApiException {
        String body = String.format("{\"id\":\"%s\"}", deviceId);
        httpPut("zone/stop", body);
    }

    public void runAllZones(String deviceId, int duration) throws RachioApiException {
        String body = String.format("{\"id\":\"%s\",\"duration\":%d}", deviceId, duration);
        httpPut("zone/start", body);
    }

    public void runNextZone(String deviceId, int duration) throws RachioApiException {
        String body = String.format("{\"id\":\"%s\",\"duration\":%d}", deviceId, duration);
        httpPut("zone/start", body);
    }

    public void rainDelay(String deviceId, int duration) throws RachioApiException {
        String body = String.format("{\"id\":\"%s\",\"duration\":%d}", deviceId, duration);
        httpPut("device/rain_delay", body);
    }

    @Nullable
    public InputStream getImageStream(String imageUrl) throws RachioApiException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(imageUrl))
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .GET()
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                throw new RachioApiException("HTTP error " + response.statusCode() + " for image request");
            }

            return response.body();
        } catch (URISyntaxException e) {
            throw new RachioApiException("Invalid image URI: " + e.getMessage(), e);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RachioApiException("Image request failed: " + e.getMessage(), e);
        }
    }
}
