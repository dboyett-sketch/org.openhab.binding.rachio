package org.openhab.binding.rachio.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The {@link RachioEvent} is responsible for holding
 * data from a Rachio webhook event
 *
 * @author Michael Lobstein - Initial contribution
 */

@NonNullByDefault
public class RachioEvent {
    private final String deviceId;
    private final String eventType;
    private final String summary;
    private final String eventId;

    public RachioEvent(String deviceId, String eventType, String summary, String eventId) {
        this.deviceId = deviceId;
        this.eventType = eventType;
        this.summary = summary;
        this.eventId = eventId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getSummary() {
        return summary;
    }

    public String getEventId() {
        return eventId;
    }

    public static RachioEvent fromJson(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, RachioEvent.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse RachioEvent from JSON", e);
        }
    }
}
