package org.openhab.binding.rachio.internal.api.dto;

import java.util.List;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Represents a Rachio device details
 */
@NonNullByDefault
public class RachioDevice {
    public String id = "";
    public String status = "";
    public String name = "";
    public String serialNumber = "";
    public String model = "";
    public double latitude;
    public double longitude;
    public String timeZone = "";
    public boolean on;
    public boolean deleted;
    @Nullable
    public List<Zone> zones;
    @Nullable
    public ScheduleRule currentSchedule;
    
    public static class Zone {
        public String id = "";
        public String name = "";
        public int zoneNumber;
        public boolean enabled;
        public int runtime;
        public int maxRuntime;
        public String imageUrl = "";
    }
    
    public static class ScheduleRule {
        public String id = "";
        public String name = "";
        public String startDate = "";
        public int totalDuration;
        public String status = "";
    }
}
