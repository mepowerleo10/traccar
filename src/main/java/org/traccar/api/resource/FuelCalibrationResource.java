package org.traccar.api.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.traccar.api.ExtendedObjectResource;
import org.traccar.model.FuelCalibration;

@Path("calibrations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FuelCalibrationResource extends ExtendedObjectResource<FuelCalibration> {

  public FuelCalibrationResource() {
    super(FuelCalibration.class);
  }

}
