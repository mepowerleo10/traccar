package org.traccar.api.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.traccar.api.ExtendedObjectResource;
import org.traccar.model.ReadingType;

@Path("fuel-sensors/readings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReadingTypeResource extends ExtendedObjectResource<ReadingType> {

  public ReadingTypeResource() {
    super(ReadingType.class);
  }

}
