package org.openhab.binding.rachio.internal.api;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.rachio.internal.handler.RachioDeviceHandler;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link RachioDevice} is responsible for holding
 * all the properties of a Rachio device
 *
 * @author Michael Lobstein - Initial contribution
 */

@NonNullByDefault
public class RachioDevice {
    private String id = "";
    private String status = "";
    private String name = "";
    private String serialNumber = "";

    @SerializedName("zones")
    private List<RachioZone> zones = new ArrayList<>();

    private transient RachioDeviceHandler handler;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public List<RachioZone> getZones() {
        return zones;
    }

    public void setZones(List<RachioZone> zones) {
        this.zones = zones;
    }

    public RachioZone getZone(String zoneId) {
        for (RachioZone zone : zones) {
            if (zoneId.equals(zone.getId())) {
                return zone;
            }
        }
        return null;
    }

    public String getLastWatered() {
        // TODO: implement this
        return "";
    }

    public void setHandler(RachioDeviceHandler handler) {
        this.handler = handler;
    }

    public RachioDeviceHandler getHandler() {
        return handler;
    }
}
