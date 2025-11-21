package org.openhab.binding.rachio.internal.handler;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.RachioDevice;
import org.openhab.binding.rachio.internal.api.RachioZone;

/**
 * The {@link RachioStatusListener} is notified when a device or zone is updated.
 */
public interface RachioStatusListener {

    /**
     * Called whenever a new device or zone status is received by the cloud handler.
     *
     * @param updatedDev   On device updates this is the new RachioDevice information, may be null
     * @param updatedZone  On zone updates this is the new RachioZone information, may be null
     * @return true if the update was processed
     */
    public boolean onThingStateChanged(@Nullable RachioDevice updatedDev, @Nullable RachioZone updatedZone);
}