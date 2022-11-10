package org.traccar.schedule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.Context;
import org.traccar.database.DirtyPositionManager;
import org.traccar.database.ProcessingQueueManager;
import org.traccar.model.DirtyPosition;
import org.traccar.model.ProcessingQueue;
import org.traccar.model.QueueTime;

public class TaskPopulateQueues implements Runnable {
  private static final Logger LOGGER = LoggerFactory.getLogger(TaskPopulateQueues.class);
  private static final int PERIOD_SECONDS = 30;

  public void schedule(ScheduledExecutorService executor) {
    executor.scheduleAtFixedRate(this, PERIOD_SECONDS, PERIOD_SECONDS, TimeUnit.SECONDS);
  }

  @Override
  public void run() {
    LOGGER.warn("Populating Processing Queues");
    try {
      DirtyPositionManager dirtyPositionManager = Context.getDirtyPositionManager();
      for (Long id : dirtyPositionManager.getAllItems()) {
        DirtyPosition dirtyPosition = dirtyPositionManager.getById(id);
        if (dirtyPosition != null) {
          addPositionToQueue(dirtyPosition);
          dirtyPositionManager.removeItem(id);
        }
      }
    } catch (Throwable t) {
      LOGGER.error("Failed to Populate Queues", t);
    }
    LOGGER.warn("Done populating Processing Queues");
  }

  private void addPositionToQueue(DirtyPosition dirtyPosition) throws Exception {
    try {
      ProcessingQueueManager processingQueueManager = Context.getProcessingQueueManager();

      long deviceId = dirtyPosition.getDeviceId();
      long positionId = dirtyPosition.getId();

      if (positionId > 0) {
        Date deviceTime = dirtyPosition.getDeviceTime();

        final LocalDate localDate = (new Date(deviceTime.getTime())).toInstant().atZone(ZoneId.systemDefault())
            .toLocalDate();
        LocalDateTime localDeviceTime = dirtyPosition.getDeviceTime().toInstant().atZone(ZoneId.systemDefault())
            .toLocalDateTime();
        String queueTime;

        if (QueueTime.MORNING.isInQueueTime(localDeviceTime)) {
          queueTime = QueueTime.MORNING.name();
        } else if (QueueTime.AFTERNOON.isInQueueTime(localDeviceTime)) {
          queueTime = QueueTime.AFTERNOON.name();
        } else if (QueueTime.EVENING.isInQueueTime(localDeviceTime)) {
          queueTime = QueueTime.EVENING.name();
        } else if (QueueTime.NIGHT.isInQueueTime(localDeviceTime)) {
          queueTime = QueueTime.NIGHT.name();
        } else {
          queueTime = "UNKNOWN";
        }

        String date = localDate.getYear() + "-" + localDate.getMonthValue() + "-" + localDate.getDayOfMonth();
        ProcessingQueue queue = processingQueueManager.getDeviceQueue(deviceId, date, queueTime);

        if (queue == null) {
          queue = new ProcessingQueue();
          queue.setDeviceId(deviceId);
          queue.setQueueTime(queueTime);
        }

        queue.addPosition(String.valueOf(positionId));
        queue.setDirty(true);
        queue.setQueueDate(date);

        if (queue.getId() > 0) {
          processingQueueManager.updateItem(queue);
        } else {
          processingQueueManager.addItem(queue);
        }
      }

    } catch (Exception e) {
      LOGGER.error("Couldn't add position to queue", e);
      throw e;
    }
  }

}
