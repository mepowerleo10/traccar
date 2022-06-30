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
                Position lastPosition = identityManager.getLastPosition(device.getId());
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
        if (sensor != null) {
            ReadingType readingType = readingTypeManager.getById(sensor.getReadingTypeId());

            if (sensor.getCalibrated()) {
                double fuelLevel = device.getFuelSlope() * position.getDouble(sensor.getFuelLevelPort())
                        + device.getFuelConstant();

                double boundedFuelLevel = getWithinBoundsFuelLevel(fuelLevel, sensor);
                position.set(Position.KEY_FUEL_LEVEL, boundedFuelLevel);

                double consumptionLitresPerHour = calculateFuelConsumptonRaterPerHour(lastPosition, position);
                position.set(Position.KEY_FUEL_CONSUMPTION, consumptionLitresPerHour);
            } else {

                double currentFuelLevel = position.getDouble(sensor.getFuelLevelPort());
                position.set(Position.KEY_FUEL_LEVEL, currentFuelLevel
                        * readingType.getConversionMultiplier());
                position.set(Position.KEY_FUEL_CONSUMPTION,
                        position.getDouble(sensor.getFuelRatePort())
                                * readingType.getConversionMultiplier());
                position.set(Position.KEY_FUEL_USED,
                        position.getDouble(sensor.getFuelConsumedPort()));
            }

            double consumptionRatePerKm = calculateFuelConsumptionRateKmPerLitre(lastPosition, position);
            position.set(Position.KEY_FUEL_CONSUMPTION_KM_PER_LITRE, consumptionRatePerKm);
        } else {
            position.set(Position.KEY_FUEL_LEVEL, 0);
            position.set(Position.KEY_FUEL_CONSUMPTION, 0);
            position.set(Position.KEY_FUEL_USED, 0);
        }

        if (position.getDouble(Position.KEY_FUEL_LEVEL) == -1) {
            position.set(Position.KEY_FUEL_LEVEL, lastPosition.getDouble(Position.KEY_FUEL_LEVEL));
        }
    }

    private double calculateFuelConsumptonRaterPerHour(Position lastPosition, Position position) {
        double consumptionLitresPerHour = 0; // litres/hour
        double currentFuelLevel = position.getDouble(Position.KEY_FUEL_LEVEL);
        double lastFuelLevel = lastPosition.getDouble(Position.KEY_FUEL_LEVEL);
        double fuelDifference = currentFuelLevel - lastFuelLevel;

        double hoursBetween = (position.getDeviceTime().getTime() - lastPosition.getDeviceTime().getTime())
                / (1000 * 60 * 60);

        if (hoursBetween != 0) {
            consumptionLitresPerHour = (fuelDifference) /* in litres */
                    / hoursBetween /* in hours */;
        }

        return consumptionLitresPerHour;
    }

    private double calculateFuelConsumptionRateKmPerLitre(Position lastPosition, Position position) {
        double consumptionKilometersPerLitre = 0; // kilometers/litre
        double currentFuelLevel = position.getDouble(Position.KEY_FUEL_LEVEL);
        double lastFuelLevel = lastPosition.getDouble(Position.KEY_FUEL_LEVEL);
        double fuelDifference = currentFuelLevel - lastFuelLevel; /* in litres */
        double odometerDifference = (position.getDouble(Position.KEY_ODOMETER)
                - lastPosition.getDouble(Position.KEY_ODOMETER)) * 0.001; /* in kilometers */

        if (fuelDifference != 0) {
            consumptionKilometersPerLitre = odometerDifference / fuelDifference; /* km/l */
        } else {
            consumptionKilometersPerLitre = lastPosition.getDouble(Position.KEY_FUEL_CONSUMPTION_KM_PER_LITRE);
        }

        return consumptionKilometersPerLitre;
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
