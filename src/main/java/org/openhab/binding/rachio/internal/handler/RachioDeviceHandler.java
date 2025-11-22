package org.openhab.binding.rachio.internal.handler;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.RachioDevice;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.binding.rachio.internal.api.RachioZone;
import org.openhab.binding.rachio.internal.config.RachioDeviceConfiguration;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.ThingHandlerCallback;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioDeviceHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Michael Lobstein - Initial contribution
 */

@NonNullByDefault
public class RachioDeviceHandler extends BaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(RachioDeviceHandler.class);

    private RachioDeviceConfiguration config = new RachioDeviceConfiguration();

    private @Nullable RachioHttp api;
    private @Nullable RachioDevice device;
    private @Nullable ScheduledFuture<?> pollingJob;

    public RachioDeviceHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            updateChannels();
            return;
        }

        String channelId = channelUID.getId();
        RachioHttp localApi = api;
        RachioDevice localDevice = device;

        if (localApi == null || localDevice == null) {
            return;
        }

        switch (channelId) {
            case CHANNEL_RUN_ALL_ZONES:
                if (command instanceof OnOffType && command == OnOffType.ON) {
                    localApi.runAllZones(localDevice.getId(), config.zoneDuration);
                }
                break;
            case CHANNEL_RUN_NEXT_ZONE:
                if (command instanceof OnOffType && command == OnOffType.ON) {
                    localApi.runNextZone(localDevice.getId(), config.zoneDuration);
                }
                break;
            default:
                logger.debug("Command received for an unknown channel: {}", channelId);
                break;
        }
    }

    @Override
    public void initialize() {
        config = getConfigAs(RachioDeviceConfiguration.class);
        logger.debug("Rachio device config: {}", config.toString());

        RachioBridgeHandler bridgeHandler = (RachioBridgeHandler) getBridge().getHandler();
        if (bridgeHandler != null) {
            api = bridgeHandler.getApi();
            device = bridgeHandler.getDevice(config.deviceId);

            if (device != null) {
                updateStatus(ThingStatus.ONLINE);
                // give the bridge a chance to fully initialize and get the device list
                scheduler.schedule(this::updateChannels, 5, TimeUnit.SECONDS);
                startPolling();
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "Could not find device: " + config.deviceId);
            }
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "No bridge available");
        }
    }

    @Override
    public void dispose() {
        stopPolling();
        super.dispose();
    }

    private void startPolling() {
        ScheduledFuture<?> localPollingJob = pollingJob;
        if (localPollingJob == null || localPollingJob.isCancelled()) {
            pollingJob = scheduler.scheduleWithFixedDelay(this::updateChannels, 30, 30, TimeUnit.SECONDS);
        }
    }

    private void stopPolling() {
        ScheduledFuture<?> localPollingJob = pollingJob;
        if (localPollingJob != null && !localPollingJob.isCancelled()) {
            localPollingJob.cancel(true);
            pollingJob = null;
        }
    }

    private void updateChannels() {
        RachioHttp localApi = api;
        RachioDevice localDevice = device;

        if (localApi == null || localDevice == null) {
            return;
        }

        // refresh the device data
        localDevice = localApi.getDevice(localDevice.getId());
        if (localDevice != null) {
            device = localDevice;

            ThingHandlerCallback callback = getCallback();
            if (callback != null) {
                callback.statusUpdated(getThing(), ThingStatus.ONLINE);

                updateState(CHANNEL_STATUS, new StringType(localDevice.getStatus()));
                updateState(CHANNEL_LAST_RAN, new StringType(localDevice.getLastWatered()));

                // update all the zone channels
                for (RachioZone zone : localDevice.getZones()) {
                    String zoneId = zone.getId();
                    updateState(CHANNEL_ZONE_PREFIX + zoneId, new StringType(zone.getName()));
                    updateState(CHANNEL_ZONE_PREFIX + zoneId + CHANNEL_ZONE_RUNNING,
                            OnOffType.from(zone.isRunning()));
                    updateState(CHANNEL_ZONE_PREFIX + zoneId + CHANNEL_ZONE_DURATION,
                            new QuantityType<>(zone.getDuration(), Units.SECOND));
                }
            }
        }
    }

    public void channelLinked(ChannelUID channelUID) {
        updateChannels();
    }
}
