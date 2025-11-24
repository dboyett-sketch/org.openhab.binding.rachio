package org.openhab.binding.rachio.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.net.http.HttpClientFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Rachio API Client
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioApi {
    private final RachioHttp rachioHttp;

    public RachioApi(HttpClientFactory httpClientFactory, String apiKey, Gson gson) {
        this.rachioHttp = new RachioHttp(httpClientFactory, apiKey, gson);
    }

    public JsonObject getPersonInfo() throws RachioApiException {
        return rachioHttp.executeGet("public/person/info");
    }

    public JsonObject getDevice(String deviceId) throws RachioApiException {
        return rachioHttp.executeGet("public/device/" + deviceId);
    }

    public JsonObject getDeviceState(String deviceId) throws RachioApiException {
        return rachioHttp.executeGet("public/device/" + deviceId + "/current_schedule");
    }

    public void startZone(String deviceId, int zoneId, int duration) throws RachioApiException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("id", deviceId);
        requestBody.addProperty("zoneId", zoneId);
        requestBody.addProperty("duration", duration);

        rachioHttp.executePut("public/zone/start", requestBody, JsonObject.class);
    }

    public void stopWatering(String deviceId) throws RachioApiException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("id", deviceId);

        rachioHttp.executePut("public/zone/stop", requestBody, JsonObject.class);
    }

    public static class RachioApiResult {
        // This inner class can be removed since we now have the standalone class
    }
}
