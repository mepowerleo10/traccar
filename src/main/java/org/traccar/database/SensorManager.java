package org.traccar.database;

import java.util.List;

import org.traccar.model.Sensor;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

public class SensorManager extends ExtendedObjectManager<Sensor> {

    public SensorManager(DataManager dataManager) {
        super(dataManager, Sensor.class);
    }

    public List<Sensor> getDeviceSensors(long deviceId) throws StorageException {
        List<Sensor> sensors;
        Storage storage = getDataManager().getStorage();
        sensors = storage.getObjects(Sensor.class,
                new Request(
                        new Columns.All(), new Condition.Equals("deviceid", "deviceid", deviceId)));

        return sensors;
    }
}
