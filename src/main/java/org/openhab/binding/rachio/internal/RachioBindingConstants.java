package org.openhab.binding.rachio.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link RachioBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioBindingConstants {

    public static final String BINDING_ID = "rachio";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_BRIDGE = new ThingTypeUID(BINDING_ID, "bridge");
    public static final ThingTypeUID THING_TYPE_DEVICE = new ThingTypeUID(BINDING_ID, "device");
    public static final ThingTypeUID THING_TYPE_ZONE = new ThingTypeUID(BINDING_ID, "zone");

    // List of all Channel ids
    public static final String CHANNEL_ZONE_RUN = "run";
    public static final String CHANNEL_ZONE_RUN_DURATION = "runDuration";
    public static final String CHANNEL_DEVICE_RUN_ALL_ZONES = "runAllZones";
    public static final String CHANNEL_DEVICE_RUN_NEXT_ZONE = "runNextZone";
    public static final String CHANNEL_DEVICE_STOP_WATERING = "stopWatering";
    public static final String CHANNEL_DEVICE_RAIN_DELAY = "rainDelay";
    public static final String CHANNEL_DEVICE_STATUS = "status";
    public static final String CHANNEL_ZONE_STATUS = "status";

    // List of all Property names
    public static final String PROPERTY_DEVICE_ID = "deviceId";
    public static final String PROPERTY_ZONE_ID = "zoneId";
    public static final String PROPERTY_API_KEY = "apiKey";
    public static final String PROPERTY_WEBHOOK_ID = "webhookId";

    // Bridge configuration parameters
    public static final String CONFIG_API_KEY = "apiKey";
    public static final String CONFIG_REFRESH_INTERVAL = "refreshInterval";
    public static final String CONFIG_WEBHOOK_URL = "webhookUrl";

    // Device configuration parameters
    public static final String CONFIG_DEVICE_ID = "deviceId";

    // Zone configuration parameters
    public static final String CONFIG_ZONE_ID = "zoneId";

    // Default values
    public static final int DEFAULT_REFRESH_INTERVAL = 60;
}
