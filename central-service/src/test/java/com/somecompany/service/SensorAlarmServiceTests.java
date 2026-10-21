package com.somecompany.service;

import com.somecompany.dto.SensorMeasurementEvent;
import com.somecompany.enums.SensorType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class SensorAlarmServiceTests {

    private static final UUID EVENT_ID = UUID.fromString("0c0f6f0e-7a54-4f5e-9d0e-3c1f5b8a2d11");
    private final SensorAlarmService sensorAlarmService = new SensorAlarmService();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(sensorAlarmService, "temperatureThreshold", 35.0);
        ReflectionTestUtils.setField(sensorAlarmService, "humidityThreshold", 50.0);
        AnsiOutput.setEnabled(AnsiOutput.Enabled.NEVER);
    }

    @AfterEach
    void tearDown() {
        AnsiOutput.setEnabled(AnsiOutput.Enabled.DETECT);
    }

    @ParameterizedTest
    @CsvSource({"TEMPERATURE, 35.1", "TEMPERATURE, 100", "HUMIDITY, 50.1", "HUMIDITY, 100"})
    void valuesAboveThresholdShouldRaiseAlarms(SensorType type, double value, CapturedOutput output) {
        sensorAlarmService.check(event(type, value));

        assertThat(output).contains("ALARM " + type);
    }

    @ParameterizedTest
    @CsvSource({"TEMPERATURE, 35", "TEMPERATURE, -10", "HUMIDITY, 50", "HUMIDITY, 40"}) // humidity 40 is above the temperature threshold
    void valuesAtOrBelowThresholdShouldNotRaiseAlarms(SensorType type, double value, CapturedOutput output) {
        sensorAlarmService.check(event(type, value));

        assertThat(output).doesNotContain("ALARM");
    }

    @Test
    void temperatureAlarmShouldBeAOneLineWithAllDetails(CapturedOutput output) {
        sensorAlarmService.check(event(SensorType.TEMPERATURE, 35.1));

        assertThat(output)
                .contains("ALARM TEMPERATURE")
                .contains("warehouse=warehouse-1; sensor=t1; value=35.1; threshold=35.0; eventId=" + EVENT_ID + "; receivedAt=2026-10-20T23:59:59.999Z");
    }

    @Test
    void humidityAlarmShouldBeAOneLineWithAllDetails(CapturedOutput output) {
        sensorAlarmService.check(event(SensorType.HUMIDITY, 50.1));

        assertThat(output)
                .contains("ALARM HUMIDITY")
                .contains("warehouse=warehouse-1; sensor=t1; value=50.1; threshold=50.0; eventId=" + EVENT_ID + "; receivedAt=2026-10-20T23:59:59.999Z");
    }

    private static SensorMeasurementEvent event(SensorType type, double value) {
        return new SensorMeasurementEvent(EVENT_ID, "warehouse-1", "t1", type, value, Instant.parse("2026-10-20T23:59:59.999Z"));
    }
}
