package com.somecompany.repository;

import com.somecompany.dto.SensorMeasurementEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import tools.jackson.databind.json.JsonMapper;

/** There is no database: an event is stored by sending it to Kafka. */
@Repository
@RequiredArgsConstructor
public class SensorMeasurementRepository {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;

    @Value("${conf.kafka.topic}")
    private String topic;

    /** Completes when Kafka has acknowledged the event. */
    public Mono<Void> save(SensorMeasurementEvent event) {
        String key = event.warehouseId() + ":" + event.sensorId();
        return Mono.fromCallable(() -> jsonMapper.writeValueAsString(event))
                .flatMap(json -> Mono.fromFuture(() -> kafkaTemplate.send(topic, key, json)))
                .subscribeOn(Schedulers.boundedElastic()) // send() blocks while Kafka is unreachable, not on the Netty thread
                .then();
    }
}
