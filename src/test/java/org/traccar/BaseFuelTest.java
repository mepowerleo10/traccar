package org.traccar;

import java.util.Date;

import org.junit.Before;
import org.traccar.handler.FuelLevelHandler;
import org.traccar.model.Device;
import org.traccar.model.Position;

public class BaseFuelTest {
    protected TestReadingManager readingManager = new TestReadingManager("MilliVolts", "mV", 1);
    protected TestCalibrationManager calibrationManager = new TestCalibrationManager();
    protected Date date;
    protected FuelLevelHandler fuelLevelHandler = null;
    protected Position position = null;
    protected Position lastPosition = null;
    protected Device device = null;
    protected final String FUEL_KEY = "fuel1";

    protected double lastVoltage = 1790;
    protected double lastMinimum = 50.0;
    protected double lastMaximum = 55.0;
    protected double lastExpected = 50;
    
    protected double currentMinimum = 30.0;
    protected double currentMaximum = 35.0;
    protected double currentVoltage = 1234;
    protected double currentExpected = 32.0;

    protected final double timeDiff = 4.32e+6;

    @Before
    public void before() {
        lastPosition = new Position();
        position = new Position();

        device = new TestIdentityManager().getById(1);
        date = new Date();

        lastPosition.setDeviceId(device.getId());
        lastPosition.setFixTime(date);
        lastPosition.set(FUEL_KEY, lastVoltage);
        lastPosition.set(Position.KEY_ODOMETER, 1);

        position.set(FUEL_KEY, currentVoltage);
        position.setDeviceId(device.getId());

    }
}
