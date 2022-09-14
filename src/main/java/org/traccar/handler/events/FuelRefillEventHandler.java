package org.traccar.handler.events;

import java.util.Collections;
import java.util.Map;

import io.netty.channel.ChannelHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.database.IdentityManager;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Position;

@ChannelHandler.Sharable
public class FuelRefillEventHandler extends BaseEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(FuelRefillEventHandler.class);

    public static final int REFILL_CHECK_MINUTES = 20;
    public static final String ATTRIBUTE_FUEL_REFILL_THRESHOLD = "fuelRefillThreshold";
    public static final String ATTRIBUTE_FUEL_REFILL_WITHIN_KM_THRESHOLD = "fuelRefillWithinKmThreshold";

    private static final String DEBUG_NAME = FuelRefillEventHandler.class.getName();

    private final IdentityManager identityManager;

    public FuelRefillEventHandler(IdentityManager identityManager) {
        this.identityManager = identityManager;
    }

    @Override
    protected Map<Event, Position> analyzePosition(Position position) {
        Event event = null;

        Device device = identityManager.getById(position.getDeviceId());
        if (device == null) {
            return null;
        }

        if (!identityManager.isLatestPosition(position)) {
            return null;
        }

        try {
            if (position.getAttributes().containsKey(Position.KEY_FUEL_LEVEL)) {
                double fuelRefillThreshold = identityManager.lookupAttributeDouble(device.getId(),
                        ATTRIBUTE_FUEL_REFILL_THRESHOLD, 0, true, false);
                double fuelRefillTimer = position.getDouble(Position.KEY_FUEL_REFILL_TIMER);
                if (fuelRefillThreshold > 0 && fuelRefillTimer > REFILL_CHECK_MINUTES) {
                    event = checkRefillWithinTime(position, fuelRefillThreshold);
                }

                double fuelRefillWithinKmThreshold = identityManager.lookupAttributeDouble(device.getId(),
                        ATTRIBUTE_FUEL_REFILL_WITHIN_KM_THRESHOLD, 0, true, false);
                if (fuelRefillWithinKmThreshold > 0 && !device.getBoolean(Device.ATTRIBUTE_STATIC)) {
                    event = checkRefillWithinKm(position, fuelRefillWithinKmThreshold);
                }
            }

        } catch (Exception e) {
            LOGGER.error(DEBUG_NAME, e.getMessage());
        }

        if (event != null) {
            return Collections.singletonMap(event, position);
        }

        return null;
    }

    private Event checkRefillWithinKm(Position position, double fuelRefillWithinKmThreshold) {
        Event refillInKmEvent = null;
        double fuelLevelIncrease = position.getDouble(Position.KEY_FUEL_INCREASE_PER_KM);

        if (fuelLevelIncrease > fuelRefillWithinKmThreshold && Math.abs(fuelLevelIncrease) != 0) {
            refillInKmEvent = generateFuelRefillEvent(position, ATTRIBUTE_FUEL_REFILL_WITHIN_KM_THRESHOLD,
                    fuelRefillWithinKmThreshold);
        }

        return refillInKmEvent;
    }

    private Event checkRefillWithinTime(Position position, double fuelRefillThreshold) throws Exception {
        Event refillInHourEvent = null;
        double fuelLevelIncrease = position.getDouble(Position.KEY_FUEL_INCREASE_PER_HOUR);

        if (fuelLevelIncrease > fuelRefillThreshold && Math.abs(fuelLevelIncrease) != 0) {
            refillInHourEvent = generateFuelRefillEvent(position, ATTRIBUTE_FUEL_REFILL_THRESHOLD,
                    fuelRefillThreshold);
        }

        return refillInHourEvent;
    }

    private Event generateFuelRefillEvent(Position position, String attributeName, double threshold) {
        Event event = new Event(Event.TYPE_DEVICE_FUEL_REFILL, position);
        event.set(attributeName, threshold);
        return event;
    }

}
