package org.traccar.model;

import java.time.LocalDateTime;
import java.time.LocalTime;

public enum QueueTime {
  MORNING(1, LocalTime.of(05, 59), LocalTime.of(12, 00), 4, 2),
  AFTERNOON(2, LocalTime.of(11, 59), LocalTime.of(18, 00), 1, 3),
  EVENING(3, LocalTime.of(17, 59), LocalTime.of(00, 00), 2, 4),
  NIGHT(4, LocalTime.of(23, 59), LocalTime.of(06, 00), 3, 1);

  private final int id;
  private final LocalTime from;
  private final LocalTime to;
  private final int prev;
  private final int next;

  QueueTime(int position, LocalTime from, LocalTime to, int prev, int next) {
    this.id = position;
    this.from = from;
    this.to = to;
    this.prev = prev;
    this.next = next;
  }

  public int id() {
    return id;
  }

  public LocalTime from() {
    return from;
  }

  public LocalTime to() {
    return to;
  }

  public int prev() {
    return prev;
  }

  public int next() {
    return next;
  }

  public static QueueTime getNextQueueTime(QueueTime queueTime) {
    for (QueueTime q : QueueTime.values()) {
      if (q.id == queueTime.next) {
        return q;
      }
    }
    return queueTime;
  }

  public static QueueTime getPreviousQueueTime(QueueTime queueTime) {
    for (QueueTime q : QueueTime.values()) {
      if (q.id == queueTime.prev) {
        return q;
      }
    }
    return queueTime;
  }

  public boolean isInQueueTime(LocalDateTime time) {
    LocalTime localTime = LocalTime.of(time.getHour(), time.getMinute());
    return localTime.isAfter(from) && localTime.isBefore(to);
  }
}
