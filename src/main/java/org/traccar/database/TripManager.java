package org.traccar.database;

import org.traccar.model.Trip;

public class TripManager extends ExtendedObjectManager<Trip> {

  public TripManager(DataManager dataManager) {
    super(dataManager, Trip.class);
  }

}
