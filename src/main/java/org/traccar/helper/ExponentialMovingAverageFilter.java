package org.traccar.helper;

import org.apache.commons.math3.exception.OutOfRangeException;

public class ExponentialMovingAverageFilter {
    private float smoothingFactor = 0.5f;

    public ExponentialMovingAverageFilter(float smoothingFactor) {
        if (smoothingFactor <= 0 || smoothingFactor >= 1) {
            throw new OutOfRangeException(smoothingFactor, 0, 1);
        }

        this.smoothingFactor = smoothingFactor;
    }

    public double[] filter(double[] values) {
        int length = values.length;
        double[] result = new double[length];
        result[0] = values[0];
        double k = 1 - smoothingFactor;
        for (int i = 1; i < length; i++) {
            result[i] = values[i] * smoothingFactor + result[i - 1] * k;
        }

        return result;
    }
}
