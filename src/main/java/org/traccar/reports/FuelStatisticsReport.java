package org.traccar.reports;

import java.math.BigDecimal;
import java.util.List;

import org.traccar.filter.MedianFilter;
import org.traccar.model.Position;

public class FuelStatisticsReport {
  private static final int REFILL_THRESHOLD = 30; // litres
  private static final int REFILL_TIMER_MAX = 30; // minutes

  private List<Position> positions;

  private double initialFuelLevel;

  public double getInitialFuelLevel() {
    return initialFuelLevel;
  }

  public void setInitialFuelLevel(double initialFuelLevel) {
    this.initialFuelLevel = initialFuelLevel;
  }

  private double finalFuelLevel;

  public double getFinalFuelLevel() {
    return finalFuelLevel;
  }

  public void setFinalFuelLevel(double finalFuelLevel) {
    this.finalFuelLevel = finalFuelLevel;
  }

  private BigDecimal fuelUsed;

  public double getFuelUsed() {
    return fuelUsed.doubleValue();
  }

  public void setFuelUsed(BigDecimal fuelUsed) {
    this.fuelUsed = fuelUsed;
  }

  private BigDecimal fuelRefilled;

  public double getFuelRefilled() {
    return fuelRefilled.doubleValue();
  }

  public void setFuelRefilled(BigDecimal fuelRefilled) {
    this.fuelRefilled = fuelRefilled;
  }

  private long refillTimer;

  private double refilledWithinTimer;

  private int numberOfRefills;

  public int getNumberOfRefills() {
    return numberOfRefills;
  }

  public void setNumberOfRefills(int numberOfRefills) {
    this.numberOfRefills = numberOfRefills;
  }

  private double maxSpeed;

  public double getMaxSpeed() {
    return maxSpeed;
  }

  public void setMaxSpeed(double maxSpeed) {
    this.maxSpeed = maxSpeed;
  }

  public FuelStatisticsReport(List<Position> positions) {
    this.positions = positions;
    this.fuelUsed = new BigDecimal(0.0);
    this.fuelRefilled = new BigDecimal(0.0);
  }

  public void compute() {
    if (positions != null && !positions.isEmpty()) {

      MedianFilter filter = new MedianFilter(25);
      filter.filterPositions(positions);

      Position initialPosition = null;
      Position previousPosition = null;

      for (Position position : positions) {
        if (!position.getValid())
          continue;

        if (initialPosition == null || initialFuelLevel <= 0) {
          initialPosition = position;
          initialFuelLevel = getNormalizedFuelValue(position);
        }

        if (previousPosition != null && !previousPosition.getDeviceTime().equals(position.getDeviceTime())) {
          computeUsageOrRefill(previousPosition, position);

        }

        if (position.getSpeed() > maxSpeed) {
          maxSpeed = position.getSpeed();
        }

        previousPosition = position;
      }

      if (previousPosition != null) {
        double finalLevel = getNormalizedFuelValue(previousPosition);
        finalFuelLevel = finalLevel > 0 ? finalLevel : finalFuelLevel;
      }

      fuelUsed = BigDecimal.valueOf(initialFuelLevel + fuelRefilled.doubleValue() - finalFuelLevel);

      if (fuelUsed.doubleValue() < 0) {
        fuelUsed = BigDecimal.valueOf(0);
      }

    }
  }

  private void computeUsageOrRefill(Position previousPosition, Position position) {
    double previousLevel = getNormalizedFuelValue(previousPosition);
    double currentLevel = getNormalizedFuelValue(position);
    finalFuelLevel = currentLevel > 0 ? currentLevel : finalFuelLevel;

    double millisecondsBetween = (position.getFixTime().getTime() - previousPosition.getFixTime().getTime());
    refillTimer += millisecondsBetween * 1.6667e-5;

    if (previousLevel > 0 && currentLevel > 0) {
      double difference = Math.round(currentLevel - previousLevel);

      if (difference <= 0) {
        fuelUsed = BigDecimal
            .valueOf(Math.abs(difference) + fuelUsed.doubleValue());
      } else {
        checkAndUpdateRefill(difference);
      }
    }
  }

  private void checkAndUpdateRefill(double difference) {
    refilledWithinTimer += difference;

    if (refillTimer % REFILL_TIMER_MAX >= 0) {
      refillTimer = refillTimer % REFILL_TIMER_MAX;

      if (refilledWithinTimer >= REFILL_THRESHOLD) {
        fuelRefilled = BigDecimal.valueOf(refilledWithinTimer + fuelRefilled.doubleValue());
        refilledWithinTimer = 0;
        numberOfRefills++;
      }
    }
  }

  private static double getNormalizedFuelValue(Position position) {
    if (position.getAttributes().containsKey(Position.KEY_FUEL_NORMALIZED)) {
      return position.getDouble(Position.KEY_FUEL_NORMALIZED);
    } else if (position.getAttributes().containsKey(Position.KEY_FUEL_LEVEL)) {
      return position.getDouble(Position.KEY_FUEL_LEVEL);
    } else {
      return -1;
    }
  }

}
