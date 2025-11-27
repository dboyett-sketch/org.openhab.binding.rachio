package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Represents zone operations and status
 */
@NonNullByDefault
public class RachioZone {
    public String zoneId = "";
    public String name = "";
    public boolean enabled;
    public int runtime;
    public int maxRuntime;
    public String imageUrl = "";
    
    public RachioZone() {}
    
    public RachioZone(String zoneId, String name, boolean enabled, int runtime) {
        this.zoneId = zoneId;
        this.name = name;
        this.enabled = enabled;
        this.runtime = runtime;
    }
}
