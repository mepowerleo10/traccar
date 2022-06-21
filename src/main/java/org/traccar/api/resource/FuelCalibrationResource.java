package org.traccar.api.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.traccar.Context;
import org.traccar.api.ExtendedObjectResource;
import org.traccar.model.FuelCalibration;
import org.traccar.storage.StorageException;

@Path("calibrations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FuelCalibrationResource extends ExtendedObjectResource<FuelCalibration> {

  public FuelCalibrationResource() {
    super(FuelCalibration.class);
  }

  @Override
  public Response add(FuelCalibration calibration) throws StorageException {
    Response response = super.add(calibration);
    Context.getDeviceManager().updateFuelSlopeAndConstant(calibration.getDeviceId());
    return response;
  }

  @Override
  public Response remove(long id) throws StorageException {
    long deviceId = Context.getFuelCalibrationManager().getById(id).getDeviceId();
    Response response = super.remove(id);
    Context.getDeviceManager().updateFuelSlopeAndConstant(deviceId);
    return response;
  }

  @Override
  public Response update(FuelCalibration calibration) throws StorageException {
    Response response = super.update(calibration);
    Context.getDeviceManager().updateFuelSlopeAndConstant(calibration.getDeviceId());
    return response;
  }

}
