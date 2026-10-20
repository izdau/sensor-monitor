package com.somecompany.dto;

import com.somecompany.enums.SensorType;

import java.time.Instant;
import java.util.UUID;

/** The JSON message that is produced from sensor data and sent to Kafka.
 * See "Message contract" in the README for more info. */
public record SensorMeasurementEvent(
        UUID eventId,
        String warehouseId,
        String sensorId,
        SensorType sensorType,
        double value,
        Instant receivedAt) {
}
