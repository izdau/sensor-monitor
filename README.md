# Sensor Monitor

A small event-driven system that watches the temperature and humidity sensors of one or more warehouses.

- **warehouse-service** receives sensor measurements over UDP and publishes them to Kafka.
- **central-service** consumes the measurements from Kafka and raises alarms in its log
  when a configured threshold is exceeded.

Java 25, Spring Boot, reactive with Reactor (Flux/Mono) on Reactor Netty, Gradle, Apache Kafka.

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

Kafka will sit between the **warehouse-service** and **central-service**. This will help to handle the spikes 
and keep the readings if either of the service gets down or restarts.

In production: 3 Kafka brokers (replication factor 3, `min.insync.replicas=2`, `acks=all`). Every
event is on at least 2 brokers and 1 broker can fail. 5 brokers (factor 5, min in-sync 3) survive 2.
Locally one broker is enough.

## Message contract

The (JSON) contract between **warehouse-service** and **central-service**, the message that will be sent to Kafka topic `sensor-measurements` with the key `warehouseId:sensorId`:

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

`sensorType` is set by the UDP port (3344 or 3355), `receivedAt` by **warehouse-service** on arrival.

## Test warehouse-service locally

1. Start Kafka and Kafka UI by running following command from project root (Docker Desktop must be running): `docker compose up -d`
2. Start **warehouse-service** by running its main class `WarehouseServiceApplication` (in IntelliJ).
   It listens on UDP port 3344 (temperature) and 3355 (humidity).
3. Send sensor data with netcat:
   ```bash
   echo "sensor_id=t1; value=30" | nc -u -w1 localhost 3344
   echo "sensor_id=h1; value=40" | nc -u -w1 localhost 3355
   ```
   Git Bash on Windows has no `nc`, but bash can send UDP by itself:
   ```bash
   echo "sensor_id=t1; value=30" > /dev/udp/localhost/3344
   echo "sensor_id=h1; value=40" > /dev/udp/localhost/3355
   ```
4. See the JSON messages in Kafka UI at http://localhost:8080 : **Topics** -> `sensor-measurements` -> **Messages**.
5. Stop Kafka and Kafka UI: `docker compose down`

Sensor data in any other format, e.g. `sensor_id=t1;value=30`, is dropped with a warning in the application log.
