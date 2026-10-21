package com.somecompany.service;

import com.somecompany.dto.SensorMeasurementEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AlarmService {

    @Value("${conf.threshold.temperature}")
    private double temperatureThreshold;
    @Value("${conf.threshold.humidity}")
    private double humidityThreshold;

    /** Raise an alarm (one log line) when the value is above the threshold of its sensor type. */
    public void check(SensorMeasurementEvent event) {
        double threshold = switch (event.sensorType()) {
            case TEMPERATURE -> temperatureThreshold;
            case HUMIDITY -> humidityThreshold;
        };
        if (event.value() > threshold) {
            String type = "%-11s".formatted(event.sensorType()); // to align "TEMPERATURE" and "HUMIDITY" in alarm logs
            log.warn(AnsiOutput.toString(AnsiColor.RED,
                    "ALARM " + type + ": warehouse={}; sensor={}; value={}; threshold={}; eventId={}; receivedAt={}"),
                    event.warehouseId(), event.sensorId(), event.value(), threshold, event.eventId(), event.receivedAt());
        }
    }
}
