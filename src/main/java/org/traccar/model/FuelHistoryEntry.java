package org.traccar.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude
public class FuelHistoryEntry {
    public FuelHistoryEntry(long positionId, double value) {
        this.positionId = positionId;
        this.value = value;
    }

    private long positionId;

    public long getPositionId() {
        return positionId;
    }

    public void setPositionId(long positionId) {
        this.positionId = positionId;
    }

    private double value;

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}
