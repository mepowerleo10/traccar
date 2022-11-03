package org.traccar;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.traccar.database.SensorManager;
import org.traccar.model.Sensor;
import org.traccar.storage.StorageException;

public class TestSensorManager extends SensorManager {
    private Sensor sensor = new Sensor();

    public TestSensorManager(Sensor sensor) {
        super(null);
        this.sensor = sensor;
    }

    public TestSensorManager(long deviceId, long typeId, long readingTypeId, String fuelPort, boolean isCalibrated,
            int groupNo) {
        super(null);
        sensor.setId(1L);
        sensor.setName("Fuel Sensor #1");
        sensor.setDeviceId(deviceId);
        sensor.setTypeId(typeId);
        sensor.setReadingTypeId(readingTypeId);
        sensor.setFuelPort(fuelPort);
        sensor.setIsCalibrated(isCalibrated);
        sensor.setGroupNo(groupNo);
    }

    @Override
    public Sensor getById(long itemId) {
        return this.sensor;
    }

    @Override
    public Set<Long> getAllDeviceItems(long deviceId) {
        return Set.copyOf(Collections.singleton(1L));
    }

    @Override
    public List<Sensor> getDeviceSensors(long deviceId) throws StorageException {
        return List.of(sensor);
    }
}
