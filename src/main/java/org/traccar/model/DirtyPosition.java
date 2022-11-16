package org.traccar.model;

import java.util.Date;

import org.traccar.storage.StorageName;

@StorageName("tc_dirty_positions")
public class DirtyPosition extends BaseModel {
  private long deviceId;

  public long getDeviceId() {
    return deviceId;
  }

  public void setDeviceId(long deviceId) {
    this.deviceId = deviceId;
  }

  private long positionId;

  public long getPositionId() {
    return positionId;
  }

  public void setPositionId(long positionId) {
    this.positionId = positionId;
  }

  private Date deviceTime;

  public Date getDeviceTime() {
    return deviceTime;
  }

  public void setDeviceTime(Date deviceTime) {
    this.deviceTime = deviceTime;
  }
}
