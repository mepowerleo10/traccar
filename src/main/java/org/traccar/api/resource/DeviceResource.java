/*
 * Copyright 2015 - 2018 Anton Tananaev (anton@traccar.org)
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

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.traccar.Context;
import org.traccar.api.BaseObjectResource;
import org.traccar.database.DeviceManager;
import org.traccar.database.FuelCalibrationManager;
import org.traccar.helper.LogAction;
import org.traccar.model.Device;
import org.traccar.model.DeviceAccumulators;
import org.traccar.model.FuelCalibration;
import org.traccar.storage.StorageException;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Path("devices")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DeviceResource extends BaseObjectResource<Device> {

    public DeviceResource() {
        super(Device.class);
    }

    @GET
    public Collection<Device> get(
            @QueryParam("all") boolean all, @QueryParam("userId") long userId,
            @QueryParam("uniqueId") List<String> uniqueIds,
            @QueryParam("id") List<Long> deviceIds) {
        DeviceManager deviceManager = Context.getDeviceManager();
        Set<Long> result;
        if (all) {
            if (Context.getPermissionsManager().getUserAdmin(getUserId())) {
                result = deviceManager.getAllItems();
            } else {
                Context.getPermissionsManager().checkManager(getUserId());
                result = deviceManager.getManagedItems(getUserId());
            }
        } else if (uniqueIds.isEmpty() && deviceIds.isEmpty()) {
            if (userId == 0) {
                userId = getUserId();
            }
            Context.getPermissionsManager().checkUser(getUserId(), userId);
            if (Context.getPermissionsManager().getUserAdmin(getUserId())) {
                result = deviceManager.getAllUserItems(userId);
            } else {
                result = deviceManager.getUserItems(userId);
            }
        } else {
            result = new HashSet<>();
            for (String uniqueId : uniqueIds) {
                Device device = deviceManager.getByUniqueId(uniqueId);
                Context.getPermissionsManager().checkDevice(getUserId(), device.getId());
                result.add(device.getId());
            }
            for (Long deviceId : deviceIds) {
                Context.getPermissionsManager().checkDevice(getUserId(), deviceId);
                result.add(deviceId);
            }
        }
        return deviceManager.getItems(result);
    }

    @Path("{id}/accumulators")
    @PUT
    public Response updateAccumulators(DeviceAccumulators entity) throws StorageException {
        if (!Context.getPermissionsManager().getUserAdmin(getUserId())) {
            Context.getPermissionsManager().checkManager(getUserId());
            Context.getPermissionsManager().checkPermission(Device.class, getUserId(), entity.getDeviceId());
        }
        Context.getDeviceManager().resetDeviceAccumulators(entity);
        LogAction.resetDeviceAccumulators(getUserId(), entity.getDeviceId());
        return Response.noContent().build();
    }

    @Path("{deviceId}/calibrations")
    @POST
    public Collection<FuelCalibration> addDeviceCalibrations(
            @PathParam("deviceId") long deviceId,
            List<FuelCalibration> calibrations) throws StorageException {

        SimpleRegression regression = new SimpleRegression(true);

        Context.getPermissionsManager().checkAdmin(getUserId());
        FuelCalibrationManager calibrationManager = Context.getFuelCalibrationManager();
        for (FuelCalibration calibration : calibrations) {
            regression.addData(calibration.getVoltage(), calibration.getVoltage());
            calibration.setDeviceId(deviceId);
            if (calibration.getId() == 0) {
                calibrationManager.addItem(calibration);
                LogAction.create(getUserId(), calibration);
            } else {
                calibrationManager.updateItem(calibration);
                LogAction.edit(getUserId(), calibration);
            }

        }

        DeviceManager deviceManager = Context.getDeviceManager();
        Device device = deviceManager.getById(deviceId);
        device.setFuelSlope(regression.getSlope());
        device.setFuelConstant(regression.getIntercept());
        deviceManager.updateItem(device);

        return calibrationManager.getDeviceFuelCalibrations(deviceId);
    }

    @Path("{deviceId}/calibrations")
    @GET
    public List<FuelCalibration> getDeviceFuelCalibrations(
            @PathParam("deviceId") long deviceId) throws StorageException {

        FuelCalibrationManager calibrationManager = Context.getFuelCalibrationManager();
        List<FuelCalibration> fuelCalibrations = calibrationManager.getDeviceFuelCalibrations(deviceId);
        if (fuelCalibrations == null) {
            return new LinkedList<>();
        }

        return fuelCalibrations;
    }

    @Path("{deviceId}/slope")
    @GET
    public Map<String, Double> getSlopeAndConstant(@PathParam("deviceId") long deviceId) throws StorageException {

        SimpleRegression regression = new SimpleRegression();
        List<FuelCalibration> fuelCalibrations = Context.getFuelCalibrationManager()
                .getDeviceFuelCalibrations(deviceId);

        Map<String, Double> result = new HashMap<>();

        for (FuelCalibration calibration : fuelCalibrations) {
            regression.addData(calibration.getVoltage(), calibration.getFuelLevel());
        }

        result.put("slope", regression.getSlope());
        result.put("intercept", regression.getIntercept());

        return result;
    }
}
