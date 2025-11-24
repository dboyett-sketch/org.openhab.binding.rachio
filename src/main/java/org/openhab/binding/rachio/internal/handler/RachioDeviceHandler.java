package org.openhab.binding.rachio.internal.handler;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.RachioApiException;
import org.openhab.binding.rachio.internal.api.RachioDevice;
import org.openhab.binding.rachio.internal.api.RachioHttp;
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
 * The {@link RachioDeviceHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioDeviceHandler extends BaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(RachioDeviceHandler.class);

    private @Nullable ScheduledFuture<?> refreshJob;
    private final Gson gson = new Gson();

    private @Nullable String deviceId;

    public RachioDeviceHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Rachio device handler.");
        deviceId = (String) getConfig().get(DEVICE_ID);

        if (deviceId == null || deviceId.trim().isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Device ID is required");
            return;
        }

        scheduler.execute(this::initializeDevice);
    }

    private void initializeDevice() {
        updateStatus(ThingStatus.ONLINE);
        startRefreshJob();
    }

    private void startRefreshJob() {
        ScheduledFuture<?> job = refreshJob;
        if (job != null && !job.isCancelled()) {
            job.cancel(false);
        }

        refreshJob = scheduler.scheduleWithFixedDelay(this::refreshDevice, 1, 60, TimeUnit.SECONDS);
    }

    private void refreshDevice() {
        String localDeviceId = deviceId;
        if (localDeviceId == null) {
            return;
        }

        RachioHttp api = getApi();
        if (api == null) {
            return;
        }

        try {
            JsonObject deviceData = api.getDevice(localDeviceId);
            if (deviceData != null) {
                // Convert JSON to RachioDevice object
                RachioDevice device = gson.fromJson(deviceData, RachioDevice.class);
                updateDeviceStatus(device);
                updateStatus(ThingStatus.ONLINE);
            }
        } catch (Exception e) {
            logger.debug("Failed to refresh device {}: {}", localDeviceId, e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    private void updateDeviceStatus(RachioDevice device) {
        // Update device channels based on device status
        // You can add channel updates here based on device data
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
            switch (channelUID.getId()) {
                case CHANNEL_START_ALL_ZONES:
                    api.runAllZones(localDeviceId, DEFAULT_DURATION);
                    break;
                case CHANNEL_START_NEXT_ZONE:
                    api.runNextZone(localDeviceId, DEFAULT_DURATION);
                    break;
                case CHANNEL_STOP_WATERING:
                    api.stopWatering(localDeviceId);
                    break;
                case CHANNEL_RAIN_DELAY:
                    if (command instanceof DecimalType) {
                        int duration = ((DecimalType) command).intValue();
                        api.rainDelay(localDeviceId, duration);
                    }
                    break;
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
