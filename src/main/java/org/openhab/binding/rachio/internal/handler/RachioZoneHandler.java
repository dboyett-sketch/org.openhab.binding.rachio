package org.openhab.binding.rachio.internal.handler;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

import java.math.BigDecimal;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.measure.Quantity;
import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.binding.rachio.internal.api.RachioDevice;
import org.openhab.binding.rachio.internal.api.RachioZone;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioZoneHandler} is responsible for handling commands for a single Rachio zone
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioZoneHandler extends BaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(RachioZoneHandler.class);

    private @Nullable String zoneId;
    private @Nullable String deviceId;
    private @Nullable RachioHttp localApi;
    private @Nullable ScheduledFuture<?> refreshJob;

    public RachioZoneHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            refreshZoneStatus();
            return;
        }

        RachioHttp localApi = this.localApi;
        String localZoneId = this.zoneId;

        if (localApi == null || localZoneId == null) {
            logger.debug("Zone handler not properly initialized");
            return;
        }

        try {
            switch (channelUID.getId()) {
                case CHANNEL_ZONE_RUN:
                    if (command instanceof QuantityType) {
                        // FIXED: Handle QuantityType properly without type parameter issues
                        QuantityType<?> quantity = (QuantityType<?>) command;
                        int duration = quantity.toBigDecimal().intValue();
                        String response = localApi.runZone(localZoneId, duration);
                        if (response != null && !response.isEmpty()) {
                            logger.debug("Started zone {} for {} seconds", localZoneId, duration);
                        } else {
                            logger.debug("Failed to start zone {}", localZoneId);
                        }
                    }
                    break;
                case CHANNEL_ZONE_RUN_DURATION:
                    if (command instanceof QuantityType) {
                        // FIXED: Handle QuantityType properly without type parameter issues
                        QuantityType<?> quantity = (QuantityType<?>) command;
                        int duration = quantity.toBigDecimal().intValue();
                        String response = localApi.runZone(localZoneId, duration);
                        if (response != null && !response.isEmpty()) {
                            logger.debug("Started zone {} for {} seconds via duration channel", localZoneId, duration);
                        } else {
                            logger.debug("Failed to start zone {} via duration channel", localZoneId);
                        }
                    }
                    break;
                default:
                    logger.debug("Unhandled channel: {}", channelUID.getId());
            }
        } catch (Exception e) {
            logger.debug("Error handling command for zone {}: {}", localZoneId, e.getMessage(), e);
        }
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Rachio zone handler");
        zoneId = (String) getThing().getConfiguration().get(PROPERTY_ZONE_ID);
        deviceId = (String) getThing().getConfiguration().get(PROPERTY_DEVICE_ID);

        if (zoneId == null || zoneId.isEmpty()) {
            // FIXED: Updated ThingStatus call with proper parameters
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Zone ID not configured");
            return;
        }

        RachioBridgeHandler bridgeHandler = getBridgeHandler();
        if (bridgeHandler == null) {
            // FIXED: Updated ThingStatus call with proper parameters
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "Bridge not available");
            return;
        }

        localApi = bridgeHandler.getApi();
        if (localApi == null) {
            // FIXED: Updated ThingStatus call with proper parameters
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "Bridge API not available");
            return;
        }

        // FIXED: Updated ThingStatus call with proper parameters
        updateStatus(ThingStatus.ONLINE, ThingStatusDetail.NONE, "Zone initialized successfully");
        startRefreshJob();
    }

    private @Nullable RachioBridgeHandler getBridgeHandler() {
        return getBridge() != null ? (RachioBridgeHandler) getBridge().getHandler() : null;
    }

    // ADDED: Missing getDevice method used in the bridge handler
    public @Nullable RachioDevice getDevice(String deviceId) {
        RachioBridgeHandler bridgeHandler = getBridgeHandler();
        if (bridgeHandler != null) {
            return bridgeHandler.getDevice(deviceId);
        }
        return null;
    }

    private void startRefreshJob() {
        refreshJob = scheduler.scheduleWithFixedDelay(this::refreshZoneStatus, 10, 60, TimeUnit.SECONDS);
    }

    private void refreshZoneStatus() {
        try {
            RachioHttp localApi = this.localApi;
            String localZoneId = this.zoneId;

            if (localApi != null && localZoneId != null) {
                RachioZone zone = localApi.getZone(localZoneId);
                if (zone != null) {
                    updateZoneChannels(zone);
                    // FIXED: Updated ThingStatus call with proper parameters
                    updateStatus(ThingStatus.ONLINE, ThingStatusDetail.NONE, "Zone status updated");
                } else {
                    // FIXED: Updated ThingStatus call with proper parameters
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Failed to get zone status");
                }
            }
        } catch (Exception e) {
            logger.debug("Error refreshing zone status: {}", e.getMessage(), e);
            // FIXED: Updated ThingStatus call with proper parameters
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Refresh error: " + e.getMessage());
        }
    }

    private void updateZoneChannels(RachioZone zone) {
        // Update zone channels with current status
        // Implementation depends on your channel structure
        logger.debug("Updating channels for zone: {}", zone.getId());
        
        // Example channel updates - adjust based on your actual channels
        if (zone.getName() != null) {
            updateState(CHANNEL_ZONE_STATUS, new org.openhab.core.library.types.StringType(zone.getName()));
        }
        
        // Add more channel updates as needed based on your zone properties
    }

    @Override
    public void dispose() {
        ScheduledFuture<?> localRefreshJob = refreshJob;
        if (localRefreshJob != null) {
            localRefreshJob.cancel(true);
            refreshJob = null;
        }
        super.dispose();
    }

    public @Nullable String getZoneId() {
        return zoneId;
    }

    public @Nullable String getDeviceId() {
        return deviceId;
    }
}
