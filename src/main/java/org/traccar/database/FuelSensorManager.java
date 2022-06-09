package org.traccar.database;

import org.traccar.model.FuelSensor;

public class FuelSensorManager extends ExtendedObjectManager<FuelSensor> {

  public FuelSensorManager(DataManager dataManager) {
    super(dataManager, FuelSensor.class);
  }

}
