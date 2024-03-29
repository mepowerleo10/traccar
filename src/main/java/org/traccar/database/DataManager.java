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
package org.traccar.database;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.traccar.Context;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.BaseModel;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Permission;
import org.traccar.model.Position;
import org.traccar.model.Server;
import org.traccar.model.Statistics;
import org.traccar.model.User;
import org.traccar.storage.DatabaseStorage;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Limit;
import org.traccar.storage.query.Order;
import org.traccar.storage.query.Request;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.exception.LiquibaseException;
import liquibase.resource.FileSystemResourceAccessor;
import liquibase.resource.ResourceAccessor;

public class DataManager {

    private final Config config;

    private DataSource dataSource;

    public DataSource getDataSource() {
        return dataSource;
    }

    private final Storage storage;

    public Storage getStorage() {
        return storage;
    }

    private final boolean forceLdap;

    public DataManager(Config config) throws Exception {
        this.config = config;

        forceLdap = config.getBoolean(Keys.LDAP_FORCE);

        initDatabase();
        initDatabaseSchema();

        storage = new DatabaseStorage(dataSource);
    }

    private void initDatabase() throws Exception {

        String driverFile = config.getString(Keys.DATABASE_DRIVER_FILE);
        if (driverFile != null) {
            ClassLoader classLoader = ClassLoader.getSystemClassLoader();
            try {
                Method method = classLoader.getClass().getDeclaredMethod("addURL", URL.class);
                method.setAccessible(true);
                method.invoke(classLoader, new File(driverFile).toURI().toURL());
            } catch (NoSuchMethodException e) {
                Method method = classLoader.getClass()
                        .getDeclaredMethod("appendToClassPathForInstrumentation", String.class);
                method.setAccessible(true);
                method.invoke(classLoader, driverFile);
            }
        }

        String driver = config.getString(Keys.DATABASE_DRIVER);
        if (driver != null) {
            Class.forName(driver);
        }

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName(driver);
        hikariConfig.setJdbcUrl(config.getString(Keys.DATABASE_URL));
        hikariConfig.setUsername(config.getString(Keys.DATABASE_USER));
        hikariConfig.setPassword(config.getString(Keys.DATABASE_PASSWORD));
        hikariConfig.setConnectionInitSql(config.getString(Keys.DATABASE_CHECK_CONNECTION));
        hikariConfig.setIdleTimeout(600000);

        int maxPoolSize = config.getInteger(Keys.DATABASE_MAX_POOL_SIZE);
        if (maxPoolSize != 0) {
            hikariConfig.setMaximumPoolSize(maxPoolSize);
        }

        dataSource = new HikariDataSource(hikariConfig);
    }

    private void initDatabaseSchema() throws LiquibaseException {

        if (config.hasKey(Keys.DATABASE_CHANGELOG)) {

            ResourceAccessor resourceAccessor = new FileSystemResourceAccessor(new File("."));

            Database database = DatabaseFactory.getInstance().openDatabase(
                    config.getString(Keys.DATABASE_URL),
                    config.getString(Keys.DATABASE_USER),
                    config.getString(Keys.DATABASE_PASSWORD),
                    config.getString(Keys.DATABASE_DRIVER),
                    null, null, null, resourceAccessor);

            Liquibase liquibase = new Liquibase(
                    config.getString(Keys.DATABASE_CHANGELOG), resourceAccessor, database);

            liquibase.clearCheckSums();

            liquibase.update(new Contexts());
        }
    }

    public User login(String email, String password) throws StorageException {
        User user = storage.getObject(User.class, new Request(
                new Columns.Include("id", "login", "hashedPassword", "salt"),
                new Condition.Or(
                        new Condition.Equals("email", "email", email.trim()),
                        new Condition.Equals("login", "email"))));
        LdapProvider ldapProvider = Context.getLdapProvider();
        if (user != null) {
            if (ldapProvider != null && user.getLogin() != null && ldapProvider.login(user.getLogin(), password)
                    || !forceLdap && user.isPasswordValid(password)) {
                return user;
            }
        } else {
            if (ldapProvider != null && ldapProvider.login(email, password)) {
                user = ldapProvider.getUser(email);
                Context.getUsersManager().addItem(user);
                return user;
            }
        }
        return null;
    }

    public void updateUserPassword(User user) throws StorageException {
        storage.updateObject(user, new Request(
                new Columns.Include("hashedPassword", "salt"),
                new Condition.Equals("id", "id")));
    }

    public void updateDeviceStatus(Device device) throws StorageException {
        storage.updateObject(device, new Request(
                new Columns.Include("lastUpdate"),
                new Condition.Equals("id", "id")));
    }

    public Long getPositionsCount(long deviceId, Date from, Date to) throws StorageException {
        return storage.getRowCount(Position.class, new Request(new Condition.And(
                new Condition.Equals("deviceId", "deviceId", deviceId),
                new Condition.Between("fixTime", "from", from, "to", to))));
    }

    public Collection<Position> getPositions(long deviceId, Date from, Date to) throws StorageException {
        return storage.getObjects(Position.class, new Request(
                new Columns.All(),
                new Condition.And(
                        new Condition.Equals("deviceId", "deviceId", deviceId),
                        new Condition.Between("fixTime", "from", from, "to", to)),
                new Order("fixTime")));
    }

    public Collection<Position> getPositions(long deviceId, Date from, Date to, int limit) throws StorageException {
        return storage.getObjects(Position.class, new Request(
                new Columns.All(),
                new Condition.And(
                        new Condition.Equals("deviceId", "deviceId", deviceId),
                        new Condition.Between("fixTime", "from", from, "to", to)),
                new Order("fixTime"), new Limit(limit)));
    }

    public Collection<Position> getPositions(long deviceId, Date from, Date to, long id, int limit)
            throws StorageException {
        Condition equalsDeviceId = new Condition.Equals("deviceId", "deviceId", deviceId);
        Condition fromAndTo = new Condition.Between("fixTime", "from", from, "to", to);
        Condition greaterThanId = new Condition.Compare("id", ">", "id", id);
        Condition condition = Condition.merge(List.of(equalsDeviceId,
                fromAndTo, greaterThanId));

        return storage.getObjects(Position.class, new Request(
                new Columns.All(),
                condition,
                new Order("fixTime"),
                new Limit(limit)));
    }

    public Position getPrecedingPosition(long deviceId, Date date) throws StorageException {
        return storage.getObject(Position.class, new Request(
                new Columns.All(),
                new Condition.And(
                        new Condition.Equals("deviceId", "deviceId", deviceId),
                        new Condition.Compare("fixTime", "<=", "time", date)),
                new Order(true, "fixTime"),
                new Limit(1)));
    }

    public void updateLatestPosition(Position position) throws StorageException {
        if (position.getValid()) {
            Device device = new Device();
            device.setId(position.getDeviceId());
            device.setPositionId(position.getId());
            storage.updateObject(device, new Request(
                    new Columns.Include("positionId"),
                    new Condition.Equals("id", "id")));
        }
    }

    public Collection<Position> getLatestPositions() throws StorageException {
        List<Position> positions = new LinkedList<>();
        List<Device> devices = storage.getObjects(Device.class, new Request(new Columns.Include("positionId")));
        for (Device device : devices) {
            positions.addAll(storage.getObjects(Position.class, new Request(
                    new Columns.All(),
                    new Condition.Equals("id", "id", device.getPositionId()))));
        }
        return positions;
    }

    public Server getServer() throws StorageException {
        return storage.getObject(Server.class, new Request(new Columns.All()));
    }

    public Collection<Event> getEvents(long deviceId, Date from, Date to) throws StorageException {
        return storage.getObjects(Event.class, new Request(
                new Columns.All(),
                new Condition.And(
                        new Condition.Equals("deviceId", "deviceId", deviceId),
                        new Condition.Between("eventTime", "from", from, "to", to)),
                new Order("eventTime")));
    }

    public Collection<Event> getEventsByPage(long lastId, List<String> types) throws StorageException {
        List<Condition> conditions = new ArrayList<>();

        Condition hasDevice = new Condition.Compare("deviceid", ">", "deviceid", 0);
        conditions.add(hasDevice);

        Condition isNotOfType = ignoreEventsOfTypes(types);
        // conditions.add(isNotOfType);

        Condition lastIdIsGreaterThan = new Condition.Compare("id", ">", "id", lastId);
        conditions.add(lastIdIsGreaterThan);

        Collection<Event> events = storage.getObjects(Event.class,
                new Request(new Columns.All(), Condition.merge(conditions), new Order("id"),
                        new Limit(1000)));

        return events.stream().filter(event -> !(types.stream().anyMatch(type -> type.equals(event.getType()))))
                .map(event -> setEventProperties(storage, event))
                .collect(Collectors.toList());

    }

    private Condition ignoreEventsOfTypes(List<String> types) {
        List<Condition> conditions = new ArrayList<>();

        for (String type : types) {
            conditions.add(new Condition.Compare("type", "!=", "type", type));
        }
        return Condition.merge(conditions);

    }

    private Event setEventProperties(Storage storage, Event event) {
        String uniqueId = Context.getDeviceManager().getById(event.getDeviceId()).getUniqueId();
        event.setDeviceUniqueId(uniqueId);
        try {
            Position eventPosition = storage.getObject(Position.class,
                    new Request(new Columns.All(), new Condition.Equals("id", "id", event.getPositionId())));
            event.setPosition(eventPosition);
        } catch (StorageException e) {
            event.setPosition(null);
        }
        return event;
    }

    public Collection<Statistics> getStatistics(Date from, Date to) throws StorageException {
        return storage.getObjects(Statistics.class, new Request(
                new Columns.All(),
                new Condition.Between("captureTime", "from", from, "to", to),
                new Order("captureTime")));
    }

    public Collection<Permission> getPermissions(Class<? extends BaseModel> owner, Class<? extends BaseModel> property)
            throws StorageException, ClassNotFoundException {
        return storage.getPermissions(owner, property);
    }

    public void linkObject(Class<?> owner, long ownerId, Class<?> property, long propertyId, boolean link)
            throws StorageException {
        if (link) {
            storage.addPermission(new Permission(owner, ownerId, property, propertyId));
        } else {
            storage.removePermission(new Permission(owner, ownerId, property, propertyId));
        }
    }

    public <T extends BaseModel> T getObject(Class<T> clazz, long entityId) throws StorageException {
        return storage.getObject(clazz, new Request(
                new Columns.All(),
                new Condition.Equals("id", "id", entityId)));
    }

    public <T extends BaseModel> Collection<T> getObjects(Class<T> clazz) throws StorageException {
        return storage.getObjects(clazz, new Request(new Columns.All()));
    }

    public void addObject(BaseModel entity) throws StorageException {
        entity.setId(storage.addObject(entity, new Request(new Columns.Exclude("id"))));
    }

    public void updateObject(BaseModel entity) throws StorageException {
        storage.updateObject(entity, new Request(
                new Columns.Exclude("id"),
                new Condition.Equals("id", "id")));
    }

    public void removeObject(Class<? extends BaseModel> clazz, long entityId) throws StorageException {
        storage.removeObject(clazz, new Request(new Condition.Equals("id", "id", entityId)));
    }

}
