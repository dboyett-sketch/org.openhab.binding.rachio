package org.openhab.binding.rachio.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link RachioBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Michael Lobstein - Initial contribution
 */
@NonNullByDefault
public class RachioBindingConstants {

    public static final String BINDING_ID = "rachio";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_BRIDGE = new ThingTypeUID(BINDING_ID, "bridge");
    public static final ThingTypeUID THING_TYPE_DEVICE = new ThingTypeUID(BINDING_ID, "device");
    public static final ThingTypeUID THING_TYPE_ZONE = new ThingTypeUID(BINDING_ID, "zone");

    // List of all Channel ids
    public static final String CHANNEL_STATUS = "status";
    public static final String CHANNEL_LAST_RAN = "lastRan";
    public static final String CHANNEL_RUN_ALL_ZONES = "runAllZones";
    public static final String CHANNEL_RUN_NEXT_ZONE = "runNextZone";

    public static final String CHANNEL_ZONE_PREFIX = "zone";
    public static final String CHANNEL_ZONE_RUN = "run";
    public static final String CHANNEL_ZONE_RUNNING = "Running";
    public static final String CHANNEL_ZONE_DURATION = "Duration";

    // List of all Properties
    public static final String PROPERTY_ID = "id";
    public static final String PROPERTY_DEVICE_ID = "deviceId";
    public static final String PROPERTY_ZONE_ID = "zoneId";
    public static final String PROPERTY_SERIAL_NUMBER = "serialNumber";
    public static final String PROPERTY_ZONE_NUMBER = "zoneNumber";

    // Configuration parameters
    public static final String CONFIG_API_KEY = "apiKey";
    public static final String CONFIG_WEBHOOK_ID = "webhookId";
    public static final String CONFIG_DEVICE_ID = "deviceId";
    public static final String CONFIG_ZONE_ID = "zoneId";
    public static final String CONFIG_ZONE_DURATION = "zoneDuration";
    public static final String CONFIG_DURATION = "duration";
    public static final String CONFIG_REFRESH = "refresh";

    // Default values
    public static final int DEFAULT_REFRESH_PERIOD = 30;
    public static final int DEFAULT_ZONE_DURATION = 300;
}
