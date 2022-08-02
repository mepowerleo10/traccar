package org.traccar;

import org.traccar.database.ReadingTypeManager;
import org.traccar.model.ReadingType;

public class TestReadingManager extends ReadingTypeManager {

    private String measurementMetric;
    private String metricSymbol;
    private double conversionMultiplier;

    private ReadingType createReadingType() {
        ReadingType readingType = new ReadingType();
        readingType.setId(1);
        readingType.setMeasurementMetric(measurementMetric);
        readingType.setMetricSymbol(metricSymbol);
        readingType.setConversionMultiplier(conversionMultiplier);

        return readingType;
    }

    public TestReadingManager(String measurementMetric, String metricSymbol,
            double conversionMultiplier) {
        super(null);
        this.measurementMetric = measurementMetric;
        this.metricSymbol = metricSymbol;
        this.conversionMultiplier = conversionMultiplier;
    }

    @Override
    public ReadingType getById(long itemId) {
        return createReadingType();
    }

}
