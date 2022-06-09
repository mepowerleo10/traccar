package org.traccar.model;

import org.traccar.storage.StorageName;

@StorageName("tc_reading_types")
public class ReadingType extends ExtendedModel {

  public ReadingType() {
  }

  private String measurementMetric;

  public String getMeasurementMetric() {
    return measurementMetric;
  }

  public void setMeasurementMetric(String measurementMetric) {
    this.measurementMetric = measurementMetric;
  }

  private String metricSymbol;

  public String getMetricSymbol() {
    return metricSymbol;
  }

  public void setMetricSymbol(String metricSymbol) {
    this.metricSymbol = metricSymbol;
  }

  private double conversionMultiplier;

  public double getConversionMultiplier() {
    return conversionMultiplier;
  }

  public void setConversionMultiplier(double conversionMultiplier) {
    this.conversionMultiplier = conversionMultiplier;
  }

}
