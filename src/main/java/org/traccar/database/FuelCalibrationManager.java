package org.traccar.database;

import org.traccar.model.FuelCalibration;

public class FuelCalibrationManager extends ExtendedObjectManager<FuelCalibration> {

  public FuelCalibrationManager(DataManager dataManager) {
    super(dataManager, FuelCalibration.class);
  }

}
