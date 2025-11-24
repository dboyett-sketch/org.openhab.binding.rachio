package org.openhab.binding.rachio.internal.handler;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.RachioApiException;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * The {@link RachioBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Damion Boyett - Initial contribution
 */
@NonNullByDefault
public class RachioBridgeHandler extends BaseBridgeHandler {
    private final Logger logger = LoggerFactory.getLogger(RachioBridgeHandler.class);

    private @Nullable RachioHttp localApi;
    private final Gson gson = new Gson();
    private @Nullable ScheduledFuture<?> refreshJob;

    public RachioBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // No commands to handle for bridge
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Rachio bridge handler.");

        String apiKey = (String) getConfig().get(API_KEY);
        if (apiKey == null || apiKey.trim().isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "API Key is required");
            return;
        }

        localApi = new RachioHttp(apiKey, gson);
        scheduler.execute(this::initializeBridge);
    }

    private void initializeBridge() {
        RachioHttp api = localApi;
        if (api == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "API not initialized");
            return;
        }

        try {
            JsonObject personInfo = api.getPerson();
            logger.debug("Successfully connected to Rachio API for user: {}", 
                personInfo.get("username").getAsString());
            
            updateStatus(ThingStatus.ONLINE);
            startRefreshJob();
        } catch (RachioApiException e) {
            logger.debug("Failed to initialize Rachio bridge: {}", e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    private void startRefreshJob() {
        ScheduledFuture<?> job = refreshJob;
        if (job != null && !job.isCancelled()) {
            job.cancel(false);
        }

        refreshJob = scheduler.scheduleWithFixedDelay(this::refreshBridge, 1, 300, TimeUnit.SECONDS);
    }

    private void refreshBridge() {
        RachioHttp api = localApi;
        if (api == null || thing.getStatus() != ThingStatus.ONLINE) {
            return;
        }

        try {
            // Simple API call to verify connectivity
            api.getPerson();
        } catch (RachioApiException e) {
            logger.debug("Rachio bridge refresh failed: {}", e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    public @Nullable RachioHttp getApi() {
        return localApi;
    }

    public @Nullable JsonObject getDevice(String deviceId) {
        RachioHttp api = localApi;
        if (api == null) {
            return null;
        }

        try {
            return api.getDevice(deviceId);
        } catch (RachioApiException e) {
            logger.debug("Failed to get device {}: {}", deviceId, e.getMessage());
            return null;
        }
    }

    @Override
    public void dispose() {
        ScheduledFuture<?> job = refreshJob;
        if (job != null && !job.isCancelled()) {
            job.cancel(true);
        }
        refreshJob = null;
        localApi = null;
        super.dispose();
    }
}
