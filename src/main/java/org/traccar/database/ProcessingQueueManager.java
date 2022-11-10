package org.traccar.database;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.traccar.Context;
import org.traccar.model.Position;
import org.traccar.model.ProcessingQueue;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

public class ProcessingQueueManager extends ExtendedObjectManager<ProcessingQueue> {

  public ProcessingQueueManager(DataManager dataManager) {
    super(dataManager, ProcessingQueue.class);
  }

  public ProcessingQueue getDeviceQueue(long deviceId, String date, String queueTime) throws StorageException {
    ProcessingQueue queue;
    Storage storage = getDataManager().getStorage();
    // Date date =
    // Date.from(queueDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

    Condition deviceIdEquals = new Condition.Equals("deviceid", "deviceid", deviceId);
    Condition queueDateEquals = new Condition.Equals("queuedate", "queuedate", date);
    Condition queueTimeEquals = new Condition.Equals("queuetime", "queuetime", queueTime);
    queue = storage.getObject(ProcessingQueue.class, new Request(
        new Columns.All(), new Condition.And(deviceIdEquals, new Condition.And(queueDateEquals, queueTimeEquals))));

    return queue;
  }

  public synchronized Collection<ProcessingQueue> getDirtyQueues() throws StorageException {
    Collection<ProcessingQueue> dirtyProcessingQueues;
    Storage storage = getDataManager().getStorage();

    dirtyProcessingQueues = storage.getObjects(ProcessingQueue.class,
        new Request(new Columns.All(), new Condition.Equals("dirty", "dirty", true)));

    return dirtyProcessingQueues;
  }

  public synchronized Collection<Position> getPositions(List<Long> ids) throws StorageException {

    Collection<Position> result = ids.parallelStream().map(id -> {
      try {
        return Context.getDataManager().getObject(Position.class, id);
      } catch (StorageException e) {
        e.printStackTrace();
      }
      return null;
    }).dropWhile(position -> position == null).collect(Collectors.toList());

    /*
     * for (long itemId : itemIds) {
     * Position item = Context.getDataManager().getObject(Position.class, itemId);
     * if (item != null) {
     * result.add(item);
     * }
     * }
     */

    return result;
  }

}
