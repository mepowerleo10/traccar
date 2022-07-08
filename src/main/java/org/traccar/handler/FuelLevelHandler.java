package org.traccar.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.BaseDataHandler;
import org.traccar.database.FuelSensorManager;
import org.traccar.database.IdentityManager;
import org.traccar.database.ReadingTypeManager;
import org.traccar.model.Device;
import org.traccar.model.FuelSensor;
import org.traccar.model.Position;
import org.traccar.model.ReadingType;

import io.netty.channel.ChannelHandler;

@ChannelHandler.Sharable
public class FuelLevelHandler extends BaseDataHandler {
    private final IdentityManager identityManager;
    private final ReadingTypeManager readingTypeManager;
    private final FuelSensorManager fuelSensorManager;
    private static final int DEVICE_OFF_VOLTAGE = 129;
    private static final int VOLTAGE_MONITORING_WINDOW_SIZE = 15; // IN MINUTES

    private static final Logger LOGGER = LoggerFactory.getLogger(FuelLevelHandler.class);

    public FuelLevelHandler(IdentityManager identityManager, ReadingTypeManager readingTypeManager,
            FuelSensorManager fuelSensorManager) {
        this.identityManager = identityManager;
        this.readingTypeManager = readingTypeManager;
        this.fuelSensorManager = fuelSensorManager;
    }

    @Override
    protected Position handlePosition(Position position) {
        if (!position.getAttributes().containsKey(Position.KEY_FUEL_LEVEL)) {
            Device device = identityManager.getById(position.getDeviceId());

            if (device != null && device.getFuelSensorId() > 0) {
                Position lastPosition = identityManager != null
                        ? identityManager.getLastPosition(position.getDeviceId())
                        : null;
                FuelSensor sensor = fuelSensorManager.getById(device.getFuelSensorId());
                calculateDeviceFuelAtPosition(lastPosition, position, device, sensor);
                LOGGER.info("Device ID", device.getId());
                LOGGER.info("Fuel Level", position.getDouble(Position.KEY_FUEL_LEVEL));
            }
        }
        return position;
    }

    private void calculateDeviceFuelAtPosition(Position lastPosition, Position position, Device device,
            FuelSensor sensor) {
        LOGGER.info("Calculating fuel for device: " + device.getUniqueId());
        if (sensor != null && lastPosition != null) {
            ReadingType readingType = readingTypeManager.getById(sensor.getReadingTypeId());

            if (sensor.getCalibrated()) {
                try {
                    double fuelLevel = calculateCalibratedDeviceFuelLevel(device, position, lastPosition, sensor);
                    position.set(Position.KEY_FUEL_LEVEL, fuelLevel);
                } catch (Exception e) {
                    position.set(Position.KEY_FUEL_TIMER, 0);
                    position.set(Position.KEY_FUEL_CURRENT_MAX_VOLTAGE, 0);
                }

            } else {

                double currentFuelLevel = position.getDouble(sensor.getFuelLevelPort());
                position.set(Position.KEY_FUEL_LEVEL, currentFuelLevel
                        * readingType.getConversionMultiplier());
                position.set(Position.KEY_FUEL_USED,
                        position.getDouble(sensor.getFuelConsumedPort()));
            }

            calculateFuelConsumptonRatePerHour(lastPosition, position);
            calculateFuelConsumptionRateKmPerLitre(lastPosition, position);
        } else {
            position.set(Position.KEY_FUEL_LEVEL, 0);
            position.set(Position.KEY_FUEL_CONSUMPTION, 0);
            position.set(Position.KEY_FUEL_CONSUMPTION_KM_PER_LITRE, 0);
            position.set(Position.KEY_FUEL_USED, 0);
        }

        if (position.getDouble(Position.KEY_FUEL_LEVEL) == -1) {
            position.set(Position.KEY_FUEL_LEVEL, lastPosition.getDouble(Position.KEY_FUEL_LEVEL));
        }
    }

    private double calculateCalibratedDeviceFuelLevel(Device device, Position last, Position position,
            FuelSensor sensor) {
        double currentMaxVoltage = last.getDouble(Position.KEY_FUEL_CURRENT_MAX_VOLTAGE);
        double currentVoltageReading = position.getDouble(sensor.getFuelLevelPort());

        double fuelLevel = 0;
        double lastFuelLevel = last.getDouble(Position.KEY_FUEL_LEVEL);

        double minutesBetween = (position.getFixTime().getTime() - last.getFixTime().getTime()) * 1.66667e-5;
        double timeBetween = last.getDouble(Position.KEY_FUEL_TIMER);

        if (currentVoltageReading > currentMaxVoltage) {
            currentMaxVoltage = currentVoltageReading;
        }

        timeBetween += minutesBetween;
        if (timeBetween >= VOLTAGE_MONITORING_WINDOW_SIZE) {
            fuelLevel = device.getFuelSlope() * currentMaxVoltage + device.getFuelConstant();
            currentMaxVoltage = currentVoltageReading;
            timeBetween = timeBetween % VOLTAGE_MONITORING_WINDOW_SIZE;
        } else {
            fuelLevel = lastFuelLevel;
        }

        position.set(Position.KEY_FUEL_TIMER, timeBetween);
        position.set(Position.KEY_FUEL_CURRENT_MAX_VOLTAGE, currentMaxVoltage);

        fuelLevel = getWithinBoundsFuelLevel(fuelLevel, lastFuelLevel, sensor);

        return fuelLevel;
    }

    private double getWithinBoundsFuelLevel(double fuelLevel, double lastFuelLevel, FuelSensor sensor) {
        if (fuelLevel < sensor.getLowerBound()) {
            return lastFuelLevel;
        }

        if (fuelLevel > sensor.getUpperBound()) {
            return sensor.getUpperBound();
        }

        return fuelLevel;
    }

    private void calculateFuelConsumptonRatePerHour(Position lastPosition, Position position) {
        double consumptionLitresPerHour = 0; // litres/hour
        double currentFuelLevel = position.getDouble(Position.KEY_FUEL_LEVEL);
        double lastFuelLevel = lastPosition.getDouble(Position.KEY_FUEL_LEVEL);
        double fuelDifference = currentFuelLevel - lastFuelLevel;

        double hoursBetween = (position.getFixTime().getTime() - lastPosition.getFixTime().getTime()) * 2.77778e-7;
        double positionsCount = lastPosition.getInteger(Position.KEY_FUEL_POSITIONS_COUNT_WITHIN_HOUR);
        positionsCount += 1;

        double thresholdTime = lastPosition.getDouble(Position.KEY_FUEL_THRESHOLD_TIME);
        thresholdTime += hoursBetween;

        if (hoursBetween != 0) {
            consumptionLitresPerHour = fuelDifference / hoursBetween;
        }

        double totalFuelConsumedWithinHour = lastPosition.getDouble(Position.KEY_FUEL_TOTAL_CONSUMED_WITHIN_HOUR);
        totalFuelConsumedWithinHour += fuelDifference;

        if (Math.abs(consumptionLitresPerHour) != 0) {
            consumptionLitresPerHour = lastPosition.getDouble(Position.KEY_FUEL_CONSUMPTION);
        }

        position.set(Position.KEY_FUEL_POSITIONS_COUNT_WITHIN_HOUR, positionsCount);
        position.set(Position.KEY_FUEL_THRESHOLD_TIME, thresholdTime);
        position.set(Position.KEY_FUEL_TOTAL_CONSUMED_WITHIN_HOUR, totalFuelConsumedWithinHour);
        position.set(Position.KEY_FUEL_CONSUMPTION, consumptionLitresPerHour);

        checkAndResetTimeCounters(position);
    }

    private void calculateFuelConsumptionRateKmPerLitre(Position lastPosition, Position position) {
        double consumptionKilometersPerLitre = 0; // kilometers/litre
        double currentFuelLevel = position.getDouble(Position.KEY_FUEL_LEVEL);
        double lastFuelLevel = lastPosition.getDouble(Position.KEY_FUEL_LEVEL);
        double fuelDifference = currentFuelLevel - lastFuelLevel; /* in litres */

        double odometerDifference = (position.getDouble(Position.KEY_ODOMETER)
                - lastPosition.getDouble(Position.KEY_ODOMETER)) * 0.001; /* in kilometers */

        int positionsCount = lastPosition.getInteger(Position.KEY_FUEL_POSITIONS_COUNT_WITHIN_KM);
        positionsCount += 1;

        double thresholdDistance = lastPosition.getDouble(Position.KEY_FUEL_THRESHOLD_DISTANCE);
        thresholdDistance += odometerDifference;

        if (fuelDifference != 0) {
            consumptionKilometersPerLitre = odometerDifference != 0 ? odometerDifference / fuelDifference : 0;
        }

        double totalFuelConsumedWithinKm = lastPosition.getDouble(Position.KEY_FUEL_TOTAL_CONSUMED_WITHIN_KM);
        totalFuelConsumedWithinKm += fuelDifference;

        if (consumptionKilometersPerLitre != 0) {
            consumptionKilometersPerLitre = lastPosition.getDouble(Position.KEY_FUEL_CONSUMPTION_KM_PER_LITRE);
        }

        position.set(Position.KEY_FUEL_POSITIONS_COUNT_WITHIN_KM, positionsCount);
        position.set(Position.KEY_FUEL_THRESHOLD_DISTANCE, thresholdDistance);
        position.set(Position.KEY_FUEL_TOTAL_CONSUMED_WITHIN_KM, totalFuelConsumedWithinKm);
        position.set(Position.KEY_FUEL_CONSUMPTION_KM_PER_LITRE, consumptionKilometersPerLitre);

        checkAndResetDistanceCounters(position);
    }

    private void checkAndResetTimeCounters(Position position) {
        double thresholdTime = position.getDouble(Position.KEY_FUEL_THRESHOLD_TIME);

        if (thresholdTime >= 1) {
            double positionsCount = position.getInteger(Position.KEY_FUEL_POSITIONS_COUNT_WITHIN_HOUR);
            double totalFuelConsumedWithinHour = position.getDouble(Position.KEY_FUEL_TOTAL_CONSUMED_WITHIN_HOUR);
            double averageConsumption = totalFuelConsumedWithinHour / (thresholdTime * positionsCount);
            position.set(Position.KEY_FUEL_THRESHOLD_TIME, thresholdTime % 1);
            position.set(Position.KEY_FUEL_CONSUMPTION, averageConsumption);

            /* reset the counters */
            position.set(Position.KEY_FUEL_POSITIONS_COUNT_WITHIN_HOUR, 0);
            position.set(Position.KEY_FUEL_TOTAL_CONSUMED_WITHIN_HOUR, 0);
        }
    }

    private void checkAndResetDistanceCounters(Position position) {
        double thresholdDistance = position.getDouble(Position.KEY_FUEL_THRESHOLD_DISTANCE);

        if (thresholdDistance >= 1) {
            double positionsCount = position.getInteger(Position.KEY_FUEL_POSITIONS_COUNT_WITHIN_KM);
            double totalFuelConsumedWithinKm = position.getDouble(Position.KEY_FUEL_TOTAL_CONSUMED_WITHIN_KM);
            double averageConsumption = totalFuelConsumedWithinKm / (thresholdDistance * positionsCount);

            position.set(Position.KEY_FUEL_THRESHOLD_DISTANCE, thresholdDistance % 1);
            position.set(Position.KEY_FUEL_CONSUMPTION_KM_PER_LITRE, averageConsumption);

            /* reset the counters */
            position.set(Position.KEY_FUEL_POSITIONS_COUNT_WITHIN_KM, 0);
            position.set(Position.KEY_FUEL_TOTAL_CONSUMED_WITHIN_KM, 0);

        }
    }

}
