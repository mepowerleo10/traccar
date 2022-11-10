package org.traccar.handler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.BaseDataHandler;
import org.traccar.database.ProcessingQueueManager;
import org.traccar.model.Position;
import org.traccar.model.ProcessingQueue;
import org.traccar.model.QueueTime;

import io.netty.channel.ChannelHandler;

@ChannelHandler.Sharable
public class QueueDataHandler extends BaseDataHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(QueueDataHandler.class);

  private final ProcessingQueueManager processingQueueManager;

  public QueueDataHandler(ProcessingQueueManager processingQueueManager) {
    this.processingQueueManager = processingQueueManager;
  }

  @Override
  protected Position handlePosition(Position position) {
    try {
      long deviceId = position.getDeviceId();
      long positionId = position.getId();

      if (positionId > 0) {
        Date deviceTime = position.getDeviceTime();

        final LocalDate localDate = (new Date(deviceTime.getTime())).toInstant().atZone(ZoneId.systemDefault())
            .toLocalDate();
        LocalDateTime localDeviceTime = position.getDeviceTime().toInstant().atZone(ZoneId.systemDefault())
            .toLocalDateTime();
        String queueTime;

        if (QueueTime.MORNING.isInQueueTime(localDeviceTime)) {
          queueTime = QueueTime.MORNING.id();
        } else if (QueueTime.AFTERNOON.isInQueueTime(localDeviceTime)) {
          queueTime = QueueTime.AFTERNOON.id();
        } else if (QueueTime.EVENING.isInQueueTime(localDeviceTime)) {
          queueTime = QueueTime.EVENING.id();
        } else if (QueueTime.NIGHT.isInQueueTime(localDeviceTime)) {
          queueTime = QueueTime.NIGHT.id();
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
    }

    return position;
  }

}
