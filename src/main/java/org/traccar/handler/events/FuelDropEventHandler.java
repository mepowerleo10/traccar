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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.database.IdentityManager;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Position;

import io.netty.channel.ChannelHandler;

@ChannelHandler.Sharable
public class FuelDropEventHandler extends BaseEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(FuelDropEventHandler.class);

    public static final String ATTRIBUTE_FUEL_DROP_THRESHOLD = "fuelDropThreshold";
    public static final String ATTRIBUTE_FUEL_DROP_WITHIN_KM_THRESHOLD = "fuelDropWithinKmThreshold";
    public static final String DEBUG_NAME = FuelDropEventHandler.class.getName();

    private final IdentityManager identityManager;

    public FuelDropEventHandler(IdentityManager identityManager) {
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
                double fuelDropThreshold = identityManager
                        .lookupAttributeDouble(device.getId(), ATTRIBUTE_FUEL_DROP_THRESHOLD,
                                0, true, false);

                if (fuelDropThreshold > 0 && position.getAttributes().containsKey(Position.KEY_FUEL_LEVEL)) {
                    event = checkDropWithinHour(position, fuelDropThreshold);
                }

                double fuelDropWithinKmThreshold = identityManager
                        .lookupAttributeDouble(device.getId(), ATTRIBUTE_FUEL_DROP_WITHIN_KM_THRESHOLD, 0, true, false);
                if (fuelDropWithinKmThreshold > 0 && !device.getBoolean(Device.ATTRIBUTE_STATIC)
                        && position.getAttributes().containsKey(Position.KEY_FUEL_LEVEL)) {
                    event = checkDropWithinKilometer(position, fuelDropWithinKmThreshold);
                }
            }

        } catch (Exception e) {
            LOGGER.error("id: " + device.getUniqueId() + e.getStackTrace().toString());
        }

        if (event != null) {
            return Collections.singletonMap(event, position);
        }

        return null;
    }

    private Event checkDropWithinKilometer(Position position, double fuelDropKmPerLitre) throws Exception {
        Event event = null;
        double fuelConsumed = Math.abs(position.getDouble(Position.KEY_FUEL_CONSUMPTION_PER_KM));

        if (fuelConsumed != 0 && fuelConsumed > fuelDropKmPerLitre) {
            event = generateFuelDropEvent(position, ATTRIBUTE_FUEL_DROP_WITHIN_KM_THRESHOLD, fuelDropKmPerLitre);
        }

        LOGGER.info(identityManager.getById(position.getDeviceId()).getName() + " Consumed: " + fuelConsumed
                + ", Drop Threshold (KM): " + fuelDropKmPerLitre);

        return event;
    }

    private Event checkDropWithinHour(Position position, double fuelDropLitresPerHour) throws Exception {
        Event dropWithinHourEvent = null;
        double fuelConsumed = Math.abs(position.getDouble(Position.KEY_FUEL_CONSUMPTION_PER_HOUR));

        if (fuelConsumed != 0 && fuelConsumed > fuelDropLitresPerHour) {
            dropWithinHourEvent = generateFuelDropEvent(position, ATTRIBUTE_FUEL_DROP_THRESHOLD,
                    fuelDropLitresPerHour);
        }

        LOGGER.info(identityManager.getById(position.getDeviceId()).getName() + " Consumed: " + fuelConsumed
                + ", Drop Threshold (HR): " + fuelDropLitresPerHour);

        return dropWithinHourEvent;
    }

    private Event generateFuelDropEvent(Position position, String attributeName, double fuelDropThreshold) {
        Event event = new Event(Event.TYPE_DEVICE_FUEL_DROP, position);
        event.set(attributeName, fuelDropThreshold);
        return event;
    }

}
