package org.traccar.database;

import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.regression.SimpleRegression;
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
        new Columns.All(), new Condition.Equals("deviceid", "deviceid", deviceId)));

    return fuelCalibrations;
  }

  public void updateSlopeAndConstant(FuelCalibration calibration) throws StorageException {
    SimpleRegression regression = new SimpleRegression(true);
    Map<Double, Double> calibrationEntries = calibration.getCalibrationEntries();

    for (Map.Entry<Double, Double> entry : calibrationEntries.entrySet()) {
      Double voltage = entry.getKey();
      Double fuelLevel = entry.getValue();
      regression.addData(voltage, fuelLevel);
    }

    calibration.setSlope(regression.getSlope());
    calibration.setConstant(regression.getIntercept());
  }

  @Override
  public void addItem(FuelCalibration calibration) throws StorageException {
    updateSlopeAndConstant(calibration);
    super.addItem(calibration);
  }

  @Override
  public void updateItem(FuelCalibration calibration) throws StorageException {
    updateSlopeAndConstant(calibration);
    super.updateItem(calibration);
  }

}
