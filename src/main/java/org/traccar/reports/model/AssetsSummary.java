package org.traccar.reports.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.traccar.Context;
import org.traccar.model.Device;
import org.traccar.model.Group;
import org.traccar.model.Position;
import org.traccar.model.Sensor;
import org.traccar.storage.StorageException;

public final class AssetsSummary {
  private AssetsSummary() {
  }

  public static Collection<AssetReport> getObjects(long userId) throws StorageException {
    ArrayList<AssetReport> result = new ArrayList<>();
    for (long deviceId : Context.getDeviceManager().getAllUserItems(userId)) {
      Device device = Context.getDeviceManager().getById(deviceId);
      if (device != null) {
        result.add(calculateSummaryResults(device));
      }
    }
    return result;
  }

  private static AssetReport calculateSummaryResults(
      Device device) throws StorageException {

    Group group = Context.getGroupsManager().getById(device.getGroupId());
    Position position = Context.getDeviceManager().getLastPosition(device.getId());
    List<Sensor> sensors = Context.getSensorManager().getDeviceSensors(device.getId());
    AssetReport report = new AssetReport();

    report.setDeviceId(device.getId());
    report.setDeviceName(device.getName());
    report.setModel(device.getModel());
    report.setLastUpdate(device.getLastUpdate());
    report.setLastPositionUpdate(device.getLastPositionUpdate());

    if (group != null) {
      report.setZone(group.getName());
    }

    HashMap<Integer, Integer> sensorMap = new HashMap<>();
    for (Sensor sensor : sensors) {
      int groupCount = sensorMap.getOrDefault(sensor.getGroupNo(), -1);
      sensorMap.put(sensor.getGroupNo(), groupCount);
      report.setNoOfSensors(report.getNoOfSensors() + 1);
    }

    if (position != null) {
      report.setTotalDistance(position.getDouble(Position.KEY_TOTAL_DISTANCE));
      report.setTotalFuelSpent(position.getDouble(Position.KEY_TOTAL_FUEL_USED));
      report.setTotalFuelRefilled(position.getDouble(Position.KEY_TOTAL_FUEL_REFILLED));
    }

    report.setNoOfTanks(sensorMap.size());

    return report;
  }

}
