package org.traccar.model;

import org.traccar.storage.StorageName;

@StorageName("tc_trips")
public class Trip extends ScheduledModel {

  public Trip() {
  }

  public Trip(String title, String description, long deviceId, long driverId, long geofenceId, double startingLatitude,
      double startingLongitude, double finalLatitude, double finalLongitude) {
    this.title = title;
    this.description = description;
    this.deviceId = deviceId;
    this.driverId = driverId;
    this.geofenceId = geofenceId;
    this.startingLatitude = startingLatitude;
    this.startingLongitude = startingLongitude;
    this.finalLatitude = finalLatitude;
    this.finalLongitude = finalLongitude;
  }

  private String title;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  private String description;

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  private long deviceId;

  public long getDeviceId() {
    return deviceId;
  }

  public void setDeviceId(long deviceId) {
    this.deviceId = deviceId;
  }

  private long driverId;

  public long getDriverId() {
    return driverId;
  }

  public void setDriverId(long driverId) {
    this.driverId = driverId;
  }

  private long geofenceId;

  public long getGeofenceId() {
    return geofenceId;
  }

  public void setGeofenceId(long geofenceId) {
    this.geofenceId = geofenceId;
  }

  private double startingLatitude;

  public double getStartingLatitude() {
    return startingLatitude;
  }

  public void setStartingLatitude(double startingLatitude) {
    this.startingLatitude = startingLatitude;
  }

  private double startingLongitude;

  public double getStartingLongitude() {
    return startingLongitude;
  }

  public void setStartingLongitude(double startingLongitude) {
    this.startingLongitude = startingLongitude;
  }

  private double finalLatitude;

  public double getFinalLatitude() {
    return finalLatitude;
  }

  public void setFinalLatitude(double finalLatitude) {
    this.finalLatitude = finalLatitude;
  }

  private double finalLongitude;

  public double getFinalLongitude() {
    return finalLongitude;
  }

  public void setFinalLongitude(double finalLongitude) {
    this.finalLongitude = finalLongitude;
  }

}