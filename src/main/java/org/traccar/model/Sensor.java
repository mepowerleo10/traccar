package org.traccar.model;

import org.traccar.storage.StorageName;

@StorageName("tc_sensor")
public class Sensor extends ExtendedModel{
    public Sensor() {}

    private String name;

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    private long deviceId;
    public long getDeviceId() {
        return deviceId;
    }
    public void setDeviceId(long deviceId) {
        this.deviceId = deviceId;
    }

    private long typeId;
    public long getTypeId() {
        return typeId;
    }
    public void setTypeId(long typeId) {
        this.typeId = typeId;
    }

    private long readingTypeId;
    public long getReadingTypeId() {
        return readingTypeId;
    }
    public void setReadingTypeId(long readingTypeId) {
        this.readingTypeId = readingTypeId;
    }

    private String fuelPort;
    public String getFuelPort() {
        return fuelPort;
    }
    public void setFuelPort(String fuelPort) {
        this.fuelPort = fuelPort;
    }

    private boolean isCalibrated;
    public boolean setIsCalibrated() {
        return isCalibrated;
    }
    public void setIsCalibrated(boolean isCalibrated) {
        this.isCalibrated = isCalibrated;
    }

    private long calibrationId;
    public long getCalibrationId() {
        return calibrationId;
    }
    public void setCalibrationId(long calibrationId) {
        this.calibrationId = calibrationId;
    }

    private int group;
    public int getGroup() {
        return group;
    }
    public void setGroup(int group) {
        this.group = group;
    }
}
