package org.traccar.model;

import org.traccar.storage.StorageName;

@StorageName("tc_fuel_sensors")
public class FuelSensor extends ExtendedModel {

  public FuelSensor() {
  }

  private String name;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  private String description;

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  private long readingTypeId;

  public long getReadingTypeId() {
    return readingTypeId;
  }

  public void setReadingTypeId(long readingTypeId) {
    this.readingTypeId = readingTypeId;
  }

  private String fuelConsumedPort;

  public String getFuelConsumedPort() {
    return fuelConsumedPort;
  }

  public void setFuelConsumedPort(String fuelConsumedPort) {
    this.fuelConsumedPort = fuelConsumedPort;
  }

  private String fuelLevelPort;

  public String getFuelLevelPort() {
    return fuelLevelPort;
  }

  public void setFuelLevelPort(String fuelLevelPort) {
    this.fuelLevelPort = fuelLevelPort;
  }

  private String fuelRatePort;

  public String getFuelRatePort() {
    return fuelRatePort;
  }

  public void setFuelRatePort(String fuelRatePort) {
    this.fuelRatePort = fuelRatePort;
  }

  private boolean calibrated;

  public boolean getCalibrated() {
    return calibrated;
  }

  public void setCalibrated(boolean calibrated) {
    this.calibrated = calibrated;
  }

}
