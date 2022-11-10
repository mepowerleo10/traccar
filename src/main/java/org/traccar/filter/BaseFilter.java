package org.traccar.filter;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.traccar.model.Position;

public interface BaseFilter {
  default void sortPositions(List<Position> positions) {
    Collections.sort(positions, new Comparator<Position>() {
      @Override
      public int compare(Position positionA, Position positionB) {
        return positionA.getDeviceTime().compareTo(positionB.getDeviceTime());
      }

    });
  }

  void filterPositions(List<Position> positions);
}
