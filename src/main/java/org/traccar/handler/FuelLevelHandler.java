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
        if (sensor != null && lastPosition != null) {
            ReadingType readingType = readingTypeManager.getById(sensor.getReadingTypeId());

            if (sensor.getCalibrated()) {
                double fuelLevel = device.getFuelSlope() * position.getDouble(sensor.getFuelLevelPort())
                        + device.getFuelConstant();

                double boundedFuelLevel = getWithinBoundsFuelLevel(fuelLevel, sensor);
                position.set(Position.KEY_FUEL_LEVEL, boundedFuelLevel);

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

    private void calculateFuelConsumptonRatePerHour(Position lastPosition, Position position) {
        double consumptionLitresPerHour = 0; // litres/hour
        double currentFuelLevel = position.getDouble(Position.KEY_FUEL_LEVEL);
        double lastFuelLevel = lastPosition.getDouble(Position.KEY_FUEL_LEVEL);
        double fuelDifference = currentFuelLevel - lastFuelLevel;

        double hoursBetween = (position.getFixTime().getTime() - lastPosition.getFixTime().getTime());
                // / (1000 * 60 * 60);
        hoursBetween = hoursBetween * 2.77778e-7;

        double positionsCount = lastPosition.getInteger(Position.KEY_FUEL_POSITIONS_COUNT_WITHIN_HOUR);
        positionsCount += 1;

        double thresholdTime = lastPosition.getDouble(Position.KEY_FUEL_THRESHOLD_TIME);
        thresholdTime += hoursBetween;

        if (hoursBetween != 0) {
            consumptionLitresPerHour = fuelDifference / hoursBetween;
        }

        double totalFuelConsumedWithinHour = lastPosition.getDouble(Position.KEY_FUEL_TOTAL_CONSUMED_WITHIN_HOUR);
        totalFuelConsumedWithinHour += fuelDifference;

        position.set(Position.KEY_FUEL_POSITIONS_COUNT_WITHIN_HOUR, positionsCount);
        position.set(Position.KEY_FUEL_THRESHOLD_TIME, thresholdTime);
        position.set(Position.KEY_FUEL_TOTAL_CONSUMED_WITHIN_HOUR, totalFuelConsumedWithinHour);
        position.set(Position.KEY_FUEL_CONSUMPTION, consumptionLitresPerHour);
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

        position.set(Position.KEY_FUEL_POSITIONS_COUNT_WITHIN_KM, positionsCount);
        position.set(Position.KEY_FUEL_THRESHOLD_DISTANCE, thresholdDistance);
        position.set(Position.KEY_FUEL_TOTAL_CONSUMED_WITHIN_KM, totalFuelConsumedWithinKm);
        position.set(Position.KEY_FUEL_CONSUMPTION_KM_PER_LITRE, consumptionKilometersPerLitre);
    }

    private double getWithinBoundsFuelLevel(double fuelLevel, FuelSensor sensor) {
        if (fuelLevel < sensor.getLowerBound()) {
            return -1;
        }

        if (fuelLevel > sensor.getUpperBound()) {
            return sensor.getUpperBound();
        }

        return fuelLevel;
    }

}
