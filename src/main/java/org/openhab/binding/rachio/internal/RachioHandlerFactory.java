package org.openhab.binding.rachio.internal;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.rachio.internal.handler.RachioBridgeHandler;
import org.openhab.binding.rachio.internal.handler.RachioDeviceHandler;
import org.openhab.binding.rachio.internal.handler.RachioZoneHandler;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.openhab.binding.rachio.internal.RachioBindingConstants.*;

/**
 * The {@link RachioHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Michael Lobstein - Initial contribution
 */

@NonNullByDefault
@Component(service = ThingHandlerFactory.class, configurationPid = "binding.rachio")
public class RachioHandlerFactory extends BaseThingHandlerFactory {
    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Set.of(THING_TYPE_BRIDGE, THING_TYPE_DEVICE,
            THING_TYPE_ZONE);
    
    private final Logger logger = LoggerFactory.getLogger(RachioHandlerFactory.class);

    @Activate
    public void activate() {
        logger.debug("RachioHandlerFactory activated");
    }

    @Deactivate
    public void deactivate() {
        logger.debug("RachioHandlerFactory deactivated");
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (THING_TYPE_BRIDGE.equals(thingTypeUID)) {
            return new RachioBridgeHandler((Bridge) thing);
        } else if (THING_TYPE_DEVICE.equals(thingTypeUID)) {
            return new RachioDeviceHandler(thing);
        } else if (THING_TYPE_ZONE.equals(thingTypeUID)) {
            return new RachioZoneHandler(thing);
        }

        return null;
    }
}
