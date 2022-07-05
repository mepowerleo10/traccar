package org.traccar.handler.events;

import java.util.Collections;
import java.util.Map;

import io.netty.channel.ChannelHandler;

import org.traccar.database.IdentityManager;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Position;

@ChannelHandler.Sharable
public class FuelRefillEventHandler extends BaseEventHandler {

    public static final String ATTRIBUTE_FUEL_REFILL_THRESHOLD = "fuelRefillThreshold";
    public static final String ATTRIBUTE_FUEL_REFILL_WITHIN_KM_THRESHOLD = "fuelRefillWithinKmThreshold";

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

        if (position.getAttributes().containsKey(Position.KEY_FUEL_LEVEL)) {

            double fuelRefillThreshold = identityManager.lookupAttributeDouble(device.getId(),
                    ATTRIBUTE_FUEL_REFILL_THRESHOLD, 0, true, false);
            if (fuelRefillThreshold > 0) {
                event = checkRefillWithinHour(position, fuelRefillThreshold);
            }

            double fuelRefillWithinKmThreshold = identityManager.lookupAttributeDouble(device.getId(),
                    ATTRIBUTE_FUEL_REFILL_WITHIN_KM_THRESHOLD, 0, true, false);
            if (fuelRefillWithinKmThreshold > 0) {
                event = checkRefillWithinKm(position, fuelRefillWithinKmThreshold);
            }

            if (event != null) {
                return Collections.singletonMap(event, position);
            }

        }

        return null;
    }

    private Event checkRefillWithinKm(Position position, double fuelRefillWithinKmThreshold) {
        Event event = null;
        double averageConsumption = position.getDouble(Position.KEY_FUEL_CONSUMPTION_KM_PER_LITRE);

        if (averageConsumption > fuelRefillWithinKmThreshold && Math.abs(averageConsumption) != 0) {
            event = generateFuelRefillEvent(position, ATTRIBUTE_FUEL_REFILL_WITHIN_KM_THRESHOLD,
                    fuelRefillWithinKmThreshold);
        }

        return event;
    }

    private Event checkRefillWithinHour(Position position, double fuelRefillThreshold) {
        Event event = null;
        double averageConsumption = position.getDouble(Position.KEY_FUEL_CONSUMPTION);

        if (averageConsumption > fuelRefillThreshold && Math.abs(averageConsumption) != 0) {
            event = generateFuelRefillEvent(position, ATTRIBUTE_FUEL_REFILL_THRESHOLD,
                    fuelRefillThreshold);
        }

        return event;
    }

    private Event generateFuelRefillEvent(Position position, String attributeName, double fuelDropThreshold) {
        Event event = new Event(Event.TYPE_DEVICE_FUEL_REFILL, position);
        event.set(attributeName, fuelDropThreshold);
        return event;
    }

}
