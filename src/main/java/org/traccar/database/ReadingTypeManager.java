package org.traccar.database;

import org.traccar.model.ReadingType;

public class ReadingTypeManager extends ExtendedObjectManager<ReadingType> {

  public ReadingTypeManager(DataManager dataManager) {
    super(dataManager, ReadingType.class);
  }

}
