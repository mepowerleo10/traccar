package org.traccar.schedule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.Context;
import org.traccar.config.Keys;
import org.traccar.database.DataManager;
import org.traccar.database.ProcessingQueueManager;
import org.traccar.filter.BaseFilter;
import org.traccar.filter.MovingModeFilter;
import org.traccar.model.Position;
import org.traccar.model.ProcessingQueue;
import org.traccar.model.Server;
import org.traccar.storage.StorageException;

public class TaskNormalizeFuelLevels implements Runnable {

  private long periodSeconds;
  private int filterWindowSize;
  private static final Logger LOGGER = LoggerFactory.getLogger(TaskNormalizeFuelLevels.class);

  private BaseFilter filter;

  public TaskNormalizeFuelLevels() {
    periodSeconds = Context.getConfig().getInteger(Keys.FUEL_QUEUE_FILTERING_PERIOD, 10 * 60);
    filterWindowSize = Context.getConfig().getInteger(Keys.FUEL_QUEUE_FILTERING_WINDOW);
    filter = new MovingModeFilter(filterWindowSize);
  }

  public void shedule(ScheduledExecutorService executor) {
    executor.scheduleAtFixedRate(this, periodSeconds, periodSeconds, TimeUnit.SECONDS);
  }

  public void init() throws StorageException {
    Map<String, Object> serverAttributes = Context.getDataManager().getServer().getAttributes();
    filterWindowSize = (int) serverAttributes.getOrDefault(Server.FUEL_QUEUE_FILTERING_WINDOW, filterWindowSize);
    filter = new MovingModeFilter(filterWindowSize);
  }

  @Override
  public void run() {
    LOGGER.info("Start work on Dirty Queues");
    try {
      init();

      ProcessingQueueManager queueManager = Context.getProcessingQueueManager();
      Collection<Long> dirtyQueues = queueManager.getDirtyQueues();

      for (Long queueId : dirtyQueues) {
        ProcessingQueue queue = queueManager.getById(queueId);
        ArrayList<Position> positions = new ArrayList<>();
        DataManager dataManager = Context.getDataManager();
        HashSet<String> orderedPositions = new HashSet<>();

        for (Long id : queue.getPositionsAsLong()) {
          Position position = dataManager.getObject(Position.class, id);
          if (position != null) {
            positions.add(position);
          } else {
            queue.getPositions().remove(String.valueOf(id));
          }
        }

        filter.filterPositions(positions);

        for (Position position : positions) {
          orderedPositions.add(String.valueOf(position.getId()));
          dataManager.updateObject(position);
        }

        queue.setPositions(orderedPositions);
        queue.setDirty(false);
        queueManager.updateItem(queue);
      }

      LOGGER.info("Finished work on Dirty Queues");

    } catch (Throwable t) {
      LOGGER.error("Failed to get dirty queues", t);
    }
  }

}
