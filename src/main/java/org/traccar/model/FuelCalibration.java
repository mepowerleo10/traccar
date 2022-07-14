package org.traccar.model;

import java.util.Map;
import java.util.TreeMap;

import org.traccar.storage.StorageName;

@StorageName("tc_fuel_calibrations")
public class FuelCalibration extends BaseModel {

  public FuelCalibration() {
  }

  private Long deviceId;

  public Long getDeviceId() {
    return deviceId;
  }

  public void setDeviceId(Long deviceId) {
    this.deviceId = deviceId;
  }

  private long sensorId;

  public long getSensorId() {
    return sensorId;
  }

  public void setSensorId(long sensorId) {
    this.sensorId = sensorId;
  }

  private double slope;

  public double getSlope() {
    return slope;
  }

  public void setSlope(double slope) {
    this.slope = slope;
  }

  private double constant;

  public double getConstant() {
    return constant;
  }

  public void setConstant(double constant) {
    this.constant = constant;
  }

  private Map<Double, Double> calibrationEntries = new TreeMap<>();

  public Map<Double, Double> getCalibrationEntries() {
    return calibrationEntries;
  }

  public void setCalibrationEntries(Map<Double, Double> calibrationEntries) {
    this.calibrationEntries = calibrationEntries;
  }

  public void setCalibrationEntry(Double fuelLevel, Double voltage) {
    if (voltage != null) {
      calibrationEntries.put(fuelLevel, voltage);
    }
  }

  public Double getCalibrationEntry(Double fuelLevel) {
    if (calibrationEntries.containsKey(fuelLevel)) {
      return ((Number) calibrationEntries.get(fuelLevel)).doubleValue();
    } else {
      return 0.00;
    }
  }

}
