package org.openhab.binding.rachio.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.Gson;

/**
 * The {@link RachioEvent} class represents a webhook event from Rachio
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioEvent {
    // MAKE ALL FIELDS PUBLIC TO FIX ACCESS ERRORS
    public @Nullable String eventType;
    public @Nullable String deviceId;
    public @Nullable String timestamp;
    public @Nullable String summary;
    public @Nullable String topic;
    public @Nullable String type;
    public @Nullable String subType;
    public @Nullable String zoneName;
    public @Nullable Integer zoneNumber;
    public @Nullable String zoneRunState;
    public @Nullable String zoneRunStatus;
    public @Nullable Integer duration;
    
    // Default constructor
    public RachioEvent() {
    }
    
    // Constructor with parameters for convenience
    public RachioEvent(@Nullable String eventType, @Nullable String deviceId, @Nullable String timestamp) {
        this.eventType = eventType;
        this.deviceId = deviceId;
        this.timestamp = timestamp;
    }
    
    // ADDED: Missing fromJson method
    public static RachioEvent fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, RachioEvent.class);
    }
    
    // GETTER METHODS - Add these to maintain encapsulation while fixing access issues
    public @Nullable String getEventType() { 
        return eventType; 
    }
    
    public @Nullable String getDeviceId() { 
        return deviceId; 
    }
    
    public @Nullable String getTimestamp() { 
        return timestamp; 
    }
    
    public @Nullable String getSummary() { 
        return summary; 
    }
    
    public @Nullable String getTopic() { 
        return topic; 
    }
    
    public @Nullable String getType() { 
        return type; 
    }
    
    public @Nullable String getSubType() { 
        return subType; 
    }
    
    public @Nullable String getZoneName() { 
        return zoneName; 
    }
    
    public @Nullable Integer getZoneNumber() { 
        return zoneNumber; 
    }
    
    public @Nullable String getZoneRunState() { 
        return zoneRunState; 
    }
    
    public @Nullable String getZoneRunStatus() { 
        return zoneRunStatus; 
    }
    
    public @Nullable Integer getDuration() { 
        return duration; 
    }
    
    // SETTER METHODS - Optional, but useful
    public void setEventType(@Nullable String eventType) {
        this.eventType = eventType;
    }
    
    public void setDeviceId(@Nullable String deviceId) {
        this.deviceId = deviceId;
    }
    
    public void setTimestamp(@Nullable String timestamp) {
        this.timestamp = timestamp;
    }
    
    public void setSummary(@Nullable String summary) {
        this.summary = summary;
    }
    
    public void setTopic(@Nullable String topic) {
        this.topic = topic;
    }
    
    public void setType(@Nullable String type) {
        this.type = type;
    }
    
    public void setSubType(@Nullable String subType) {
        this.subType = subType;
    }
    
    public void setZoneName(@Nullable String zoneName) {
        this.zoneName = zoneName;
    }
    
    public void setZoneNumber(@Nullable Integer zoneNumber) {
        this.zoneNumber = zoneNumber;
    }
    
    public void setZoneRunState(@Nullable String zoneRunState) {
        this.zoneRunState = zoneRunState;
    }
    
    public void setZoneRunStatus(@Nullable String zoneRunStatus) {
        this.zoneRunStatus = zoneRunStatus;
    }
    
    public void setDuration(@Nullable Integer duration) {
        this.duration = duration;
    }
    
    @Override
    public String toString() {
        return "RachioEvent{" +
                "eventType='" + eventType + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", summary='" + summary + '\'' +
                ", topic='" + topic + '\'' +
                ", type='" + type + '\'' +
                ", subType='" + subType + '\'' +
                ", zoneName='" + zoneName + '\'' +
                ", zoneNumber=" + zoneNumber +
                ", zoneRunState='" + zoneRunState + '\'' +
                ", zoneRunStatus='" + zoneRunStatus + '\'' +
                ", duration=" + duration +
                '}';
    }
}
