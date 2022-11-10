#!/usr/bin/python

import csv
import math
import urllib.request, urllib.parse, urllib.error
import http.client
import time
import random

id = "123456789012345"
server = "localhost:5055"
period = 1
step = 0.001
device_speed = 40
driver_id = "123456"

file_path = "Route_JU0175.csv"

waypoints = []

with open(file_path, newline="") as csvfile:
    route_reader = csv.reader(
        csvfile, delimiter=",", quotechar='"', skipinitialspace=True
    )
    next(route_reader, None)
    for row in route_reader:
        waypoints.append(tuple(float(i) for i in row))


""" points = []

for i in range(0, len(waypoints)):
    (lat1, lon1, fuel1) = waypoints[i]
    (lat2, lon2, fuel2) = waypoints[(i + 1) % len(waypoints)]
    length = math.sqrt((lat2 - lat1) ** 2 + (lon2 - lon1) ** 2)
    count = int(math.ceil(length / step))
    for j in range(0, count):
        lat = lat1 + (lat2 - lat1) * j / count
        lon = lon1 + (lon2 - lon1) * j / count
        points.append((lat, lon, fuel1)) """


def send(
    conn,
    lat,
    lon,
    course,
    speed,
    battery,
    alarm,
    ignition,
    accuracy,
    rpm,
    fuel: tuple[float],
    driverUniqueId,
):
    params = (
        ("id", id),
        ("timestamp", int(time.time())),
        ("lat", lat),
        ("lon", lon),
        ("bearing", course),
        ("speed", speed),
        ("batt", battery),
    )
    if alarm:
        params = params + (("alarm", "sos"),)
    if ignition:
        params = params + (("ignition", "true"),)
    else:
        params = params + (("ignition", "false"),)
    if accuracy:
        params = params + (("accuracy", accuracy),)
    if rpm:
        params = params + (("rpm", rpm),)
    for i in range(0, len(fuel)):
        params = params + ((f"fuel{(i + 1)}", fuel[i]),)
    if driverUniqueId:
        params = params + (("driverUniqueId", driverUniqueId),)
    conn.request("GET", "?" + urllib.parse.urlencode(params))
    conn.getresponse().read()


def course(lat1, lon1, lat2, lon2):
    lat1 = lat1 * math.pi / 180
    lon1 = lon1 * math.pi / 180
    lat2 = lat2 * math.pi / 180
    lon2 = lon2 * math.pi / 180
    y = math.sin(lon2 - lon1) * math.cos(lat2)
    x = math.cos(lat1) * math.sin(lat2) - math.sin(lat1) * math.cos(lat2) * math.cos(
        lon2 - lon1
    )
    return (math.atan2(y, x) % (2 * math.pi)) * 180 / math.pi


index = 0

conn = http.client.HTTPConnection(server)

while True:
    (lat1, lon1, fuel1, fuel2) = waypoints[index % len(waypoints)]
    (lat2, lon2, _, _) = waypoints[(index + 1) % len(waypoints)]
    speed = device_speed if (index % len(waypoints)) != 0 else 0
    alarm = (index % 10) == 0
    battery = random.randint(0, 100)
    ignition = (index % len(waypoints)) != 0
    accuracy = 100 if (index % 10) == 0 else 0
    rpm = random.randint(500, 4000)
    fuel = (fuel1, fuel2)
    driverUniqueId = driver_id if (index % len(waypoints)) == 0 else False
    send(
        conn,
        lat1,
        lon1,
        course(lat1, lon1, lat2, lon2),
        speed,
        battery,
        alarm,
        ignition,
        accuracy,
        rpm,
        fuel,
        driverUniqueId,
    )
    time.sleep(period)
    index += 1
