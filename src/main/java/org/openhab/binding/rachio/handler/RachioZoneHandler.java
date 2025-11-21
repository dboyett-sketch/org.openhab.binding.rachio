package org.openhab.binding.rachio.handler;

import static org.openhab.binding.rachio.RachioBindingConstants.*;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.openhab.binding.rachio.internal.api.*;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.ThingTypeUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.openhab.binding.rachio.RachioBindingConstants.*;

@Component(service = ThingHandler.class, configurationPolicy = ConfigurationPolicy.OPTIONAL)
@NonNullByDefault
public class RachioZoneHandler extends BaseThingHandler implements RachioStatusListener {
    private final Logger zoneLogger = LoggerFactory.getLogger(RachioZoneHandler.class);

    @Nullable private RachioBridgeHandler cloudHandler;
    @Nullable private Bridge bridge;
    @Nullable private RachioDevice dev;
    @Nullable private RachioZone zone;

    private final Map<String, State> channelData = new HashMap<>();

    public RachioZoneHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        zoneLogger.debug("RachioZone: Initializing zone '{}'", getThing().getUID());

        try {
            bridge = getBridge();
            if (bridge != null) {
                ThingHandler handler = bridge.getHandler();
                if (handler instanceof RachioBridgeHandler) {
                    cloudHandler = (RachioBridgeHandler) handler;
                    zone = cloudHandler.getZoneByUID(getThing().getUID());
                    if (zone != null) {
                        zone.setThingHandler(this);
                        zoneLogger.debug("RachioZoneHandler: bridge UID for zone '{}' is {}", zone.name, zone.getBridgeUID());
                        dev = cloudHandler.getDevByUID(zone.getDevUID());
                    }
                }
            }

            if (bridge == null || cloudHandler == null || dev == null || zone == null) {
                zoneLogger.debug("RachioZone: Thing initialization failed!");
            } else {
                cloudHandler.registerStatusListener(this);
                if (bridge.getStatus() != ThingStatus.ONLINE) {
                    zoneLogger.debug("RachioZone: Bridge is offline");
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
                } else {
                    updateProperties(zone.fillProperties());
                    updateStatus(dev.getStatus());
                    return;
                }
            }
        } catch (Exception e) {
            zoneLogger.error("RachioZone: Initialization failed: {}", e.getMessage());
        }

        updateStatus(ThingStatus.OFFLINE);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        String channel = channelUID.getId();
        zoneLogger.debug("RachioZone.handleCommand {} for {}", command, channelUID);

        if (cloudHandler == null || zone == null) {
            zoneLogger.debug("RachioZone: Cloud handler or zone not initialized");
            return;
        }

        try {
            if (command instanceof RefreshType) {
                postChannelData();
            } else if (channel.equals(CHANNEL_ZONE_ENABLED)) {
                if (command instanceof OnOffType) {
                    // Placeholder for enable/disable logic
                }
            } else if (channel.equals(CHANNEL_ZONE_RUN)) {
                if (command instanceof OnOffType) {
                    if (command == OnOffType.ON) {
                        int runtime = zone.getStartRunTime();
                        if (runtime == 0 && cloudHandler != null) {
                            runtime = cloudHandler.getDefaultRuntime();
                            zoneLogger.debug("RachioZone: Using default runtime of {} seconds", runtime);
                        }
                        zoneLogger.info("RachioZone: Starting zone '{} [{}]' for {} seconds", zone.name, zone.zoneNumber, runtime);
                        cloudHandler.startZone(zone.id, runtime);
                    } else {
                        zoneLogger.info("RachioZone: Stopping watering for device '{}'", dev != null ? dev.id : "unknown");
                        if (dev != null) {
                            cloudHandler.stopWatering(dev.id);
                        }
                    }
                } else {
                    zoneLogger.debug("RachioZone: Invalid command type for '{}': {}", channel, command);
                }
            } else if (channel.equals(CHANNEL_ZONE_RUN_TIME)) {
                if (command instanceof DecimalType) {
                    int runtime = ((DecimalType) command).intValue();
                    zoneLogger.info("RachioZone: Zone runtime set to {} seconds", runtime);
                    zone.setStartRunTime(runtime);
                } else {
                    zoneLogger.debug("RachioZone: Invalid command type for runtime: {}", command);
                }
            }
        } catch (RachioApiException e) {
            handleError(e.toString());
        } catch (Throwable e) {
            handleError(e.getMessage() != null ? e.getMessage() : e.toString());
        }
    }

    private void handleError(String errorMessage) {
        zoneLogger.error("RachioZoneHandler: {}", errorMessage);
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, errorMessage);
    }

    @Override
    public boolean onThingStateChanged(@Nullable RachioDevice updatedDev, @Nullable RachioZone updatedZone) {
        if (updatedZone != null && zone != null && zone.id.equals(updatedZone.id)) {
            zoneLogger.debug("RachioZone: Update for zone '{}' received", zone.id);
            zone.update(updatedZone);
            postChannelData();
            updateStatus(dev != null ? dev.getStatus() : ThingStatus.ONLINE);
            return true;
        }
        return false;
    }

    public boolean webhookEvent(RachioEvent event) {
        boolean update = false;
        try {
            if (dev != null) {
                dev.setEvent(event);
            }

            String zoneName = event.zoneName;
            if ("ZONE_STATUS".equals(event.type)) {
                switch (event.zoneRunStatus.state) {
                    case "STARTED":
                        zoneLogger.info("RachioZone[{}]: '{}' STARTED watering ({})",
                                zone.zoneNumber, zoneName, event.timestamp);
                        updateState(CHANNEL_ZONE_RUN, OnOffType.ON);
                        break;
                    case "ZONE_STOPPED":
                    case "ZONE_COMPLETED":
                        zoneLogger.info("RachioZone[{}]: '{}' STOPPED watering (timestamp={}, current={}, duration={}sec/{}min, flowVolume={})",
                                zone.zoneNumber, zoneName, event.timestamp, event.zoneCurrent,
                                event.duration, event.durationInMinutes, event.flowVolume);
                        updateState(CHANNEL_ZONE_RUN, OnOffType.OFF);
                        break;
                    default:
                        zoneLogger.info("RachioZone: Event for zone[{}] '{}': {} (status={}, duration={}sec)",
                                zone.zoneNumber, zoneName, event.summary, event.zoneRunStatus.state, event.duration);
                        break;
                }
                update = true;
            } else if ("ZONE_DELTA".equals(event.subType)) {
                zoneLogger.info("RachioZone: DELTA Event for zone#{} '{}': {}.{}",
                        zone.zoneNumber, zone.name, event.category, event.action);
                update = true;
            } else {
                zoneLogger.debug("RachioZone: Unhandled event type '{}_{}' for zone '{}'",
                        event.type, event.subType, zoneName);
            }

            if (update) {
                postChannelData();
            }
        } catch (Throwable e) {
            zoneLogger.error("RachioZone: Unable to process event: {}", e.getMessage());
        }

        return update;
    }

    public void postChannelData() {
        if (zone != null) {
            updateChannel(CHANNEL_ZONE_NAME, new StringType(zone.name));
            updateChannel(CHANNEL_ZONE_NUMBER, new DecimalType(zone.zoneNumber));
            updateChannel(CHANNEL_ZONE_ENABLED, zone.getEnabled());
            updateChannel(CHANNEL_ZONE_RUN, OnOffType.OFF);
            updateChannel(CHANNEL_ZONE_RUN_TIME, new DecimalType(zone.getStartRunTime()));
            updateChannel(CHANNEL_ZONE_RUN_TOTAL, new DecimalType(zone.runtime));
            updateChannel(CHANNEL_ZONE_IMAGEURL, new StringType(zone.imageUrl));
        }
    }

    private boolean updateChannel(String channelName, State newValue) {
        State currentValue = channelData.get(channelName);
        if (currentValue != null && currentValue.equals(newValue)) {
            return false;
        }

        channelData.put(channelName, newValue);
        updateState(channelName, newValue);
        return true;
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        super.bridgeStatusChanged(bridgeStatusInfo);

        zoneLogger.trace("RachioZoneHandler: Bridge status changed to {}", bridgeStatusInfo.getStatus());
        if (bridgeStatusInfo.getStatus() == ThingStatus.ONLINE) {
            updateProperties(zone.fillProperties());
            updateStatus(dev != null ? dev.getStatus() : ThingStatus.ONLINE);
            postChannelData();
                } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
        }
    }

    public void shutdown() {
        updateStatus(ThingStatus.OFFLINE);
    }

    private void updateProperties() {
        if (cloudHandler != null && zone != null) {
            zoneLogger.trace("RachioZoneHandler: Updating zone properties");
            Map<String, String> props = zone.fillProperties();
            if (props != null) {
                updateProperties(props);
            } else {
                zoneLogger.debug("RachioZoneHandler: No properties available for update");
            }
        }
    }
}