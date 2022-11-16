package org.traccar.database;

import org.traccar.model.DirtyPosition;

public class DirtyPositionManager extends BaseObjectManager<DirtyPosition> {

  public DirtyPositionManager(DataManager dataManager) {
    super(dataManager, DirtyPosition.class);
  }

}
