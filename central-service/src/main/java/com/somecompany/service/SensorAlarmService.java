package com.somecompany.service;

import com.somecompany.config.GeofenceProperties;
import com.somecompany.dto.Point;
import com.somecompany.dto.SensorMeasurementEvent;
import com.somecompany.enums.SensorType;
import com.somecompany.util.GeofenceUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class SensorAlarmService {

    @Value("${conf.threshold.temperature}")
    private double temperatureThreshold;
    @Value("${conf.threshold.humidity}")
    private double humidityThreshold;
    private final GeofenceProperties geofenceProperties;

    // value=35.1
    private static final Pattern HUMIDITY_AND_TEMPERATURE_VALUE_REGEX = Pattern.compile("value=(-?\\d{1,4}(?:\\.\\d+)?)");
    // lat=123, lonh=123
    private static final Pattern LOCATION_VALUE_REGEX = Pattern.compile("lat=(-?\\d{1,3}(?:\\.\\d+)?), long=(-?\\d{1,3}(?:\\.\\d+)?)");




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
        String sensorStrValue = event.value();
        Matcher matcher;
        switch (event.sensorType()) {
            case HUMIDITY, TEMPERATURE:
                matcher = HUMIDITY_AND_TEMPERATURE_VALUE_REGEX.matcher(sensorStrValue);
                if (!matcher.matches())
                    throw new IllegalArgumentException("sensor value is invalid: " + sensorStrValue + "; event: " + event);

                double sensorValue = Double.parseDouble(matcher.group(1));
                return event.sensorType() == SensorType.HUMIDITY ? sensorValue > humidityThreshold : sensorValue > temperatureThreshold;
            case LOCATION: //lat=123, lon=123
                matcher = LOCATION_VALUE_REGEX.matcher(sensorStrValue);
                if (!matcher.matches())
                    throw new IllegalArgumentException("sensor value is invalid: " + sensorStrValue + "; event: " + event);

                Point currentLoc = new Point(Double.parseDouble(matcher.group(1)), Double.parseDouble(matcher.group(2)));
                return !GeofenceUtil.isInsideGeofence(currentLoc, geofenceProperties.getPoints());
        }

        return false;
    }
}
