package org.openhab.binding.rachio.internal.api;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.dto.RachioPerson;
import org.openhab.binding.rachio.internal.api.dto.RachioDevice;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.api.dto.RachioEventSummary;
import org.openhab.binding.rachio.internal.api.dto.RachioWebhookEvent;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

/**
 * Professional-grade HTTP client for Rachio API with rate limiting, retry logic, and error handling
 */
@Component(service = RachioHttp.class)
@NonNullByDefault
public class RachioHttp {
    private final Logger logger = LoggerFactory.getLogger(RachioHttp.class);
    
    // JSON parser with proper configuration
    private final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .create();
    
    // API configuration
    private static final String BASE_URL = "https://api.rach.io/1/public";
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;
    private static final int REQUEST_TIMEOUT_MS = 30000;
    
    // Rate limiting - Rachio allows 1700 requests per hour
    private static final int RATE_LIMIT_REQUESTS = 1500; // Conservative limit
    private static final Duration RATE_LIMIT_PERIOD = Duration.ofHours(1);
    private final AtomicInteger requestCount = new AtomicInteger(0);
    private Instant rateLimitWindowStart = Instant.now();
    private final Object rateLimitLock = new Object();
    
    // API key management
    private final Map<String, String> thingApiKeys = new ConcurrentHashMap<>();
    
    // Circuit breaker state
    private volatile boolean circuitOpen = false;
    private volatile Instant circuitOpenedAt;
    private static final Duration CIRCUIT_TIMEOUT = Duration.ofMinutes(1);
    
    // Executor for background tasks
    private final ScheduledExecutorService scheduler;
    private @Nullable ScheduledFuture<?> cleanupTask;

    @Activate
    public RachioHttp() {
        this.scheduler = java.util.concurrent.Executors.newScheduledThreadPool(1);
        startCleanupTask();
        logger.info("RachioHttp service activated");
    }

    @Deactivate
    public void deactivate() {
        ScheduledFuture<?> task = cleanupTask;
        if (task != null) {
            task.cancel(true);
        }
        scheduler.shutdown();
        logger.info("RachioHttp service deactivated");
    }

    /**
     * Register API key for a specific thing
     */
    public void registerThing(String thingId, String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("API key cannot be null or empty");
        }
        if (apiKey.length() != 36) {
            logger.warn("API key for thing {} may be invalid (expected 36 characters, got {})", 
                       thingId, apiKey.length());
        }
        
        thingApiKeys.put(thingId, apiKey);
        logger.debug("Registered API key for thing: {}", thingId);
    }

    /**
     * Unregister API key for a thing
     */
    public void unregisterThing(String thingId) {
        thingApiKeys.remove(thingId);
        logger.debug("Unregistered API key for thing: {}", thingId);
    }

    /**
     * Get person information (primary API call for authentication and device discovery)
     */
    public @Nullable RachioPerson getPerson(String thingId) throws RachioException {
        return executeWithRetry(thingId, "/person/info", RachioPerson.class);
    }

    /**
     * Get device details
     */
    public @Nullable RachioDevice getDevice(String thingId, String deviceId) throws RachioException {
        return executeWithRetry(thingId, "/device/" + deviceId, RachioDevice.class);
    }

    /**
     * Get device event summary
     */
    public @Nullable RachioEventSummary getDeviceEventSummary(String thingId, String deviceId) throws RachioException {
        return executeWithRetry(thingId, "/device/" + deviceId + "/event", RachioEventSummary.class);
    }

    /**
     * Start a zone
     */
    public void startZone(String thingId, String zoneId, int durationSeconds) throws RachioException {
        if (durationSeconds <= 0 || durationSeconds > 3600) {
            throw new IllegalArgumentException("Duration must be between 1 and 3600 seconds");
        }
        
        String payload = String.format("{\"id\":\"%s\",\"duration\":%d}", zoneId, durationSeconds);
        executeWithRetry(thingId, "/zone/start", "PUT", payload, Object.class);
        logger.debug("Started zone {} for {} seconds", zoneId, durationSeconds);
    }

    /**
     * Stop watering
     */
    public void stopWatering(String thingId, String deviceId) throws RachioException {
        executeWithRetry(thingId, "/device/" + deviceId + "/stop", "PUT", "{}", Object.class);
        logger.debug("Stopped watering for device {}", deviceId);
    }

    /**
     * Set zone enabled state
     */
    public void setZoneEnabled(String thingId, String zoneId, boolean enabled) throws RachioException {
        String payload = String.format("{\"id\":\"%s\",\"enabled\":%s}", zoneId, enabled);
        executeWithRetry(thingId, "/zone/enable", "PUT", payload, Object.class);
        logger.debug("Set zone {} enabled: {}", zoneId, enabled);
    }

    /**
     * Get image data (for image servlet)
     */
    public byte @Nullable [] getImage(String thingId, String imageId) throws RachioException {
        HttpURLConnection connection = null;
        try {
            checkRateLimit();
            checkCircuitBreaker();
            
            String apiKey = getApiKey(thingId);
            URL url = new URL(BASE_URL + "/image/" + imageId);
            connection = (HttpURLConnection) url.openConnection();
            configureConnection(connection, "GET", apiKey);
            
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (var inputStream = connection.getInputStream()) {
                    return inputStream.readAllBytes();
                }
            } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                return null;
            } else {
                handleErrorResponse(connection, responseCode);
                return null;
            }
        } catch (Exception e) {
            handleException(e);
            throw new RachioException("Failed to fetch image: " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    // ========== PRIVATE IMPLEMENTATION ==========

    private <T> @Nullable T executeWithRetry(String thingId, String endpoint, Class<T> responseType) 
            throws RachioException {
        return executeWithRetry(thingId, endpoint, "GET", null, responseType);
    }

    private <T> @Nullable T executeWithRetry(String thingId, String endpoint, String method, 
                                           @Nullable String payload, Class<T> responseType) 
            throws RachioException {
        RachioException lastException = null;
        
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return executeRequest(thingId, endpoint, method, payload, responseType);
            } catch (RachioException e) {
                lastException = e;
                
                // Don't retry on client errors (4xx) except 429 (rate limit)
                if (e.getStatusCode() != null && 
                    e.getStatusCode() >= 400 && 
                    e.getStatusCode() < 500 && 
                    e.getStatusCode() != 429) {
                    break;
                }
                
                // Don't retry if we're out of retries
                if (attempt == MAX_RETRIES) {
                    break;
                }
                
                // Exponential backoff
                long delay = RETRY_DELAY_MS * (long) Math.pow(2, attempt - 1);
                logger.debug("Request failed (attempt {}/{}), retrying in {} ms: {}", 
                           attempt, MAX_RETRIES, delay, e.getMessage());
                
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RachioException("Request interrupted", ie);
                }
            }
        }
        
        // If we get here, all retries failed
        if (lastException != null) {
            // Open circuit breaker on persistent failures
            if (lastException.getStatusCode() == null || lastException.getStatusCode() >= 500) {
                openCircuitBreaker();
            }
            throw lastException;
        }
        
        throw new RachioException("Request failed after " + MAX_RETRIES + " attempts");
    }

    private <T> @Nullable T executeRequest(String thingId, String endpoint, String method,
                                         @Nullable String payload, Class<T> responseType) 
            throws RachioException {
        HttpURLConnection connection = null;
        try {
            checkRateLimit();
            checkCircuitBreaker();
            
            String apiKey = getApiKey(thingId);
            URL url = new URL(BASE_URL + endpoint);
            connection = (HttpURLConnection) url.openConnection();
            configureConnection(connection, method, apiKey);
            
            // Send payload for PUT/POST requests
            if (payload != null && (method.equals("PUT") || method.equals("POST"))) {
                connection.setDoOutput(true);
                try (var outputStream = connection.getOutputStream()) {
                    outputStream.write(payload.getBytes());
                    outputStream.flush();
                }
            }
            
            int responseCode = connection.getResponseCode();
            incrementRequestCount();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (var reader = new InputStreamReader(connection.getInputStream())) {
                    return gson.fromJson(reader, responseType);
                }
            } else if (responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                return null; // Successful but no content
            } else {
                handleErrorResponse(connection, responseCode);
                return null;
            }
        } catch (JsonSyntaxException e) {
            throw new RachioException("Invalid JSON response from Rachio API", e);
        } catch (IOException e) {
            handleException(e);
            throw new RachioException("Network error communicating with Rachio API: " + e.getMessage(), e);
        } catch (Exception e) {
            handleException(e);
            throw new RachioException("Unexpected error: " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void configureConnection(HttpURLConnection connection, String method, String apiKey) 
            throws IOException {
        connection.setRequestMethod(method);
        connection.setConnectTimeout(REQUEST_TIMEOUT_MS);
        connection.setReadTimeout(REQUEST_TIMEOUT_MS);
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", "openHAB-Rachio-Binding/5.0.1");
        connection.setUseCaches(false);
        connection.setDoInput(true);
    }

    private void checkRateLimit() throws RachioException {
        synchronized (rateLimitLock) {
            // Reset counter if we're in a new window
            if (Duration.between(rateLimitWindowStart, Instant.now()).compareTo(RATE_LIMIT_PERIOD) > 0) {
                requestCount.set(0);
                rateLimitWindowStart = Instant.now();
            }
            
            // Check if we're over limit
            if (requestCount.get() >= RATE_LIMIT_REQUESTS) {
                throw new RachioException("Rate limit exceeded (" + RATE_LIMIT_REQUESTS + 
                                        " requests per " + RATE_LIMIT_PERIOD + ")");
            }
        }
    }

    private void incrementRequestCount() {
        requestCount.incrementAndGet();
    }

    private void checkCircuitBreaker() throws RachioException {
        if (circuitOpen) {
            if (Duration.between(circuitOpenedAt, Instant.now()).compareTo(CIRCUIT_TIMEOUT) > 0) {
                // Timeout expired, try again
                circuitOpen = false;
                logger.info("Circuit breaker closed after timeout");
            } else {
                throw new RachioException("Circuit breaker open - API temporarily unavailable");
            }
        }
    }

    private void openCircuitBreaker() {
        if (!circuitOpen) {
            circuitOpen = true;
            circuitOpenedAt = Instant.now();
            logger.warn("Circuit breaker opened due to persistent failures");
        }
    }

    private String getApiKey(String thingId) throws RachioException {
        String apiKey = thingApiKeys.get(thingId);
        if (apiKey == null) {
            throw new RachioException("No API key registered for thing: " + thingId);
        }
        return apiKey;
    }

    private void handleErrorResponse(HttpURLConnection connection, int responseCode) throws RachioException {
        String errorMessage = "HTTP " + responseCode;
        try {
            if (connection.getErrorStream() != null) {
                try (var reader = new InputStreamReader(connection.getErrorStream())) {
                    var errorResponse = gson.fromJson(reader, Object.class);
                    if (errorResponse != null) {
                        errorMessage += " - " + errorResponse.toString();
                    }
                }
            }
        } catch (Exception e) {
            // Ignore - we'll use the basic error message
        }
        
        switch (responseCode) {
            case 401:
                throw new RachioException("Authentication failed - check API key", responseCode);
            case 403:
                throw new RachioException("Access forbidden", responseCode);
            case 404:
                throw new RachioException("Resource not found", responseCode);
            case 429:
                throw new RachioException("Rate limit exceeded", responseCode);
            case 500:
            case 502:
            case 503:
                throw new RachioException("Rachio API service unavailable", responseCode);
            default:
                throw new RachioException("API request failed: " + errorMessage, responseCode);
        }
    }

    private void handleException(Exception e) {
        logger.debug("API request exception: {}", e.getMessage(), e);
    }

    private void startCleanupTask() {
        cleanupTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                // Reset rate limit counter if window expired
                synchronized (rateLimitLock) {
                    if (Duration.between(rateLimitWindowStart, Instant.now()).compareTo(RATE_LIMIT_PERIOD) > 0) {
                        requestCount.set(0);
                        rateLimitWindowStart = Instant.now();
                        logger.debug("Rate limit counter reset");
                    }
                }
                
                // Close circuit breaker if timeout expired
                if (circuitOpen && 
                    Duration.between(circuitOpenedAt, Instant.now()).compareTo(CIRCUIT_TIMEOUT) > 0) {
                    circuitOpen = false;
                    logger.info("Circuit breaker closed by cleanup task");
                }
            } catch (Exception e) {
                logger.debug("Cleanup task error: {}", e.getMessage());
            }
        }, 5, 5, TimeUnit.MINUTES); // Run every 5 minutes
    }
}
