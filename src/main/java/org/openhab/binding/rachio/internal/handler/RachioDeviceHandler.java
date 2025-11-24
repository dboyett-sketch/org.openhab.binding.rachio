package org.openhab.binding.rachio.internal.handler;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

import java.math.BigDecimal;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.binding.rachio.internal.api.RachioDevice;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.Units;
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
 * The {@link RachioDeviceHandler} is responsible for handling commands for a Rachio device
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioDeviceHandler extends BaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(RachioDeviceHandler.class);

    private @Nullable String deviceId;
    private @Nullable RachioHttp localApi;
    private @Nullable ScheduledFuture<?> refreshJob;

    public RachioDeviceHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            refreshDeviceStatus();
            return;
        }

        RachioHttp localApi = this.localApi;
        String localDeviceId = this.deviceId;

        if (localApi == null || localDeviceId == null) {
            logger.debug("Device handler not properly initialized");
            return;
        }

        try {
            switch (channelUID.getId()) {
                case CHANNEL_DEVICE_RUN_ALL_ZONES:
                    if (command instanceof QuantityType) {
                        // FIXED: Updated to use runAllZones method
                        QuantityType<?> quantity = (QuantityType<?>) command;
                        int duration = quantity.intValue();
                        localApi.runAllZones(localDeviceId, duration);
                        logger.debug("Started all zones on device {} for {} seconds", localDeviceId, duration);
                    }
                    break;
                case CHANNEL_DEVICE_RUN_NEXT_ZONE:
                    if (command instanceof QuantityType) {
                        // FIXED: Updated to use runNextZone method
                        QuantityType<?> quantity = (QuantityType<?>) command;
                        int duration = quantity.intValue();
                        localApi.runNextZone(localDeviceId, duration);
                        logger.debug("Started next zone on device {} for {} seconds", localDeviceId, duration);
                    }
                    break;
                case CHANNEL_DEVICE_STOP_WATERING:
                    localApi.stopWatering(localDeviceId);
                    logger.debug("Stopped watering on device {}", localDeviceId);
                    break;
                default:
                    logger.debug("Unhandled channel: {}", channelUID.getId());
            }
        } catch (Exception e) {
            logger.debug("Error handling command for device {}: {}", localDeviceId, e.getMessage(), e);
        }
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Rachio device handler");
        deviceId = (String) getThing().getConfiguration().get(PROPERTY_DEVICE_ID);

        if (deviceId == null || deviceId.isEmpty()) {
            // FIXED: Updated ThingStatus call
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Device ID not configured");
            return;
        }

        RachioBridgeHandler bridgeHandler = getBridgeHandler();
        if (bridgeHandler == null) {
            // FIXED: Updated ThingStatus call
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "Bridge not available");
            return;
        }

        localApi = bridgeHandler.getApi();
        if (localApi == null) {
            // FIXED: Updated ThingStatus call
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "Bridge API not available");
            return;
        }

        // FIXED: Updated ThingStatus call
        updateStatus(ThingStatus.ONLINE);
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
        refreshJob = scheduler.scheduleWithFixedDelay(this::refreshDeviceStatus, 10, 60, TimeUnit.SECONDS);
    }

    private void refreshDeviceStatus() {
        try {
            RachioHttp localApi = this.localApi;
            String localDeviceId = this.deviceId;

            if (localApi != null && localDeviceId != null) {
                RachioDevice device = localApi.getDevice(localDeviceId);
                if (device != null) {
                    updateDeviceChannels(device);
                }
            }
        } catch (Exception e) {
            logger.debug("Error refreshing device status: {}", e.getMessage(), e);
        }
    }

    private void updateDeviceChannels(RachioDevice device) {
        // Update device channels with current status
        // Implementation depends on your channel structure
        logger.debug("Updating channels for device: {}", device.getName());
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
}
