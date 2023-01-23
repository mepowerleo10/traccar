/*
 * Copyright 2015 - 2022 Anton Tananaev (anton@traccar.org)
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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.traccar.Context;
import org.traccar.api.BaseResource;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.database.Backup;
import org.traccar.helper.LogAction;
import org.traccar.model.Position;
import org.traccar.model.ProcessingQueue;
import org.traccar.model.Server;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.StorageName;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Request;

@Path("server")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ServerResource extends BaseResource {

    public static final String APPLICATION_SQL = "application/sql";
    private static final String CONTENT_DISPOSITION_VALUE_SQL = "attachment; filename=backup.sql";

    @Inject
    private Storage storage;

    @PermitAll
    @GET
    public Server get() throws StorageException {
        return storage.getObject(Server.class, new Request(new Columns.All()));
    }

    @PUT
    public Response update(Server entity) throws StorageException {
        Context.getPermissionsManager().checkAdmin(getUserId());
        Context.getPermissionsManager().updateServer(entity);
        LogAction.edit(getUserId(), entity);
        return Response.ok(entity).build();
    }

    @Path("geocode")
    @GET
    public String geocode(@QueryParam("latitude") double latitude, @QueryParam("longitude") double longitude) {
        if (Context.getGeocoder() != null) {
            return Context.getGeocoder().getAddress(latitude, longitude, null);
        } else {
            throw new RuntimeException("Reverse geocoding is not enabled");
        }
    }

    @Path("timezones")
    @GET
    public Collection<String> timezones() {
        return Arrays.asList(TimeZone.getAvailableIDs());
    }

    @Path("backup")
    @GET
    @Produces(APPLICATION_SQL)
    public Response getServerBackup() throws StorageException, IOException, InterruptedException {
        Context.getPermissionsManager().checkAdmin(getUserId());
        Config config = Context.getConfig();
        List<Class<?>> ignoreTables = List.of(Position.class, ProcessingQueue.class);

        FileInputStream stream = new FileInputStream(Backup.getDatabaseBackup(
                config.getString(Keys.DATABASE_BACKUP_FILE_PATH, "schema/data"), config.getString(Keys.DATABASE_USER),
                config.getString(Keys.DATABASE_PASSWORD),
                config.getString(Keys.DATABASE_HOST, "localhost"), config.getString(Keys.DATABASE_NAME, "traccar"),
                true,
                ignoreTables.stream().map(model -> model.getAnnotation(StorageName.class).value())
                        .collect(Collectors.toList())));
        return Response.ok(stream.readAllBytes()).header(HttpHeaders.CONTENT_DISPOSITION, CONTENT_DISPOSITION_VALUE_SQL)
                .build();
    }
}
