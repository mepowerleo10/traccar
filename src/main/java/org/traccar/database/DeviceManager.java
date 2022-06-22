/*
 * Copyright 2016 - 2020 Anton Tananaev (anton@traccar.org)
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
package org.traccar.database;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.Context;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.Command;
import org.traccar.model.Device;
import org.traccar.model.DeviceAccumulators;
import org.traccar.model.DeviceState;
import org.traccar.model.FuelCalibration;
import org.traccar.model.FuelSensor;
import org.traccar.model.Group;
import org.traccar.model.Position;
import org.traccar.model.ReadingType;
import org.traccar.model.Server;
import org.traccar.storage.StorageException;

public class DeviceManager extends BaseObjectManager<Device> implements IdentityManager, ManagableObjects {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceManager.class);

    private final Config config;
    private final long dataRefreshDelay;

    private Map<String, Device> devicesByUniqueId;
    private Map<String, Device> devicesByPhone;
    private final AtomicLong devicesLastUpdate = new AtomicLong();

    private final Map<Long, Position> positions = new ConcurrentHashMap<>();

    private final Map<Long, DeviceState> deviceStates = new ConcurrentHashMap<>();

    public DeviceManager(DataManager dataManager) {
        super(dataManager, Device.class);
        this.config = Context.getConfig();
        try {
            writeLock();
            if (devicesByPhone == null) {
                devicesByPhone = new ConcurrentHashMap<>();
            }
            if (devicesByUniqueId == null) {
                devicesByUniqueId = new ConcurrentHashMap<>();
            }
        } finally {
            writeUnlock();
        }
        dataRefreshDelay = config.getLong(Keys.DATABASE_REFRESH_DELAY) * 1000;
        refreshLastPositions();
    }

    @Override
    public long addUnknownDevice(String uniqueId) {
        Device device = new Device();
        device.setName(uniqueId);
        device.setUniqueId(uniqueId);
        device.setCategory(Context.getConfig().getString(Keys.DATABASE_REGISTER_UNKNOWN_DEFAULT_CATEGORY));

        long defaultGroupId = Context.getConfig().getLong(Keys.DATABASE_REGISTER_UNKNOWN_DEFAULT_GROUP_ID);
        if (defaultGroupId != 0) {
            device.setGroupId(defaultGroupId);
        }

        try {
            addItem(device);

            LOGGER.info("Automatically registered device " + uniqueId);

            if (defaultGroupId != 0) {
                Context.getPermissionsManager().refreshDeviceAndGroupPermissions();
                Context.getPermissionsManager().refreshAllExtendedPermissions();
            }

            return device.getId();
        } catch (StorageException e) {
            LOGGER.warn("Automatic device registration error", e);
            return 0;
        }
    }

    public void updateDeviceCache(boolean force) {
        long lastUpdate = devicesLastUpdate.get();
        if ((force || System.currentTimeMillis() - lastUpdate > dataRefreshDelay)
                && devicesLastUpdate.compareAndSet(lastUpdate, System.currentTimeMillis())) {
            refreshItems();
        }
    }

    @Override
    public Device getByUniqueId(String uniqueId) {
        boolean forceUpdate;
        try {
            readLock();
            forceUpdate = !devicesByUniqueId.containsKey(uniqueId) && !config.getBoolean(Keys.DATABASE_IGNORE_UNKNOWN);
        } finally {
            readUnlock();
        }
        updateDeviceCache(forceUpdate);
        try {
            readLock();
            return devicesByUniqueId.get(uniqueId);
        } finally {
            readUnlock();
        }
    }

    @Override
    public String getDevicePassword(long id, String protocol, String defaultPassword) {

        String password = lookupAttributeString(id, Command.KEY_DEVICE_PASSWORD, null, false, false);
        if (password != null) {
            return password;
        }

        if (protocol != null) {
            password = Context.getConfig().getString(Keys.PROTOCOL_DEVICE_PASSWORD.withPrefix(protocol));
            if (password != null) {
                return password;
            }
        }

        return defaultPassword;
    }

    @Override
    public Set<Long> getAllItems() {
        Set<Long> result = super.getAllItems();
        if (result.isEmpty()) {
            updateDeviceCache(true);
            result = super.getAllItems();
        }
        return result;
    }

    public Collection<Device> getAllDevices() {
        return getItems(getAllItems());
    }

    public Set<Long> getAllUserItems(long userId) {
        return Context.getPermissionsManager().getDevicePermissions(userId);
    }

    @Override
    public Set<Long> getUserItems(long userId) {
        if (Context.getPermissionsManager() != null) {
            Set<Long> result = new HashSet<>();
            for (long deviceId : Context.getPermissionsManager().getDevicePermissions(userId)) {
                Device device = getById(deviceId);
                if (device != null && !device.getDisabled()) {
                    result.add(deviceId);
                }
            }
            return result;
        } else {
            return new HashSet<>();
        }
    }

    public Set<Long> getAllManagedItems(long userId) {
        Set<Long> result = new HashSet<>(getAllUserItems(userId));
        for (long managedUserId : Context.getUsersManager().getUserItems(userId)) {
            result.addAll(getAllUserItems(managedUserId));
        }
        return result;
    }

    @Override
    public Set<Long> getManagedItems(long userId) {
        Set<Long> result = new HashSet<>(getUserItems(userId));
        for (long managedUserId : Context.getUsersManager().getUserItems(userId)) {
            result.addAll(getUserItems(managedUserId));
        }
        return result;
    }

    private void addByUniqueId(Device device) {
        try {
            writeLock();
            if (devicesByUniqueId == null) {
                devicesByUniqueId = new ConcurrentHashMap<>();
            }
            devicesByUniqueId.put(device.getUniqueId(), device);
        } finally {
            writeUnlock();
        }
    }

    private void removeByUniqueId(String deviceUniqueId) {
        try {
            writeLock();
            if (devicesByUniqueId != null) {
                devicesByUniqueId.remove(deviceUniqueId);
            }
        } finally {
            writeUnlock();
        }
    }

    private void addByPhone(Device device) {
        try {
            writeLock();
            if (devicesByPhone == null) {
                devicesByPhone = new ConcurrentHashMap<>();
            }
            devicesByPhone.put(device.getPhone(), device);
        } finally {
            writeUnlock();
        }
    }

    private void removeByPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return;
        }
        try {
            writeLock();
            if (devicesByPhone != null) {
                devicesByPhone.remove(phone);
            }
        } finally {
            writeUnlock();
        }
    }

    @Override
    protected void addNewItem(Device device) {
        super.addNewItem(device);
        addByUniqueId(device);
        if (device.getPhone() != null && !device.getPhone().isEmpty()) {
            addByPhone(device);
        }
        if (Context.getGeofenceManager() != null) {
            Position lastPosition = getLastPosition(device.getId());
            if (lastPosition != null) {
                device.setGeofenceIds(Context.getGeofenceManager().getCurrentDeviceGeofences(lastPosition));
            }
        }
    }

    @Override
    protected void updateCachedItem(Device device) {
        Device cachedDevice = getById(device.getId());
        cachedDevice.setName(device.getName());
        cachedDevice.setGroupId(device.getGroupId());
        cachedDevice.setCategory(device.getCategory());
        cachedDevice.setContact(device.getContact());
        cachedDevice.setModel(device.getModel());
        cachedDevice.setDisabled(device.getDisabled());
        cachedDevice.setAttributes(device.getAttributes());
        if (!device.getUniqueId().equals(cachedDevice.getUniqueId())) {
            removeByUniqueId(cachedDevice.getUniqueId());
            cachedDevice.setUniqueId(device.getUniqueId());
            addByUniqueId(cachedDevice);
        }
        if (device.getPhone() != null && !device.getPhone().isEmpty()
                && !device.getPhone().equals(cachedDevice.getPhone())) {
            String phone = cachedDevice.getPhone();
            removeByPhone(phone);
            cachedDevice.setPhone(device.getPhone());
            addByPhone(cachedDevice);
        }
    }

    @Override
    protected void removeCachedItem(long deviceId) {
        Device cachedDevice = getById(deviceId);
        if (cachedDevice != null) {
            String deviceUniqueId = cachedDevice.getUniqueId();
            String phone = cachedDevice.getPhone();
            super.removeCachedItem(deviceId);
            removeByUniqueId(deviceUniqueId);
            removeByPhone(phone);
        }
        positions.remove(deviceId);
    }

    public void updateDeviceStatus(Device device) throws StorageException {
        getDataManager().updateDeviceStatus(device);
        Device cachedDevice = getById(device.getId());
        if (cachedDevice != null) {
            cachedDevice.setStatus(device.getStatus());
        }
    }

    private void refreshLastPositions() {
        if (getDataManager() != null) {
            try {
                for (Position position : getDataManager().getLatestPositions()) {
                    positions.put(position.getDeviceId(), position);
                }
            } catch (StorageException error) {
                LOGGER.warn("Load latest positions error", error);
            }
        }
    }

    public boolean isLatestPosition(Position position) {
        Position lastPosition = getLastPosition(position.getDeviceId());
        return lastPosition == null || position.getFixTime().compareTo(lastPosition.getFixTime()) >= 0;
    }

    public void updateLatestPosition(Position position) throws StorageException {

        if (isLatestPosition(position)) {

            getDataManager().updateLatestPosition(position);

            Device device = getById(position.getDeviceId());
            if (device != null) {
                device.setPositionId(position.getId());

                Position lastPosition = positions.get(position.getDeviceId());
                FuelSensor sensor = Context.getFuelSensorManager().getById(device.getFuelSensorId());
                calculateDeviceFuelAtPosition(lastPosition, position, device, sensor);

            }

            positions.put(position.getDeviceId(), position);

            if (Context.getConnectionManager() != null) {
                Context.getConnectionManager().updatePosition(position);
            }
        }
    }

    private double getWithinBoundsFuelLevel(double fuelLevel, FuelSensor sensor) {
        if (fuelLevel < sensor.getLowerBound()) {
            return -1;
        }

        if (fuelLevel > sensor.getUpperBound()) {
            return sensor.getUpperBound();
        }

        return fuelLevel;
    }

    private void calculateDeviceFuelAtPosition(Position lastPosition, Position position, Device device,
            FuelSensor sensor) {
        if (sensor != null) {
            ReadingType readingType = Context.getReadingTypeManager().getById(sensor.getReadingTypeId());

            if (sensor.getCalibrated()) {
                double fuelLevel = device.getFuelSlope() * position.getDouble(sensor.getFuelLevelPort())
                        + device.getFuelConstant();

                double boundedFuelLevel = getWithinBoundsFuelLevel(fuelLevel, sensor);
                position.set(Position.KEY_FUEL_LEVEL, boundedFuelLevel);

                double consumptionRate = calculateFuelConsumptionRate(lastPosition, position);
                position.set(Position.KEY_FUEL_CONSUMPTION, consumptionRate);

                if (boundedFuelLevel > 0) {
                    device.set(Position.KEY_FUEL_LEVEL, boundedFuelLevel);
                }
            } else {

                double currentFuelLevel = position.getDouble(sensor.getFuelLevelPort());
                position.set(Position.KEY_FUEL_LEVEL, currentFuelLevel
                        * readingType.getConversionMultiplier());
                position.set(Position.KEY_FUEL_CONSUMPTION,
                        position.getDouble(sensor.getFuelRatePort())
                                * readingType.getConversionMultiplier());
                position.set(Position.KEY_FUEL_USED,
                        position.getDouble(sensor.getFuelConsumedPort()));

                if (currentFuelLevel > 0) {
                    device.set(Position.KEY_FUEL_LEVEL, currentFuelLevel);
                }
            }
        } else {
            position.set(Position.KEY_FUEL_LEVEL, 0);
            position.set(Position.KEY_FUEL_CONSUMPTION, 0);
            position.set(Position.KEY_FUEL_USED, 0);
        }
    }

    private double calculateFuelConsumptionRate(Position lastPosition, Position position) {
        double consumptionMetersPerLitre = 0; // meters/litre
        double lastFuelLevel = lastPosition.getDouble(Position.KEY_FUEL_LEVEL);
        double currentFuelLevel = position.getDouble(Position.KEY_FUEL_LEVEL);

        consumptionMetersPerLitre = Math.abs((position.getDouble(Position.KEY_ODOMETER)
                - lastPosition.getDouble(Position.KEY_ODOMETER)) / (currentFuelLevel - lastFuelLevel));

        return consumptionMetersPerLitre;
    }

    @Override
    public Position getLastPosition(long deviceId) {
        return positions.get(deviceId);
    }

    public Collection<Position> getInitialState(long userId) {

        List<Position> result = new LinkedList<>();

        if (Context.getPermissionsManager() != null) {
            for (long deviceId : Context.getPermissionsManager().getUserAdmin(userId)
                    ? getAllUserItems(userId)
                    : getUserItems(userId)) {
                if (positions.containsKey(deviceId)) {
                    result.add(positions.get(deviceId));
                }
            }
        }

        return result;
    }

    @Override
    public boolean lookupAttributeBoolean(
            long deviceId, String attributeName, boolean defaultValue, boolean lookupServer, boolean lookupConfig) {
        Object result = lookupAttribute(deviceId, attributeName, lookupServer, lookupConfig);
        if (result != null) {
            return result instanceof String ? Boolean.parseBoolean((String) result) : (Boolean) result;
        }
        return defaultValue;
    }

    @Override
    public String lookupAttributeString(
            long deviceId, String attributeName, String defaultValue, boolean lookupServer, boolean lookupConfig) {
        Object result = lookupAttribute(deviceId, attributeName, lookupServer, lookupConfig);
        return result != null ? (String) result : defaultValue;
    }

    @Override
    public int lookupAttributeInteger(
            long deviceId, String attributeName, int defaultValue, boolean lookupServer, boolean lookupConfig) {
        Object result = lookupAttribute(deviceId, attributeName, lookupServer, lookupConfig);
        if (result != null) {
            return result instanceof String ? Integer.parseInt((String) result) : ((Number) result).intValue();
        }
        return defaultValue;
    }

    @Override
    public long lookupAttributeLong(
            long deviceId, String attributeName, long defaultValue, boolean lookupServer, boolean lookupConfig) {
        Object result = lookupAttribute(deviceId, attributeName, lookupServer, lookupConfig);
        if (result != null) {
            return result instanceof String ? Long.parseLong((String) result) : ((Number) result).longValue();
        }
        return defaultValue;
    }

    public double lookupAttributeDouble(
            long deviceId, String attributeName, double defaultValue, boolean lookupServer, boolean lookupConfig) {
        Object result = lookupAttribute(deviceId, attributeName, lookupServer, lookupConfig);
        if (result != null) {
            return result instanceof String ? Double.parseDouble((String) result) : ((Number) result).doubleValue();
        }
        return defaultValue;
    }

    private Object lookupAttribute(long deviceId, String attributeName, boolean lookupServer, boolean lookupConfig) {
        Object result = null;
        Device device = getById(deviceId);
        if (device != null) {
            result = device.getAttributes().get(attributeName);
            if (result == null) {
                long groupId = device.getGroupId();
                while (groupId != 0) {
                    Group group = Context.getGroupsManager().getById(groupId);
                    if (group != null) {
                        result = group.getAttributes().get(attributeName);
                        if (result != null) {
                            break;
                        }
                        groupId = group.getGroupId();
                    } else {
                        groupId = 0;
                    }
                }
            }
            if (result == null && lookupServer) {
                Server server = Context.getPermissionsManager().getServer();
                result = server.getAttributes().get(attributeName);
            }
            if (result == null && lookupConfig) {
                result = Context.getConfig().getString(attributeName);
            }
        }
        return result;
    }

    public void resetDeviceAccumulators(DeviceAccumulators deviceAccumulators) throws StorageException {
        Position last = positions.get(deviceAccumulators.getDeviceId());
        if (last != null) {
            if (deviceAccumulators.getTotalDistance() != null) {
                last.getAttributes().put(Position.KEY_TOTAL_DISTANCE, deviceAccumulators.getTotalDistance());
            }
            if (deviceAccumulators.getHours() != null) {
                last.getAttributes().put(Position.KEY_HOURS, deviceAccumulators.getHours());
            }
            getDataManager().addObject(last);
            updateLatestPosition(last);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public DeviceState getDeviceState(long deviceId) {
        DeviceState deviceState = deviceStates.get(deviceId);
        if (deviceState == null) {
            deviceState = new DeviceState();
            deviceStates.put(deviceId, deviceState);
        }
        return deviceState;
    }

    public void setDeviceState(long deviceId, DeviceState deviceState) {
        deviceStates.put(deviceId, deviceState);
    }

    public void updateFuelSlopeAndConstant(long deviceId) throws StorageException {
        Device device = getById(deviceId);
        SimpleRegression regression = new SimpleRegression(true);
        List<FuelCalibration> fuelCalibrations = Context.getFuelCalibrationManager()
                .getDeviceFuelCalibrations(device.getId());
        if (fuelCalibrations.size() > 0) {
            for (FuelCalibration calibration : fuelCalibrations) {
                regression.addData(calibration.getVoltage(), calibration.getFuelLevel());
            }
            device.setFuelSlope(regression.getSlope());
            device.setFuelConstant(regression.getIntercept());
        } else {
            device.setFuelSlope(0);
            device.setFuelConstant(0);
        }
        Context.getDeviceManager().updateItem(device);
    }

}
