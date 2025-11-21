package org.openhab.binding.rachio.internal.api;

import java.util.HashMap;

import org.openhab.binding.rachio.internal.api.RachioApi.RachioApiResult;
import org.openhab.binding.rachio.internal.api.RachioCloudDevice.RachioCloudNetworkSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioEvent} represents a webhook event from the Rachio cloud API.
 *
 * @author Markus Michels
 */
public class RachioEvent {
    private final Logger logger = LoggerFactory.getLogger(RachioEvent.class);
        public static class RachioEventProperty {
        public String propertyName;
        public String oldValue;
        public String newValue;
    }

    public static class RachioZoneStatus {
        public Integer duration = 0;
        public String scheduleType = "";
        public Integer zoneNumber = 0;
        public String executionType = "";
        public String state = "";
        public String startTime = "";
        public String endTime = "";
    }

    public String externalId = "";
    public String routingId = "";
    public String connectId = "";
    public String correlationId = "";
    public String scheduleId = "";
    public String deviceId = "";
    public String zoneId = "";
    public String id = "";

    public String timeZone = "";
    public String timestamp = "";
    public String timeForSummary = "";
    public String startTime = "";
    public String endTime = "";

    public long eventDate = -1;
    public long createDate = -1;
    public long lastUpdateDate = -1;
    public int sequence = -1;
    public String status = "";

    public String type = "";
    public String subType = "";
    public String eventType = "";
    public String category = "";
    public String topic = "";
    public String action = "";
    public String summary = "";
    public String description = "";
    public String title = "";
    public String pushTitle = "";

    public String icon = "";
    public String iconUrl = "";

    // ZONE_STATUS
    public Integer zoneNumber = 0;
    public String zoneName = "";
    public Integer zoneCurrent = 0;
    public String zoneRunState = "";
    public Integer duration = 0;
    public Integer durationInMinutes = 0;
    public Integer flowVolume = 0;
    public RachioZoneStatus zoneRunStatus;

    // SCHEDULE_STATUS
    public String scheduleName = "";
    public String scheduleType = "";

    // DEVICE_STATUS
    public String deviceName = "";
    public RachioCloudNetworkSettings network;
    public String pin = "";

    public RachioApiResult apiResult = new RachioApiResult();
    public HashMap<String, String> eventParms;
    public HashMap<String, RachioEventProperty> deltaProperties;

    public RachioEvent() {
        // Default constructor
    }
}