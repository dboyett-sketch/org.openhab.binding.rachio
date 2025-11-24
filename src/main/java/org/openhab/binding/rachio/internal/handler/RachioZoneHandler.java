package org.openhab.binding.rachio.internal.handler;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.RachioApiException;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.binding.rachio.internal.api.RachioZone;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * The {@link RachioZoneHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioZoneHandler extends BaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(RachioZoneHandler.class);

    private @Nullable ScheduledFuture<?> refreshJob;
    private final Gson gson = new Gson();

    private @Nullable String zoneId;
    private @Nullable String deviceId;

    public RachioZoneHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Rachio zone handler.");
        zoneId = (String) getConfig().get(ZONE_ID);

        if (zoneId == null || zoneId.trim().isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Zone ID is required");
            return;
        }

        // Get device ID from bridge for API calls
        RachioBridgeHandler bridgeHandler = (RachioBridgeHandler) getBridge().getHandler();
        if (bridgeHandler != null) {
            // You might need to get the device ID associated with this zone
            // This is a simplification - you may need to adjust based on your data model
            deviceId = "default"; // You'll need to set this properly
        }

        scheduler.execute(this::initializeZone);
    }

    private void initializeZone() {
        updateStatus(ThingStatus.ONLINE);
        startRefreshJob();
    }

    private void startRefreshJob() {
        ScheduledFuture<?> job = refreshJob;
        if (job != null && !job.isCancelled()) {
            job.cancel(false);
        }

        refreshJob = scheduler.scheduleWithFixedDelay(this::refreshZone, 1, 60, TimeUnit.SECONDS);
    }

    private void refreshZone() {
        String localZoneId = zoneId;
        if (localZoneId == null) {
            return;
        }

        RachioHttp api = getApi();
        if (api == null) {
            return;
        }

        try {
            JsonObject zoneData = api.getZone(localZoneId);
            if (zoneData != null) {
                // Convert JSON to RachioZone object
                RachioZone zone = gson.fromJson(zoneData, RachioZone.class);
                updateZoneStatus(zone);
                updateStatus(ThingStatus.ONLINE);
            }
        } catch (Exception e) {
            logger.debug("Failed to refresh zone {}: {}", localZoneId, e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    private void updateZoneStatus(RachioZone zone) {
        // Update zone channels based on zone status
        // You can add channel updates here based on zone data
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        String localDeviceId = deviceId;
        if (localDeviceId == null) {
            return;
        }

        RachioHttp api = getApi();
        if (api == null) {
            return;
        }

        try {
            if (CHANNEL_START_ZONE.equals(channelUID.getId())) {
                if (command instanceof DecimalType) {
                    int duration = ((DecimalType) command).intValue();
                    api.runZone(localDeviceId, duration);
                }
            }
            updateStatus(ThingStatus.ONLINE);
        } catch (RachioApiException e) {
            logger.debug("Failed to execute command {}: {}", channelUID.getId(), e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    private @Nullable RachioHttp getApi() {
        RachioBridgeHandler bridgeHandler = (RachioBridgeHandler) getBridge().getHandler();
        return bridgeHandler != null ? bridgeHandler.getApi() : null;
    }

    @Override
    public void dispose() {
        ScheduledFuture<?> job = refreshJob;
        if (job != null && !job.isCancelled()) {
            job.cancel(true);
        }
        refreshJob = null;
        super.dispose();
    }
}
