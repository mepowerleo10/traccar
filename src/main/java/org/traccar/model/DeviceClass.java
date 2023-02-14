package org.traccar.model;

import org.traccar.storage.StorageName;

@StorageName("tc_device_classes")
public class DeviceClass extends ExtendedModel {
  private String name;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  private String externalIdentifier;

  public String getExternalIdentifier() {
    return externalIdentifier;
  }

  public void setExternalIdentifier(String externalIdentifier) {
    this.externalIdentifier = externalIdentifier;
  }

}
