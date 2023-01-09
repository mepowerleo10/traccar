package org.traccar.database;

import org.traccar.model.DeviceClass;

public class DeviceClassManager extends ExtendedObjectManager<DeviceClass> {

  public DeviceClassManager(DataManager dataManager) {
    super(dataManager, DeviceClass.class);
  }

}
