package org.traccar.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.BaseDataHandler;
import org.traccar.database.FuelCalibrationManager;
import org.traccar.database.IdentityManager;
import org.traccar.database.ReadingTypeManager;
import org.traccar.model.Device;
import org.traccar.model.FuelCalibration;
import org.traccar.model.Position;
import org.traccar.model.ReadingType;

import io.netty.channel.ChannelHandler;

@ChannelHandler.Sharable
public class FuelLevelHandler extends BaseDataHandler {
    private final IdentityManager identityManager;
    private final ReadingTypeManager readingTypeManager;
    private final FuelCalibrationManager fuelCalibrationManager;
    private static final int VOLTAGE_MONITORING_WINDOW_SIZE = 20; // IN MINUTES
    private Map<Integer, Double> sensorsFuelLevels = new HashMap<>();
    private Map<Integer, Integer> sensorsGroupCount = new HashMap<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(FuelLevelHandler.class);

    public FuelLevelHandler(IdentityManager identityManager, ReadingTypeManager readingTypeManager,
            FuelCalibrationManager fuelCalibrationManager) {
        this.identityManager = identityManager;
        this.readingTypeManager = readingTypeManager;
        this.fuelCalibrationManager = fuelCalibrationManager;
    }

    @Override
    protected Position handlePosition(Position position) {
        if (!position.getAttributes().containsKey(Position.KEY_FUEL_LEVEL)) {
            Device device = identityManager.getById(position.getDeviceId());
            List<Map<String, Object>> sensors = device.getSensors();

            if (device != null) {
                Position lastPosition = identityManager != null
                        ? identityManager.getLastPosition(position.getDeviceId())
                        : null;

                if (sensors.size() > 1 && lastPosition != null) {
                    for (Map<String, Object> sensor : sensors) {
                        try {
                            calculateSensorFuelAtPosition(lastPosition, position, device, sensor);
                        } catch (Exception e) {
                            LOGGER.error(e.getMessage());
                        }
                    }

                    double fuelLevel = 0.0;
                    for (int group : sensorsGroupCount.keySet()) {
                        fuelLevel = position.getDouble(Position.KEY_FUEL_LEVEL);
                        fuelLevel += sensorsFuelLevels.get(group) / sensorsGroupCount.get(group);
                    }

                    position.set(Position.KEY_FUEL_LEVEL, fuelLevel);

                    calculateFuelConsumptonRatePerHour(lastPosition, position);
                    calculateFuelConsumptionRateKmPerLitre(lastPosition, position);

                } else {
                    position.set(Position.KEY_FUEL_LEVEL, 0);
                    position.set(Position.KEY_FUEL_CONSUMPTION, 0);
                    position.set(Position.KEY_FUEL_CONSUMPTION_KM_PER_LITRE, 0);
                    position.set(Position.KEY_FUEL_USED, 0);
                }

                LOGGER.info("Device ID", device.getId());
                LOGGER.info("Fuel Level", position.getDouble(Position.KEY_FUEL_LEVEL));
            }
        }

        return position;
    }

    private void calculateDeviceFuelAtPosition(Position lastPosition, Position position, Device device,
            Map<String, Object> sensor) {

        LOGGER.info("Calculating fuel for device: " + device.getUniqueId());

        String fuelLevelPort = (String) sensor.getOrDefault(Device.SENSOR_FUEL_PORT, "");
        ReadingType readingType = readingTypeManager
                .getById((long) sensor.getOrDefault(Device.SENSOR_READING_ID, 0));
        boolean sensorIsCalibrated = (boolean) sensor.get(Device.SENSOR_ISCALIBRATED);

        if (sensorIsCalibrated) {
            try {
                FuelCalibration fuelCalibration = fuelCalibrationManager
                        .getById(((Number) sensor.get(Device.SENSOR_CALIBRRATION)).longValue());
                double fuelLevel = getCalibratedDeviceFuelLevel(device, lastPosition, position, fuelLevelPort,
                        fuelCalibration);
                position.set(Position.KEY_FUEL_LEVEL, fuelLevel);
            } catch (Exception e) {
                position.set(Position.KEY_FUEL_TIMER, 0);
                position.set(Position.KEY_FUEL_CURRENT_MAX_VOLTAGE, 0);
            }

        } else {
            double currentFuelLevel = position.getDouble(fuelLevelPort);
            position.set(Position.KEY_FUEL_LEVEL, currentFuelLevel
                    * readingType.getConversionMultiplier());
            position.set(Position.KEY_FUEL_USED, 0);
        }

    }

    private void calculateSensorFuelAtPosition(Position lastPosition, Position position, Device device,
            Map<String, Object> sensor) {

        LOGGER.info("Calculating fuel for sensor: " + sensor.get(Device.SENSOR_NAME));

        String fuelLevelPort = (String) sensor.getOrDefault(Device.SENSOR_FUEL_PORT, "fuel1");
        ReadingType readingType = readingTypeManager
                .getById((long) sensor.getOrDefault(Device.SENSOR_READING_ID, 0));
        boolean sensorIsCalibrated = (boolean) sensor.get(Device.SENSOR_ISCALIBRATED);
        int sensorGroup = ((Number) sensor.get(Device.SENSOR_GROUP)).intValue();
        double fuelLevel = 0;

        if (sensorIsCalibrated) {

            FuelCalibration fuelCalibration = fuelCalibrationManager
                    .getById(((Number) sensor.get(Device.SENSOR_CALIBRRATION)).longValue());
            fuelLevel = getCalibratedSensorFuelLevel(position, fuelLevelPort,
                    fuelCalibration);

        } else {
            fuelLevel = position.getDouble(fuelLevelPort) * readingType.getConversionMultiplier();
        }

        double groupFuelLevel = sensorsFuelLevels.getOrDefault(sensorGroup, 0.0);
        groupFuelLevel += fuelLevel;
        sensorsFuelLevels.put(sensorGroup, groupFuelLevel);

        int groupCount = sensorsGroupCount.getOrDefault(sensorGroup, 0);
        groupCount += 1;
        sensorsGroupCount.put(sensorGroup, groupCount);

    }

    private double getCalibratedSensorFuelLevel(Position position, String fuelLevelPort,
            FuelCalibration fuelCalibration) {

        double currentVoltageReading = position.getDouble(fuelLevelPort);

        double fuelLevel = 0;

        for (Map<String, Double> calibrationEntry : fuelCalibration.getCalibrationEntries()) {
            if (calibrationEntry.get(FuelCalibration.VOLTAGE) < currentVoltageReading) {
                continue;
            } else {
                fuelLevel = calibrationEntry.get(FuelCalibration.SLOPE) * currentVoltageReading
                        + calibrationEntry.get(FuelCalibration.INTERCEPT);
                break;
            }
        }
        fuelLevel = getWithinBoundsSensorFuelLevel(fuelLevel);

        return fuelLevel;
    }

    private double getWithinBoundsSensorFuelLevel(double fuelLevel) {
        if (fuelLevel < 0) {
            return 0;
        }

        return fuelLevel;
    }

    private double getCalibratedDeviceFuelLevel(Device device, Position last, Position position,
            String fuelLevelPort, FuelCalibration fuelCalibration) {

        double currentMaxVoltage = last.getDouble(Position.KEY_FUEL_CURRENT_MAX_VOLTAGE);
        double currentVoltageReading = position.getDouble(fuelLevelPort);

        double fuelLevel = 0;
        double lastFuelLevel = last.getDouble(Position.KEY_FUEL_LEVEL);

        double minutesBetween = (position.getFixTime().getTime() - last.getFixTime().getTime()) * 1.66667e-5;
        double timeBetween = last.getDouble(Position.KEY_FUEL_TIMER);

        if (currentVoltageReading > currentMaxVoltage) {
            currentMaxVoltage = currentVoltageReading;
        }

        timeBetween += minutesBetween;
        if (timeBetween >= VOLTAGE_MONITORING_WINDOW_SIZE) {
            for (Map<String, Double> calibrationEntry : fuelCalibration.getCalibrationEntries()) {
                if (calibrationEntry.get(FuelCalibration.VOLTAGE) < currentMaxVoltage) {
                    continue;
                } else {
                    fuelLevel = calibrationEntry.get(FuelCalibration.SLOPE) * currentMaxVoltage
                            + calibrationEntry.get(FuelCalibration.INTERCEPT);
                }
            }
            fuelLevel = getWithinBoundsFuelLevel(fuelLevel, lastFuelLevel);
            currentMaxVoltage = currentVoltageReading;
            timeBetween = timeBetween % VOLTAGE_MONITORING_WINDOW_SIZE;
        } else {
            fuelLevel = lastFuelLevel;
        }

        position.set(Position.KEY_FUEL_TIMER, timeBetween);
        position.set(Position.KEY_FUEL_CURRENT_MAX_VOLTAGE, currentMaxVoltage);

        return fuelLevel;
    }

    private double getWithinBoundsFuelLevel(double fuelLevel, double lastFuelLevel) {
        if (fuelLevel < 0) {
            return lastFuelLevel;
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

        double totalFuelConsumedWithinHour = lastPosition.getDouble(Position.KEY_FUEL_TOTAL_CONSUMED_WITHIN_HOUR);
        totalFuelConsumedWithinHour += fuelDifference;

        if (Math.abs(fuelDifference) != 0 && Math.abs(hoursBetween) != 0) {
            consumptionLitresPerHour = fuelDifference / hoursBetween;
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

        if (Math.abs(fuelDifference) != 0) {
            consumptionKilometersPerLitre = odometerDifference != 0 ? odometerDifference / fuelDifference : 0;
        }

        double totalFuelConsumedWithinKm = lastPosition.getDouble(Position.KEY_FUEL_TOTAL_CONSUMED_WITHIN_KM);
        totalFuelConsumedWithinKm += fuelDifference;

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
            position.set(Position.KEY_FUEL_RATE_HOUR, averageConsumption);

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
            position.set(Position.KEY_FUEL_RATE_KM, averageConsumption);

            /* reset the counters */
            position.set(Position.KEY_FUEL_POSITIONS_COUNT_WITHIN_KM, 0);
            position.set(Position.KEY_FUEL_TOTAL_CONSUMED_WITHIN_KM, 0);

        }
    }

}
