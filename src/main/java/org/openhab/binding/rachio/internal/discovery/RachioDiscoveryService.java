package org.openhab.binding.rachio.internal.discovery;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.api.RachioHttp;
import org.openhab.binding.rachio.internal.api.dto.RachioPerson;
import org.openhab.binding.rachio.internal.api.dto.RachioException;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RachioDiscoveryService} discovers Rachio controllers on the local network
 * and through the Rachio API.
 *
 * @author Brian Gleason - Initial contribution
 */
@Component(service = DiscoveryService.class, immediate = true, configurationPid = "discovery.rachio")
@NonNullByDefault
public class RachioDiscoveryService extends AbstractDiscoveryService {

    private final Logger logger = LoggerFactory.getLogger(RachioDiscoveryService.class);
    
    @Reference
    private RachioHttp rachioHttp;
    
    private @Nullable ScheduledFuture<?> backgroundDiscoveryJob;

    public RachioDiscoveryService() {
        super(SUPPORTED_THING_TYPES_UIDS, 10, true);
    }

    @Activate
    @Override
    public void activate(@Nullable Map<String, @Nullable Object> configProperties) {
        super.activate(configProperties);
        startBackgroundDiscovery();
    }

    @Override
    public void deactivate() {
        stopBackgroundDiscovery();
        super.deactivate();
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return SUPPORTED_THING_TYPES_UIDS;
    }

    @Override
    public void startScan() {
        logger.debug("Starting Rachio discovery scan");
        discoverDevices();
    }

    @Override
    public synchronized void stopScan() {
        super.stopScan();
        logger.debug("Stopped Rachio discovery scan");
    }

    private void discoverDevices() {
        try {
            // For discovery, we need to handle the case where no API key is configured yet
            // We'll look for things that have been manually configured with API keys
            
            logger.debug("Rachio discovery: Looking for configured devices via API");
            
            // Note: In a real implementation, you might need to get API keys from existing things
            // or prompt the user to configure an API key first
            
        } catch (Exception e) {
            logger.debug("Discovery failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Discover devices for a specific API key
     */
    public void discoverDevices(String apiKey, ThingUID bridgeUID) {
        try {
            // Create a temporary thing ID for discovery
            String discoveryThingId = "discovery-" + System.currentTimeMillis();
            rachioHttp.registerThing(discoveryThingId, apiKey);
            
            RachioPerson person = rachioHttp.getPerson(discoveryThingId);
            if (person != null && person.devices != null) {
                for (RachioPerson.Device device : person.devices) {
                    deviceDiscovered(device, bridgeUID);
                }
            }
            
            // Clean up
            rachioHttp.unregisterThing(discoveryThingId);
            
        } catch (RachioException e) {
            logger.warn("Failed to discover devices with provided API key: {}", e.getMessage());
        } catch (Exception e) {
            logger.debug("Unexpected error during discovery: {}", e.getMessage(), e);
        }
    }

    private void deviceDiscovered(RachioPerson.Device device, @Nullable ThingUID bridgeUID) {
        ThingUID thingUID = getThingUID(device, bridgeUID);
        
        if (thingUID != null) {
            Map<String, Object> properties = new HashMap<>();
            properties.put(DEVICE_ID, device.id);
            properties.put(Thing.PROPERTY_SERIAL_NUMBER, device.serialNumber);
            properties.put(Thing.PROPERTY_MODEL_ID, device.model);
            properties.put(Thing.PROPERTY_VENDOR, "Rachio");
            properties.put(Thing.PROPERTY_FIRMWARE_VERSION, "Unknown"); // Rachio API doesn't provide this
            properties.put(Thing.PROPERTY_LATITUDE, String.valueOf(device.latitude));
            properties.put(Thing.PROPERTY_LONGITUDE, String.valueOf(device.longitude));
            
            String label = "Rachio " + device.model + " - " + device.name;

            DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID)
                    .withProperties(properties)
                    .withBridge(bridgeUID)
                    .withLabel(label)
                    .withRepresentationProperty(DEVICE_ID)
                    .build();

            thingDiscovered(discoveryResult);
            logger.debug("Discovered Rachio device: {}", device.name);
        }
    }

    private @Nullable ThingUID getThingUID(RachioPerson.Device device, @Nullable ThingUID bridgeUID) {
        ThingTypeUID thingTypeUID = THING_TYPE_DEVICE;
        
        if (bridgeUID != null) {
            return new ThingUID(thingTypeUID, bridgeUID, device.id);
        } else {
            return new ThingUID(thingTypeUID, device.id);
        }
    }

    @Override
    protected void startBackgroundDiscovery() {
        logger.debug("Starting Rachio background discovery");
        backgroundDiscoveryJob = scheduler.scheduleWithFixedDelay(this::startScan, 0, 60, TimeUnit.MINUTES);
    }

    @Override
    protected void stopBackgroundDiscovery() {
        ScheduledFuture<?> job = backgroundDiscoveryJob;
        if (job != null && !job.isCancelled()) {
            job.cancel(true);
            backgroundDiscoveryJob = null;
        }
        logger.debug("Stopped Rachio background discovery");
    }
}
