package org.traccar.handler;

import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.BaseDataHandler;
import org.traccar.database.DataManager;
import org.traccar.database.IdentityManager;
import org.traccar.filter.ExponentialMovingAverageFilter;
import org.traccar.model.Device;
import org.traccar.model.FuelHistory;
import org.traccar.model.FuelHistoryEntry;
import org.traccar.model.Position;
import org.traccar.storage.StorageException;

import io.netty.channel.ChannelHandler;

@ChannelHandler.Sharable
public class FuelDataSmoothingHandler extends BaseDataHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(FuelDataSmoothingHandler.class);
    private static final String ATTRIBUTE_FUEL_SMOOTHING_WINDOW_SIZE = "fuelSmoothingWindowSize";

    private final IdentityManager identityManager;
    private final ExponentialMovingAverageFilter emaFilter;
    private final DataManager dataManager;

    public FuelDataSmoothingHandler(IdentityManager identityManager, DataManager dataManager) {
        this.identityManager = identityManager;
        this.emaFilter = new ExponentialMovingAverageFilter(0.3f);
        this.dataManager = dataManager;
    }

    @Override
    protected Position handlePosition(Position position) {
        if (position.getAttributes().containsKey(Position.KEY_FUEL_LEVEL)) {
            try {
                Device device = identityManager.getById(position.getDeviceId());
                Position last = identityManager.getLastPosition(device.getId());

                if (last != null) {
                    filterFuelData(device, last, position);
                }

            } catch (Exception e) {
                LOGGER.error("Error Filtering Position Data", e);
            }
        }

        return position;
    }

    private void filterFuelData(Device device, Position last, Position position) {
        int windowSize = identityManager.lookupAttributeInteger(device.getId(),
                ATTRIBUTE_FUEL_SMOOTHING_WINDOW_SIZE, 5, true, false);
        FuelHistory fuelHistory;
        fuelHistory = (FuelHistory) last.getAttributes().getOrDefault(Position.KEY_FUEL_HISTORY,
                new FuelHistory(new LinkedList<FuelHistoryEntry>(), windowSize));

        FuelHistoryEntry entry = new FuelHistoryEntry(position.getId(), position.getDouble(Position.KEY_FUEL_LEVEL));
        fuelHistory.push(entry);

        double[] values = new double[fuelHistory.size()];

        int i = -1;
        for (FuelHistoryEntry item : fuelHistory.getHistory()) {
            values[++i] = item.getValue();
        }

        // double[] results = emaFilter.filter(values);
        // updateFuelValues(fuelHistory, results);

        position.getAttributes().put(Position.KEY_FUEL_HISTORY, fuelHistory);

    }

    private void updateFuelValues(FuelHistory fuelHistory, double[] results) {
        int midPosition = Math.floorDiv(fuelHistory.size(), 2);
        long positionId = fuelHistory.getHistory().get(midPosition).getPositionId();
        double normalizedFuelValue = results[midPosition];
        try {
            Position position = dataManager.getObject(Position.class, positionId);
            double rawFuelValue = position.getDouble(Position.KEY_FUEL_LEVEL);
            position.set(Position.KEY_FUEL_NORMALIZED, normalizedFuelValue);
            position.set(Position.KEY_FUEL_LEVEL, normalizedFuelValue);
            position.set(Position.KEY_FUEL_RAW, rawFuelValue);

            dataManager.updateObject(position);

        } catch (StorageException e) {
            LOGGER.error("Failed to get position: " + positionId, e);
        }
    }

}
