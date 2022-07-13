package org.traccar.database;

import java.util.List;

import org.traccar.model.FuelCalibration;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

public class FuelCalibrationManager extends ExtendedObjectManager<FuelCalibration> {

  public FuelCalibrationManager(DataManager dataManager) {
    super(dataManager, FuelCalibration.class);
  }

  public List<FuelCalibration> getDeviceFuelCalibrations(long deviceId) throws StorageException {
    List<FuelCalibration> fuelCalibrations;
    Storage storage = getDataManager().getStorage();
    fuelCalibrations = storage.getObjects(FuelCalibration.class, new Request(
        new Columns.All(), new Condition.Equals("deviceId", "deviceId", deviceId)));

    return fuelCalibrations;
  }

}