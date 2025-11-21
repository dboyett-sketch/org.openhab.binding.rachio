package org.openhab.binding.rachio.internal.api;

import static org.openhab.binding.rachio.RachioBindingConstants.*;

import java.time.Instant;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.MessageDigest;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class RachioApi {
    private static final Logger logger = LoggerFactory.getLogger(RachioApi.class);
    private static final String MD5_HASH_ALGORITHM = "MD5";
    private static final String UTF8_CHAR_SET = "UTF-8";

    private static final Gson gson = new GsonBuilder()
        .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC)
        .disableHtmlEscaping()
        .serializeNulls()
        .registerTypeAdapterFactory(new SafeReflectiveTypeAdapterFactory())
        .registerTypeAdapter(Instant.class, new InstantTypeAdapter()) // ← Add this line
        .create();

    public static class RachioApiResult {
        private final Logger logger = LoggerFactory.getLogger(RachioApiResult.class);

        public String requestMethod = "";
        public String url = "";
        public String apikey = "";
        public Integer responseCode = 0;
        public String resultString = "";

        public Integer apiCalls = 0;
        public Integer rateLimit = 0;
        public Integer rateRemaining = 0;
        public String rateReset = "";

        public void setRateLimit(int rateLimit, int rateRemaining, String rateReset) {
            this.rateLimit = rateLimit;
            this.rateRemaining = rateRemaining;
            this.rateReset = rateReset;

            if (this.rateLimit == 0 || this.rateRemaining == 0) {
                return;
            }

            if (isRateLimitCritical()) {
                logger.error("RachioApi: API calls nearing critical limit: limit={}, remaining={}, reset={}",
                        rateLimit, rateRemaining, rateReset);
            } else if (isRateLimitWarning()) {
                logger.info("RachioApi: API calls nearing warning threshold: limit={}, remaining={}, reset={}",
                        rateLimit, rateRemaining, rateReset);
            } else {
                logger.trace("RachioApi: API usage: limit={}, remaining={}, reset={}",
                        rateLimit, this.rateRemaining, this.rateReset);
            }
        }

        public boolean isRateLimitWarning() {
            return rateRemaining > 0 && rateRemaining < RACHIO_RATE_LIMIT_WARNING;
        }

        public boolean isRateLimitCritical() {
            return rateRemaining > 0 && rateRemaining <= RACHIO_RATE_LIMIT_CRITICAL;
        }

        public boolean isRateLimitBlocked() {
            return rateRemaining > 0 && rateRemaining <= RACHIO_RATE_LIMIT_BLOCK;
        }
    }

    protected String apikey = "";
    protected String personId = "";
    protected String userName = "";
    protected String fullName = "";
    protected String email = "";

    protected RachioApiResult lastApiResult = new RachioApiResult();
    protected static final Integer externalIdSalt = (int) (Math.random() * 50 + 1);

    private HashMap<String, RachioDevice> deviceList = new HashMap<>();
    private RachioHttp httpApi = null;

    public RachioApi(String personId) {
        this.personId = personId;
    }

    public RachioApiResult getLastApiResult() {
        return lastApiResult;
    }

    protected void setApiResult(RachioApiResult result) {
        lastApiResult = result;
    }

    public String getPersonId() {
        return personId;
    }

    public String getExternalId() {
        String hash = "OH_" + getMD5Hash(apikey) + "_" + externalIdSalt;
        return getMD5Hash(hash);
    }
        public boolean initialize(String apikey, ThingUID bridgeUID) throws RachioApiException {
        this.apikey = apikey;
        httpApi = new RachioHttp(this.apikey);

        if (initializePersonId() && initializeDevices(bridgeUID) && initializeZones()) {
            logger.trace("Rachio API initialized");
            return true;
        }

        httpApi = null;
        logger.error("RachioApi.initialize(): API initialization failed!");
        return false;
    }

    private Boolean initializePersonId() throws RachioApiException {
        if (!personId.isEmpty()) {
            logger.trace("RachioApi: Using cached personId '{}'", personId);
            return true;
        }

        lastApiResult = httpApi.httpGet(APIURL_BASE + APIURL_GET_PERSON, null);
        RachioCloudPersonId pid = gson.fromJson(lastApiResult.resultString, RachioCloudPersonId.class);
        personId = pid.id;
        logger.debug("Using personId '{}'", personId);

        if (lastApiResult.isRateLimitCritical()) {
            String errorMessage = MessageFormat.format(
                    "Rachio Cloud API Rate Limit is critical ({0} of {1}), reset at {2}",
                    lastApiResult.rateRemaining, lastApiResult.rateLimit, lastApiResult.rateReset);
            throw new RachioApiException(errorMessage, lastApiResult);
        }

        return true;
    }

    private Boolean initializeDevices(ThingUID bridgeUID) throws RachioApiException {
        if (httpApi == null) {
            logger.debug("RachioApi.initializeDevices: httpAPI not initialized");
            return false;
        }

        String json = httpApi.httpGet(APIURL_BASE + APIURL_GET_PERSONID + "/" + personId, null).resultString;
        logger.trace("RachioApi: Initialize from JSON='{}'", json);

        RachioCloudStatus cloudStatus = gson.fromJson(json, RachioCloudStatus.class);
        userName = cloudStatus.username;
        fullName = cloudStatus.fullName;
        email = cloudStatus.email;

        deviceList.clear();
        for (RachioCloudDevice device : cloudStatus.devices) {
            if (!device.deleted) {
                ThingUID deviceUID = new ThingUID("rachio:device:" + bridgeUID.getId() + ":" + device.id);
                RachioDevice dev = new RachioDevice(device, bridgeUID, deviceUID);
                deviceList.put(device.id, dev);
                logger.trace("RachioApi: Device '{}' initialized, {} zones", device.name, device.zones.size());
            }
        }

        return true;
    }

    public Boolean initializeZones() {
        return true;
    }

    public HashMap<String, RachioDevice> getDevices() {
        return deviceList;
    }

    public RachioDevice getDevByUID(ThingUID bridgeUID, ThingUID thingUID) {
        for (Map.Entry<String, RachioDevice> entry : deviceList.entrySet()) {
            RachioDevice dev = entry.getValue();
            logger.trace("RachioDev.getDevByUID: bridge {} / {}, device {} / {}",
                    bridgeUID, dev.bridge_uid, thingUID, dev.dev_uid);
            if (dev.bridge_uid.equals(bridgeUID) && dev.getUID().equals(thingUID)) {
                logger.trace("RachioApi: Device '{}' found.", dev.name);
                return dev;
            }
        }
        logger.debug("RachioApi.getDevByUID: Unable to map UID to device");
        return null;
    }

    public RachioZone getZoneByUID(ThingUID bridgeUID, ThingUID zoneUID) {
        for (RachioDevice dev : deviceList.values()) {
            for (RachioZone zone : dev.getZones().values()) {
                if (zone.getUID().equals(zoneUID)) {
                    return zone;
                }
            }
        }
        return null;
    }
        public void stopWatering(String deviceId) throws RachioApiException {
        logger.debug("RachioApi: Stop watering for device '{}'", deviceId);
        httpApi.httpPut(APIURL_BASE + APIURL_DEV_PUT_STOP, "{ \"id\" : \"" + deviceId + "\" }");
    }

    public void enableDevice(String deviceId) throws RachioApiException {
        logger.debug("RachioApi: Enable device '{}'", deviceId);
        httpApi.httpPut(APIURL_BASE + APIURL_DEV_PUT_ON, "{ \"id\" : \"" + deviceId + "\" }");
    }

    public void disableDevice(String deviceId) throws RachioApiException {
        logger.debug("RachioApi: Disable device '{}'", deviceId);
        httpApi.httpPut(APIURL_BASE + APIURL_DEV_PUT_OFF, "{ \"id\" : \"" + deviceId + "\" }");
    }

    public void rainDelay(String deviceId, Integer delay) throws RachioApiException {
        logger.debug("RachioApi: Start rain delay for device '{}'", deviceId);
        httpApi.httpPut(APIURL_BASE + APIURL_DEV_PUT_RAIN_DELAY,
                "{ \"id\" : \"" + deviceId + "\", \"duration\" : " + delay + " }");
    }

    public void runMultilpeZones(String zoneListJson) throws RachioApiException {
        logger.debug("RachioApi: Start multiple zones '{}'", zoneListJson);
        httpApi.httpPut(APIURL_BASE + APIURL_ZONE_PUT_MULTIPLE_START, zoneListJson);
    }

    public void runZone(String zoneId, int duration) throws RachioApiException {
        logger.debug("RachioApi: Start zone '{}' for {} sec", zoneId, duration);
        httpApi.httpPut(APIURL_BASE + APIURL_ZONE_PUT_START,
                "{ \"id\" : \"" + zoneId + "\", \"duration\" : " + duration + " }");
    }

    public void getDeviceInfo(String deviceId) throws RachioApiException {
        httpApi.httpGet(APIURL_BASE + APIURL_GET_DEVICE + "/" + deviceId, null);
    }

    public void registerWebHook(String deviceId, String callbackUrl, String externalId, Boolean clearAllCallbacks)
            throws RachioApiException {
        logger.debug("RachioApi: Register webhook, url={}, externalId={}, clearAllCallbacks={}",
                callbackUrl, externalId, clearAllCallbacks);

        String json = "";
        try {
            json = httpApi.httpGet(APIURL_BASE + APIURL_DEV_QUERY_WEBHOOK + "/" + deviceId + "/webhook", null).resultString;
            logger.debug("RachioApi: Registered webhooks for device '{}': {}", deviceId, json);
            json = "{\"webhooks\":" + json + "}";
            RachioApiWebHookList list = gson.fromJson(json, RachioApiWebHookList.class);

            for (int i = 0; i < list.webhooks.size(); i++) {
                RachioApiWebHookEntry whe = list.webhooks.get(i);
                logger.debug("RachioApi: WebHook #{}: id='{}', url='{}', externalId='{}'",
                        i, whe.id, whe.url, whe.externalId);
                if (clearAllCallbacks || whe.url.equals(callbackUrl)) {
                    logger.debug("RachioApi: Deleting existing webhook '{}'", whe.id);
                    httpApi.httpDelete(APIURL_BASE + APIURL_DEV_DELETE_WEBHOOK + "/" + whe.id, null);
                }
            }
        } catch (Exception e) {
            logger.debug("RachioApi: WebHook cleanup failed: {}, JSON='{}'", e.getMessage(), json);
        }

        String jsonData = "{ " +
                "\"device\":{\"id\":\"" + deviceId + "\"}, " +
                "\"externalId\" : \"" + externalId + "\", " +
                "\"url\" : \"" + callbackUrl + "\", " +
                "\"eventTypes\" : [" +
                "{\"id\" : \"" + WHE_DEVICE_STATUS + "\"}, " +
                "{\"id\" : \"" + WHE_RAIN_DELAY + "\"}, " +
                "{\"id\" : \"" + WEATHER_INTELLIGENCE + "\"}, " +
                "{\"id\" : \"" + WHE_WATER_BUDGET + "\"}, " +
                "{\"id\" : \"" + WHE_ZONE_DELTA + "\"}, " +
                "{\"id\" : \"" + WHE_SCHEDULE_STATUS + "\"}, " +
                "{\"id\" : \"" + WHE_ZONE_STATUS + "\"}, " +
                "{\"id\" : \"" + WHE_RAIN_SENSOR_DETECTION + "\"}, " +
                "{\"id\" : \"" + WHE_DELTA + "\"} " +
                "]" +
                "}";

        httpApi.httpPost(APIURL_BASE + APIURL_DEV_POST_WEBHOOK, jsonData);
    }
        public Map<String, String> fillProperties() {
        Map<String, String> properties = new HashMap<>();
        properties.put(Thing.PROPERTY_VENDOR, BINDING_VENDOR);
        properties.put(PROPERTY_APIKEY, apikey);
        properties.put(PROPERTY_PERSON_ID, personId);
        properties.put(PROPERTY_PERSON_USER, userName);
        properties.put(PROPERTY_PERSON_NAME, fullName);
        properties.put(PROPERTY_PERSON_EMAIL, email);
        return properties;
    }

    protected static String getMD5Hash(String unhashed) {
        try {
            byte[] bytesOfMessage = unhashed.getBytes(UTF8_CHAR_SET);
            MessageDigest md5 = MessageDigest.getInstance(MD5_HASH_ALGORITHM);
            byte[] hash = md5.digest(bytesOfMessage);

            StringBuilder sb = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception exp) {
            return null;
        }
    }

    public static void copyMatchingFields(Object fromObj, Object toObj) {
        if (fromObj == null || toObj == null) {
            throw new NullPointerException("Source and destination objects must be non-null");
        }

        Field[] fields = fromObj.getClass().getFields();
        for (Field f : fields) {
            try {
                String fname = f.getName();
                Field t = toObj.getClass().getSuperclass().getDeclaredField(fname);

                if (t.getType() == f.getType()) {
                    f.setAccessible(true);
                    t.setAccessible(true);

                    if (t.getType() == String.class || t.getType().isPrimitive() || Number.class.isAssignableFrom(t.getType())
                            || t.getType() == Boolean.class || t.getType() == Character.class) {
                        t.set(toObj, f.get(fromObj));
                    } else if (t.getType() == Date.class) {
                        Date d = (Date) f.get(fromObj);
                        t.set(toObj, d != null ? d.clone() : null);
                    } else if (t.getType() == ArrayList.class) {
                        ArrayList<?> a = (ArrayList<?>) f.get(fromObj);
                        t.set(toObj, a != null ? (ArrayList<?>) a.clone() : null);
                    } else {
                        logger.debug("RachioApiInternal: Unable to update field '{}', type '{}'", t.getName(), t.getType());
                    }
                }
            } catch (NoSuchFieldException | IllegalAccessException ex) {
                logger.debug("RachioApiInternal: Skipping field '{}'", f.getName());
            }
        }
    }
}