package org.traccar.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.BaseDataHandler;
import org.traccar.database.DirtyPositionManager;
import org.traccar.model.DirtyPosition;
import org.traccar.model.Position;

import io.netty.channel.ChannelHandler;

@ChannelHandler.Sharable
public class QueueDataHandler extends BaseDataHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(QueueDataHandler.class);

  private final DirtyPositionManager dirtyPositionManager;

  public QueueDataHandler(DirtyPositionManager dirtyPositionManager) {
    this.dirtyPositionManager = dirtyPositionManager;
  }

  @Override
  protected Position handlePosition(Position position) {
    try {
      long positionId = position.getId();

      if (positionId > 0 && position.getAttributes().containsKey(Position.KEY_FUEL_LEVEL)) {
        DirtyPosition dirtyPosition = new DirtyPosition();
        dirtyPosition.setDeviceId(position.getDeviceId());
        dirtyPosition.setPositionId(positionId);
        dirtyPosition.setDeviceTime(position.getDeviceTime());

        dirtyPositionManager.addItem(dirtyPosition);
      }

    } catch (Exception e) {
      LOGGER.error("Couldn't add position to queue", e);
    }

    return position;
  }

}
