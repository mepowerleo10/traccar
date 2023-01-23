package org.traccar;

import java.util.Date;

import org.junit.Before;
import org.traccar.handler.FuelLevelHandler;
import org.traccar.model.Device;
import org.traccar.model.Position;

public class BaseFuelTest {
    protected final String FUEL_KEY = "fuel1";
    protected TestReadingManager readingManager = new TestReadingManager("MilliVolts", "mV", 1);
    protected TestCalibrationManager calibrationManager = new TestCalibrationManager();
    protected TestSensorManager sensorManager = new TestSensorManager(
            1,
            1,
            1,
            FUEL_KEY,
            true,
            0);
    protected Date date;
    protected FuelLevelHandler fuelLevelHandler = null;
    protected Position position = null;
    protected Position lastPosition = null;
    protected Position initialPosition = null;
    protected Device device = null;

    protected double initialVoltage = 1786.0;
    protected double initialMinimumFuelLevel = 45.0;
    protected double initialMaximumFuelLevel = 55.0;
    protected double initialExpectedFuelLevel = 50;

    protected double lastVoltage = 1790;
    protected double lastMinimumFuelLevel = 50.0;
    protected double lastMaximumFuelLevel = 55.0;
    protected double lastExpectedFuelLevel = 50;

    protected double currentVoltage = 1234;
    protected double currentMinimumFuelLevel = 30.0;
    protected double currentMaximumFuelLevel = 35.0;
    protected double currentExpectedFuelLevel = 32.0;

    protected final double timeDiff = 4.32e+6;

    @Before
    public void before() {
        initialPosition = new Position();
        lastPosition = new Position();
        position = new Position();

        device = new TestIdentityManager().getById(1);
        date = new Date();

        initialPosition.setValid(true);
        initialPosition.setDeviceId(device.getId());
        initialPosition.setFixTime(date);
        initialPosition.set(FUEL_KEY, initialVoltage);
        initialPosition.set(Position.KEY_ODOMETER, 1);

        lastPosition.setValid(true);
        lastPosition.setDeviceId(device.getId());
        lastPosition.setFixTime(new Date(((Number) (date.getTime() + timeDiff)).longValue()));
        lastPosition.set(FUEL_KEY, lastVoltage);
        lastPosition.set(Position.KEY_ODOMETER, 500);

        position.setFixTime(new Date(((Number) (lastPosition.getFixTime().getTime() + timeDiff)).longValue()));
        position.setValid(true);
        position.set(FUEL_KEY, currentVoltage);
        position.setDeviceId(device.getId());

    }
}
