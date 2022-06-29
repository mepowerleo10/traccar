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

    private final IdentityManager identityManager;

    public FuelRefillEventHandler(IdentityManager identityManager) {
        this.identityManager = identityManager;
    }

    @Override
    protected Map<Event, Position> analyzePosition(Position position) {

        Device device = identityManager.getById(position.getDeviceId());
        if (device == null) {
            return null;
        }

        if (!identityManager.isLatestPosition(position)) {
            return null;
        }

        double fuelRefillThreshold = identityManager.lookupAttributeDouble(device.getId(),
                ATTRIBUTE_FUEL_REFILL_THRESHOLD, 0, true, false);
        if (fuelRefillThreshold > 0) {
            Position lastPosition = identityManager.getLastPosition(position.getDeviceId());
            if (position.getAttributes().containsKey(Position.KEY_FUEL_LEVEL)
                    && lastPosition != null
                    && lastPosition.getAttributes().containsKey(Position.KEY_FUEL_LEVEL)) {

                double fuelDifference = position.getDouble(Position.KEY_FUEL_LEVEL)
                        - lastPosition.getDouble(Position.KEY_FUEL_LEVEL);

                if (position.getAttributes().containsKey(Position.KEY_MOTION)
                        && position.getBoolean(Position.KEY_MOTION)
                        && fuelDifference > fuelRefillThreshold) {
                    Event event = new Event(Event.TYPE_DEVICE_FUEL_REFILL, position);
                    return Collections.singletonMap(event, position);
                }
            }
        }

        return null;
    }

}
