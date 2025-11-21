package org.openhab.binding.rachio.internal.handler;

import java.util.HashMap;
import java.util.Map;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.ThingTypeUID;
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
import org.openhab.binding.rachio.RachioBindingConstants;
import org.openhab.binding.rachio.internal.RachioConfiguration;
import org.openhab.binding.rachio.internal.api.RachioApiException;
import org.openhab.binding.rachio.internal.api.RachioDevice;
import org.openhab.binding.rachio.internal.api.RachioEvent;
import org.openhab.binding.rachio.internal.api.RachioZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.openhab.binding.rachio.RachioBindingConstants.*;

@Component(service = ThingHandler.class, configurationPolicy = ConfigurationPolicy.OPTIONAL)
@NonNullByDefault
public class RachioDeviceHandler extends BaseThingHandler implements RachioStatusListener {

    private final Logger deviceLogger = LoggerFactory.getLogger(RachioDeviceHandler.class);
    private RachioConfiguration thingConfig = new RachioConfiguration();

    @Nullable private Bridge bridge;
    @Nullable private RachioBridgeHandler cloudHandler;
    @Nullable private RachioDevice dev;
    private final Map<String, State> channelData = new HashMap<>();

    public RachioDeviceHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        deviceLogger.debug("RachioDevice: Initializing Rachio device '{}'", getThing().getUID());

        String errorMessage = "";
        try {
            bridge = getBridge();
            if (bridge != null) {
                ThingHandler handler = bridge.getHandler();
                if (handler instanceof RachioBridgeHandler) {
                    cloudHandler = (RachioBridgeHandler) handler;
                    dev = cloudHandler.getDevByUID(this.getThing().getUID());
                    if (dev != null) {
                        deviceLogger.debug("RachioDeviceHandler: bridge UID = {}", dev.bridge_uid);
                        dev.setThingHandler(this);
                        cloudHandler.registerStatusListener(this);
                        cloudHandler.registerWebHook(dev.id);

                        if (bridge.getStatus() != ThingStatus.ONLINE) {
                            deviceLogger.debug("Rachio: Bridge is offline!");
                            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
                        } else {
                            updateProperties(dev.fillProperties());
                            updateStatus(dev.getStatus());
                            deviceLogger.debug("RachioDevice: Rachio device '{}' initialized", getThing().getUID());
                            return;
                        }
                    }
                }
            }
            errorMessage = "Initialization failed";
        } catch (RachioApiException e) {
            errorMessage = e.toString();
        } catch (Throwable e) {
            errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown error";
        } finally {
            if (!errorMessage.isEmpty()) {
                deviceLogger.error("RachioDevice: {}", errorMessage);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, errorMessage);
            }
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        String channel = channelUID.getId();
        deviceLogger.debug("RachioDevice.handleCommand {} for {}", command, channelUID);

        if (cloudHandler == null || dev == null) {
            deviceLogger.debug("RachioDevice: Cloud handler or device not initialized!");
            return;
        }

        String errorMessage = "";
        try {
            if (command instanceof RefreshType) {
                postChannelData();
            } else if (channel.equals(CHANNEL_DEVICE_ACTIVE)) {
                if (command instanceof OnOffType) {
                    if (command == OnOffType.OFF) {
                        deviceLogger.info("RachioDevice: Pause device '{}' (disable watering, schedules etc.)", dev.name);
                        cloudHandler.disableDevice(dev.id);
                    } else {
                        deviceLogger.info("RachioDevice: Resume device '{}' (enable watering, schedules etc.)", dev.name);
                        cloudHandler.enableDevice(dev.id);
                    }
                }
            } else if (channel.equals(CHANNEL_DEVICE_RUN_TIME) && command instanceof DecimalType) {
                int runtime = ((DecimalType) command).intValue();
                deviceLogger.info("RachioDevice: Default Runtime for zones set to {} sec", runtime);
                dev.setRunTime(runtime);
            } else if (channel.equals(CHANNEL_DEVICE_RUN_ZONES) && command instanceof StringType) {
                deviceLogger.info("RachioDevice: Run multiple zones: '{}' ('' = ALL)", command);
                dev.setRunZones(command.toString());
            } else if (channel.equals(CHANNEL_DEVICE_RUN) && command == OnOffType.ON) {
                deviceLogger.info("RachioDevice: START watering zones '{}' ('' = ALL)", dev.getRunZones());
                cloudHandler.runMultipleZones(dev.getAllRunZonesJson(cloudHandler.getDefaultRuntime()));
            } else if (channel.equals(CHANNEL_DEVICE_STOP) && command == OnOffType.ON) {
                deviceLogger.info("RachioDevice: STOP watering for device '{}'", dev.name);
                cloudHandler.stopWatering(dev.id);
                updateState(CHANNEL_DEVICE_STOP, OnOffType.OFF);
            } else if (channel.equals(CHANNEL_DEVICE_RAIN_DELAY) && command instanceof DecimalType) {
                int delay = ((DecimalType) command).intValue();
                deviceLogger.info("RachioDevice: Start rain delay cycle for {} sec", delay);
                dev.setRainDelayTime(delay);
                cloudHandler.startRainDelay(dev.id, delay);
            }
        } catch (RachioApiException e) {
            errorMessage = e.toString();
        } catch (Throwable e) {
            errorMessage = e.getMessage();
        } finally {
            if (!errorMessage.isEmpty()) {
                deviceLogger.error("RachioDevice.handleCommand: {}", errorMessage);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, errorMessage);
            }
        }
    }

    private void postChannelData() {
        if (dev != null) {
            deviceLogger.debug("RachioDevice: Updating status");

            updateChannel(CHANNEL_DEVICE_NAME, new StringType(dev.getThingName()));
            updateChannel(CHANNEL_DEVICE_ONLINE, dev.getOnline());
            updateChannel(CHANNEL_DEVICE_ACTIVE, dev.getEnabled());
            updateChannel(CHANNEL_DEVICE_PAUSED, dev.getSleepMode());
            updateChannel(CHANNEL_DEVICE_STOP, OnOffType.OFF);
            updateChannel(CHANNEL_DEVICE_RUN_ZONES, new StringType(dev.getRunZones()));
            updateChannel(CHANNEL_DEVICE_RUN_TIME, new DecimalType(dev.getRunTime()));
            updateChannel(CHANNEL_DEVICE_RAIN_DELAY, new DecimalType(dev.rainDelay));
            updateChannel(CHANNEL_DEVICE_EVENT, new StringType(dev.getEvent()));
            updateChannel(CHANNEL_DEVICE_LATITUDE, new DecimalType(dev.longitude));
            updateChannel(CHANNEL_DEVICE_LONGITUDE, new DecimalType(dev.latitude));
            updateChannel(CHANNEL_DEVICE_SCHEDULE, new StringType(dev.scheduleName));
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
    public boolean onThingStateChanged(@Nullable RachioDevice updatedDev, @Nullable RachioZone updatedZone) {
        if (updatedDev != null && dev != null && dev.id.equals(updatedDev.id)) {
            deviceLogger.debug("RachioDevice: Update for device '{}' received.", dev.id);
            dev.update(updatedDev);
            postChannelData();
            updateStatus(dev.getStatus());
            return true;
        }
        return false;
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        super.bridgeStatusChanged(bridgeStatusInfo);

        deviceLogger.debug("RachioDeviceHandler: Bridge Status changed to {}", bridgeStatusInfo.getStatus());
        if (bridgeStatusInfo.getStatus() == ThingStatus.ONLINE) {
            updateProperties(dev.fillProperties());
            postChannelData();
            updateStatus(dev.getStatus());
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
        }
    }

    public void shutdown() {
        if (dev != null) {
            dev.setStatus("OFFLINE");
        }
        updateStatus(ThingStatus.OFFLINE);
    }

    public boolean webhookEvent(RachioEvent event) {
    boolean update = true;
    String errorMessage = "";

    try {
        if (dev != null) {
            dev.setEvent(event);
        }

        if ("ZONE_STATUS".equals(event.type)) {
            RachioZone zone = dev != null ? dev.getZoneByNumber(event.zoneRunStatus.zoneNumber) : null;
            if (zone != null && zone.getThingHandler() != null) {
                return zone.getThingHandler().webhookEvent(event);
            }
        } else if ("ZONE_DELTA".equals(event.subType)) {
            RachioZone zone = dev != null ? dev.getZoneById(event.zoneId) : null;
            if (zone != null && zone.getThingHandler() != null) {
                return zone.getThingHandler().webhookEvent(event);
            }
        } else if ("DEVICE_STATUS".equals(event.type)) {
            deviceLogger.info("Rachio device {} ('{}') changed to status '{}'", dev.name, dev.id, event.subType);

            switch (event.subType) {
                case "COLD_REBOOT":
                    deviceLogger.info("Rachio device {} rebooted. IP={}/{}, GW={}, DNS={}/{}, RSSI={}",
                            dev.name, dev.network.ip, dev.network.nm, dev.network.gw,
                            dev.network.dns1, dev.network.dns2, dev.network.rssi);
                    dev.setNetwork(event.network);
                    break;
                case "ONLINE":
                case "OFFLINE":
                case "OFFLINE_NOTIFICATION":
                    dev.setStatus(event.subType);
                    break;
                case "SLEEP_MODE_ON":
                case "SLEEP_MODE_OFF":
                    dev.setSleepMode(event.subType);
                    break;
                case "RAIN_DELAY_ON":
                case "RAIN_DELAY_OFF":
                case "RAIN_SENSOR_DETECTION_ON":
                case "RAIN_SENSOR_DETECTION_OFF":
                    update = false; // informational only
                    break;
                default:
                    update = false;
                    break;
            }
        } else if ("SCHEDULE_STATUS".equals(event.type)) {
            deviceLogger.info("Schedule '{}' for device '{}': {} (start={}, end={}, duration={}min)",
                    event.scheduleName, dev.name, event.summary,
                    event.startTime, event.endTime, event.durationInMinutes);
        } else {
            update = false;
        }

        if (update) {
            postChannelData();
            return true;
        }

        deviceLogger.debug("RachioDevice: Unhandled event '{}.{}' for device '{}' ({}): {}",
                event.type, event.subType, dev.name, dev.id, event.summary);
        return false;

    } catch (Throwable e) {
        deviceLogger.error("RachioDevice: Unable to process '{}.{}' - {}: {}",
                event.type, event.subType, event.summary, e.getMessage());
        return false;
 
        }
    }
}