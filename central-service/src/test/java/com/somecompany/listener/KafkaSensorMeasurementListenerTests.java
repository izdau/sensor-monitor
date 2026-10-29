package com.somecompany.listener;

import com.somecompany.dto.SensorMeasurementEvent;
import com.somecompany.enums.SensorType;
import com.somecompany.service.SensorAlarmService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class KafkaSensorMeasurementListenerTests {

    @Mock
    private SensorAlarmService sensorAlarmService;
    private KafkaSensorMeasurementListener listener;

    @BeforeEach
    void setUp() {
        listener = new KafkaSensorMeasurementListener(JsonMapper.builder().build(), sensorAlarmService);
    }

    @Test
    void validJsonMessageShouldBeCheckedByAlarmService() {
        listener.onMessage("""
                {
                  "eventId": "0c0f6f0e-7a54-4f5e-9d0e-3c1f5b8a2d11",
                  "warehouseId": "warehouse-1",
                  "sensorId": "t1",
                  "sensorType": "TEMPERATURE",
                  "value": 30.5,
                  "receivedAt": "2026-10-20T14:03:21.512Z"
                }
                """);

        verify(sensorAlarmService).check(new SensorMeasurementEvent(UUID.fromString("0c0f6f0e-7a54-4f5e-9d0e-3c1f5b8a2d11"),
                "warehouse-1", "t1", SensorType.TEMPERATURE, 30.5, Instant.parse("2026-10-20T14:03:21.512Z")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"garbage", "", "{\"sensorType\": \"PRESSURE\"}"})
    void invalidJsonMessagesShouldNotBeCheckedByAlarmService(String message) {
        listener.onMessage(message);

        verifyNoInteractions(sensorAlarmService);
    }
}
