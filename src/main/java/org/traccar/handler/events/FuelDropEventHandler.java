/*
 * Copyright 2017 - 2019 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.handler.events;

import java.util.Collections;
import java.util.Map;

import org.traccar.database.IdentityManager;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Position;

import io.netty.channel.ChannelHandler;

@ChannelHandler.Sharable
public class FuelDropEventHandler extends BaseEventHandler {

    public static final String ATTRIBUTE_FUEL_DROP_THRESHOLD = "fuelDropThreshold";
    public static final String ATTRIBUTE_FUEL_DROP_WITHIN_KM_THRESHOLD = "fuelDropPerWithinKmThreshold";

    private final IdentityManager identityManager;

    public FuelDropEventHandler(IdentityManager identityManager) {
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

        double fuelDropThreshold = identityManager
                .lookupAttributeDouble(device.getId(), ATTRIBUTE_FUEL_DROP_THRESHOLD, 0, true, false);
        if (fuelDropThreshold > 0 && position.getAttributes().containsKey(Position.KEY_FUEL_LEVEL)) {
            Event event = checkDropWithinHour(position, fuelDropThreshold);
            if (event != null) {
                return Collections.singletonMap(event, position);
            }
        }

        double fuelDropWithinKmThreshold = identityManager
                .lookupAttributeDouble(device.getId(), ATTRIBUTE_FUEL_DROP_WITHIN_KM_THRESHOLD, 0, true, false);
        if (fuelDropWithinKmThreshold > 0 && position.getAttributes().containsKey(Position.KEY_MOTION)
                && position.getAttributes().containsKey(Position.KEY_FUEL_LEVEL)) {
            Event event = checkDropWithinKilometer(position, fuelDropWithinKmThreshold);
            if (event != null) {
                return Collections.singletonMap(event, position);
            }
        }

        return null;
    }

    private Event checkDropWithinKilometer(Position position, double fuelDropKmPerLitre) {
        double thresholdDistance = position.getDouble(Position.KEY_FUEL_THRESHOLD_DISTANCE);
        Event event = null;

        if (thresholdDistance >= 1) {
            double positionsCount = position.getInteger(Position.KEY_FUEL_POSITIONS_COUNT_WITHIN_KM);
            double totalFuelConsumedWithinKm = position.getDouble(Position.KEY_FUEL_TOTAL_CONSUMED_WITHIN_KM);
            double averageConsumption = totalFuelConsumedWithinKm / (thresholdDistance * positionsCount);

            if (averageConsumption < (-fuelDropKmPerLitre) && Math.abs(averageConsumption) != 0) {
                event = generateFuelDropEvent(position, fuelDropKmPerLitre);
            }

            position.set(Position.KEY_FUEL_THRESHOLD_DISTANCE, thresholdDistance % 1);

            /* reset the counters */
            position.set(Position.KEY_FUEL_POSITIONS_COUNT_WITHIN_KM, 0);
            position.set(Position.KEY_FUEL_TOTAL_CONSUMED_WITHIN_KM, 0);

        }

        return event;
    }

    private Event checkDropWithinHour(Position position, double fuelDropLitresPerHour) {
        double thresholdTime = position.getDouble(Position.KEY_FUEL_THRESHOLD_TIME);
        Event event = null;

        if (thresholdTime >= 1) {
            double positionsCount = position.getInteger(Position.KEY_FUEL_POSITIONS_COUNT_WITHIN_HOUR);
            double totalFuelConsumedWithinHour = position.getDouble(Position.KEY_FUEL_TOTAL_CONSUMED_WITHIN_HOUR);
            double averageConsumption = totalFuelConsumedWithinHour / (thresholdTime * positionsCount);

            if (averageConsumption < (-fuelDropLitresPerHour) && Math.abs(averageConsumption) != 0) {
                event = generateFuelDropEvent(position, fuelDropLitresPerHour);
            }

            position.set(Position.KEY_FUEL_THRESHOLD_TIME, thresholdTime % 1);

            /* reset the counters */
            position.set(Position.KEY_FUEL_POSITIONS_COUNT_WITHIN_HOUR, 0);
            position.set(Position.KEY_FUEL_TOTAL_CONSUMED_WITHIN_HOUR, 0);

        }

        return event;
    }

    private Event generateFuelDropEvent(Position position, double fuelDropThreshold) {
        Event event = new Event(Event.TYPE_DEVICE_FUEL_DROP, position);
        event.set(ATTRIBUTE_FUEL_DROP_THRESHOLD, fuelDropThreshold);
        return event;
    }

}
