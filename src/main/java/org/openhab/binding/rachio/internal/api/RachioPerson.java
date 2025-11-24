package org.openhab.binding.rachio.internal.api;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link RachioPerson} class represents a Rachio user account
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioPerson {
    public String id = "";
    public String username = "";
    public String fullName = "";
    public String email = "";
    public List<RachioDevice> devices;
    
    public String getId() { 
        return id; 
    }
    
    public String getUsername() { 
        return username; 
    }
    
    public String getFullName() { 
        return fullName; 
    }
    
    public String getEmail() { 
        return email; 
    }
    
    public List<RachioDevice> getDevices() { 
        return devices; 
    }
}
