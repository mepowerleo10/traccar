package org.traccar.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Test;
import org.traccar.BaseFuelTest;
import org.traccar.TestIdentityManager;
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.model.Sensor;

public class FuelLevelHandlerTest extends BaseFuelTest {

    @Test
    public void testPositionPropertiesCorrectedness() {
        assertEquals(1, position.getDeviceId());
        assertEquals(1, device.getSensors().size());
        assertEquals(1, device.getSensors().get(0).get(Device.SENSOR_CALIBRRATION));
    }

    @Test
    public void testCalculateFuel() {

        fuelLevelHandler = new FuelLevelHandler(new TestIdentityManager(),
                readingManager, calibrationManager, sensorManager);

        assertEquals(0.0, position.getDouble(Position.KEY_FUEL_LEVEL), 0);

        initialPosition = fuelLevelHandler.handlePosition(initialPosition);
        assertFuelRange(initialExpectedFuelLevel, initialMinimumFuelLevel, initialMaximumFuelLevel, initialPosition);
        
        fuelLevelHandler = new FuelLevelHandler(new TestIdentityManager(initialPosition), readingManager,
                calibrationManager, sensorManager);
        
        lastPosition = fuelLevelHandler.handlePosition(lastPosition);
        assertFuelRange(lastExpectedFuelLevel, lastMinimumFuelLevel, lastMaximumFuelLevel, lastPosition);

        fuelLevelHandler = new FuelLevelHandler(new TestIdentityManager(lastPosition), readingManager,
                calibrationManager, sensorManager);

        position.setFixTime(new Date(((Number) (date.getTime() + timeDiff * 2)).longValue()));
        position = fuelLevelHandler.handlePosition(position);
        assertFuelRange(currentExpectedFuelLevel, currentMinimumFuelLevel, currentMaximumFuelLevel, position);

    }

    @Test
    public void testCalculateFuelWithNoSensorValue() {

        Position positionWithNoFuelValue = new Position();
        positionWithNoFuelValue.setDeviceId(device.getId());

        fuelLevelHandler = new FuelLevelHandler(new TestIdentityManager(),
                readingManager, calibrationManager, sensorManager);
        Sensor sensor = sensorManager.getById(1);

        assertFalse(positionWithNoFuelValue.getAttributes().containsKey(sensor.getFuelPort()));

        assertFalse(positionWithNoFuelValue.getAttributes().containsKey(Position.KEY_FUEL_LEVEL));

    }

    @Test
    public void testFuelConsumptionOverHourBelowKm() {
        setUpParameters(timeDiff, 500);
        assertEquals(0.0, lastPosition.getDouble(Position.KEY_FUEL_TOTAL_CONSUMED_WITHIN_HOUR), 0);
        assertNotEquals(0.0, position.getDouble(Position.KEY_FUEL_TOTAL_CONSUMED_WITHIN_KM), 0);
    }

    @Test
    public void testFuelConsumptionBelowHourBelowKm() {
        setUpParameters(720000, 500);
        assertNotEquals(0.0, position.getDouble(Position.KEY_FUEL_TOTAL_CONSUMED_WITHIN_HOUR), 0);
        assertNotEquals(0.0, position.getDouble(Position.KEY_FUEL_TOTAL_CONSUMED_WITHIN_KM), 0);
    }

    @Test
    public void testFuelConsumptionBelowHourOverKm() {
        setUpParameters(720000, 1500);
        assertNotEquals(0.0, position.getDouble(Position.KEY_FUEL_TOTAL_CONSUMED_WITHIN_HOUR), 0);
        assertEquals(0.0, position.getDouble(Position.KEY_FUEL_TOTAL_CONSUMED_WITHIN_KM), 0);
    }

    @Test
    public void testFuelConsumptionOverHourOverKm() {
        setUpParameters(timeDiff, 1500);
        assertEquals(0.0, lastPosition.getDouble(Position.KEY_FUEL_TOTAL_CONSUMED_WITHIN_HOUR), 0);
        assertEquals(0.0, position.getDouble(Position.KEY_FUEL_TOTAL_CONSUMED_WITHIN_KM), 0);
    }

    @Test
    public void testFuelIncreaseOverHourBelowKm() {
        increaseVoltage();
        setUpParameters(timeDiff, 500);
        assertEquals(0.0, lastPosition.getDouble(Position.KEY_FUEL_TOTAL_INCREASED_WITHIN_HOUR), 0);
        assertNotEquals(0.0, position.getDouble(Position.KEY_FUEL_TOTAL_INCREASED_WITHIN_KM), 0);
    }

    @Test
    public void testFuelIncreaseBelowHourBelowKm() {
        increaseVoltage();
        setUpParameters(720000, 500);
        assertNotEquals(0.0, position.getDouble(Position.KEY_FUEL_TOTAL_INCREASED_WITHIN_HOUR), 0);
        assertNotEquals(0.0, position.getDouble(Position.KEY_FUEL_TOTAL_INCREASED_WITHIN_KM), 0);
    }

    @Test
    public void testFuelIncreaseBelowHourOverKm() {
        increaseVoltage();
        setUpParameters(720000, 1500);
        assertNotEquals(0.0, position.getDouble(Position.KEY_FUEL_TOTAL_INCREASED_WITHIN_HOUR), 0);
        assertEquals(0.0, position.getDouble(Position.KEY_FUEL_TOTAL_INCREASED_WITHIN_KM), 0);
    }

    @Test
    public void testFuelIncreaseOverHourOverKm() {
        increaseVoltage();
        setUpParameters(timeDiff, 1500);
        assertEquals(0.0, lastPosition.getDouble(Position.KEY_FUEL_TOTAL_INCREASED_WITHIN_HOUR), 0);
        assertEquals(0.0, position.getDouble(Position.KEY_FUEL_TOTAL_INCREASED_WITHIN_KM), 0);
    }

    public void increaseVoltage() {
        position.set(FUEL_KEY, 2120);
    }

    public void setUpParameters(double deltaTime, double deltaOdometer) {
        fuelLevelHandler = new FuelLevelHandler(new TestIdentityManager(),
                readingManager, calibrationManager, sensorManager);
        lastPosition = fuelLevelHandler.handlePosition(lastPosition);

        position.setFixTime(new Date(((Number) (date.getTime() + deltaTime)).longValue()));
        position.set(Position.KEY_ODOMETER, deltaOdometer);
        fuelLevelHandler = new FuelLevelHandler(new TestIdentityManager(lastPosition), readingManager,
                calibrationManager, sensorManager);
        position = fuelLevelHandler.handlePosition(position);
    }

    public void assertFuelRange(double expected, double minimumLevel, double maximumLevel, Position position) {
        assertTrue("Position must have: " + Position.KEY_FUEL_LEVEL,
                position.getAttributes().containsKey(Position.KEY_FUEL_LEVEL));

        double actual = position.getDouble(Position.KEY_FUEL_LEVEL);

        assertTrue("Fuel: " + actual + " must be greater than " + minimumLevel, actual > minimumLevel);
        assertTrue("Fuel: " + actual + " must be less than " + maximumLevel, actual <= maximumLevel);
        assertEquals(expected, position.getDouble(Position.KEY_FUEL_LEVEL), 0.9);
    }
}
