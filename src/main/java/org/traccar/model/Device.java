/*
 * Copyright 2012 - 2022 Anton Tananaev (anton@traccar.org)
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
package org.traccar.model;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.traccar.storage.QueryExtended;
import org.traccar.storage.QueryIgnore;
import org.traccar.storage.StorageName;

@StorageName("tc_devices")
public class Device extends GroupedModel {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private String uniqueId;

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public static final String STATUS_UNKNOWN = "unknown";
    public static final String STATUS_ONLINE = "online";
    public static final String STATUS_OFFLINE = "offline";
    public static final String SENSOR_TYPE = "typeId";
    public static final String SENSOR_CALIBRRATION = "calibrationId";
    public static final String SENSOR_ISCALIBRATED = "isCalibrated";
    public static final String SENSOR_FUEL_PORT = "fuelPort";
    public static final String SENSOR_READING_ID = "readingId";
    public static final String SENSOR_LOWER_BOUND = "sensorLowerBound";
    public static final String SENSOR_UPPER_BOUND = "sensorUpperBound";
    public static final String SENSOR_GROUP = "group";
    public static final String SENSOR_NAME = "name";
    public static final String ATTRIBUTE_STATIC = "static";
    public static final String KEY_OMNICOMM_RECORD_NUMBER = "omnicommRecordNumber";
    public static final String KEY_OMNICOMM_LAST_IDENTIFICATION_TIME = "omnicommLastIdentificationTime";
    public static final String KEY_OMNICOMM_LAST_ARCHIVE_TIME = "omnicommLastArchiveTime";
    public static final String KEY_OMNICOMM_LAST_DELETE_TIME = "omnicommLastDeleteTime";
    public static final String KEY_OMNICOMM_ATTEMPTS_TIMER = "omnicommAttemptsTimer";

    private String status;

    @QueryIgnore
    public String getStatus() {
        return status != null ? status : STATUS_OFFLINE;
    }

    @QueryIgnore
    public void setStatus(String status) {
        this.status = status;
    }

    private Date lastUpdate;

    public Date getLastUpdate() {
        return this.lastUpdate;
    }

    @QueryExtended
    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    private Date lastPositionUpdate;

    public Date getLastPositionUpdate() {
        return this.lastPositionUpdate;
    }

    public void setLastPositionUpdate(Date lastPositionUpdate) {
        this.lastPositionUpdate = lastPositionUpdate;
    }

    private long positionId;

    @QueryIgnore
    public long getPositionId() {
        return positionId;
    }

    @QueryIgnore
    public void setPositionId(long positionId) {
        this.positionId = positionId;
    }

    private List<Long> geofenceIds;

    @QueryIgnore
    public List<Long> getGeofenceIds() {
        return geofenceIds;
    }

    @QueryIgnore
    public void setGeofenceIds(List<Long> geofenceIds) {
        this.geofenceIds = geofenceIds;
    }

    private String phone;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    private String model;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    private String contact;

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    private String category;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    private boolean disabled;

    public boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    private List<Map<String, Object>> sensors;

    public List<Map<String, Object>> getSensors() {
        return sensors;
    }

    public void setSensors(List<Map<String, Object>> sensors) {
        this.sensors = sensors;
    }

    private long classId;

    public long getClassId() {
        return classId;
    }

    public void setClassId(long classId) {
        this.classId = classId;
    }
}
