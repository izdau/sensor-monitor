package com.somecompany.service;

import com.somecompany.dto.SensorMeasurementEvent;
import com.somecompany.enums.SensorType;
import com.somecompany.producer.SensorMeasurementProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SensorMeasurementServiceTests {

    @Mock
    private SensorMeasurementProducer sensorMeasurementProducer;
    @InjectMocks
    private SensorMeasurementService sensorMeasurementService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(sensorMeasurementService, "warehouseId", "warehouse-1");
    }

    @Test
    void validSensorDataShouldBeSentAsAnEvent() {
        when(sensorMeasurementProducer.send(any())).thenReturn(Mono.empty());

        StepVerifier.create(sensorMeasurementService.process(SensorType.TEMPERATURE, Flux.just("sensor_id=t1; value=36.5")))
                .verifyComplete();

        ArgumentCaptor<SensorMeasurementEvent> sent = ArgumentCaptor.forClass(SensorMeasurementEvent.class);
        verify(sensorMeasurementProducer).send(sent.capture());
        assertThat(sent.getValue())
                .usingRecursiveComparison().ignoringFields("eventId", "receivedAt")
                .isEqualTo(new SensorMeasurementEvent(null, "warehouse-1", "t1", SensorType.TEMPERATURE, 36.5, null));
        assertThat(sent.getValue().eventId()).isNotNull();
        assertThat(sent.getValue().receivedAt()).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"sensor_id=t1; value=30", "sensor_id=t1; value=-4.5", "sensor_id=t1; value=30\n", "sensor_id=h1; value=50\r\n",
            "sensor_id=t1; value=9999.99999", "sensor_id=t1; value=-9999.99999"})
    void validSensorDataFormatsShouldBeSent(String sensorData) {
        when(sensorMeasurementProducer.send(any())).thenReturn(Mono.empty());

        StepVerifier.create(sensorMeasurementService.process(SensorType.TEMPERATURE, Flux.just(sensorData))).verifyComplete();

        verify(sensorMeasurementProducer).send(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"garbage", "", "sensor_id=t1;value=30", "sensor_id=t1;  value=30", " sensor_id=t1; value=30",
            "sensor_id=h1; value=30 ", "sensor_id=; value=30", "sensor_id=h1; value=abc", "SENSOR_ID=t1; VALUE=30",
            "sensor_id=t1; value=10000", "sensor_id=t1; value=1.123456"})
    void invalidSensorDataFormatsShouldBeDiscarded(String sensorData) {
        StepVerifier.create(sensorMeasurementService.process(SensorType.TEMPERATURE, Flux.just(sensorData))).verifyComplete();

        verifyNoInteractions(sensorMeasurementProducer);
    }

    @Test
    void invalidSensorDataFormatsShouldNotStopTheStream() {
        when(sensorMeasurementProducer.send(any())).thenReturn(Mono.empty());

        StepVerifier.create(sensorMeasurementService.process(SensorType.TEMPERATURE, Flux.just("garbage", "sensor_id=t1; value=30")))
                .verifyComplete();

        verify(sensorMeasurementProducer).send(any());
    }

    @Test
    void failedSendsShouldNotStopTheStream() {
        when(sensorMeasurementProducer.send(any())).thenReturn(Mono.error(new IllegalStateException("kafka is down"))).thenReturn(Mono.empty());

        StepVerifier.create(sensorMeasurementService.process(SensorType.TEMPERATURE, Flux.just("sensor_id=t1; value=30", "sensor_id=t2; value=31")))
                .verifyComplete();

        verify(sensorMeasurementProducer, times(2)).send(any());
    }
}
