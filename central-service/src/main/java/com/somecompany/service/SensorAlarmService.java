package com.somecompany.service;

import com.somecompany.config.GeofenceProperties;
import com.somecompany.dto.Point;
import com.somecompany.dto.SensorMeasurementEvent;
import com.somecompany.util.GeofenceUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SensorAlarmService {

    @Value("${conf.threshold.temperature}")
    private double temperatureThreshold;
    @Value("${conf.threshold.humidity}")
    private double humidityThreshold;
    private final GeofenceProperties geofenceProperties;
    // value=1.0 or lat=123, lon=123

    /** Raise an alarm (one log line) when the value is above the threshold of its sensor type. */
    public void check(SensorMeasurementEvent event) {
        String threshold = switch (event.sensorType()) {
            case TEMPERATURE -> String.valueOf(temperatureThreshold);
            case HUMIDITY -> String.valueOf(humidityThreshold);
            case LOCATION -> geofenceProperties.getPoints().toString();
        };
        boolean thresholdOver = validateSensorValueAndCheckThreshold(event);
        if (thresholdOver) {
            String type = "%-11s".formatted(event.sensorType()); // to align "TEMPERATURE" and "HUMIDITY" in alarm logs
            log.warn(AnsiOutput.toString(AnsiColor.RED,
                    "ALARM " + type + ": warehouse={}; sensor={}; value={}; threshold={}; eventId={}; receivedAt={}"),
                    event.warehouseId(), event.sensorId(), event.value(), threshold, event.eventId(), event.receivedAt());
        }
    }

    private boolean validateSensorValueAndCheckThreshold(SensorMeasurementEvent event) {
        String sensorValue = event.value();
        switch (event.sensorType()) {
            case HUMIDITY:
                if (!sensorValue.startsWith("value=")) {
                    throw new IllegalArgumentException("sensor value is invalid: " + sensorValue);
                }
                sensorValue = sensorValue.replace("value=", "");

                try {
                    double humidity = Double.parseDouble(sensorValue);
                    return humidity > humidityThreshold;
                } catch (Exception e) {
                    throw new IllegalArgumentException("the humidity value is not valid: " + sensorValue);
                }
            case TEMPERATURE:
                if (!sensorValue.startsWith("value=")) {
                    throw new IllegalArgumentException("sensor value is invalid: " + sensorValue);
                }
                sensorValue = sensorValue.replace("value=", "");

                try {
                    double temperature = Double.parseDouble(sensorValue);
                    return temperature > temperatureThreshold;
                } catch (Exception e) {
                    throw new IllegalArgumentException("the humidity value is not valid: " + sensorValue);
                }
            case LOCATION: //lat=123, lon=123
                if (!sensorValue.contains(", "))
                    throw new IllegalArgumentException("Invalid sensor value for location: " + sensorValue);

                String[] valueParts = sensorValue.split(",");
                if (!valueParts[0].trim().startsWith("lat="))
                    throw new IllegalArgumentException("Invalid sensor value for location: " + sensorValue);
                double latitude;
                try {
                    latitude = Double.parseDouble(valueParts[0].trim().replace("lat=", ""));
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid sensor value for location: " + sensorValue);
                }
                if (!valueParts[1].trim().startsWith("long="))
                    throw new IllegalArgumentException("Invalid sensor value for location: " + sensorValue);

                double longitude;
                try {
                    longitude = Double.parseDouble(valueParts[1].trim().replace("long=", ""));
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid sensor value for location: " + sensorValue);
                }

                Point currentLoc = new Point(latitude, longitude);
                return GeofenceUtil.isInsideGeofence(currentLoc, geofenceProperties.getPoints());
        }

        return false;
    }
}
