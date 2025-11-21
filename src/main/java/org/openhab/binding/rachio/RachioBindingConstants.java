package org.openhab.binding.rachio;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link RachioBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Markus Michels (markus7017)
 */
@NonNullByDefault
public class RachioBindingConstants {

    public static final String BINDING_ID = "rachio";
    public static final String BINDING_VENDOR = "Rachio";

    public static int BINDING_DISCOVERY_TIMEOUT = 60;
    public static int PORT_REFRESH_INTERVAL = 60;

    // Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_CLOUD = new ThingTypeUID(BINDING_ID, "cloud");
    public static final ThingTypeUID THING_TYPE_DEVICE = new ThingTypeUID(BINDING_ID, "device");
    public static final ThingTypeUID THING_TYPE_ZONE = new ThingTypeUID(BINDING_ID, "zone");

    public static final Set<ThingTypeUID> SUPPORTED_BRIDGE_THING_TYPES_UIDS = Stream.of(THING_TYPE_CLOUD)
            .collect(Collectors.toSet());
    public static final Set<ThingTypeUID> SUPPORTED_DEVICE_THING_TYPES_UIDS = Stream
            .of(THING_TYPE_DEVICE, THING_TYPE_ZONE).collect(Collectors.toSet());
    public static final Set<ThingTypeUID> SUPPORTED_ZONE_THING_TYPES_UIDS = Stream.of(THING_TYPE_ZONE)
            .collect(Collectors.toSet());
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Stream
            .concat(SUPPORTED_BRIDGE_THING_TYPES_UIDS.stream(), SUPPORTED_DEVICE_THING_TYPES_UIDS.stream())
            .collect(Collectors.toSet());

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = SUPPORTED_THING_TYPES_UIDS;

    // Config options
    public static final String PARAM_APIKEY = "apikey";
    public static final String PARAM_POLLING_INTERVAL = "pollingInterval";
    public static final String PARAM_DEF_RUNTIME = "defaultRuntime";
    public static final String PARAM_CALLBACK_URL = "callbackUrl";
    public static final String PARAM_CLEAR_CALLBACK = "clearAllCallbacks";
    public static final String PARAM_IPFILTER = "ipFilter";

    // Non-standard Properties
    public static final String PROPERTY_IP_ADDRESS = "ipAddress";
    public static final String PROPERTY_IP_MASK = "ipMask";
    public static final String PROPERTY_IP_GW = "ipGateway";
    public static final String PROPERTY_IP_DNS1 = "ipDNS1";
    public static final String PROPERTY_IP_DNS2 = "ipDNS2";
    public static final String PROPERTY_WIFI_RSSI = "wifiSignal";
    public static final String PROPERTY_APIKEY = "apikey";
    public static final String PROPERTY_NAME = "name";
    public static final String PROPERTY_MODEL = "model";
    public static final String PROPERTY_EXT_ID = "externalId";
    public static final String PROPERTY_DEV_ID = "deviceId";
    public static final String PROPERTY_ZONE_ID = "zoneId";
    public static final String PROPERTY_PERSON_ID = "personId";
    public static final String PROPERTY_PERSON_USER = "accounUserName";
    public static final String PROPERTY_PERSON_NAME = "accountFullName";
    public static final String PROPERTY_PERSON_EMAIL = "accountEMail";

    // Defaults
    public static int DEFAULT_HTTP_TIMEOUT = 15 * 1000;
    public static int DEFAULT_POLLING_INTERVAL = 120;
    public static int DEFAULT_ZONE_RUNTIME = 300;

    // Device Channels
    public static final String CHANNEL_DEVICE_NAME = "name";
    public static final String CHANNEL_DEVICE_ACTIVE = "active";
    public static final String CHANNEL_DEVICE_ONLINE = "online";
    public static final String CHANNEL_DEVICE_PAUSED = "paused";
    public static final String CHANNEL_DEVICE_RUN = "run";
    public static final String CHANNEL_DEVICE_RUN_ZONES = "runZones";
    public static final String CHANNEL_DEVICE_RUN_TIME = "runTime";
    public static final String CHANNEL_DEVICE_STOP = "stop";
    public static final String CHANNEL_DEVICE_EVENT = "event";
    public static final String CHANNEL_DEVICE_LATITUDE = "latitude";
    public static final String CHANNEL_DEVICE_LONGITUDE = "longitude";
    public static final String CHANNEL_DEVICE_SCHEDULE = "scheduleName";
    public static final String CHANNEL_DEVICE_RAIN_DELAY = "rainDelay";

    // Zone Channels
    public static final String CHANNEL_ZONE_NAME = "name";
    public static final String CHANNEL_ZONE_NUMBER = "number";
    public static final String CHANNEL_ZONE_ENABLED = "enabled";
    public static final String CHANNEL_ZONE_RUN = "run";
    public static final String CHANNEL_ZONE_RUN_TIME = "runTime";
    public static final String CHANNEL_ZONE_RUN_TOTAL = "runTotal";
    public static final String CHANNEL_ZONE_IMAGEURL = "imageUrl";

    // HTTP
    public static final String HTTP_METHOD_GET = "GET";
    public static final String HTTP_METHOD_PUT = "PUT";
    public static final String HTTP_METHOD_POST = "POST";
    public static final String HTTP_METHOD_DELETE = "DELETE";
    public static final int HTTP_TIMOUT = 15000;

    // API URLs
    public static final String APIURL_BASE = "https://api.rach.io/1/public/";
    public static final String APIURL_GET_PERSON = "person/info";
    public static final String APIURL_GET_PERSONID = "person";
    public static final String APIURL_GET_DEVICE = "device";
    public static final String APIURL_DEV_PUT_ON = "device/on";
    public static final String APIURL_DEV_PUT_OFF = "device/off";
    public static final String APIURL_DEV_PUT_STOP = "device/stop_water";
    public static final String APIURL_DEV_PUT_RAIN_DELAY = "device/rain_delay";
    public static final String APIURL_DEV_POST_WEBHOOK = "notification/webhook";
    public static final String APIURL_DEV_QUERY_WEBHOOK = "notification";
    public static final String APIURL_DEV_DELETE_WEBHOOK = "notification/webhook";
    public static final String APIURL_ZONE_PUT_START = "zone/start";
    public static final String APIURL_ZONE_PUT_MULTIPLE_START = "zone/start_multiple";

    // Webhook Events
    public static final String WHE_DEVICE_STATUS = "5";
    public static final String WHE_RAIN_DELAY = "6";
    public static final String WEATHER_INTELLIGENCE = "7";
    public static final String WHE_WATER_BUDGET = "8";
    public static final String WHE_SCHEDULE_STATUS = "9";
    public static final String WHE_ZONE_STATUS = "10";
    public static final String WHE_RAIN_SENSOR_DETECTION = "11";
    public static final String WHE_ZONE_DELTA = "12";
    public static final String WHE_DELTA = "14";

    // Servlet Paths
    public static final String SERVLET_WEBHOOK_PATH = "/rachio/webhook";
    public static final String SERVLET_WEBHOOK_APPLICATION_JSON = "application/json";
    public static final String SERVLET_WEBHOOK_CHARSET = "utf-8";
    public static final String SERVLET_WEBHOOK_USER_AGENT = "Mozilla/5.0";
    public static final String SERVLET_IMAGE_PATH = "/rachio/images";
    public static final String SERVLET_IMAGE_MIME_TYPE = "image/png";
    public static final String SERVLET_IMAGE_URL_BASE = "https://prod-media-photo.rach.io/";

    // Rate Limit Headers
    public static final String RACHIO_JSON_RATE_LIMIT = "X-RateLimit-Limit";
    public static final String RACHIO_JSON_RATE_REMAINING = "X-RateLimit-Remaining";
    public static final String RACHIO_JSON_RATE_RESET = "X-RateLimit-Reset";
    public static final int RACHIO_RATE_LIMIT_WARNING = 200;
    public static final int RACHIO_RATE_LIMIT_CRITICAL = 100;
    public static final int RACHIO_RATE_LIMIT_BLOCK = 20;
    public static final int RACHIO_RATE_SKIP_CALLS = 5;

    // AWS IP Ranges
    public static final String AWS_IPADDR_DOWNLOAD_URL = "https://ip-ranges.amazonaws.com/ip-ranges.json";
    public static final String AWS_IPADDR_REGION_FILTER = "us-";
}