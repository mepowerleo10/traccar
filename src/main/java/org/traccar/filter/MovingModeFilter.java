package org.traccar.filter;

import java.util.List;

import org.apache.commons.math3.stat.StatUtils;
import org.traccar.model.Position;

public class MovingModeFilter implements BaseFilter {

  private int windowSize;

  public int getWindowSize() {
    return windowSize;
  }

  public MovingModeFilter(int windowSize) {
    this.windowSize = windowSize;
  }

  @Override
  public void filterPositions(List<Position> positions) {
    sortPositions(positions);
    int minLeftOrRight = Math.floorDiv(windowSize, 2);
    for (int i = minLeftOrRight; i < positions.size() - minLeftOrRight; i++) {
      double[] rawFuelValues = new double[windowSize];

      for (int j = -minLeftOrRight; j <= minLeftOrRight; j++) {
        rawFuelValues[j + minLeftOrRight] = positions.get(i + j).getDouble(Position.KEY_FUEL_LEVEL);
      }

      double[] results = StatUtils.mode(rawFuelValues);
      int length = results.length;
      double medianFuelLevel = results[length - 1];
      Position position = positions.get(i);
      position.set(Position.KEY_FUEL_NORMALIZED, medianFuelLevel);

    }
  }

  public double computeMedian(double[] values) {
    int midPoint = Math.floorDiv(windowSize, 2);

    if (windowSize % 2 == 0) {
      return (values[midPoint] + values[midPoint + 1]) / 2;
    } else {
      return values[midPoint + 1];
    }
  }
}
