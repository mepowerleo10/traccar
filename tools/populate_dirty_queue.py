#!../.venv/bin/python

from typing import List
from sqlalchemy.orm import declarative_base, sessionmaker
from sqlalchemy import (
    Column,
    Integer,
    String,
    JSON,
    DateTime,
    Boolean,
    Float,
    Sequence,
    create_engine,
    __version__,
)

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


engine = create_engine("mysql://root:welcome@localhost/traccar", echo=True)
Session = sessionmaker(bind=engine)

session = Session()

dirty_positions: List[DirtyPosition] = list()
for position in session.query(Position).all():
    contains_fuel_key = "fuel" in position.attributes
    if contains_fuel_key:
        dirty_position = DirtyPosition(
            device_id=position.device_id,
            position_id=position.id,
            device_time=position.device_time,
        )
        dirty_positions.append(dirty_position)

session.add_all(dirty_positions)
session.commit()
