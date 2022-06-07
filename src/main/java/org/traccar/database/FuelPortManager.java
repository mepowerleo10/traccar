package org.traccar.database;

import org.traccar.model.FuelPort;

public class FuelPortManager extends ExtendedObjectManager<FuelPort> {

  public FuelPortManager(DataManager dataManager) {
    super(dataManager, FuelPort.class);
  }

}
