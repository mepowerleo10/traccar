package org.traccar.handler;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.math3.exception.OutOfRangeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.BaseDataHandler;
import org.traccar.database.FuelCalibrationManager;
import org.traccar.database.IdentityManager;
import org.traccar.database.ReadingTypeManager;
import org.traccar.database.SensorManager;
import org.traccar.handler.events.FuelRefillEventHandler;
import org.traccar.model.FuelCalibration;
import org.traccar.model.Position;
import org.traccar.model.ReadingType;
import org.traccar.model.Sensor;

import io.netty.channel.ChannelHandler;

@ChannelHandler.Sharable
public class FuelLevelHandler extends BaseDataHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FuelLevelHandler.class);

    private final IdentityManager identityManager;
    private final ReadingTypeManager readingTypeManager;
    private final FuelCalibrationManager fuelCalibrationManager;
    private final SensorManager sensorManager;

    public FuelLevelHandler(IdentityManager identityManager, ReadingTypeManager readingTypeManager,
            FuelCalibrationManager fuelCalibrationManager, SensorManager sensorManager) {
        this.identityManager = identityManager;
        this.readingTypeManager = readingTypeManager;
        this.fuelCalibrationManager = fuelCalibrationManager;
        this.sensorManager = sensorManager;
    }

    class Groups {
        private Map<Integer, Double> fuelLevels;
        private Map<Integer, Integer> count;

        Groups() {
            this.fuelLevels = new ConcurrentHashMap<>();
            this.count = new ConcurrentHashMap<>();
        }
    }

    @Override
    protected Position handlePosition(Position position) {
        Groups groups = new Groups();

        if (!position.getAttributes().containsKey(Position.KEY_FUEL_LEVEL)) {
            Long deviceId = position.getDeviceId();
            if (deviceId != null) {
                try {
                    Collection<Sensor> sensors;
                    Position lastPosition = identityManager != null
                            ? identityManager.getLastPosition(position.getDeviceId())
                            : null;

                    sensors = sensorManager.getDeviceSensors(deviceId);

                    if (sensors.size() > 0) {
                        int i = -1;
                        for (Sensor sensor : sensors) {
                            ++i;

                            try {
                                calculateSensorFuelAtPosition(i, position, lastPosition, deviceId, sensor, groups);
                            } catch (Exception e) {
                                LOGGER.error("DeviceId: " + deviceId + ", Sensor Fuel Error",
                                        e);
                            }
                        }

                        double fuelLevel = 0.0;
                        for (int group : groups.count.keySet()) {
                            double tankFuelLevel = groups.fuelLevels.getOrDefault(group, 0.0)
                                    / groups.count.getOrDefault(group, 0);
                            fuelLevel += tankFuelLevel; // update the total fuel level
                            position.set(Position.KEY_TANK + group, tankFuelLevel); // update tank fuel level
                            LOGGER.info("[" + position.getDeviceTime() + "] DeviceId: " + deviceId + ", tank" + group
                                    + "FuelLevel: " + tankFuelLevel);
                        }

                        boolean hasFuelData = groups.count.keySet().size() > 0;

                        if (hasFuelData && fuelLevel >= 0) {
                            position.set(Position.KEY_FUEL_LEVEL, fuelLevel);
                        }

                        if (lastPosition != null && hasFuelData) {
                            calculateFuelConsumption(lastPosition, position);
                        }

                    }

                } catch (Exception e) {
                    LOGGER.error(e.getStackTrace().toString());
                }
            }
        }

        return position;
    }

    private void calculateSensorFuelAtPosition(int sensorIndex, Position position, Position last, Long deviceId,
            Sensor sensor, Groups groups) throws Exception {

        String fuelLevelPort = sensor.getFuelPort();
        ReadingType readingType = readingTypeManager
                .getById(sensor.getReadingTypeId());
        boolean sensorIsCalibrated = sensor.getIsCalibrated();
        int sensorGroup = sensor.getGroupNo();
        double fuelLevel = 0;

        boolean positionContainsFuelValue = position.getAttributes().containsKey(fuelLevelPort);

        if (!positionContainsFuelValue) {
            throw new NullPointerException("Fuel port: " + fuelLevelPort + " does not have a value");
        }

        double currentVoltageReading = position.getDouble(fuelLevelPort);

        position.set(Position.KEY_FUEL_VOLTAGE + sensorIndex, currentVoltageReading);

        if (sensorIsCalibrated) {

            FuelCalibration fuelCalibration = fuelCalibrationManager
                    .getById(sensor.getCalibrationId());
            fuelLevel = getCalibratedSensorFuelLevel(position, currentVoltageReading,
                    fuelCalibration);

            LOGGER.info("Computed fuel for: " + " DeviceID: " + deviceId + ", Sensor ID: " + sensor.getId()
                    + ", Calibration ID: " + fuelCalibration.getId());
            position.set("fuelSensor" + sensorIndex, sensor.toString());

        } else {
            fuelLevel = currentVoltageReading * readingType.getConversionMultiplier();
        }

        double groupFuelLevel = groups.fuelLevels.getOrDefault(sensorGroup, 0.0);
        groupFuelLevel += fuelLevel;
        groups.fuelLevels.put(sensorGroup, groupFuelLevel);

        int groupCount = groups.count.getOrDefault(sensorGroup, 0);
        groupCount += 1;
        groups.count.put(sensorGroup, groupCount);

    }

    private double getCalibratedSensorFuelLevel(Position position, double currentVoltageReading,
            FuelCalibration fuelCalibration) throws Exception {

        double fuelLevel = 0;
        double index = -1;

        Map<String, Double> lastLeastCalibration = fuelCalibration.getCalibrationEntries().get(0);

        double calibrationsSize = fuelCalibration.getCalibrationEntries().size();
        double minimumVoltageLevel = lastLeastCalibration.get(FuelCalibration.VOLTAGE);
        double maximumVoltageLevel = fuelCalibration.getCalibrationEntries().get((int) (calibrationsSize - 1))
                .get(FuelCalibration.VOLTAGE);

        if (currentVoltageReading < minimumVoltageLevel || currentVoltageReading > maximumVoltageLevel) {
            throw new OutOfRangeException(currentVoltageReading, minimumVoltageLevel, maximumVoltageLevel);
        }

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

        if (0 <= fuelLevel && fuelLevel < 1) {
            fuelLevel = 0;
        }

        LOGGER.info("[" + position.getDeviceTime() + "] Done computing fuel level for deviceid: "
                + position.getDeviceId() + ", calibration: "
                + lastLeastCalibration + "fuelLevel: " + fuelLevel);
        position.set("fuelCalibration" + fuelCalibration.getId(), lastLeastCalibration.toString());

        return fuelLevel;
    }

    private double getFuelDifference(Position lastPosition, Position position) {
        double currentFuelLevel = position.getDouble(Position.KEY_FUEL_LEVEL);
        double lastFuelLevel = lastPosition.getDouble(Position.KEY_FUEL_LEVEL);
        double fuelDifference = currentFuelLevel - lastFuelLevel;
        return fuelDifference;
    }

    private void calculateFuelConsumption(Position lastPosition, Position position) {
        if (lastPosition.getFixTime().before(position.getFixTime())) {
            double fuelDifference = getFuelDifference(lastPosition, position);
            double totalFuelUsed = position.getDouble(Position.KEY_TOTAL_FUEL_USED);
            double totalFuelRefilled = position.getDouble(Position.KEY_TOTAL_FUEL_REFILLED);

            if (fuelDifference < 0) {
                position.set(Position.KEY_FUEL_USED, fuelDifference);

                totalFuelUsed += fuelDifference;
            } else {
                totalFuelRefilled += fuelDifference;
            }

            position.set(Position.KEY_TOTAL_FUEL_USED, totalFuelUsed);
            position.set(Position.KEY_TOTAL_FUEL_REFILLED, totalFuelRefilled);

            calculateFuelConsumptonRatePerHour(lastPosition, position, fuelDifference);
            calculateFuelConsumptionRateKmPerLitre(lastPosition, position, fuelDifference);
        }
    }

    private void calculateFuelConsumptonRatePerHour(Position lastPosition, Position position, double fuelDifference) {

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
        double totalFuelConsumedWithinHour = position.getDouble(Position.KEY_FUEL_TOTAL_CONSUMED_WITHIN_HOUR);
        double totalFuelIncreasedWithinHour = position.getDouble(Position.KEY_FUEL_TOTAL_INCREASED_WITHIN_HOUR);
        if (thresholdTime >= 1) {
            double averageConsumption = totalFuelConsumedWithinHour / thresholdTime;

            position.set(Position.KEY_FUEL_THRESHOLD_TIME, thresholdTime % 1);
            position.set(Position.KEY_FUEL_CONSUMPTION, averageConsumption);

            /* reset the counters */
            position.set(Position.KEY_FUEL_TOTAL_CONSUMED_WITHIN_HOUR, 0);
            position.set(Position.KEY_FUEL_TOTAL_INCREASED_WITHIN_HOUR, 0);
        } else {
            position.set(Position.KEY_FUEL_CONSUMPTION, lastAverageConsumption);
        }

        double fuelRefillTimer = position.getDouble(Position.KEY_FUEL_REFILL_TIMER);
        if (fuelRefillTimer > FuelRefillEventHandler.REFILL_CHECK_MINUTES) {
            fuelRefillTimer = fuelRefillTimer % FuelRefillEventHandler.REFILL_CHECK_MINUTES;
            position.set(Position.KEY_FUEL_REFILL_TIMER, fuelRefillTimer);
        }

        position.set(Position.KEY_FUEL_CONSUMPTION_PER_HOUR, totalFuelConsumedWithinHour);
        position.set(Position.KEY_FUEL_INCREASE_PER_HOUR, totalFuelIncreasedWithinHour);
    }

    private void calculateFuelConsumptionRateKmPerLitre(Position lastPosition, Position position,
            double fuelDifference) {

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
        double totalFuelConsumedWithinKm = position.getDouble(Position.KEY_FUEL_TOTAL_CONSUMED_WITHIN_KM);
        double totalFuelIncreasedWithinKm = position.getDouble(Position.KEY_FUEL_TOTAL_INCREASED_WITHIN_KM);

        if (thresholdDistance >= 1) {
            double averageConsumption = totalFuelConsumedWithinKm / thresholdDistance;

            position.set(Position.KEY_FUEL_THRESHOLD_DISTANCE, thresholdDistance % 1);
            position.set(Position.KEY_FUEL_CONSUMPTION_KM_PER_LITRE, averageConsumption);

            /* reset the counters */
            position.set(Position.KEY_FUEL_TOTAL_CONSUMED_WITHIN_KM, 0);
            position.set(Position.KEY_FUEL_TOTAL_INCREASED_WITHIN_KM, 0);
        } else {
            position.set(Position.KEY_FUEL_CONSUMPTION_KM_PER_LITRE, lastAverageConsumption);
        }

        position.set(Position.KEY_FUEL_CONSUMPTION_PER_KM, totalFuelConsumedWithinKm);
        position.set(Position.KEY_FUEL_INCREASE_PER_KM, totalFuelIncreasedWithinKm);
    }

}
