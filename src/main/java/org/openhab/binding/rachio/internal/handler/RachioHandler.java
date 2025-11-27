package org.openhab.binding.rachio.internal.handler;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.binding.rachio.internal.api.dto.RachioWebhookEvent;
import org.openhab.binding.rachio.internal.api.dto.RachioEventSummary;
import org.openhab.binding.rachio.internal.api.dto.RachioPerson;
import org.openhab.binding.rachio.internal.api.dto.RachioZone;
import org.openhab.binding.rachio.internal.api.dto.RachioException;
import org.openhab.binding.rachio.internal.config.RachioConfiguration;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.ImperialUnits;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * The {@link RachioHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Brian Gleason - Initial contribution
 */
@NonNullByDefault
public class RachioHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(RachioHandler.class);
    private final Gson gson = new Gson();
    
    @Reference
    private RachioHttp rachioHttp;
    
    private @Nullable RachioPerson person;
    private @Nullable ScheduledFuture<?> pollingJob;
    private int pollingInterval = 30;
    private @Nullable String deviceId;

    public RachioHandler(Thing thing) {
        super(thing);
    }

    @Activate
    @Override
    public void initialize() {
        logger.debug("Initializing Rachio handler.");
        RachioConfiguration config = getConfigAs(RachioConfiguration.class);

        String apiKey = config.apiKey;
        if (apiKey == null || apiKey.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "API Key not set");
            return;
        }

        pollingInterval = config.pollingInterval;
        if (pollingInterval < 1) {
            pollingInterval = 30;
        }

        // Register with RachioHttp service
        String thingId = getThing().getUID().toString();
        rachioHttp.registerThing(thingId, apiKey);

        // Start the polling job
        startPolling();

        updateStatus(ThingStatus.UNKNOWN);

        logger.debug("Finished initializing Rachio handler.");
    }

    @Deactivate
    @Override
    public void dispose() {
        logger.debug("Disposing Rachio handler.");
        stopPolling();
        
        // Unregister from RachioHttp service
        String thingId = getThing().getUID().toString();
        rachioHttp.unregisterThing(thingId);
        
        super.dispose();
    }

    private void startPolling() {
        ScheduledFuture<?> job = pollingJob;
        if (job == null || job.isCancelled()) {
            pollingJob = scheduler.scheduleWithFixedDelay(this::poll, 10, pollingInterval, TimeUnit.SECONDS);
        }
    }

    private void stopPolling() {
        ScheduledFuture<?> job = pollingJob;
        if (job != null && !job.isCancelled()) {
            job.cancel(true);
            pollingJob = null;
        }
    }

    private void poll() {
        logger.debug("Polling for Rachio data");
        try {
            String thingId = getThing().getUID().toString();

            // Get person data if not already available
            if (person == null) {
                person = rachioHttp.getPerson(thingId);
                if (person == null) {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Failed to get person data");
                    return;
                }
            }

            // Update device status
            updateDeviceStatus();

            updateStatus(ThingStatus.ONLINE);

        } catch (RachioException e) {
            logger.debug("Error during polling: {}", e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        } catch (Exception e) {
            logger.debug("Unexpected error during polling: {}", e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Unexpected error: " + e.getMessage());
        }
    }

    private void updateDeviceStatus() {
        if (person == null || person.devices == null) {
            return;
        }

        try {
            String thingId = getThing().getUID().toString();
            
            // Update each device
            for (RachioPerson.Device device : person.devices) {
                // Store the first device ID for webhook routing
                if (this.deviceId == null) {
                    this.deviceId = device.id;
                }
                
                RachioEventSummary summary = rachioHttp.getDeviceEventSummary(thingId, device.id);
                if (summary != null) {
                    updateDeviceChannels(device.id, summary);
                }
            }
        } catch (RachioException e) {
            logger.debug("Error updating device status: {}", e.getMessage(), e);
        }
    }

    private void updateDeviceChannels(String deviceId, RachioEventSummary summary) {
        // Update common device channels
        updateState(new ChannelUID(getThing().getUID(), DEVICE, deviceId, STATUS), 
                   new StringType(summary.status));

        // Update zone channels if available
        if (summary.zoneData != null) {
            for (RachioEventSummary.ZoneSummary zone : summary.zoneData) {
                updateZoneChannels(deviceId, zone);
            }
        }
    }

    private void updateZoneChannels(String deviceId, RachioEventSummary.ZoneSummary zone) {
        String zoneId = zone.zoneId;
        
        updateState(new ChannelUID(getThing().getUID(), ZONE, deviceId, zoneId, ZONE_NAME), 
                   new StringType(zone.name));
        updateState(new ChannelUID(getThing().getUID(), ZONE, deviceId, zoneId, ZONE_ENABLED), 
                   zone.enabled ? OnOffType.ON : OnOffType.OFF);
        updateState(new ChannelUID(getThing().getUID(), ZONE, deviceId, zoneId, ZONE_RUNTIME), 
                   new QuantityType<>(zone.runtime, ImperialUnits.SECOND));
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            // Refresh data for this channel
            scheduler.execute(this::poll);
            return;
        }

        try {
            String groupId = channelUID.getGroupId();
            if (groupId == null) {
                return;
            }

            if (ZONE.equals(groupId)) {
                handleZoneCommand(channelUID, command);
            } else if (DEVICE.equals(groupId)) {
                handleDeviceCommand(channelUID, command);
            }
        } catch (Exception e) {
            logger.error("Error handling command: {}", e.getMessage(), e);
        }
    }

    private void handleZoneCommand(ChannelUID channelUID, Command command) {
        // Extract deviceId and zoneId from channel UID
        String[] parts = channelUID.getGroupId().split("#");
        if (parts.length < 3) {
            return;
        }
        String deviceId = parts[1];
        String zoneId = parts[2];
        String channelId = channelUID.getIdWithoutGroup();

        try {
            String thingId = getThing().getUID().toString();
            
            if (ZONE_ENABLED.equals(channelId) && command instanceof OnOffType) {
                boolean enable = (command == OnOffType.ON);
                rachioHttp.setZoneEnabled(thingId, zoneId, enable);
                
                // Update local state
                updateState(channelUID, (OnOffType) command);
            } else if (RUN_ZONE.equals(channelId) && command instanceof DecimalType) {
                int duration = ((DecimalType) command).intValue();
                rachioHttp.startZone(thingId, zoneId, duration);
                logger.info("Started zone {} for {} seconds", zoneId, duration);
            }
        } catch (RachioException e) {
            logger.error("Error handling zone command: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error handling zone command: {}", e.getMessage(), e);
        }
    }

    private void handleDeviceCommand(ChannelUID channelUID, Command command) {
        String[] parts = channelUID.getGroupId().split("#");
        if (parts.length < 2) {
            return;
        }
        String deviceId = parts[1];
        String channelId = channelUID.getIdWithoutGroup();

        try {
            String thingId = getThing().getUID().toString();
            
            if (STOP_WATERING.equals(channelId) && command instanceof OnOffType && command == OnOffType.ON) {
                rachioHttp.stopWatering(thingId, deviceId);
                // Reset the switch
                updateState(channelUID, OnOffType.OFF);
                logger.info("Stopped watering for device {}", deviceId);
            }
        } catch (RachioException e) {
            logger.error("Error handling device command: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error handling device command: {}", e.getMessage(), e);
        }
    }

    public void handleWebhookCall(HttpServletRequest req) {
        try {
            String payload = req.getReader().lines().reduce("", (accumulator, actual) -> accumulator + actual);
            logger.debug("Received webhook payload: {}", payload);

            // Use the new RachioWebhookEvent DTO
            RachioWebhookEvent event = gson.fromJson(payload, RachioWebhookEvent.class);
            if (event != null) {
                processWebhookEvent(event);
            }
        } catch (IOException | JsonSyntaxException e) {
            logger.error("Error processing webhook: {}", e.getMessage(), e);
        }
    }

    public void handleImageCall(HttpServletRequest req, HttpServletResponse resp) {
        try {
            String pathInfo = req.getPathInfo();
            if (pathInfo != null && pathInfo.startsWith("/")) {
                String imageId = pathInfo.substring(1);
                String thingId = getThing().getUID().toString();
                
                byte[] imageData = rachioHttp.getImage(thingId, imageId);
                if (imageData != null) {
                    resp.setContentType("image/jpeg");
                    resp.getOutputStream().write(imageData);
                    return;
                }
            }
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } catch (RachioException e) {
            logger.error("Error serving image: {}", e.getMessage(), e);
            try {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } catch (Exception ex) {
                // Ignore
            }
        } catch (IOException e) {
            logger.error("Error serving image: {}", e.getMessage(), e);
            try {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } catch (Exception ex) {
                // Ignore
            }
        }
    }

    private void processWebhookEvent(RachioWebhookEvent event) {
        logger.debug("Processing webhook event: {}", event.eventType);

        // Check if this webhook is for our device
        String localDeviceId = this.deviceId;
        if (localDeviceId != null && !localDeviceId.equals(event.deviceId)) {
            logger.debug("Webhook event not for our device. Our: {}, Event: {}", localDeviceId, event.deviceId);
            return;
        }

        // Update relevant channels based on event type
        if (event.device != null) {
            updateDeviceFromEvent(event.device);
        }

        if (event.zone != null) {
            updateZoneFromEvent(event);
        }

        // Trigger a refresh to get latest data
        scheduler.schedule(this::poll, 1, TimeUnit.SECONDS);
    }

    private void updateDeviceFromEvent(RachioWebhookEvent.WebhookDevice device) {
        if (device.id != null && !device.id.isEmpty()) {
            updateState(new ChannelUID(getThing().getUID(), DEVICE, device.id, STATUS),
                       new StringType(device.status));
            updateState(new ChannelUID(getThing().getUID(), DEVICE, device.id, "power"),
                       device.on ? OnOffType.ON : OnOffType.OFF);
        }
    }

    private void updateZoneFromEvent(RachioWebhookEvent event) {
        if (event.zone != null && event.deviceId != null && !event.deviceId.isEmpty()) {
            String zoneId = event.zone.id;
            if (zoneId != null && !zoneId.isEmpty()) {
                updateState(new ChannelUID(getThing().getUID(), ZONE, event.deviceId, zoneId, ZONE_STATUS),
                           new StringType(event.zone.status));
                
                if (event.zone.duration > 0) {
                    updateState(new ChannelUID(getThing().getUID(), ZONE, event.deviceId, zoneId, "lastDuration"),
                               new QuantityType<>(event.zone.duration, ImperialUnits.SECOND));
                }
            }
        }
    }

    /**
     * Check if this handler manages the specified device
     */
    public boolean handlesDevice(String deviceId) {
        return deviceId != null && deviceId.equals(this.deviceId);
    }

    /**
     * Get the device ID managed by this handler
     */
    @Nullable
    public String getDeviceId() {
        return deviceId;
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singleton(RachioActionHandler.class);
    }
}
