package org.openhab.binding.rachio.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link RachioZone} class represents a Rachio irrigation zone
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioZone {
    // Public fields to match the API JSON structure
    public @Nullable String id;
    public @Nullable String name;
    public @Nullable Integer zoneNumber;
    public @Nullable Boolean enabled;
    public @Nullable String imageUrl;
    public @Nullable Double efficiency;
    public @Nullable Double runtime;
    public @Nullable Boolean isRunning;
    public @Nullable String status;
    public @Nullable Double depthOfWater;
    public @Nullable Double maxRuntime;
    
    // Default constructor
    public RachioZone() {
    }
    
    // GETTER METHODS
    public @Nullable String getId() { 
        return id; 
    }
    
    public @Nullable String getName() { 
        return name; 
    }
    
    public @Nullable Integer getZoneNumber() { 
        return zoneNumber; 
    }
    
    public @Nullable Boolean getEnabled() { 
        return enabled; 
    }
    
    public @Nullable String getImageUrl() { 
        return imageUrl; 
    }
    
    public @Nullable Double getEfficiency() { 
        return efficiency; 
    }
    
    public @Nullable Double getRuntime() { 
        return runtime; 
    }
    
    public @Nullable Boolean getIsRunning() { 
        return isRunning; 
    }
    
    public @Nullable String getStatus() { 
        return status; 
    }
    
    public @Nullable Double getDepthOfWater() { 
        return depthOfWater; 
    }
    
    public @Nullable Double getMaxRuntime() { 
        return maxRuntime; 
    }
    
    // SETTER METHODS
    public void setId(@Nullable String id) {
        this.id = id;
    }
    
    public void setName(@Nullable String name) {
        this.name = name;
    }
    
    public void setZoneNumber(@Nullable Integer zoneNumber) {
        this.zoneNumber = zoneNumber;
    }
    
    public void setEnabled(@Nullable Boolean enabled) {
        this.enabled = enabled;
    }
    
    public void setImageUrl(@Nullable String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public void setEfficiency(@Nullable Double efficiency) {
        this.efficiency = efficiency;
    }
    
    public void setRuntime(@Nullable Double runtime) {
        this.runtime = runtime;
    }
    
    public void setIsRunning(@Nullable Boolean isRunning) {
        this.isRunning = isRunning;
    }
    
    public void setStatus(@Nullable String status) {
        this.status = status;
    }
    
    public void setDepthOfWater(@Nullable Double depthOfWater) {
        this.depthOfWater = depthOfWater;
    }
    
    public void setMaxRuntime(@Nullable Double maxRuntime) {
        this.maxRuntime = maxRuntime;
    }
    
    @Override
    public String toString() {
        return "RachioZone{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", zoneNumber=" + zoneNumber +
                ", enabled=" + enabled +
                ", isRunning=" + isRunning +
                ", status='" + status + '\'' +
                '}';
    }
}
