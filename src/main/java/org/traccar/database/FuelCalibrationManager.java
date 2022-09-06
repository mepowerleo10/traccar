package org.traccar.database;

import static org.traccar.model.FuelCalibration.FUEL_LEVEL;
import static org.traccar.model.FuelCalibration.INTERCEPT;
import static org.traccar.model.FuelCalibration.SLOPE;
import static org.traccar.model.FuelCalibration.VOLTAGE;

import java.util.Comparator;
import java.util.Iterator;
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

  public List<FuelCalibration> getSensorFuelCalibrations(long sensorId) throws StorageException {
    List<FuelCalibration> fuelCalibrations;
    Storage storage = getDataManager().getStorage();
    fuelCalibrations = storage.getObjects(FuelCalibration.class, new Request(
        new Columns.All(), new Condition.Equals("sensorid", "sensorid", sensorId)));

    return fuelCalibrations;
  }

  @Override
  public void addItem(FuelCalibration calibration) throws StorageException {
    deleteOldSensorCalibrations(calibration);
    updateSlopeAndConstant(calibration);
    super.addItem(calibration);
    super.refreshItems();
  }

  @Override
  public void updateItem(FuelCalibration calibration) throws StorageException {
    deleteOldSensorCalibrations(calibration);
    updateSlopeAndConstant(calibration);
    super.updateItem(calibration);
    super.refreshItems();
  }

  private void deleteOldSensorCalibrations(FuelCalibration calibration) throws StorageException {
    List<FuelCalibration> oldCalibrations = getSensorFuelCalibrations(calibration.getSensorId());
    for (var c : oldCalibrations) {
      super.removeItem(c.getId());
    }
  }

  public void updateSlopeAndConstant(FuelCalibration calibration) {

    List<Map<String, Double>> calibrationEntries = calibration.getCalibrationEntries();

    calibrationEntries
        .sort(new Comparator<Map<String, Double>>() {

          @Override
          public int compare(Map<String, Double> calibrationA, Map<String, Double> calibrationB) {
            return ((Number) calibrationA.get(VOLTAGE)).intValue() - ((Number) calibrationB.get(VOLTAGE)).intValue();
          }

        });

    SimpleRegression regression = new SimpleRegression(true);
    Iterator<Map<String, Double>> iterator = calibrationEntries.iterator();

    Map<String, Double> currentCalibration = iterator.next();
    if (currentCalibration != null) {
      regression.addData(currentCalibration.get(VOLTAGE), currentCalibration.get(FUEL_LEVEL));

      while (iterator.hasNext()) {
        Map<String, Double> nextCalibration = iterator.next();
        regression.addData(nextCalibration.get(VOLTAGE), nextCalibration.get(FUEL_LEVEL));

        currentCalibration.put(SLOPE, regression.getSlope());
        currentCalibration.put(INTERCEPT, regression.getIntercept());

        regression.removeData(currentCalibration.get(VOLTAGE), currentCalibration.get(FUEL_LEVEL));
        currentCalibration = nextCalibration;
      }
    }

    calibration.setCalibrationEntries(calibrationEntries);
  }

}
