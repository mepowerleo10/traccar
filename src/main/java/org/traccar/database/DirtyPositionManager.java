package org.traccar.database;

import java.util.ArrayList;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.Context;
import org.traccar.model.DirtyPosition;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Limit;
import org.traccar.storage.query.Order;
import org.traccar.storage.query.Request;

public class DirtyPositionManager extends BaseObjectManager<DirtyPosition> {
  private static final Logger LOGGER = LoggerFactory.getLogger(DirtyPositionManager.class);

  public DirtyPositionManager(DataManager dataManager) {
    super(dataManager, DirtyPosition.class);
  }

  public DirtyPositionIterable getIterator() {
    return new DirtyPositionIterable();
  }

  public class DirtyPositionIterable implements Iterable<DirtyPosition> {
    private DirtyPositionIterator iterator;

    @Override
    public Iterator<DirtyPosition> iterator() {
      iterator = new DirtyPositionIterator(Context.getDataManager().getStorage());
      return iterator;
    }

  }

  class DirtyPositionIterator implements Iterator<DirtyPosition> {
    private Storage storage;
    private long lastId = 0;
    private ArrayList<DirtyPosition> dirtyPositions = new ArrayList<>();
    private int index = -1;

    DirtyPositionIterator(Storage storage) {
      this.storage = storage;
    }

    @Override
    public boolean hasNext() {
      return index < dirtyPositions.size();
    }

    @Override
    public DirtyPosition next() {

      if (index == dirtyPositions.size()) {

        Request request = new Request(
            new Columns.All(),
            new Condition.Compare("id", ">", "id", lastId),
            new Order("id"), new Limit(20));

        try {
          dirtyPositions = new ArrayList<>();

          dirtyPositions = (ArrayList<DirtyPosition>) storage.getObjects(DirtyPosition.class,
              request);

          if (dirtyPositions.size() > 0) {
            index = 0;
          }

        } catch (StorageException e) {
          LOGGER.error("Failed to get dirty positions", e);
        } catch (Throwable t) {
          LOGGER.error(t.getMessage());
        }

      }

      return dirtyPositions.get(++index);
    }

  }

}
