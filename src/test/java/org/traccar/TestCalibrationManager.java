package org.traccar;

import java.util.HashMap;
import java.util.Map;

import org.traccar.database.FuelCalibrationManager;
import org.traccar.model.FuelCalibration;

public class TestCalibrationManager extends FuelCalibrationManager {
    private FuelCalibration fuelCalibration = new FuelCalibration();

    public FuelCalibration getFuelCalibration() {
        return fuelCalibration;
    }

    private Double[][] calibrationEntries = {
            { 0.0, 0.0 },
            { 181.0, 5.0 },
            { 367.0, 10.0 },
            { 632.0, 15.0 },
            { 804.0, 20.0 },
            { 1144.0, 30.0 },
            { 1322.0, 35.0 },
            { 1479.0, 40.0 },
            { 1630.0, 45.0 },
            { 1783.0, 50.0 },
            { 1947.0, 55.0 },
            { 2110.0, 60.0 },
            { 2302.0, 65.0 },
            { 2508.0, 70.0 },
            { 2720.0, 75.0 },
            { 3011.0, 80.0 },
            { 3404.0, 85.0 },
            { 3800.0, 90.0 }
    };

    /*
     * private Double[][] calibrationEntries = {
     * { 0.0, 0.0 },
     * { 142d, 5d, },
     * { 834d, 15d, },
     * { 1488d, 25d },
     * { 2069d, 35d },
     * { 2441d, 45d },
     * { 2789d, 55d },
     * { 3143d, 65d },
     * { 3522d, 75d },
     * { 3891d, 85d },
     * { 4064d, 90d },
     * { 4095d, 92d }
     * };
     */

    public double getMaximumCalibration() {
        int size = calibrationEntries.length;
        return calibrationEntries[size - 1][0];
    }

    public double getMinimumCalibration() {
        return calibrationEntries[0][1];
    }

    private void setUpCalibrationEntries() {
        for (Double[] entryArray : calibrationEntries) {
            Map<String, Double> entry = new HashMap<>();
            entry.put(FuelCalibration.VOLTAGE, entryArray[0]);
            entry.put(FuelCalibration.FUEL_LEVEL, entryArray[1]);
            fuelCalibration.add(entry);
        }

        updateSlopeAndConstant(fuelCalibration);
    }

    public TestCalibrationManager() {
        super(null);
        setUpCalibrationEntries();
        fuelCalibration.setId(1);
        fuelCalibration.setDeviceId((long) 1);
    }

    @Override
    public FuelCalibration getById(long itemId) {
        return fuelCalibration;
    }

}
