package com.somecompany.service;

import com.somecompany.dto.SensorMeasurementEvent;
import com.somecompany.enums.SensorType;
import com.somecompany.producer.SensorMeasurementProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class SensorMeasurementService {

    // sensor_id=t1; value=30 (netcat adds a line break at the end), value: -9999.99999 to 9999.99999
    private static final Pattern SENSOR_DATA_REGEX_PATTERN = Pattern.compile("sensor_id=([\\w.-]{1,64}); value=(-?\\d{1,4}(?:\\.\\d{1,5})?)\\R?");

    private final SensorMeasurementProducer sensorMeasurementProducer; // sends the sensor measurement event to Kafka

    @Value("${conf.warehouse-id}")
    private String warehouseId;

    /** Validates each sensor data and sends it to Kafka. Bad sensor data is logged and skipped, the stream goes on. */
    public Mono<Void> process(SensorType type, Flux<String> sensorData) {
        return sensorData
                .flatMap(data -> Mono.fromCallable(() -> toEvent(type, data))
                        .flatMap(sensorMeasurementProducer::send)
                        .onErrorResume(e -> {
                            log.warn("dropped {} sensor data: {}", type, e.getMessage());
                            return Mono.empty();
                        }))
                .then();
    }

    private SensorMeasurementEvent toEvent(SensorType type, String data) {
        Matcher matcher = SENSOR_DATA_REGEX_PATTERN.matcher(data);
        if (!matcher.matches())
            throw new IllegalArgumentException("expected 'sensor_id=<id>; value=<number from -9999.99999 to 9999.99999>' but received '%.40s'".formatted(data.strip()));

        return new SensorMeasurementEvent(UUID.randomUUID(), warehouseId, matcher.group(1), type, Double.parseDouble(matcher.group(2)), Instant.now());
    }
}
