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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

import org.traccar.Context;
import org.traccar.api.BaseObjectResource;
import org.traccar.database.DeviceManager;
import org.traccar.database.FuelCalibrationManager;
import org.traccar.helper.LogAction;
import org.traccar.model.Device;
import org.traccar.model.DeviceAccumulators;
import org.traccar.model.FuelCalibration;
import org.traccar.storage.StorageException;

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

    private void updateExistingCalibration(long deviceId, FuelCalibrationManager calibrationManager,
            FuelCalibration calibration)
            throws StorageException {

        FuelCalibration existingCalibration = calibrationManager.getById(calibration.getId());
        if (existingCalibration != null && existingCalibration.getDeviceId() == deviceId) {
            if (calibration.getVoltage() >= 0) {
                existingCalibration.setVoltage(calibration.getVoltage());
            }

            if (calibration.getFuelLevel() >= 0) {
                existingCalibration.setFuelLevel(calibration.getFuelLevel());
            }

            existingCalibration.setAttributes(calibration.getAttributes());
            calibrationManager.updateItem(existingCalibration);
            LogAction.edit(getUserId(), existingCalibration);
        } else {
            throw new StorageException("Trying to update an inexistent calibration");
        }
    }

    @Path("{deviceId}/calibrations")
    @POST
    public Collection<FuelCalibration> addDeviceCalibrations(
            @PathParam("deviceId") long deviceId,
            List<FuelCalibration> calibrations) throws StorageException {

        Context.getPermissionsManager().checkAdmin(getUserId());
        FuelCalibrationManager calibrationManager = Context.getFuelCalibrationManager();

        Set<Long> existingCalibrations = calibrationManager.getAllDeviceItems(deviceId);
        if (existingCalibrations.size() > 0) {
            for (long existingCalibration : existingCalibrations) {
                calibrationManager.removeItem(existingCalibration);
            }
        }

        for (FuelCalibration calibration : calibrations) {
            calibration.setDeviceId(deviceId);
            if (calibration.getId() == 0) {
                calibrationManager.addItem(calibration);
                LogAction.create(getUserId(), calibration);
            } else {
                updateExistingCalibration(deviceId, calibrationManager, calibration);

            }

        }
        Context.getDeviceManager().updateFuelSlopeAndConstant(deviceId);

        return calibrationManager.getDeviceFuelCalibrations(deviceId);
    }

    @Path("{deviceId}/calibrations")
    @PUT
    public Collection<FuelCalibration> updateDeviceFuelCalibrations(
            @PathParam("deviceId") long deviceId,
            List<FuelCalibration> calibrations) throws StorageException {

        Context.getPermissionsManager().checkAdmin(getUserId());
        FuelCalibrationManager calibrationManager = Context.getFuelCalibrationManager();
        for (FuelCalibration calibration : calibrations) {
            updateExistingCalibration(deviceId, calibrationManager, calibration);
        }

        Context.getDeviceManager().updateFuelSlopeAndConstant(deviceId);

        return calibrationManager.getDeviceFuelCalibrations(deviceId);
    }

    @Path("{deviceId}/calibrations")
    @GET
    public Collection<FuelCalibration> getDeviceFuelCalibrations(
            @PathParam("deviceId") long deviceId) throws StorageException {

        return Context.getFuelCalibrationManager().getDeviceFuelCalibrations(deviceId);
    }

}
