package org.traccar.model;

import org.traccar.storage.StorageName;

@StorageName("tc_fuel_calibrations")
public class FuelCalibration extends ExtendedModel {

  public FuelCalibration() {
  }

  private Long deviceId;

  public Long getDeviceId() {
    return deviceId;
  }

  public void setDeviceId(Long deviceId) {
    this.deviceId = deviceId;
  }

  private Double voltage;

  public Double getVoltage() {
    return voltage;
  }

  public void setVoltage(Double voltage) {
    this.voltage = voltage;
  }

  private Double fuelLevel;

  public Double getFuelLevel() {
    return fuelLevel;
  }

  public void setFuelLevel(Double fuelLevel) {
    this.fuelLevel = fuelLevel;
  }
}
