package org.openhab.binding.rachio.internal.handler;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.RachioDevice;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.binding.rachio.internal.api.RachioZone;
import org.openhab.binding.rachio.internal.config.RachioZoneConfiguration;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
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
 * The {@link RachioZoneHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Michael Lobstein - Initial contribution
 */

@NonNullByDefault
public class RachioZoneHandler extends BaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(RachioZoneHandler.class);

    private RachioZoneConfiguration config = new RachioZoneConfiguration();

    private @Nullable RachioHttp api;
    private @Nullable RachioDevice device;
    private @Nullable RachioZone zone;
    private @Nullable ScheduledFuture<?> pollingJob;

    public RachioZoneHandler(Thing thing) {
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
        RachioZone localZone = zone;

        if (localApi == null || localZone == null) {
            return;
        }

        switch (channelId) {
            case CHANNEL_ZONE_RUN:
                if (command instanceof OnOffType && command == OnOffType.ON) {
                    localApi.runZone(localZone.getId(), config.duration);
                }
                break;
            case CHANNEL_ZONE_DURATION:
                if (command instanceof QuantityType) {
                    @SuppressWarnings("unchecked")
                    QuantityType<Integer> duration = ((QuantityType<Integer>) command).toUnit(Units.SECOND);
                    if (duration != null) {
                        localApi.runZone(localZone.getId(), duration.intValue());
                    }
                }
                break;
            default:
                logger.debug("Command received for an unknown channel: {}", channelId);
                break;
        }
    }

    @Override
    public void initialize() {
        config = getConfigAs(RachioZoneConfiguration.class);
        logger.debug("Rachio zone config: {}", config.toString());

        RachioBridgeHandler bridgeHandler = (RachioBridgeHandler) getBridge().getHandler();
        if (bridgeHandler != null) {
            api = bridgeHandler.getApi();
            device = bridgeHandler.getDevice(config.deviceId);

            if (device != null) {
                zone = device.getZone(config.zoneId);
                if (zone != null) {
                    updateStatus(ThingStatus.ONLINE);
                    // give the bridge a chance to fully initialize and get the device list
                    scheduler.schedule(this::updateChannels, 5, TimeUnit.SECONDS);
                    startPolling();
                } else {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                            "Could not find zone: " + config.zoneId);
                }
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
        RachioZone localZone = zone;

        if (localApi == null || localDevice == null || localZone == null) {
            return;
        }

        // refresh the device data
        localDevice = localApi.getDevice(localDevice.getId());
        if (localDevice != null) {
            device = localDevice;
            localZone = localDevice.getZone(localZone.getId());
            if (localZone != null) {
                zone = localZone;

                ThingHandlerCallback callback = getCallback();
                if (callback != null) {
                    callback.statusUpdated(getThing(), ThingStatus.ONLINE);

                    updateState(CHANNEL_ZONE_RUN, OnOffType.from(localZone.isRunning()));
                    updateState(CHANNEL_ZONE_DURATION, new QuantityType<>(localZone.getDuration(), Units.SECOND));
                }
            }
        }
    }

    public void channelLinked(ChannelUID channelUID) {
        updateChannels();
    }
}
