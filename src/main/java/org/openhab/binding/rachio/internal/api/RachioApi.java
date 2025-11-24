package org.openhab.binding.rachio.internal.api;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.ThingUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The {@link RachioApi} class provides high-level API operations for Rachio
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioApi {
    private final Logger logger = LoggerFactory.getLogger(RachioApi.class);

    private final RachioHttp httpApi;
    private final Gson gson;
    private static final String API_BASE_URL = "https://api.rach.io/1/public";

    public RachioApi(RachioHttp httpApi) {
        this.httpApi = httpApi;
        this.gson = new Gson();
    }

    public @Nullable RachioPerson getPersonInfo() {
        try {
            // FIXED: Remove RachioApiResult wrapper - directly return String
            String response = httpApi.httpGet(API_BASE_URL + "/person/info", "application/json");
            if (response != null && !response.isEmpty()) {
                return gson.fromJson(response, RachioPerson.class);
            }
        } catch (Exception e) {
            logger.debug("Error getting person info: {}", e.getMessage());
        }
        return null;
    }

    public @Nullable List<RachioDevice> getDevices() {
        try {
            // FIXED: Remove RachioApiResult wrapper - directly return String
            String response = httpApi.httpGet(API_BASE_URL + "/person/info", "application/json");
            if (response != null && !response.isEmpty()) {
                RachioPerson person = gson.fromJson(response, RachioPerson.class);
                if (person != null && person.getDevices() != null) {
                    return person.getDevices();
                }
            }
        } catch (Exception e) {
            logger.debug("Error getting devices: {}", e.getMessage());
        }
        return new ArrayList<>();
    }

    public @Nullable RachioDevice getDevice(String deviceId) {
        try {
            // FIXED: Remove RachioApiResult wrapper - directly return String
            String response = httpApi.httpGet(API_BASE_URL + "/device/" + deviceId, "application/json");
            if (response != null && !response.isEmpty()) {
                return gson.fromJson(response, RachioDevice.class);
            }
        } catch (Exception e) {
            logger.debug("Error getting device {}: {}", deviceId, e.getMessage());
        }
        return null;
    }

    // FIXED: Completely rewrote this method to remove invalid constructor and field references
    public @Nullable List<RachioDevice> getDevices(ThingUID bridgeUID) {
        List<RachioDevice> devices = new ArrayList<>();
        try {
            RachioPerson person = getPersonInfo();
            if (person != null && person.getDevices() != null) {
                for (RachioDevice cloudDevice : person.getDevices()) {
                    // FIXED: Create device using simple field copying instead of invalid constructor
                    RachioDevice device = new RachioDevice();
                    // Copy all fields from cloudDevice
                    if (cloudDevice.getId() != null) {
                        device.setId(cloudDevice.getId());
                    }
                    if (cloudDevice.getName() != null) {
                        device.setName(cloudDevice.getName());
                    }
                    if (cloudDevice.getStatus() != null) {
                        device.setStatus(cloudDevice.getStatus());
                    }
                    if (cloudDevice.getSerialNumber() != null) {
                        device.setSerialNumber(cloudDevice.getSerialNumber());
                    }
                    if (cloudDevice.getModel() != null) {
                        device.setModel(cloudDevice.getModel());
                    }
                    if (cloudDevice.getMacAddress() != null) {
                        device.setMacAddress(cloudDevice.getMacAddress());
                    }
                    if (cloudDevice.getZones() != null) {
                        device.setZones(cloudDevice.getZones());
                    }
                    devices.add(device);
                }
            }
        } catch (Exception e) {
            logger.debug("Error getting devices for bridge {}: {}", bridgeUID, e.getMessage());
        }
        return devices;
    }

    // FIXED: Completely rewrote this method to remove invalid method calls and field references
    public @Nullable List<RachioZone> getZones(String deviceId, ThingUID bridgeUID, ThingUID deviceUID) {
        List<RachioZone> zones = new ArrayList<>();
        try {
            RachioDevice device = getDevice(deviceId);
            if (device != null && device.getZones() != null) {
                // FIXED: Use proper list iteration instead of invalid values() method
                for (RachioZone zone : device.getZones()) {
                    // FIXED: Remove invalid getUID() call and field references
                    // Just add the zone as-is without modification
                    zones.add(zone);
                }
            }
        } catch (Exception e) {
            logger.debug("Error getting zones for device {}: {}", deviceId, e.getMessage());
        }
        return zones;
    }

    public boolean startZone(String zoneId, int duration) {
        try {
            String response = httpApi.runZone(zoneId, duration);
            return response != null && !response.isEmpty();
        } catch (Exception e) {
            logger.debug("Error starting zone {}: {}", zoneId, e.getMessage());
            return false;
        }
    }

    public boolean stopWatering(String deviceId) {
        try {
            String response = httpApi.stopWatering(deviceId);
            return response != null && !response.isEmpty();
        } catch (Exception e) {
            logger.debug("Error stopping watering on device {}: {}", deviceId, e.getMessage());
            return false;
        }
    }

    public boolean setRainDelay(String deviceId, int duration) {
        try {
            String response = httpApi.rainDelay(deviceId, duration);
            return response != null && !response.isEmpty();
        } catch (Exception e) {
            logger.debug("Error setting rain delay on device {}: {}", deviceId, e.getMessage());
            return false;
        }
    }

    public boolean startMultipleZones(String deviceId, List<String> zoneIds, List<Integer> durations) {
        try {
            // FIXED: Remove resultString reference
            JsonObject request = new JsonObject();
            request.addProperty("deviceId", deviceId);
            
            JsonArray zonesArray = new JsonArray();
            for (int i = 0; i < zoneIds.size(); i++) {
                JsonObject zoneObj = new JsonObject();
                zoneObj.addProperty("id", zoneIds.get(i));
                zoneObj.addProperty("duration", durations.get(i));
                zonesArray.add(zoneObj);
            }
            request.add("zones", zonesArray);
            
            String jsonData = gson.toJson(request);
            String response = httpApi.httpPut(API_BASE_URL + "/zone/start_multiple", jsonData);
            return response != null && !response.isEmpty();
        } catch (Exception e) {
            logger.debug("Error starting multiple zones on device {}: {}", deviceId, e.getMessage());
            return false;
        }
    }

    public @Nullable String getWebhook(String webhookId) {
        try {
            // FIXED: Remove resultString reference
            return httpApi.httpGet(API_BASE_URL + "/webhook/" + webhookId, "application/json");
        } catch (Exception e) {
            logger.debug("Error getting webhook {}: {}", webhookId, e.getMessage());
            return null;
        }
    }

    public boolean deleteWebhook(String webhookId) {
        try {
            // FIXED: Remove resultString reference
            String response = httpApi.httpDelete(API_BASE_URL + "/webhook/" + webhookId, "application/json");
            return response != null && !response.isEmpty();
        } catch (Exception e) {
            logger.debug("Error deleting webhook {}: {}", webhookId, e.getMessage());
            return false;
        }
    }

    public @Nullable String createWebhook(String url, String externalId) {
        try {
            // FIXED: Remove resultString reference
            return httpApi.createWebhook(url, externalId);
        } catch (Exception e) {
            logger.debug("Error creating webhook: {}", e.getMessage());
            return null;
        }
    }

    public @Nullable String getDeviceState(String deviceId) {
        try {
            // FIXED: Remove resultString reference
            return httpApi.httpGet(API_BASE_URL + "/device/" + deviceId + "/state", "application/json");
        } catch (Exception e) {
            logger.debug("Error getting device state for {}: {}", deviceId, e.getMessage());
            return null;
        }
    }

    public boolean runAllZones(String deviceId, int duration) {
        try {
            String response = httpApi.runAllZones(deviceId, duration);
            return response != null && !response.isEmpty();
        } catch (Exception e) {
            logger.debug("Error running all zones on device {}: {}", deviceId, e.getMessage());
            return false;
        }
    }

    public boolean runNextZone(String deviceId, int duration) {
        try {
            String response = httpApi.runNextZone(deviceId, duration);
            return response != null && !response.isEmpty();
        } catch (Exception e) {
            logger.debug("Error running next zone on device {}: {}", deviceId, e.getMessage());
            return false;
        }
    }
}
