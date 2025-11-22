package org.openhab.binding.rachio.internal.handler;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.RachioDevice;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.binding.rachio.internal.api.RachioPerson;
import org.openhab.binding.rachio.internal.api.RachioZone;
import org.openhab.binding.rachio.internal.api.RachioEvent;
import org.openhab.binding.rachio.internal.config.RachioBridgeConfiguration;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Michael Lobstein - Initial contribution
 */

@NonNullByDefault
public class RachioBridgeHandler extends BaseBridgeHandler {
    private final Logger logger = LoggerFactory.getLogger(RachioBridgeHandler.class);

    private RachioBridgeConfiguration config = new RachioBridgeConfiguration();

    private @Nullable RachioHttp api;
    private @Nullable RachioPerson person;
    private final List<RachioDevice> devices = new CopyOnWriteArrayList<>();
    private final List<RachioStatusListener> listeners = new CopyOnWriteArrayList<>();
    private @Nullable ScheduledFuture<?> pollingJob;

    public RachioBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // No commands to handle for the bridge
    }

    @Override
    public void initialize() {
        config = getConfigAs(RachioBridgeConfiguration.class);
        logger.debug("Rachio bridge config: {}", config.toString());

        if (config.apiKey.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "API key not configured");
            return;
        }

        api = new RachioHttp(config.apiKey);
        api.setHandler(this);

        updateStatus(ThingStatus.UNKNOWN);

        // give the binding a chance to get the device list
        scheduler.schedule(this::updateThings, 5, TimeUnit.SECONDS);
        startPolling();
    }

    @Override
    public void dispose() {
        stopPolling();
        if (api != null) {
            api.dispose();
        }
        super.dispose();
    }

    /**
     * Get the API instance
     */
    public @Nullable RachioHttp getApi() {
        return api;
    }

    /**
     * Get a device by ID
     */
    public @Nullable RachioDevice getDevice(String deviceId) {
        for (RachioDevice device : devices) {
            if (deviceId.equals(device.getId())) {
                return device;
            }
        }
        return null;
    }

    /**
     * Get all devices
     */
    public List<RachioDevice> getDevices() {
        return new ArrayList<>(devices);
    }

    /**
     * Add a status listener
     */
    public void addStatusListener(RachioStatusListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove a status listener
     */
    public void removeStatusListener(RachioStatusListener listener) {
        listeners.remove(listener);
    }

    private void startPolling() {
        ScheduledFuture<?> localPollingJob = pollingJob;
        if (localPollingJob == null || localPollingJob.isCancelled()) {
            pollingJob = scheduler.scheduleWithFixedDelay(this::updateThings, 30, 30, TimeUnit.SECONDS);
        }
    }

    private void stopPolling() {
        ScheduledFuture<?> localPollingJob = pollingJob;
        if (localPollingJob != null && !localPollingJob.isCancelled()) {
            localPollingJob.cancel(true);
            pollingJob = null;
        }
    }

    private void updateThings() {
        RachioHttp localApi = api;

        if (localApi == null) {
            return;
        }

        try {
            // Get person data which includes devices
            person = localApi.getPerson();
            if (person != null) {
                devices.clear();
                devices.addAll(person.getDevices());
                updateStatus(ThingStatus.ONLINE);

                // Notify listeners
                for (RachioStatusListener listener : listeners) {
                    listener.onRefreshRequested();
                }

                logger.debug("Found {} devices", devices.size());
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Unable to get person data");
            }
        } catch (Exception e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            logger.debug("Error updating things: {}", e.getMessage());
        }
    }

    /**
     * Handle a webhook event
     */
    public void onWebhookEvent(RachioEvent event) {
        String eventType = event.getEventType();
        String deviceId = event.getDeviceId();

        logger.debug("Received webhook event: {} for device: {}", eventType, deviceId);

        if ("DEVICE_STATUS_EVENT".equals(eventType)) {
            // Update device status
            for (RachioStatusListener listener : listeners) {
                listener.updateDeviceStatus(ThingStatus.ONLINE);
            }
        } else if ("ZONE_STATUS_EVENT".equals(eventType)) {
            // Update zone status - need to find which zone
            RachioDevice device = getDevice(deviceId);
            if (device != null) {
                // This is a simplification - you'll need to extract zoneId from the event
                // In a real implementation, you'd parse the event to get the specific zone
                for (RachioZone zone : device.getZones()) {
                    for (RachioStatusListener listener : listeners) {
                        listener.updateZoneStatus(zone.getId(), ThingStatus.ONLINE);
                    }
                }
            }
        }
    }

    /**
     * Notify listeners of thing state changes
     */
    public void notifyThingStateChanged(RachioDevice device, RachioZone zone) {
        for (RachioStatusListener listener : listeners) {
            listener.onThingStateChanged(device, zone);
        }
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singleton(RachioActions.class);
    }
}
