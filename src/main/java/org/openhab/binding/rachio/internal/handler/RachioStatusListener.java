package org.openhab.binding.rachio.internal.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingStatus;

/**
 * The {@link RachioStatusListener} is notified when a Rachio device or zone status changes.
 *
 * @author Michael Lobstein - Initial contribution
 */

@NonNullByDefault
public interface RachioStatusListener {
    void onRefreshRequested();
    
    void updateDeviceStatus(ThingStatus status);

    void updateZoneStatus(String zoneId, ThingStatus status);
}
