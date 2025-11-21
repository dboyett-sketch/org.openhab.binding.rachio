package org.openhab.binding.rachio.internal.discovery;

import java.util.Collections;
import java.util.Set;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import org.openhab.binding.rachio.RachioBindingConstants;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.thing.ThingTypeUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    service = DiscoveryService.class,
    property = { "bindingId=rachio" }
)
public class RachioDiscoveryService extends AbstractDiscoveryService {

    private static final Logger discoveryLogger = LoggerFactory.getLogger(RachioDiscoveryService.class);
    private static final int TIMEOUT = 30;

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES =
            Collections.singleton(RachioBindingConstants.THING_TYPE_CLOUD);

    public RachioDiscoveryService() {
        super(SUPPORTED_THING_TYPES, TIMEOUT);
    }

    @Activate
    protected void activate() {
        discoveryLogger.debug("RachioDiscoveryService activated");
        // Temporarily disabled - will implement proper discovery later
    }

    @Deactivate
    protected void deactivate() {
        discoveryLogger.debug("RachioDiscoveryService deactivated");
    }

    @Override
    protected void startScan() {
        discoveryLogger.debug("RachioDiscoveryService: Discovery scan started (temporarily disabled)");
        // Discovery temporarily disabled - bridges must be manually created
        stopScan();
    }
}