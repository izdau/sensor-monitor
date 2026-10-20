# Sensor Monitor

A small event-driven system that watches the temperature and humidity sensors of one or more warehouses.

- **warehouse-service** receives sensor measurements over UDP, validates them and publishes them to Kafka.
- **central-service** consumes the measurements from Kafka and raises alarms in its log
  when a configured threshold is exceeded.

Java 25, Spring Boot, reactive with Flux/Mono on Reactor Netty, Gradle, Apache Kafka.

## Compile and test

| Check | CMD | Git Bash |
|---|---|---|
| Compile only (fast, good after renames) | `gradlew.bat compileJava compileTestJava` | `./gradlew compileJava compileTestJava` |
| Full check: compile, tests, jars | `gradlew.bat clean build` | `./gradlew clean build` |

## High-level architecture

```text
 +----------------------+    +----------------------+
 |  Temperature sensors |    |   Humidity sensors   |
 +----------------------+    +----------------------+
            | UDP 3344                  | UDP 3355
            +-------------+-------------+
                          v
                +-------------------+
                | warehouse-service |  <- validates sensor data and produces
                +-------------------+     corresponding JSON for Kafka
                          | JSON
                          v
                +-------------------+
                |       Kafka       |
                +-------------------+
                          |
                          v
                +-------------------+
                |  central-service  |
                +-------------------+
                          |
                          v
                  ALARM in the log
```

Kafka will sit between the **warehouse-service** and **central-service**. This will help to keep the sensor readings if 
either of the services goes down or restarts as well as handle sudden spikes.

In production: 3 Kafka brokers (replication factor 3, `min.insync.replicas=2`, `acks=all`). Every
event is on at least 2 brokers, therefore system survives 1 broker fail. For 5 brokers (factor 5, min in-sync 3) system will survive 2 broker failures.
Locally one broker is enough.

## Message contract

The (JSON) contract between **warehouse-service** and **central-service**, the message that will be sent to Kafka topic 
`sensor-measurements` with the key `warehouseId:sensorId`:

```json
{
  "eventId": "0c0f6f0e-7a54-4f5e-9d0e-3c1f5b8a2d11",
  "warehouseId": "warehouse-1",
  "sensorId": "t1",
  "sensorType": "TEMPERATURE",
  "value": 30.5,
  "receivedAt": "2026-10-20T14:03:21.512Z"
}
```

`sensorType` is set by the UDP port (3344 or 3355), `receivedAt` is set by **warehouse-service** on sensor data arrival.

## Test warehouse-service locally

1. Start Kafka and Kafka UI by running following command from project root (Docker Desktop must be running): `docker compose up -d`
2. Start **warehouse-service** by running its main class `WarehouseServiceApplication` (in IntelliJ).
   It listens on UDP port 3344 (temperature) and 3355 (humidity).
3. Send sensor data with netcat:
   ```bash
   echo "sensor_id=t1; value=30" | nc -u -w1 localhost 3344
   echo "sensor_id=h1; value=40" | nc -u -w1 localhost 3355
   ```
   If `nc` is not installed and not available in Git Bash:
   ```bash
   echo "sensor_id=t1; value=30" > /dev/udp/localhost/3344
   echo "sensor_id=h1; value=40" > /dev/udp/localhost/3355
   ```
4. See the JSON messages in Kafka UI at http://localhost:8080 : **Topics** -> `sensor-measurements` -> **Messages**.
5. Stop Kafka and Kafka UI: `docker compose down`

Sensor data in any other format, e.g. `sensor_id=t1;value=30`, is discarded with a warning message in **warehouse-service** application log.

## Test central-service (alarms) locally

1. Start Kafka (`docker compose up -d`), then run `WarehouseServiceApplication` and `CentralServiceApplication` (in IntelliJ).
2. Send sensor data that exceeds threshold, e.g. `echo "sensor_id=t1; value=35.1" | nc -u -w1 localhost 3344` or `echo "sensor_id=t1; value=35.1" > /dev/udp/localhost/3344`, if `nc` is not installed 
3. **central-service** log shows a line for every value above the threshold (temperature > 35, humidity > 50, see `application.yml`):
   ```text
   WARN ... AlarmService : ALARM TEMPERATURE: warehouse=warehouse-1; sensor=t1; value=35.1; threshold=35.0; eventId=...; receivedAt=...
   WARN ... AlarmService : ALARM HUMIDITY   : warehouse=warehouse-1; sensor=h1; value=50.1; threshold=50.0; eventId=...; receivedAt=...
   ```
   The alarm lines are in red (`spring.output.ansi.enabled` in `application.yml`).
   Measurements sent when **central-service** is down are alarmed when it starts up again.
