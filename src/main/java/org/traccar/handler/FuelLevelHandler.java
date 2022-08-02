package org.traccar.handler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private Map<Integer, Double> sensorsFuelLevels;
    private Map<Integer, Integer> sensorsGroupCount;

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
            try {
                List<Map<String, Object>> sensors = device.getSensors();
                this.sensorsFuelLevels = new HashMap<>();
                this.sensorsGroupCount = new HashMap<>();

                if (device != null) {

                    Position lastPosition = identityManager != null
                            ? identityManager.getLastPosition(position.getDeviceId())
                            : null;

                    if (sensors.size() > 0) {
                        for (Map<String, Object> sensor : sensors) {
                            try {
                                calculateSensorFuelAtPosition(position, device, sensor);
                            } catch (Exception e) {
                            }
                        }

                        double fuelLevel = 0.0;
                        for (int group : sensorsGroupCount.keySet()) {
                            fuelLevel += sensorsFuelLevels.get(group) / sensorsGroupCount.get(group); // update total
                                                                                                      // fuel level
                            position.set(Position.KEY_TANK + group, fuelLevel); // update tank fuel level
                        }

                        position.set(Position.KEY_FUEL_LEVEL, fuelLevel);
                        if (fuelLevel < 0) {
                            position.set(Position.KEY_FUEL_LEVEL, lastPosition.getDouble(Position.KEY_FUEL_LEVEL));
                        }

                        if (lastPosition != null) {
                            calculateFuelConsumptonRatePerHour(lastPosition, position);
                            calculateFuelConsumptionRateKmPerLitre(lastPosition, position);
                        }

                    } else {
                        position.set(Position.KEY_FUEL_LEVEL, 0);
                        position.set(Position.KEY_FUEL_CONSUMPTION, 0);
                        position.set(Position.KEY_FUEL_CONSUMPTION_KM_PER_LITRE, 0);
                        position.set(Position.KEY_FUEL_USED, 0);
                    }
                }
            } catch (Exception e) {
            }
        }

        return position;
    }

    private void calculateSensorFuelAtPosition(Position position, Device device,
            Map<String, Object> sensor) {

        String fuelLevelPort = (String) sensor.getOrDefault(Device.SENSOR_FUEL_PORT, "fuel");
        ReadingType readingType = readingTypeManager
                .getById(((Number) sensor.getOrDefault(Device.SENSOR_READING_ID, 0)).longValue());
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
        position.set("GROUP_COUNT", groupCount);
        sensorsGroupCount.put(sensorGroup, groupCount);

    }

    private double getCalibratedSensorFuelLevel(Position position, String fuelLevelPort,
            FuelCalibration fuelCalibration) {

        double currentVoltageReading = position.getDouble(fuelLevelPort);

        double fuelLevel = 0;
        double index = -1;

        Map<String, Double> lastLeastCalibration = fuelCalibration.getCalibrationEntries().get(0);

        for (Map<String, Double> calibrationEntry : fuelCalibration.getCalibrationEntries()) {

            index += 1;
            if (index == fuelCalibration.getCalibrationEntries().size() - 1) {
                break;
            }

            if (currentVoltageReading > calibrationEntry.get(FuelCalibration.VOLTAGE)) {
                lastLeastCalibration = calibrationEntry;
                continue;
            } else {
                break;
            }
        }

        fuelLevel = lastLeastCalibration.get(FuelCalibration.SLOPE) * currentVoltageReading
                + lastLeastCalibration.get(FuelCalibration.INTERCEPT);

        return fuelLevel;
    }

    private double getFuelDifference(Position lastPosition, Position position) {
        double currentFuelLevel = position.getDouble(Position.KEY_FUEL_LEVEL);
        double lastFuelLevel = lastPosition.getDouble(Position.KEY_FUEL_LEVEL);
        double fuelDifference = currentFuelLevel - lastFuelLevel;
        return fuelDifference;
    }

    private void calculateFuelConsumptonRatePerHour(Position lastPosition, Position position) {
        double fuelDifference = getFuelDifference(lastPosition, position);

        double millisecondsBetween = (position.getFixTime().getTime() - lastPosition.getFixTime().getTime());

        double thresholdTime = lastPosition.getDouble(Position.KEY_FUEL_THRESHOLD_TIME);
        thresholdTime += millisecondsBetween * 2.77778e-7; // hours

        double fuelRefillTimer = lastPosition.getDouble(Position.KEY_FUEL_REFILL_TIMER);
        fuelRefillTimer += millisecondsBetween * 1.6667e-5; // minutes

        double totalFuelConsumedWithinHour = lastPosition.getDouble(Position.KEY_FUEL_TOTAL_CONSUMED_WITHIN_HOUR);
        double totalFuelIncreasedWithinHour = lastPosition.getDouble(Position.KEY_FUEL_TOTAL_INCREASED_WITHIN_HOUR);

        if (fuelDifference <= 0) {
            totalFuelConsumedWithinHour += fuelDifference;
        } else {
            totalFuelIncreasedWithinHour += fuelDifference;
        }

        position.set(Position.KEY_FUEL_THRESHOLD_TIME, thresholdTime);
        position.set(Position.KEY_FUEL_REFILL_TIMER, fuelRefillTimer);
        position.set(Position.KEY_FUEL_TOTAL_CONSUMED_WITHIN_HOUR, totalFuelConsumedWithinHour);
        position.set(Position.KEY_FUEL_TOTAL_INCREASED_WITHIN_HOUR, totalFuelIncreasedWithinHour);

        updateAndResetFuelTimeCounters(position, lastPosition.getDouble(Position.KEY_FUEL_CONSUMPTION));
    }

    private void updateAndResetFuelTimeCounters(Position position, double lastAverageConsumption) {
        double thresholdTime = position.getDouble(Position.KEY_FUEL_THRESHOLD_TIME);

        if (thresholdTime >= 1) {
            double totalFuelConsumedWithinHour = position.getDouble(Position.KEY_FUEL_TOTAL_CONSUMED_WITHIN_HOUR);
            double averageConsumption = totalFuelConsumedWithinHour / thresholdTime;

            position.set(Position.KEY_FUEL_THRESHOLD_TIME, thresholdTime % 1);
            position.set(Position.KEY_FUEL_CONSUMPTION, averageConsumption);

            /* reset the counters */
            position.set(Position.KEY_FUEL_TOTAL_CONSUMED_WITHIN_HOUR, 0);
            position.set(Position.KEY_FUEL_TOTAL_INCREASED_WITHIN_HOUR, 0);
        } else {
            position.set(Position.KEY_FUEL_CONSUMPTION, lastAverageConsumption);
        }
    }

    private void calculateFuelConsumptionRateKmPerLitre(Position lastPosition, Position position) {

        double fuelDifference = getFuelDifference(lastPosition, position);

        double odometerDifference = (position.getDouble(Position.KEY_ODOMETER)
                - lastPosition.getDouble(Position.KEY_ODOMETER)) * 0.001; /* in kilometers */

        double thresholdDistance = lastPosition.getDouble(Position.KEY_FUEL_THRESHOLD_DISTANCE);
        thresholdDistance += odometerDifference;

        double totalFuelConsumedWithinKm = lastPosition.getDouble(Position.KEY_FUEL_TOTAL_CONSUMED_WITHIN_KM);
        double totalFuelIncreasedWithinKm = lastPosition.getDouble(Position.KEY_FUEL_TOTAL_INCREASED_WITHIN_KM);

        if (fuelDifference <= 0) {
            totalFuelConsumedWithinKm += fuelDifference;
        } else {
            totalFuelIncreasedWithinKm += fuelDifference;
        }

        position.set(Position.KEY_FUEL_THRESHOLD_DISTANCE, thresholdDistance);
        position.set(Position.KEY_FUEL_TOTAL_CONSUMED_WITHIN_KM, totalFuelConsumedWithinKm);
        position.set(Position.KEY_FUEL_TOTAL_INCREASED_WITHIN_KM, totalFuelIncreasedWithinKm);

        updateAndResetFuelDistanceCounters(position, position.getDouble(Position.KEY_FUEL_CONSUMPTION_KM_PER_LITRE));
    }

    private void updateAndResetFuelDistanceCounters(Position position, double lastAverageConsumption) {
        double thresholdDistance = position.getDouble(Position.KEY_FUEL_THRESHOLD_DISTANCE);

        if (thresholdDistance >= 1) {
            double totalFuelConsumedWithinKm = position.getDouble(Position.KEY_FUEL_TOTAL_CONSUMED_WITHIN_KM);
            double averageConsumption = totalFuelConsumedWithinKm / thresholdDistance;

            position.set(Position.KEY_FUEL_THRESHOLD_DISTANCE, thresholdDistance % 1);
            position.set(Position.KEY_FUEL_CONSUMPTION_KM_PER_LITRE, averageConsumption);

            /* reset the counters */
            position.set(Position.KEY_FUEL_TOTAL_CONSUMED_WITHIN_KM, 0);
            position.set(Position.KEY_FUEL_TOTAL_INCREASED_WITHIN_KM, 0);
        } else {
            position.set(Position.KEY_FUEL_CONSUMPTION_KM_PER_LITRE, lastAverageConsumption);
        }
    }

}
