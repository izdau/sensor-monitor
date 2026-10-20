package com.somecompany.service;

import com.somecompany.dto.SensorMeasurementEvent;
import com.somecompany.enums.SensorType;
import com.somecompany.repository.SensorMeasurementRepository;
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
    private SensorMeasurementRepository repository;
    @InjectMocks
    private SensorMeasurementService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "warehouseId", "warehouse-1");
    }

    @Test
    void validSensorDataShouldBeSavedAsAnEvent() {
        when(repository.save(any())).thenReturn(Mono.empty());

        StepVerifier.create(service.process(SensorType.TEMPERATURE, Flux.just("sensor_id=t1; value=36.5")))
                .verifyComplete();

        ArgumentCaptor<SensorMeasurementEvent> saved = ArgumentCaptor.forClass(SensorMeasurementEvent.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue())
                .usingRecursiveComparison().ignoringFields("eventId", "receivedAt")
                .isEqualTo(new SensorMeasurementEvent(null, "warehouse-1", "t1", SensorType.TEMPERATURE, 36.5, null));
        assertThat(saved.getValue().eventId()).isNotNull();
        assertThat(saved.getValue().receivedAt()).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"sensor_id=t1; value=30", "sensor_id=t1; value=-4.5", "sensor_id=t1; value=30\n", "sensor_id=h1; value=50\r\n"})
    void validSensorDataFormatsShouldBeSaved(String sensorData) {
        when(repository.save(any())).thenReturn(Mono.empty());

        StepVerifier.create(service.process(SensorType.TEMPERATURE, Flux.just(sensorData))).verifyComplete();

        verify(repository).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"garbage", "", "sensor_id=t1;value=30", "sensor_id=t1;  value=30", " sensor_id=t1; value=30",
            "sensor_id=h1; value=30 ", "sensor_id=; value=30", "sensor_id=h1; value=abc", "SENSOR_ID=t1; VALUE=30"})
    void invalidSensorDataFormatsShouldBeDiscarded(String sensorData) {
        StepVerifier.create(service.process(SensorType.TEMPERATURE, Flux.just(sensorData))).verifyComplete();

        verifyNoInteractions(repository);
    }

    @Test
    void invalidSensorDataFormatsShouldNotStopTheStream() {
        when(repository.save(any())).thenReturn(Mono.empty());

        StepVerifier.create(service.process(SensorType.TEMPERATURE, Flux.just("garbage", "sensor_id=t1; value=30")))
                .verifyComplete();

        verify(repository).save(any());
    }

    @Test
    void failedSavesShouldNotStopTheStream() {
        when(repository.save(any())).thenReturn(Mono.error(new IllegalStateException("kafka is down"))).thenReturn(Mono.empty());

        StepVerifier.create(service.process(SensorType.TEMPERATURE, Flux.just("sensor_id=t1; value=30", "sensor_id=t2; value=31")))
                .verifyComplete();

        verify(repository, times(2)).save(any());
    }
}
