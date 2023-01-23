package org.traccar.filter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.exception.OutOfRangeException;
import org.traccar.model.Position;

public class ExponentialMovingAverageFilter implements BaseFilter {
    private float smoothingFactor = 0.5f;
    private Double oldValue;

    public ExponentialMovingAverageFilter(float smoothingFactor) {
        if (smoothingFactor <= 0 || smoothingFactor >= 1) {
            throw new OutOfRangeException(smoothingFactor, 0, 1);
        }

        this.smoothingFactor = smoothingFactor;
    }

    public void filter(List<Double> data) {
        for (int i = 0; i < data.size(); ++i) {
            data.set(i, average(data.get(i)));
        }
    }

    public double average(double value) {
        if (oldValue == null) {
            oldValue = value;
            return value;
        }
        double newValue = oldValue + smoothingFactor * (value - oldValue);
        oldValue = newValue;
        return newValue;
    }

    @Override
    public void filterPositions(List<Position> positions) {
        List<Double> fuelLevels = new ArrayList<>();
        List<Position> filteredPositions = new ArrayList<>();

        for (Position position : positions) {
            if (position.hasFuelData()) {
                double level = position.getFuelLevel();
                fuelLevels.add(BigDecimal.valueOf(level).setScale(2, RoundingMode.DOWN).doubleValue());
                filteredPositions.add(position);
            }
        }

        filter(fuelLevels);

        for (int i = 0; i < fuelLevels.size(); i++) {
            Position position = positions.get(i);
            position.set(Position.KEY_FUEL_NORMALIZED, fuelLevels.get(i));
        }
    }
}
