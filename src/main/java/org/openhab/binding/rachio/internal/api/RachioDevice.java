package org.openhab.binding.rachio.internal.api;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link RachioDevice} class represents a Rachio irrigation device
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioDevice {
    // MAKE ALL FIELDS PUBLIC
    public @Nullable String id;
    public @Nullable String name;
    public @Nullable String status;
    public @Nullable String serialNumber;
    public @Nullable String model;
    public @Nullable String macAddress;
    public @Nullable Double latitude;
    public @Nullable Double longitude;
    public @Nullable String timeZone;
    public @Nullable Boolean on;
    public @Nullable Boolean deleted;
    public @Nullable List<RachioZone> zones;
    
    // Default constructor
    public RachioDevice() {
    }
    
    // GETTER METHODS
    public @Nullable String getId() { 
        return id; 
    }
    
    public @Nullable String getName() { 
        return name; 
    }
    
    public @Nullable String getStatus() { 
        return status; 
    }
    
    public @Nullable String getSerialNumber() { 
        return serialNumber; 
    }
    
    public @Nullable String getModel() { 
        return model; 
    }
    
    public @Nullable String getMacAddress() { 
        return macAddress; 
    }
    
    public @Nullable Double getLatitude() { 
        return latitude; 
    }
    
    public @Nullable Double getLongitude() { 
        return longitude; 
    }
    
    public @Nullable String getTimeZone() { 
        return timeZone; 
    }
    
    public @Nullable Boolean getOn() { 
        return on; 
    }
    
    public @Nullable Boolean getDeleted() { 
        return deleted; 
    }
    
    public @Nullable List<RachioZone> getZones() { 
        return zones; 
    }
    
    // SETTER METHODS
    public void setId(@Nullable String id) {
        this.id = id;
    }
    
    public void setName(@Nullable String name) {
        this.name = name;
    }
    
    public void setStatus(@Nullable String status) {
        this.status = status;
    }
    
    public void setSerialNumber(@Nullable String serialNumber) {
        this.serialNumber = serialNumber;
    }
    
    public void setModel(@Nullable String model) {
        this.model = model;
    }
    
    public void setMacAddress(@Nullable String macAddress) {
        this.macAddress = macAddress;
    }
    
    public void setLatitude(@Nullable Double latitude) {
        this.latitude = latitude;
    }
    
    public void setLongitude(@Nullable Double longitude) {
        this.longitude = longitude;
    }
    
    public void setTimeZone(@Nullable String timeZone) {
        this.timeZone = timeZone;
    }
    
    public void setOn(@Nullable Boolean on) {
        this.on = on;
    }
    
    public void setDeleted(@Nullable Boolean deleted) {
        this.deleted = deleted;
    }
    
    public void setZones(@Nullable List<RachioZone> zones) {
        this.zones = zones;
    }
    
    @Override
    public String toString() {
        return "RachioDevice{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", status='" + status + '\'' +
                ", model='" + model + '\'' +
                ", on=" + on +
                '}';
    }
}
