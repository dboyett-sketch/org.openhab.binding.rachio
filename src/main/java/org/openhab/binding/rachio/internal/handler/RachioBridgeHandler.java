package org.openhab.binding.rachio.internal.handler;

import static org.openhab.binding.rachio.RachioBindingConstants.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.RachioConfiguration;
import org.openhab.binding.rachio.internal.api.RachioApi;
import org.openhab.binding.rachio.internal.api.RachioApiException;
import org.openhab.binding.rachio.internal.api.RachioDevice;
import org.openhab.binding.rachio.internal.api.RachioZone;
import org.openhab.binding.rachio.internal.api.RachioEvent;
import org.openhab.binding.rachio.internal.api.RachioImageServlet;
import org.openhab.binding.rachio.internal.api.RachioWebHookServlet;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = ThingHandler.class, configurationPid = "bridge.rachio", configurationPolicy = ConfigurationPolicy.OPTIONAL)
@NonNullByDefault
public class RachioBridgeHandler extends BaseBridgeHandler {
    private final Logger logger = LoggerFactory.getLogger(RachioBridgeHandler.class);

    private @Nullable RachioConfiguration config;
    private @Nullable RachioApi api;
    private @Nullable ScheduledFuture<?> pollingJob;

    private @Nullable RachioImageServlet imageServlet;
    private @Nullable RachioWebHookServlet webHookServlet;

    private final List<RachioStatusListener> listeners = new ArrayList<>();

    public RachioBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    @Activate
    public void activate() {
        logger.debug("RachioBridgeHandler: Activating bridge '{}'", getThing().getUID());
    }

    @Modified
    protected void modified(Map<String, Object> configuration) {
        logger.debug("RachioBridgeHandler: Configuration modified for bridge '{}'", getThing().getUID());
        dispose();
        initialize();
    }

    @Override
    public void initialize() {
        logger.debug("RachioBridgeHandler: Initializing bridge '{}'", getThing().getUID());

        config = getConfigAs(RachioConfiguration.class);
        if (config == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Missing configuration");
            return;
        }

        if (config.apikey == null || config.apikey.trim().isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "API key is required");
            return;
        }

        scheduler.execute(() -> {
            try {
                initializeBridge();
            } catch (Exception e) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            }
        });
    }

    private void initializeBridge() {
        try {
            api = new RachioApi("");
            if (!api.initialize(config.apikey, getThing().getUID())) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "API initialization failed");
                return;
            }

            updateStatus(ThingStatus.ONLINE);
            logger.info("RachioBridgeHandler: Bridge initialized successfully for user {}", config.apikey.substring(0, 8) + "...");

            // Start polling if configured
            startPolling();

        } catch (Exception e) {
            logger.error("RachioBridgeHandler: Initialization failed: {}", e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    private void startPolling() {
        stopPolling();
        
        if (config != null && config.pollingInterval > 0) {
            pollingJob = scheduler.scheduleWithFixedDelay(this::refreshThings, 10, config.pollingInterval, TimeUnit.SECONDS);
            logger.debug("RachioBridgeHandler: Started polling with interval {} seconds", config.pollingInterval);
        }
    }

    private void stopPolling() {
        if (pollingJob != null) {
            pollingJob.cancel(true);
            pollingJob = null;
        }
    }

    private void refreshThings() {
        try {
            if (api != null) {
                // Refresh device states
                for (RachioStatusListener listener : listeners) {
                    listener.onRefreshRequested();
                }
            }
        } catch (Exception e) {
            logger.debug("RachioBridgeHandler: Error during refresh: {}", e.getMessage());
        }
    }

    @Override
    public void dispose() {
        logger.debug("RachioBridgeHandler: Disposing bridge '{}'", getThing().getUID());
        stopPolling();
        listeners.clear();
        api = null;
        super.dispose();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("RachioBridgeHandler: Command {} received for channel {}", command, channelUID);
        
        if (command instanceof RefreshType) {
            // Handle refresh if needed
        }
    }

    public String getApiKey() {
        return config != null ? config.apikey : "";
    }

    public int getDefaultRuntime() {
        return config != null ? config.defaultRuntime : 120;
    }

    public @Nullable RachioDevice getDevByUID(ThingUID thingUID) {
        final RachioApi localApi = api;
        if (localApi == null) {
            return null;
        }
        ThingUID bridgeUID = getThing().getUID();
        return localApi.getDevByUID(bridgeUID, thingUID);
    }

    public @Nullable RachioZone getZoneByUID(ThingUID zoneUID) {
        final RachioApi localApi = api;
        if (localApi == null) {
            return null;
        }
        ThingUID bridgeUID = getThing().getUID();
        return localApi.getZoneByUID(bridgeUID, zoneUID);
    }

    public void registerStatusListener(RachioStatusListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            logger.debug("RachioBridgeHandler: Registered status listener {}", listener.getClass().getSimpleName());
        }
    }

    public void unregisterStatusListener(RachioStatusListener listener) {
        listeners.remove(listener);
        logger.debug("RachioBridgeHandler: Unregistered status listener {}", listener.getClass().getSimpleName());
    }

    public void startZone(String zoneId, int runtime) throws RachioApiException {
        final RachioApi localApi = api;
        if (localApi != null) {
            localApi.runZone(zoneId, runtime);
        }
    }

    public void stopWatering(String deviceId) throws RachioApiException {
        final RachioApi localApi = api;
        if (localApi != null) {
            localApi.stopWatering(deviceId);
        }
    }

    public void startRainDelay(String deviceId, int seconds) throws RachioApiException {
        final RachioApi localApi = api;
        if (localApi != null) {
            localApi.rainDelay(deviceId, seconds);
        }
    }

    public void disableDevice(String deviceId) throws RachioApiException {
        final RachioApi localApi = api;
        if (localApi != null) {
            localApi.disableDevice(deviceId);
        }
    }

    public void enableDevice(String deviceId) throws RachioApiException {
        final RachioApi localApi = api;
        if (localApi != null) {
            localApi.enableDevice(deviceId);
        }
    }

    public void runMultipleZones(String zoneListJson) throws RachioApiException {
        final RachioApi localApi = api;
        if (localApi != null) {
            localApi.runMultilpeZones(zoneListJson);
        }
    }

    public void registerWebHook(String callbackUrl) throws RachioApiException {
        final RachioApi localApi = api;
        if (localApi == null) {
            return;
        }
        String externalId = localApi.getExternalId();
        String deviceId = "";
        if (!localApi.getDevices().isEmpty()) {
            deviceId = localApi.getDevices().entrySet().iterator().next().getKey();
        }
        if (deviceId.isEmpty()) {
            logger.warn("RachioBridgeHandler: No devices available to register webhook");
            return;
        }
        localApi.registerWebHook(deviceId, callbackUrl, externalId, Boolean.TRUE);
    }

    public void registerWebHook(String deviceId, String callbackUrl, boolean clearAllCallbacks) throws RachioApiException {
        final RachioApi localApi = api;
        if (localApi != null) {
            localApi.registerWebHook(deviceId, callbackUrl, localApi.getExternalId(), Boolean.valueOf(clearAllCallbacks));
        }
    }

    public void webHookEvent(RachioEvent event) {
        final RachioApi localApi = api;
        if (localApi == null) {
            return;
        }
        logger.debug("RachioBridgeHandler: Received webhook event type={} deviceId={} zoneId={}",
                event.eventType, event.deviceId, event.zoneId);

        RachioDevice dev = null;
        RachioZone zone = null;

        try {
            dev = localApi.getDevices().get(event.deviceId);
        } catch (Exception ignored) {
        }

        if (event.zoneId != null && !event.zoneId.isEmpty()) {
            try {
                for (RachioDevice d : localApi.getDevices().values()) {
                    for (RachioZone z : d.getZones().values()) {
                        if (event.zoneId.equals(z.id)) {
                            zone = z;
                            break;
                        }
                    }
                    if (zone != null) {
                        break;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        for (RachioStatusListener listener : listeners) {
            try {
                listener.onThingStateChanged(dev, zone);
            } catch (Exception e) {
                logger.warn("RachioBridgeHandler: Listener {} threw: {}", listener.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    public void setImageServlet(RachioImageServlet servlet) {
        this.imageServlet = servlet;
        servlet.injectBridgeHandler(this);
    }

    public void setWebHookServlet(RachioWebHookServlet servlet) {
        this.webHookServlet = servlet;
        servlet.injectBridgeHandler(this);
    }

    public @Nullable RachioConfiguration getThingConfig() {
        return config;
    }

    public @Nullable RachioImageServlet getImageServlet() {
        return imageServlet;
    }

    public @Nullable RachioWebHookServlet getWebHookServlet() {
        return webHookServlet;
    }

    public @Nullable RachioApi getApi() {
        return api;
    }
}
