package org.traccar.api.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.traccar.api.SimpleObjectResource;
import org.traccar.model.DeviceClass;

@Path("device-classes")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DeviceClassResource extends SimpleObjectResource<DeviceClass> {
  public DeviceClassResource() {
    super(DeviceClass.class);
  }
}
