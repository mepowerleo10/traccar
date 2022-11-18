/*
 * Copyright 2016 - 2021 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.api.resource;

import java.util.Collection;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.traccar.Context;
import org.traccar.api.BaseResource;
import org.traccar.model.Event;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Limit;
import org.traccar.storage.query.Order;
import org.traccar.storage.query.Request;

@Path("events")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EventResource extends BaseResource {

    @Path("{id}")
    @GET
    public Event get(@PathParam("id") long id) throws StorageException {
        Event event = Context.getDataManager().getObject(Event.class, id);
        if (event == null) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        }
        Context.getPermissionsManager().checkDevice(getUserId(), event.getDeviceId());
        return event;
    }

    @Path("page/{lastId}")
    @GET
    public Collection<Event> getEventByPage(@PathParam("lastId") long lastId) throws StorageException {
        Storage storage = Context.getDataManager().getStorage();
        Collection<Event> events = storage.getObjects(Event.class,
                new Request(new Columns.All(), new Condition.Compare("id", ">", "id", lastId), new Order("id"),
                        new Limit(1000)));

        return events.stream().filter(event -> event.getDeviceId() > 0).map(event -> {
            String uniqueId = Context.getDeviceManager().getById(event.getDeviceId()).getUniqueId();
            event.setDeviceUniqueId(uniqueId);
            return event;
        }).collect(Collectors.toList());

    }
}
