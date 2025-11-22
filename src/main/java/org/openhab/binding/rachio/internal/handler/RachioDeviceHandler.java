package org.openhab.binding.rachio.internal.handler;

import java.util.HashMap;
import java.util.Map;
import org.openhab.core.thing.ThingHandler;
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

import static org.openhab.binding.rachio.RachioBindingConstants.*;

@Component(service = RachioDeviceHandler.class, configurationPolicy = ConfigurationPolicy.OPTIONAL)
@NonNullByDefault
public class RachioDeviceHandler extends BaseThingHandler implements RachioStatusListener {

    private final Logger logger = LoggerFactory.getLogger(RachioDeviceHandler.class);

    private @Nullable RachioBridgeHandler bridgeHandler;
    private @Nullable RachioDevice device;
    private final Map<String, State> channelData = new HashMap<>();

    public RachioDeviceHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Rachio device handler for thing: {}", getThing().getUID());

        Bridge bridge = getBridge();
        if (bridge == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Bridge not found");
            return;
        }

        ThingHandler handler = bridge.getHandler();
        if (handler instanceof RachioBridgeHandler) {
            bridgeHandler = (RachioBridgeHandler) handler;
            device = bridgeHandler.getDevByUID(getThing().getUID());
            
            if (device != null) {
                device.setThingHandler(this);
                bridgeHandler.registerStatusListener(this);
                
                try {
                    bridgeHandler.registerWebHook(device.id);
                } catch (RachioApiException e) {
                    logger.warn("Failed to register webhook for device {}: {}", device.id, e.getMessage());
                }

                if (bridge.getStatus() == ThingStatus.ONLINE) {
                    updateProperties(device.fillProperties());
                    updateStatus(ThingStatus.ONLINE);
                    logger.debug("Rachio device '{}' initialized successfully", getThing().getUID());
                } else {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
                }
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Device not found in bridge");
            }
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Invalid bridge handler");
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        String channel = channelUID.getId();
        logger.debug("Handle command {} for channel {}", command, channelUID);

        if (bridgeHandler == null || device == null) {
            logger.debug("Bridge handler or device not initialized");
            return;
        }

        try {
            if (command instanceof RefreshType) {
                refreshChannels();
            } else {
                processDeviceCommand(channel, command);
            }
        } catch (Exception e) {
            logger.error("Error processing command {} for channel {}: {}", command, channel, e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    private void processDeviceCommand(String channel, Command command) throws RachioApiException {
        switch (channel) {
            case CHANNEL_DEVICE_ACTIVE:
                if (command instanceof OnOffType) {
                    if (command == OnOffType.OFF) {
                        logger.info("Pausing device '{}'", device.name);
                        bridgeHandler.disableDevice(device.id);
                    } else {
                        logger.info("Resuming device '{}'", device.name);
                        bridgeHandler.enableDevice(device.id);
                    }
                }
                break;
                
            case CHANNEL_DEVICE_RUN_TIME:
                if (command instanceof DecimalType) {
                    int runtime = ((DecimalType) command).intValue();
                    logger.info("Setting default runtime to {} seconds", runtime);
                    device.setRunTime(runtime);
                }
                break;
                
            case CHANNEL_DEVICE_RUN_ZONES:
                if (command instanceof StringType) {
                    logger.info("Run multiple zones: '{}'", command);
                    device.setRunZones(command.toString());
                }
                break;
                
            case CHANNEL_DEVICE_RUN:
                if (command == OnOffType.ON) {
                    logger.info("Starting watering for zones '{}'", device.getRunZones());
                    bridgeHandler.runMultipleZones(device.getAllRunZonesJson(bridgeHandler.getDefaultRuntime()));
                }
                break;
                
            case CHANNEL_DEVICE_STOP:
                if (command == OnOffType.ON) {
                    logger.info("Stopping watering for device '{}'", device.name);
                    bridgeHandler.stopWatering(device.id);
                    updateState(CHANNEL_DEVICE_STOP, OnOffType.OFF);
                }
                break;
                
            case CHANNEL_DEVICE_RAIN_DELAY:
                if (command instanceof DecimalType) {
                    int delay = ((DecimalType) command).intValue();
                    logger.info("Setting rain delay to {} seconds", delay);
                    device.setRainDelayTime(delay);
                    bridgeHandler.startRainDelay(device.id, delay);
                }
                break;
                
            default:
                logger.debug("Unhandled command for channel {}: {}", channel, command);
        }
    }

    private void refreshChannels() {
        if (device != null) {
            updateChannel(CHANNEL_DEVICE_NAME, new StringType(device.getThingName()));
            updateChannel(CHANNEL_DEVICE_ONLINE, device.getOnline());
            updateChannel(CHANNEL_DEVICE_ACTIVE, device.getEnabled());
            updateChannel(CHANNEL_DEVICE_PAUSED, device.getSleepMode());
            updateChannel(CHANNEL_DEVICE_STOP, OnOffType.OFF);
            updateChannel(CHANNEL_DEVICE_RUN_ZONES, new StringType(device.getRunZones()));
            updateChannel(CHANNEL_DEVICE_RUN_TIME, new DecimalType(device.getRunTime()));
            updateChannel(CHANNEL_DEVICE_RAIN_DELAY, new DecimalType(device.rainDelay));
            updateChannel(CHANNEL_DEVICE_EVENT, new StringType(device.getEvent()));
            updateChannel(CHANNEL_DEVICE_LATITUDE, new DecimalType(device.longitude));
            updateChannel(CHANNEL_DEVICE_LONGITUDE, new DecimalType(device.latitude));
            updateChannel(CHANNEL_DEVICE_SCHEDULE, new StringType(device.scheduleName));
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
    public boolean onThingStateChanged(@Nullable RachioDevice updatedDevice, @Nullable RachioZone updatedZone) {
        if (updatedDevice != null && device != null && device.id.equals(updatedDevice.id)) {
            logger.debug("Device state changed for: {}", device.id);
            device.update(updatedDevice);
            refreshChannels();
            updateStatus(device.getStatus());
            return true;
        }
        return false;
    }

    @Override
    public void onRefreshRequested() {
        refreshChannels();
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        logger.debug("Bridge status changed to: {}", bridgeStatusInfo.getStatus());
        
        if (bridgeStatusInfo.getStatus() == ThingStatus.ONLINE) {
            if (device != null) {
                updateProperties(device.fillProperties());
                refreshChannels();
                updateStatus(ThingStatus.ONLINE);
            }
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
        }
    }

    public boolean webhookEvent(RachioEvent event) {
        boolean update = true;
        
        try {
            if (device != null) {
                device.setEvent(event);
            }

            if ("ZONE_STATUS".equals(event.type)) {
                RachioZone zone = device != null ? device.getZoneByNumber(event.zoneRunStatus.zoneNumber) : null;
                if (zone != null && zone.getThingHandler() != null) {
                    return zone.getThingHandler().webhookEvent(event);
                }
            } else if ("ZONE_DELTA".equals(event.subType)) {
                RachioZone zone = device != null ? device.getZoneById(event.zoneId) : null;
                if (zone != null && zone.getThingHandler() != null) {
                    return zone.getThingHandler().webhookEvent(event);
                }
            } else if ("DEVICE_STATUS".equals(event.type)) {
                logger.info("Device {} status changed to: {}", device != null ? device.name : "unknown", event.subType);
                handleDeviceStatusEvent(event);
            } else if ("SCHEDULE_STATUS".equals(event.type)) {
                logger.info("Schedule '{}' for device '{}': {}", 
                    event.scheduleName, device != null ? device.name : "unknown", event.summary);
                update = false;
            } else {
                update = false;
            }

            if (update) {
                refreshChannels();
                return true;
            }

            logger.debug("Unhandled event type '{}.{}' for device", event.type, event.subType);
            return false;

        } catch (Exception e) {
            logger.error("Error processing webhook event: {}", e.getMessage());
            return false;
        }
    }

    private void handleDeviceStatusEvent(RachioEvent event) {
        if (device == null) return;
        
        switch (event.subType) {
            case "COLD_REBOOT":
                logger.info("Device {} rebooted. IP={}/{}, GW={}, DNS={}/{}, RSSI={}",
                        device.name, device.network.ip, device.network.nm, device.network.gw,
                        device.network.dns1, device.network.dns2, device.network.rssi);
                device.setNetwork(event.network);
                break;
            case "ONLINE":
            case "OFFLINE":
            case "OFFLINE_NOTIFICATION":
                device.setStatus(event.subType);
                break;
            case "SLEEP_MODE_ON":
            case "SLEEP_MODE_OFF":
                device.setSleepMode(event.subType);
                break;
            case "RAIN_DELAY_ON":
            case "RAIN_DELAY_OFF":
            case "RAIN_SENSOR_DETECTION_ON":
            case "RAIN_SENSOR_DETECTION_OFF":
                // Informational only - no state update needed
                break;
            default:
                // Unhandled device status
                break;
        }
    }

    @Override
    public void dispose() {
        if (bridgeHandler != null && device != null) {
            bridgeHandler.unregisterStatusListener(this);
        }
        super.dispose();
    }
}
