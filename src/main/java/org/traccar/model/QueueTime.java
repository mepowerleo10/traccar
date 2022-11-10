package org.traccar.model;

import java.time.LocalDateTime;
import java.time.LocalTime;

public enum QueueTime {
  MORNING("MORNING", LocalTime.of(05, 59), LocalTime.of(12, 00)),
  AFTERNOON("AFTERNOON", LocalTime.of(11, 59), LocalTime.of(18, 00)),
  EVENING("EVENING", LocalTime.of(17, 59), LocalTime.of(00, 00)),
  NIGHT("NIGHT", LocalTime.of(23, 59), LocalTime.of(06, 00));

  private final String id;
  private final LocalTime from;
  private final LocalTime to;

  QueueTime(String id, LocalTime from, LocalTime to) {
    this.id = id;
    this.from = from;
    this.to = to;
  }

  public String id() {
    return id;
  }

  public LocalTime from() {
    return from;
  }

  public LocalTime to() {
    return to;
  }

  public boolean isInQueueTime(LocalDateTime time) {
    LocalTime localTime = LocalTime.of(time.getHour(), time.getMinute());
    return localTime.isAfter(from) && localTime.isBefore(to);
  }
}
