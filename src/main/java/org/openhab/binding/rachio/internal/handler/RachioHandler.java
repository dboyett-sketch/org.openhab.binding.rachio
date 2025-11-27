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
import org.openhab.binding.rachio.internal.api.RachioEvent;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.binding.rachio.internal.config.RachioConfiguration;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * The {@link RachioHandler} is responsible for handling commands for Rachio devices
 */
@NonNullByDefault
public class RachioHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(RachioHandler.class);
    private final Gson gson = new Gson();
    private @Nullable RachioHttp rachioHttp;
    private @Nullable ScheduledFuture<?> pollingJob;
    private int pollingInterval = 30;

    public RachioHandler(Thing thing) {
        super(thing);
    }

    @Activate
    @Override
    public void initialize() {
        logger.debug("Initializing Rachio handler");
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

        rachioHttp = new RachioHttp(apiKey, scheduler);
        startPolling();
        updateStatus(ThingStatus.ONLINE);
        
        logger.debug("Rachio handler initialized successfully");
    }

    @Deactivate
    @Override
    public void dispose() {
        logger.debug("Disposing Rachio handler");
        stopPolling();
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
        logger.debug("Polling Rachio device data");
        // Implement polling logic here
        // This would fetch device status from Rachio API
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            // Refresh channel data
            scheduler.execute(this::poll);
            return;
        }

        try {
            String groupId = channelUID.getGroupId();
            if (groupId == null) return;

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
        // Handle zone commands (enable/disable, run zone)
        logger.debug("Handling zone command: {} = {}", channelUID, command);
    }

    private void handleDeviceCommand(ChannelUID channelUID, Command command) {
        // Handle device commands (stop watering, etc)
        logger.debug("Handling device command: {} = {}", channelUID, command);
    }

    public void handleWebhookCall(HttpServletRequest req) {
        try {
            String payload = req.getReader().lines().reduce("", (accumulator, actual) -> accumulator + actual);
            logger.debug("Received webhook: {}", payload);
            
            RachioEvent event = gson.fromJson(payload, RachioEvent.class);
            if (event != null) {
                processWebhookEvent(event);
            }
        } catch (Exception e) {
            logger.error("Error processing webhook: {}", e.getMessage(), e);
        }
    }

    public void handleImageCall(HttpServletRequest req, HttpServletResponse resp) {
        try {
            String pathInfo = req.getPathInfo();
            logger.debug("Image request: {}", pathInfo);
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND); // Temporary
        } catch (Exception e) {
            logger.error("Error serving image: {}", e.getMessage(), e);
        }
    }

    private void processWebhookEvent(RachioEvent event) {
        logger.debug("Processing webhook event: {}", event.eventType);
        // Update thing channels based on webhook event
        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singleton(RachioActionHandler.class);
    }
}
