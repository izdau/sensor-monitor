package com.somecompany.producer;

import com.somecompany.dto.SensorMeasurementEvent;
import com.somecompany.enums.SensorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.test.StepVerifier;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SensorMeasurementProducerTests {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final SensorMeasurementEvent event = new SensorMeasurementEvent(
            UUID.fromString("0c0f6f0e-7a54-4f5e-9d0e-3c1f5b8a2d11"),
            "warehouse-1",
            "t1",
            SensorType.TEMPERATURE,
            30.5,
            Instant.parse("2026-10-20T14:03:21.512Z"));

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;
    private SensorMeasurementProducer sensorMeasurementProducer;

    @BeforeEach
    void setUp() {
        sensorMeasurementProducer = new SensorMeasurementProducer(kafkaTemplate, jsonMapper);
        ReflectionTestUtils.setField(sensorMeasurementProducer, "topic", "sensor-measurements");
    }

    @Test
    void eventShouldBeSentToKafkaWithSpecifiedKeyFormat() {
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));

        StepVerifier.create(sensorMeasurementProducer.send(event)).verifyComplete();

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("sensor-measurements"), eq("warehouse-1:t1"), json.capture());
        assertThat(jsonMapper.readTree(json.getValue())).isEqualTo(jsonMapper.readTree("""
                {
                  "eventId": "0c0f6f0e-7a54-4f5e-9d0e-3c1f5b8a2d11",
                  "warehouseId": "warehouse-1",
                  "sensorId": "t1",
                  "sensorType": "TEMPERATURE",
                  "value": 30.5,
                  "receivedAt": "2026-10-20T14:03:21.512Z"
                }
                """));
    }

    @Test
    void sendShouldFailWhenKafkaIsDown() {
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("kafka is down")));

        StepVerifier.create(sensorMeasurementProducer.send(event)).expectError(IllegalStateException.class).verify();
    }
}
