package org.openhab.binding.rachio.internal.api.dto;

import java.util.List;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Represents a Rachio person (account) from the API
 */
@NonNullByDefault
public class RachioPerson {
    public String id = "";
    public String username = "";
    public String fullName = "";
    public String email = "";
    @Nullable
    public List<Device> devices;
    
    public static class Device {
        public String id = "";
        public String name = "";
        public String status = "";
        public String serialNumber = "";
        public String model = "";
        public double latitude;
        public double longitude;
        public String timeZone = "";
        public boolean on;
        @Nullable
        public List<Zone> zones;
    }
    
    public static class Zone {
        public String id = "";
        public String name = "";
        public int zoneNumber;
        public boolean enabled;
        public int runtime;
        public int maxRuntime;
        public String imageUrl = "";
    }
    
    @Nullable
    public List<String> getDeviceIds() {
        if (devices == null) return null;
        return devices.stream().map(d -> d.id).collect(java.util.stream.Collectors.toList());
    }
}
