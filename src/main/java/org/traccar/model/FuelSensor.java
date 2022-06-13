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

  private long fuelConsumedPortId;

  public long getFuelConsumedPortId() {
    return fuelConsumedPortId;
  }

  public void setFuelConsumedPortId(long fuelConsumedPortId) {
    this.fuelConsumedPortId = fuelConsumedPortId;
  }

  private long fuelLevelPortId;

  public long getFuelLevelPortId() {
    return fuelLevelPortId;
  }

  public void setFuelLevelPortId(long fuelLevelPortId) {
    this.fuelLevelPortId = fuelLevelPortId;
  }

  private long fuelRatePortId;

  public long getFuelRatePortId() {
    return fuelRatePortId;
  }

  public void setFuelRatePortId(long fuelRatePortId) {
    this.fuelRatePortId = fuelRatePortId;
  }

  private boolean calibrated;

  public boolean getCalibrated() {
    return calibrated;
  }

  public void setCalibrated(boolean calibrated) {
    this.calibrated = calibrated;
  }

}
