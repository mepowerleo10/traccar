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

  private Long readingTypeId;

  public Long getReadingTypeId() {
    return readingTypeId;
  }

  public void setReadingTypeId(Long readingTypeId) {
    this.readingTypeId = readingTypeId;
  }

  private Long fuelConsumedPortId;

  public Long getFuelConsumedPortId() {
    return fuelConsumedPortId;
  }

  public void setFuelConsumedPortId(Long fuelConsumedPortId) {
    this.fuelConsumedPortId = fuelConsumedPortId;
  }

  private Long fuelLevelPortId;

  public Long getFuelLevelPortId() {
    return fuelLevelPortId;
  }

  public void setFuelLevelPortId(Long fuelLevelPortId) {
    this.fuelLevelPortId = fuelLevelPortId;
  }

  private Long fuelRatePortId;

  public Long getFuelRatePortId() {
    return fuelRatePortId;
  }

  public void setFuelRatePortId(Long fuelRatePortId) {
    this.fuelRatePortId = fuelRatePortId;
  }

  private Boolean calibrated;

  public Boolean isCalibrated() {
    return calibrated;
  }

  public void setCalibrated(Boolean calibrated) {
    this.calibrated = calibrated;
  }

}
