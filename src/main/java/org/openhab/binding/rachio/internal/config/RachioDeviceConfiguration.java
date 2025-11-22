package org.openhab.binding.rachio.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link RachioDeviceConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Michael Lobstein - Initial contribution
 */
@NonNullByDefault
public class RachioDeviceConfiguration {
    public String deviceId = "";
    public int zoneDuration = 300;
    public int refresh = 30;
}
