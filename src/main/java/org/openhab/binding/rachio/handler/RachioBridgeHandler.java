package org.openhab.binding.rachio.handler;

import static org.openhab.binding.rachio.RachioBindingConstants.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.types.Command;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = ThingHandler.class)
@NonNullByDefault
public class RachioBridgeHandler extends BaseBridgeHandler {
    private final Logger rachioLogger = LoggerFactory.getLogger(RachioBridgeHandler.class);

    private @Nullable RachioConfiguration thingConfig;
    private @Nullable RachioApi api;

    private @Nullable RachioImageServlet imageServlet;
    private @Nullable RachioWebHookServlet webHookServlet;

    private final List<RachioStatusListener> listeners = new ArrayList<>();

    public RachioBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void initialize() {
        rachioLogger.debug("RachioBridgeHandler: Initializing bridge '{}'", getThing().getUID());

        try {
            thingConfig = getConfigAs(RachioConfiguration.class);

            if (thingConfig != null) {
                thingConfig.updateConfig(getConfig().getProperties());
                rachioLogger.debug("RachioBridgeHandler: Configuration loaded: {}", thingConfig);
            } else {
                rachioLogger.warn("RachioBridgeHandler: No configuration available");
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Missing configuration");
                return;
            }

            api = new RachioApi("");
            if (!api.initialize(thingConfig.apikey, getThing().getUID())) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "API initialization failed");
                return;
            }

            updateStatus(ThingStatus.ONLINE);
            rachioLogger.info("RachioBridgeHandler: Bridge initialized successfully");

        } catch (Exception e) {
            rachioLogger.error("RachioBridgeHandler: Initialization failed: {}", e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    @Override
    public void dispose() {
        rachioLogger.debug("RachioBridgeHandler: Disposing bridge '{}'", getThing().getUID());
        api = null;
        listeners.clear();
        super.dispose();
    }

    @Override
    public void handleCommand(org.openhab.core.thing.ChannelUID channelUID, Command command) {
        rachioLogger.debug("RachioBridgeHandler: Command {} received for channel {}", command, channelUID);
    }

    public String getApiKey() {
        return thingConfig != null ? thingConfig.apikey : "";
    }

    public int getDefaultRuntime() {
        return thingConfig != null ? thingConfig.defaultRuntime : 120;
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
            rachioLogger.debug("RachioBridgeHandler: Registered status listener {}", listener.getClass().getSimpleName());
        }
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
            rachioLogger.warn("RachioBridgeHandler: No devices available to register webhook");
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
        rachioLogger.debug("RachioBridgeHandler: Received webhook event type={} deviceId={} zoneId={}",
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
                rachioLogger.warn("RachioBridgeHandler: Listener {} threw: {}", listener.getClass().getSimpleName(), e.getMessage());
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
        return thingConfig;
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