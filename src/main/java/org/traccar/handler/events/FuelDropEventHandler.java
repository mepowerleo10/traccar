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
        double currentRateDistance = position.getDouble(Position.KEY_RATE_DISTANCE);
        if (currentRateDistance >= 1) {
            if (position.getDouble(Position.KEY_FUEL_CONSUMPTION_KM_PER_LITRE) < (-fuelDropKmPerLitre)) {
                Event event = generateFuelDropEvent(position, fuelDropKmPerLitre);
                return event;
            }

            position.set(Position.KEY_RATE_DISTANCE, 0.00);
        }

        return null;
    }

    private Event checkDropWithinHour(Position position, double fuelDropLitrePerHour) {
        double currentRateTime = position.getDouble(Position.KEY_RATE_TIME);
        if (currentRateTime >= 1) {
            if (position.getDouble(Position.KEY_FUEL_CONSUMPTION) < (-fuelDropLitrePerHour)) {
                Event event = generateFuelDropEvent(position, fuelDropLitrePerHour);
                return event;
            }

            position.set(Position.KEY_RATE_TIME, 0.00);
        }

        return null;
    }

    private Event generateFuelDropEvent(Position position, double fuelDropThreshold) {
        Event event = new Event(Event.TYPE_DEVICE_FUEL_DROP, position);
        event.set(ATTRIBUTE_FUEL_DROP_THRESHOLD, fuelDropThreshold);
        return event;
    }

}
