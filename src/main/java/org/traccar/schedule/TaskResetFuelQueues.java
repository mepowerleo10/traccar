package org.traccar.schedule;

import java.util.Collection;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.Context;
import org.traccar.config.Keys;
import org.traccar.database.ProcessingQueueManager;
import org.traccar.model.ProcessingQueue;

public class TaskResetFuelQueues implements Runnable {

  private static final Logger LOGGER = LoggerFactory.getLogger(TaskResetFuelQueues.class);

  public void shedule(ScheduledExecutorService executor) {
    executor.schedule(this, 0, TimeUnit.SECONDS);
  }

  @Override
  public void run() {
    boolean shouldRecomputeQueues = Context.getConfig().getBoolean(Keys.FUEL_QUEUE_RECOMPUTE);

    if (shouldRecomputeQueues) {
      LOGGER.info("Resetting all fuel Queues");

      try {
        ProcessingQueueManager queueManager = Context.getProcessingQueueManager();
        Collection<Long> queues = queueManager.getAllItems();

        for (Long queueId : queues) {
          ProcessingQueue queue = queueManager.getById(queueId);
          queue.setDirty(true);
          queueManager.updateItem(queue);
        }

        LOGGER.info("Done resetting fuel Queues");

      } catch (Throwable t) {
        LOGGER.error("Failed reset fuel Queues", t);
      }
    }
  }

}
