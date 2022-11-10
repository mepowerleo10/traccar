package org.traccar.schedule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.Context;
import org.traccar.config.Keys;
import org.traccar.database.ProcessingQueueManager;
import org.traccar.filter.MovingModeFilter;
import org.traccar.model.Position;
import org.traccar.model.ProcessingQueue;

public class TaskNormalizeFuelLevels implements Runnable {

  private final long periodMinutes;
  private static final Logger LOGGER = LoggerFactory.getLogger(TaskNormalizeFuelLevels.class);

  private MovingModeFilter filter;

  public TaskNormalizeFuelLevels() {
    periodMinutes = Context.getConfig().getInteger(Keys.FUEL_QUEUE_FILTERING_PERIOD, 5);
    int windowSize = Context.getConfig().getInteger(Keys.FUEL_QUEUE_FILTERING_WINDOW);
    filter = new MovingModeFilter(windowSize);
  }

  public void shedule(ScheduledExecutorService executor) {
    executor.scheduleAtFixedRate(this, periodMinutes, periodMinutes, TimeUnit.MINUTES);
  }

  @Override
  public void run() {
    LOGGER.info("Start work on Dirty Queues");
    try {
      ProcessingQueueManager queueManager = Context.getProcessingQueueManager();
      Collection<ProcessingQueue> dirtyQueues = queueManager.getDirtyQueues();

      for (ProcessingQueue queue : dirtyQueues) {
        List<Long> positionIds = queue.getPositionsAsLong();
        ArrayList<Position> positions = (ArrayList<Position>) queueManager.getPositions(positionIds);

        filter.filterPositions(positions);

        for (Position position : positions) {
          Context.getDataManager().updateObject(position);
        }

        queue.setDirty(false);
        queueManager.updateItem(queue);
      }

      LOGGER.info("Finished work on Dirty Queues");

    } catch (Throwable t) {
      LOGGER.error("Failed to get dirty queues", t);
    }
  }

}
