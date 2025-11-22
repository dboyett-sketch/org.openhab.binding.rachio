package org.openhab.binding.rachio.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.rachio.internal.handler.RachioZoneHandler;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link RachioZone} is responsible for holding
 * all the properties of a Rachio zone
 *
 * @author Michael Lobstein - Initial contribution
 */

@NonNullByDefault
public class RachioZone {
    private String id = "";
    private String name = "";
    private int zoneNumber = 0;
    private boolean enabled = false;

    @SerializedName("duration")
    private int duration = 0;

    private transient RachioZoneHandler handler;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getZoneNumber() {
        return zoneNumber;
    }

    public void setZoneNumber(int zoneNumber) {
        this.zoneNumber = zoneNumber;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public boolean isRunning() {
        // TODO: implement this
        return false;
    }

    public void setHandler(RachioZoneHandler handler) {
        this.handler = handler;
    }

    public RachioZoneHandler getHandler() {
        return handler;
    }
}
