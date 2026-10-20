package com.somecompany.listener;

import com.somecompany.dto.SensorMeasurementEvent;
import com.somecompany.service.AlarmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

/** Receives the sensor measurements from Kafka. */
@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaSensorMeasurementListener {

    private final JsonMapper jsonMapper;
    private final AlarmService alarmService;

    /** The offset is committed when the returned Mono completes. A message that cannot be processed is logged and skipped. */
    @KafkaListener(topics = "${conf.kafka.topic}")
    public Mono<Void> onMessage(String message) {
        return Mono.fromCallable(() -> jsonMapper.readValue(message, SensorMeasurementEvent.class))
                .doOnNext(alarmService::check)
                .onErrorResume(e -> {
                    log.warn("dropped sensor measurement: {}", e.getMessage());
                    return Mono.empty();
                })
                .then();
    }
}
