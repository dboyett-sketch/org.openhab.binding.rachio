package org.openhab.binding.rachio.internal.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Rachio HTTP Client for API calls
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioHttp {
    private final Logger logger = LoggerFactory.getLogger(RachioHttp.class);

    private static final String RACHIO_API_URL = "https://api.rach.io/1/";
    private static final int TIMEOUT_SECONDS = 30;

    private final HttpClientFactory httpClientFactory;
    private final String apiKey;
    private final Gson gson;

    public RachioHttp(HttpClientFactory httpClientFactory, String apiKey, Gson gson) {
        this.httpClientFactory = httpClientFactory;
        this.apiKey = apiKey;
        this.gson = gson;
    }

    public <T> T executeGet(String endpoint, Class<T> responseType) throws RachioApiException {
        try {
            HttpClient client = httpClientFactory.getCommonHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(RACHIO_API_URL + endpoint))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                throw new RachioApiException(new RachioApiResult(false,
                        "HTTP error " + response.statusCode() + ": " + response.body()));
            }

            return gson.fromJson(response.body(), responseType);
        } catch (URISyntaxException e) {
            throw new RachioApiException(new RachioApiResult(false, "Invalid URI: " + e.getMessage()));
        } catch (IOException e) {
            throw new RachioApiException(new RachioApiResult(false, "IO error: " + e.getMessage()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RachioApiException(new RachioApiResult(false, "Request interrupted: " + e.getMessage()));
        } catch (Exception e) {
            throw new RachioApiException(new RachioApiResult(false, "Unexpected error: " + e.getMessage()));
        }
    }

    public <T> T executePut(String endpoint, Object requestBody, Class<T> responseType) throws RachioApiException {
        try {
            String jsonBody = gson.toJson(requestBody);

            HttpClient client = httpClientFactory.getCommonHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(RACHIO_API_URL + endpoint))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                throw new RachioApiException(new RachioApiResult(false,
                        "HTTP error " + response.statusCode() + ": " + response.body()));
            }

            return gson.fromJson(response.body(), responseType);
        } catch (URISyntaxException e) {
            throw new RachioApiException(new RachioApiResult(false, "Invalid URI: " + e.getMessage()));
        } catch (IOException e) {
            throw new RachioApiException(new RachioApiResult(false, "IO error: " + e.getMessage()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RachioApiException(new RachioApiResult(false, "Request interrupted: " + e.getMessage()));
        } catch (Exception e) {
            throw new RachioApiException(new RachioApiResult(false, "Unexpected error: " + e.getMessage()));
        }
    }

    // Helper method for backward compatibility
    public JsonObject executeGet(String endpoint) throws RachioApiException {
        return executeGet(endpoint, JsonObject.class);
    }

    @Nullable
    public InputStream executeGetStream(String endpoint) throws RachioApiException {
        try {
            HttpClient client = httpClientFactory.getCommonHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(RACHIO_API_URL + endpoint))
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .GET()
                    .build();

            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                throw new RachioApiException(new RachioApiResult(false,
                        "HTTP error " + response.statusCode() + " for stream request"));
            }

            return response.body();
        } catch (URISyntaxException e) {
            throw new RachioApiException(new RachioApiResult(false, "Invalid URI: " + e.getMessage()));
        } catch (IOException e) {
            throw new RachioApiException(new RachioApiResult(false, "IO error: " + e.getMessage()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RachioApiException(new RachioApiResult(false, "Request interrupted: " + e.getMessage()));
        } catch (Exception e) {
            throw new RachioApiException(new RachioApiResult(false, "Unexpected error: " + e.getMessage()));
        }
    }
}
