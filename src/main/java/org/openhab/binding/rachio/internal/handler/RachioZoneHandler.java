package org.openhab.binding.rachio.internal.handler;

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
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.openhab.binding.rachio.internal.api.RachioApiException;
import org.openhab.binding.rachio.internal.api.RachioDevice;
import org.openhab.binding.rachio.internal.api.RachioEvent;
import org.openhab.binding.rachio.internal.api.RachioZone;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = RachioZoneHandler.class, configurationPolicy = ConfigurationPolicy.OPTIONAL)
@NonNullByDefault
public class RachioZoneHandler extends BaseThingHandler implements RachioStatusListener {
    private final Logger logger = LoggerFactory.getLogger(RachioZoneHandler.class);

    private @Nullable RachioBridgeHandler bridgeHandler;
    private @Nullable RachioDevice device;
    private @Nullable RachioZone zone;
    private final Map<String, State> channelData = new HashMap<>();

    public RachioZoneHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Rachio zone handler for thing: {}", getThing().getUID());

        Bridge bridge = getBridge();
        if (bridge == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Bridge not found");
            return;
        }

        ThingHandler handler = bridge.getHandler();
        if (handler instanceof RachioBridgeHandler) {
            bridgeHandler = (RachioBridgeHandler) handler;
            zone = bridgeHandler.getZoneByUID(getThing().getUID());
            
            if (zone != null) {
                zone.setThingHandler(this);
                device = bridgeHandler.getDevByUID(zone.getDevUID());
                bridgeHandler.registerStatusListener(this);

                if (bridge.getStatus() == ThingStatus.ONLINE) {
                    updateProperties(zone.fillProperties());
                    updateStatus(ThingStatus.ONLINE);
                    logger.debug("Rachio zone '{}' initialized successfully", getThing().getUID());
                } else {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
                }
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Zone not found in bridge");
            }
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Invalid bridge handler");
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        String channel = channelUID.getId();
        logger.debug("Handle command {} for channel {}", command, channelUID);

        if (bridgeHandler == null || zone == null) {
            logger.debug("Bridge handler or zone not initialized");
            return;
        }

        try {
            if (command instanceof RefreshType) {
                refreshChannels();
            } else {
                processZoneCommand(channel, command);
            }
        } catch (Exception e) {
            logger.error("Error processing command {} for channel {}: {}", command, channel, e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    private void processZoneCommand(String channel, Command command) throws RachioApiException {
        switch (channel) {
            case CHANNEL_ZONE_RUN:
                if (command instanceof OnOffType) {
                    if (command == OnOffType.ON) {
                        int runtime = zone.getStartRunTime();
                        if (runtime == 0 && bridgeHandler != null) {
                            runtime = bridgeHandler.getDefaultRuntime();
                            logger.debug("Using default runtime of {} seconds", runtime);
                        }
                        logger.info("Starting zone '{} [{}]' for {} seconds", zone.name, zone.zoneNumber, runtime);
                        bridgeHandler.startZone(zone.id, runtime);
                    } else {
                        logger.info("Stopping watering for device");
                        if (device != null) {
                            bridgeHandler.stopWatering(device.id);
                        }
                    }
                }
                break;
                
            case CHANNEL_ZONE_RUN_TIME:
                if (command instanceof DecimalType) {
                    int runtime = ((DecimalType) command).intValue();
                    logger.info("Setting zone runtime to {} seconds", runtime);
                    zone.setStartRunTime(runtime);
                }
                break;
                
            case CHANNEL_ZONE_ENABLED:
                if (command instanceof OnOffType) {
                    logger.debug("Zone enable/disable command received - functionality not implemented");
                    // Placeholder for future enable/disable functionality
                }
                break;
                
            default:
                logger.debug("Unhandled command for channel {}: {}", channel, command);
        }
    }

    @Override
    public boolean onThingStateChanged(@Nullable RachioDevice updatedDevice, @Nullable RachioZone updatedZone) {
        if (updatedZone != null && zone != null && zone.id.equals(updatedZone.id)) {
            logger.debug("Zone state changed for: {}", zone.id);
            zone.update(updatedZone);
            refreshChannels();
            updateStatus(device != null ? device.getStatus() : ThingStatus.ONLINE);
            return true;
        }
        return false;
    }

    @Override
    public void onRefreshRequested() {
        refreshChannels();
    }

    public boolean webhookEvent(RachioEvent event) {
        boolean update = false;
        
        try {
            if (device != null) {
                device.setEvent(event);
            }

            String zoneName = event.zoneName;
            if ("ZONE_STATUS".equals(event.type)) {
                update = handleZoneStatusEvent(event, zoneName);
            } else if ("ZONE_DELTA".equals(event.subType)) {
                logger.info("Zone DELTA event for zone#{} '{}': {}.{}",
                        zone != null ? zone.zoneNumber : "unknown", zoneName, event.category, event.action);
                update = true;
            } else {
                logger.debug("Unhandled event type '{}.{}' for zone '{}'",
                        event.type, event.subType, zoneName);
            }

            if (update) {
                refreshChannels();
            }
        } catch (Exception e) {
            logger.error("Error processing webhook event: {}", e.getMessage());
        }

        return update;
    }

    private boolean handleZoneStatusEvent(RachioEvent event, String zoneName) {
        if (zone == null) return false;
        
        switch (event.zoneRunStatus.state) {
            case "STARTED":
                logger.info("Zone[{}]: '{}' STARTED watering ({})",
                        zone.zoneNumber, zoneName, event.timestamp);
                updateState(CHANNEL_ZONE_RUN, OnOffType.ON);
                break;
            case "ZONE_STOPPED":
            case "ZONE_COMPLETED":
                logger.info("Zone[{}]: '{}' STOPPED watering (timestamp={}, duration={}sec)",
                        zone.zoneNumber, zoneName, event.timestamp, event.duration);
                updateState(CHANNEL_ZONE_RUN, OnOffType.OFF);
                break;
            default:
                logger.info("Zone[{}]: '{}' event: {} (status={}, duration={}sec)",
                        zone.zoneNumber, zoneName, event.summary, event.zoneRunStatus.state, event.duration);
                break;
        }
        return true;
    }

    private void refreshChannels() {
        if (zone != null) {
            updateChannel(CHANNEL_ZONE_NAME, new StringType(zone.name));
            updateChannel(CHANNEL_ZONE_NUMBER, new DecimalType(zone.zoneNumber));
            updateChannel(CHANNEL_ZONE_ENABLED, zone.getEnabled());
            updateChannel(CHANNEL_ZONE_RUN_TIME, new DecimalType(zone.getStartRunTime()));
            updateChannel(CHANNEL_ZONE_RUN_TOTAL, new DecimalType(zone.runtime));
            updateChannel(CHANNEL_ZONE_IMAGEURL, new StringType(zone.imageUrl));
            
            // Only update RUN channel if it's not already set by webhook events
            State currentRunState = channelData.get(CHANNEL_ZONE_RUN);
            if (currentRunState == null || currentRunState == OnOffType.OFF) {
                updateChannel(CHANNEL_ZONE_RUN, OnOffType.OFF);
            }
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
        logger.debug("Bridge status changed to: {}", bridgeStatusInfo.getStatus());
        
        if (bridgeStatusInfo.getStatus() == ThingStatus.ONLINE) {
            if (zone != null) {
                updateProperties(zone.fillProperties());
                refreshChannels();
                updateStatus(ThingStatus.ONLINE);
            }
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
        }
    }

    @Override
    public void dispose() {
        if (bridgeHandler != null) {
            bridgeHandler.unregisterStatusListener(this);
        }
        super.dispose();
    }
}
