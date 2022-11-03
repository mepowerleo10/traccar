package org.traccar.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.traccar.storage.StorageName;

@StorageName("tc_fuel_calibrations")
public class FuelCalibration extends BaseModel {

  public static final String FUEL_LEVEL = "fuel";
  public static final String VOLTAGE = "voltage";
  public static final String SLOPE = "slope";
  public static final String INTERCEPT = "intercept";

  public FuelCalibration() {
  }

  private Long deviceId;

  public Long getDeviceId() {
    return deviceId;
  }

  public void setDeviceId(Long deviceId) {
    this.deviceId = deviceId;
  }

  private Long sensorId;

  public Long getSensorId() {
    return sensorId;
  }

  public void setSensorId(Long sensorId) {
    this.sensorId = sensorId;
  }

  private List<Map<String, Double>> calibrationEntries = new ArrayList<>();

  public List<Map<String, Double>> getCalibrationEntries() {
    return calibrationEntries;
  }

  public void setCalibrationEntries(List<Map<String, Double>> calibrationEntries) {
    this.calibrationEntries = calibrationEntries;
  }

  public double get(int i, String key) {
    if (key != null && i < calibrationEntries.size() && calibrationEntries.get(i).containsKey(key)) {
      return ((Number) calibrationEntries.get(i).get(key)).doubleValue();
    } else {
      return 0.0;
    }
  }

  public void set(int i, Map<String, Double> calibration) {
    if (calibration != null && !calibration.isEmpty()) {
      if (i < calibrationEntries.size()) {
        calibrationEntries.set(i, calibration);
      } else {
        add(calibration);
      }
    }
  }

  public void add(Map<String, Double> calibration) {
    if (calibration != null && !calibration.isEmpty()) {
      calibrationEntries.add(calibration);
    }
  }

}
