package org.traccar.model;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.traccar.storage.QueryIgnore;
import org.traccar.storage.StorageName;

@StorageName("tc_processing_queues")
public class ProcessingQueue extends ExtendedModel {
  public ProcessingQueue() {
    this.positions = new HashSet<String>();
  }

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

  private String queueDate;

  public String getQueueDate() {
    return queueDate;
  }

  public void setQueueDate(String queueDate) {
    this.queueDate = queueDate;
  }

  private HashSet<String> positions;

  public HashSet<String> getPositions() {
    return positions;
  }

  @QueryIgnore
  public List<Long> getPositionsAsLong() {
    return positions.stream().map(id -> Long.valueOf(id)).collect(Collectors.toList());
  }

  public synchronized void setPositions(HashSet<String> positions) {
    this.positions = positions;
  }

  public void addPosition(String positionId) {
    this.positions.add(positionId);
  }

  private boolean dirty = false;

  public boolean getDirty() {
    return dirty;
  }

  public synchronized void setDirty(boolean dirty) {
    this.dirty = dirty;
  }

}
