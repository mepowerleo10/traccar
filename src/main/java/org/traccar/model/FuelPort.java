package org.traccar.model;

import org.traccar.storage.StorageName;

@StorageName("tc_fuel_ports")
public class FuelPort extends ExtendedModel {

  public FuelPort() {
  }

  private String portName;

  public String getPortName() {
    return portName;
  }

  public void setPortName(String portName) {
    this.portName = portName;
  }

  private String description;

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

}
