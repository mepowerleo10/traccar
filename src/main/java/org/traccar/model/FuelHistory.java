package org.traccar.model;

import java.util.LinkedList;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude
public class FuelHistory {
    public FuelHistory(LinkedList<FuelHistoryEntry> history, int maximumSize) {
        this.history = history;
        this.maximumSize = maximumSize;
    }

    private int maximumSize;

    public int getMaximumSize() {
        return maximumSize;
    }

    public void setMaximumSize(int maximumSize) {
        this.maximumSize = maximumSize;
    }

    private LinkedList<FuelHistoryEntry> history;

    public LinkedList<FuelHistoryEntry> getHistory() {
        return history;
    }

    public void setHistory(LinkedList<FuelHistoryEntry> history) {
        this.history = history;
    }

    public void push(FuelHistoryEntry entry) {
        if (history.size() >= maximumSize) {
            history.remove(0);
        }

        history.add(entry);
    }

    public void push(long positionId, double value) {
        FuelHistoryEntry entry = new FuelHistoryEntry(positionId, value);
        push(entry);
    }

    public void pop() {
        history.remove(0);
    }

    public int size() {
        return history.size();
    }
}
