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
                        // FIXED: Handle QuantityType properly
                        QuantityType<?> quantity = (QuantityType<?>) command;
                        int duration = quantity.toBigDecimal().intValue();
                        String response = localApi.runAllZones(localDeviceId, duration);
                        if (response != null && !response.isEmpty()) {
                            logger.debug("Started all zones on device {} for {} seconds", localDeviceId, duration);
                        } else {
                            logger.debug("Failed to start all zones on device {}", localDeviceId);
                        }
                    }
                    break;
                case CHANNEL_DEVICE_RUN_NEXT_ZONE:
                    if (command instanceof QuantityType) {
                        // FIXED: Handle QuantityType properly
                        QuantityType<?> quantity = (QuantityType<?>) command;
                        int duration = quantity.toBigDecimal().intValue();
                        String response = localApi.runNextZone(localDeviceId, duration);
                        if (response != null && !response.isEmpty()) {
                            logger.debug("Started next zone on device {} for {} seconds", localDeviceId, duration);
                        } else {
                            logger.debug("Failed to start next zone on device {}", localDeviceId);
                        }
                    }
                    break;
                case CHANNEL_DEVICE_STOP_WATERING:
                    String response = localApi.stopWatering(localDeviceId);
                    if (response != null && !response.isEmpty()) {
                        logger.debug("Stopped watering on device {}", localDeviceId);
                    } else {
                        logger.debug("Failed to stop watering on device {}", localDeviceId);
                    }
                    break;
                case CHANNEL_DEVICE_RAIN_DELAY:
                    if (command instanceof QuantityType) {
                        // FIXED: Handle QuantityType properly
                        QuantityType<?> quantity = (QuantityType<?>) command;
                        int duration = quantity.toBigDecimal().intValue();
                        String rainDelayResponse = localApi.rainDelay(localDeviceId, duration);
                        if (rainDelayResponse != null && !rainDelayResponse.isEmpty()) {
                            logger.debug("Set rain delay on device {} for {} hours", localDeviceId, duration);
                        } else {
                            logger.debug("Failed to set rain delay on device {}", localDeviceId);
                        }
                    }
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
            // FIXED: Updated ThingStatus call with proper parameters
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Device ID not configured");
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
        updateStatus(ThingStatus.ONLINE, ThingStatusDetail.NONE, "Device initialized successfully");
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
                    // FIXED: Updated ThingStatus call with proper parameters
                    updateStatus(ThingStatus.ONLINE, ThingStatusDetail.NONE, "Device status updated");
                } else {
                    // FIXED: Updated ThingStatus call with proper parameters
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Failed to get device status");
                }
            }
        } catch (Exception e) {
            logger.debug("Error refreshing device status: {}", e.getMessage(), e);
            // FIXED: Updated ThingStatus call with proper parameters
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Refresh error: " + e.getMessage());
        }
    }

    private void updateDeviceChannels(RachioDevice device) {
        // Update device channels with current status
        logger.debug("Updating channels for device: {}", device.getName());
        
        // Example channel updates - adjust based on your actual channels
        if (device.getStatus() != null) {
            updateState(CHANNEL_DEVICE_STATUS, new org.openhab.core.library.types.StringType(device.getStatus()));
        }
        
        // Update other channels as needed based on your device properties
        if (device.getOn() != null) {
            // You might have a channel for device power state
        }
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

    public @Nullable String getDeviceId() {
        return deviceId;
    }

    public @Nullable RachioHttp getApi() {
        return localApi;
    }
}
