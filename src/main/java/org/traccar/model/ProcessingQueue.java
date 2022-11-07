package org.traccar.model;

import java.time.LocalDate;
import java.util.ArrayList;

import org.traccar.storage.StorageName;

@StorageName("tc_processing_queues")
public class ProcessingQueue extends ExtendedModel {
  private long deviceId;

  public long getDeviceId() {
    return deviceId;
  }

  public void setDeviceId(long deviceId) {
    this.deviceId = deviceId;
  }

  private String queueTime;

  public String getQueueTime() {
    return queueTime;
  }

  public void setQueueTime(String queueTime) {
    this.queueTime = queueTime;
  }

  private LocalDate date;

  public LocalDate getDate() {
    return date;
  }

  public void setDate(LocalDate date) {
    this.date = date;
  }

  private ArrayList<Long> positions = new ArrayList<>();

  public ArrayList<Long> getPositions() {
    return positions;
  }

  public void setPositions(ArrayList<Long> positions) {
    this.positions = positions;
  }

  private boolean dirty = false;

  public boolean getDirty() {
    return dirty;
  }

  public void setDirty(boolean dirty) {
    this.dirty = dirty;
  }

}
