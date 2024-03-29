package org.traccar.api.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.traccar.api.ExtendedObjectResource;
import org.traccar.model.Trip;

@Path("trips")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TripResource extends ExtendedObjectResource<Trip> {

  public TripResource() {
    super(Trip.class);
  }

}
