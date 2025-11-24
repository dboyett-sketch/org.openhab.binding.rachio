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
    public static final String CHANNEL_START_ALL_ZONES = "startAllZones";
    public static final String CHANNEL_START_NEXT_ZONE = "startNextZone";
    public static final String CHANNEL_STOP_WATERING = "stopWatering";
    public static final String CHANNEL_RAIN_DELAY = "rainDelay";
    public static final String CHANNEL_START_ZONE = "startZone";

    // Bridge config properties
    public static final String API_KEY = "apiKey";
    public static final String WEBHOOK_ID = "webhookId";

    // Device config properties
    public static final String DEVICE_ID = "deviceId";

    // Zone config properties
    public static final String ZONE_ID = "zoneId";

    // Default duration for watering in seconds
    public static final int DEFAULT_DURATION = 300;
}
