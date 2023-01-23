package org.traccar.reports.model;

import java.util.Date;

public class AssetReport extends BaseReport {

  private Date lastUpdate;

  public Date getLastUpdate() {
    return lastUpdate;
  }

  public void setLastUpdate(Date lastUpdate) {
    this.lastUpdate = lastUpdate;
  }

  private Date lastPositionUpdate;

  public Date getLastPositionUpdate() {
    return lastPositionUpdate;
  }

  public void setLastPositionUpdate(Date lastPositionUpdate) {
    this.lastPositionUpdate = lastPositionUpdate;
  }

  private double totalDistance;

  public double getTotalDistance() {
    return totalDistance;
  }

  public void setTotalDistance(double totalDistance) {
    this.totalDistance = totalDistance;
  }

  private double totalFuelSpent;

  public double getTotalFuelSpent() {
    return totalFuelSpent;
  }

  public void setTotalFuelSpent(double totalFuelSpent) {
    this.totalFuelSpent = totalFuelSpent;
  }

  private double totalFuelRefilled;

  public double getTotalFuelRefilled() {
    return totalFuelRefilled;
  }

  public void setTotalFuelRefilled(double totalFuelRefilled) {
    this.totalFuelRefilled = totalFuelRefilled;
  }

  private int noOfTanks;

  public int getNoOfTanks() {
    return noOfTanks;
  }

  public void setNoOfTanks(int noOfTanks) {
    this.noOfTanks = noOfTanks;
  }

  private int noOfSensors;

  public int getNoOfSensors() {
    return noOfSensors;
  }

  public void setNoOfSensors(int noOfSensors) {
    this.noOfSensors = noOfSensors;
  }

  private String model;

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  private String zone;

  public String getZone() {
    return zone;
  }

  public void setZone(String zone) {
    this.zone = zone;
  }
}
