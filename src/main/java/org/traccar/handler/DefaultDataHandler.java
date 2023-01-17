/*
 * Copyright 2015 - 2019 Anton Tananaev (anton@traccar.org)
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
package org.traccar.handler;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.BaseDataHandler;
import org.traccar.database.DataManager;
import org.traccar.database.IdentityManager;
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.storage.StorageException;

import io.netty.channel.ChannelHandler;

@ChannelHandler.Sharable
public class DefaultDataHandler extends BaseDataHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDataHandler.class);

    private final DataManager dataManager;
    private final IdentityManager identityManager;
    private final DateFormat dateFormat = new SimpleDateFormat("dd-mm-yyyy hh:mm:ss");

    public DefaultDataHandler(IdentityManager identityManager, DataManager dataManager) {
        this.identityManager = identityManager;
        this.dataManager = dataManager;
    }

    @Override
    protected Position handlePosition(Position position) {

        try {
            Position last = identityManager.getLastPosition(position.getDeviceId());

            if (last == null || !last.getDeviceTime().equals(position.getDeviceTime())) {
                dataManager.addObject(position);
                updateDeviceLastPositionTime(position, last);
            } else {
                throw new Exception("Device ID: " + position.getDeviceId() + " position time is repeated at Time: "
                        + dateFormat.format(position.getDeviceTime()));
            }
        } catch (Exception error) {
            position = null;
            LOGGER.warn("Failed to store position", error);
        }

        return position;
    }

    private void updateDeviceLastPositionTime(Position position, Position last) throws StorageException {
        if ((last == null || last.getDeviceTime().before(position.getDeviceTime())) && position.getValid()) {
            Device device = identityManager.getById(position.getDeviceId());
            device.setLastPositionUpdate(position.getDeviceTime());
            dataManager.updateObject(device);
        }
    }

}
