package org.traccar.api.resource;

import java.util.Collection;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.traccar.api.ExtendedObjectResource;
import org.traccar.model.Device;
import org.traccar.model.FuelCalibration;

@Path("calibrations")
public class FuelCalibrationResource extends ExtendedObjectResource<Device> {

  public FuelCalibrationResource() {
    super(Device.class);
  }

  @GET
  public Collection<FuelCalibration> get(
      @QueryParam("all") boolean all,
      @QueryParam("id") long id,
      @QueryParam("deviceId") long deviceId) {

  }

}
