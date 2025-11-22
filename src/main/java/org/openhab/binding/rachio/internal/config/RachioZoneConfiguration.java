package org.openhab.binding.rachio.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link RachioZoneConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Michael Lobstein - Initial contribution
 */
@NonNullByDefault
public class RachioZoneConfiguration {
    public String deviceId = "";
    public String zoneId = "";
    public int duration = 300;
    public int refresh = 30;
}
