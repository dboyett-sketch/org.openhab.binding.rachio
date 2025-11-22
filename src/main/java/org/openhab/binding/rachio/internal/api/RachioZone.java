package org.openhab.binding.rachio.internal.api;

import static org.openhab.binding.rachio.RachioBindingConstants.*;

import java.util.HashMap;
import java.util.Map;

import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.ThingUID;
import org.openhab.binding.rachio.internal.handler.RachioZoneHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioZone} stores attributes received from the Rachio cloud API and represents a zone.
 *
 * @author Markus Michels
 */
public class RachioZone extends RachioCloudZone {
    private final Logger logger = LoggerFactory.getLogger(RachioZone.class);

    protected ThingUID dev_uid;
    protected ThingUID zone_uid;
    protected ThingUID bridge_uid;

public void setBridgeUID(ThingUID uid) {
    this.bridge_uid = uid;
}

public ThingUID getBridgeUID() {
    return bridge_uid;
}

    protected RachioZoneHandler thingHandler;
    protected String uniqueId = "";
    protected int startRunTime = 0;
        public RachioZone(RachioCloudZone zone, String uniqueId) {
        try {
            RachioApi.copyMatchingFields(zone, this);

            if (zone.imageUrl != null && zone.imageUrl.startsWith(SERVLET_IMAGE_URL_BASE)) {
                String uri = zone.imageUrl.substring(zone.imageUrl.lastIndexOf("/"));
                if (!uri.isEmpty()) {
                    this.imageUrl = SERVLET_IMAGE_PATH + uri;
                    logger.trace("RachioZone: imageUrl rewritten to '{}' for zone '{}'", imageUrl, name);
                }
            }

            this.uniqueId = uniqueId;
            logger.trace("RachioZone: Zone '{}' (number={}, id={}, enabled={}) initialized.",
                    zone.name, zone.zoneNumber, zone.id, zone.enabled);
        } catch (Exception e) {
            logger.error("RachioZone: Initialization failed: {}", e.getMessage());
        }
    }

    public void setThingHandler(RachioZoneHandler zoneHandler) {
        this.thingHandler = zoneHandler;
    }

    public RachioZoneHandler getThingHandler() {
        return thingHandler;
    }

    public void update(RachioZone updatedZone) {
        if (updatedZone == null || !id.equalsIgnoreCase(updatedZone.id)) {
            return;
        }

        zoneNumber = updatedZone.zoneNumber;
        enabled = updatedZone.enabled;
        availableWater = updatedZone.availableWater;
        efficiency = updatedZone.efficiency;
        depthOfWater = updatedZone.depthOfWater;
        runtime = updatedZone.runtime;
        lastWateredDate = updatedZone.lastWateredDate;
    }

    public boolean compare(RachioZone czone) {
        return czone != null
                && zoneNumber == czone.zoneNumber
                && enabled == czone.enabled
                && availableWater == czone.availableWater
                && efficiency == czone.efficiency
                && lastWateredDate == czone.lastWateredDate
                && depthOfWater == czone.depthOfWater
                && runtime == czone.runtime;
    }
        public void setUID(ThingUID deviceUID, ThingUID zoneUID) {
        this.dev_uid = deviceUID;
        this.zone_uid = zoneUID;
    }

    public ThingUID getUID() {
        return zone_uid;
    }

    public ThingUID getDevUID() {
        return dev_uid;
    }

    public String getThingID() {
        return uniqueId + "-" + zoneNumber;
    }

    public Map<String, String> fillProperties() {
        Map<String, String> properties = new HashMap<>();
        properties.put(PROPERTY_NAME, name);
        properties.put(PROPERTY_ZONE_ID, id);
        return properties;
    }

    public OnOffType getEnabled() {
        return enabled ? OnOffType.ON : OnOffType.OFF;
    }

    public void setStartRunTime(int runtime) {
        this.startRunTime = runtime;
    }

    public int getStartRunTime() {
        return startRunTime;
    }

    public boolean isEnable() {
        return enabled;
    }
}
