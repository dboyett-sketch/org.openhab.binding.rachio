package org.openhab.binding.rachio.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import com.google.gson.Gson;

/**
 * The {@link RachioEvent} is responsible for holding
 * data from a Rachio webhook event
 *
 * @author Michael Lobstein - Initial contribution
 */

@NonNullByDefault
public class RachioEvent {
    private String deviceId;
    private String eventType;
    private String summary;
    private String eventId;

    // Default constructor for Gson
    public RachioEvent() {
        this.deviceId = "";
        this.eventType = "";
        this.summary = "";
        this.eventId = "";
    }

    public RachioEvent(String deviceId, String eventType, String summary, String eventId) {
        this.deviceId = deviceId;
        this.eventType = eventType;
        this.summary = summary;
        this.eventId = eventId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public static RachioEvent fromJson(String json) {
        try {
            Gson gson = new Gson();
            return gson.fromJson(json, RachioEvent.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse RachioEvent from JSON", e);
        }
    }
}
