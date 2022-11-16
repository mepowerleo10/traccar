#!../.venv/bin/python

import math
from typing import List

from progress.bar import ShadyBar
from sqlalchemy import (
    JSON,
    Boolean,
    Column,
    DateTime,
    Float,
    Integer,
    Sequence,
    String,
    __version__,
    create_engine,
)
from sqlalchemy.orm import declarative_base, sessionmaker

Base = declarative_base()


class Position(Base):
    __tablename__ = "tc_positions"

    id = Column(Integer, primary_key=True)
    protocol = Column(String)
    device_id = Column("deviceid", Integer)
    server_time = Column("servertime", DateTime)
    device_time = Column("devicetime", DateTime)
    fix_time = Column("fixtime", DateTime)
    valid = Column(Boolean)
    latitude = Column(Float)
    longitude = Column(Float)
    altitude = Column(Float)
    speed = Column(Float)
    course = Column(Float)
    address = Column(String)
    attributes = Column(JSON)
    accuracy = Column(Float)
    network = Column(JSON)

    def __repr__(self) -> str:
        return (
            "<Position(id=%d, device_id=%d, protocol='%s', device_time='%s', latitude='%s', longitude='%s'>"
            % (
                self.id,
                self.device_id,
                self.protocol,
                self.device_time,
                self.latitude,
                self.longitude,
            )
        )


class DirtyPosition(Base):
    __tablename__ = "tc_dirty_positions"

    id = Column(Integer, primary_key=True)
    device_id = Column("deviceid", Integer)
    position_id = Column("positionid", Integer)
    device_time = Column("devicetime", DateTime)

    def __repr__(self) -> str:
        return (
            "<DirtyPosition(id=%d, device_id=%d, position_id=%d, device_time=%s)>"
            % (self.id, self.device_id, self.position_id, self.device_time)
        )


engine = create_engine("mysql://root:welcome@localhost/traccar", echo=False)
Session = sessionmaker(bind=engine)

session = Session()


def paginate_query(query, position_id, page_size=None):
    query.order_by(Position.id)

    if position_id > 0:
        query = query.filter(Position.id > position_id)

    if page_size:
        query = query.limit(page_size)

    return query


positions_scanned = 0
positions_inserted = 0


def insert_dirty_position(session, progress_suffix, dirty_positions):
    with ShadyBar(
        f"Inserting to {DirtyPosition.__tablename__}",
        max=len(dirty_positions),
        suffix=progress_suffix,
    ) as bar:
        for dirty_position in dirty_positions:
            session.add(dirty_position)
            bar.next()

        session.commit()


query = session.query(Position)

offset = 1000
length = 0
last_id = 0

progress_suffix = "Item %(index)d of %(max)d items"

with ShadyBar(
    f"Querying from {Position.__tablename__} with offset={offset}",
    suffix="Page %(index)d - Elapsed: %(elapsed_td)s",
) as bar:
    while True:
        positions: List[Position] = list()
        positions = paginate_query(query, last_id, offset).all()

        dirty_positions: List[DirtyPosition] = list()
        length = len(positions)
        positions_scanned += length

        if length > 0:
            for position in positions:
                contains_fuel_key = "fuel" in position.attributes
                if contains_fuel_key:
                    dirty_position = DirtyPosition(
                        device_id=position.device_id,
                        position_id=position.id,
                        device_time=position.device_time,
                    )
                    dirty_positions.append(dirty_position)
                    positions_inserted += 1
                last_id = position.id
            if len(dirty_positions) > 0:
                insert_dirty_position(session, progress_suffix, dirty_positions)
        else:
            break

        bar.next()

session.commit()

print(
    f"\nFinished Succesfully\n-------------------------\nScanned: {positions_scanned}\nInserted: {positions_inserted}"
)
