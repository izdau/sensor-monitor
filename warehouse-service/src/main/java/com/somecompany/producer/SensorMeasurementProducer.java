package com.somecompany.producer;

import com.somecompany.dto.SensorMeasurementEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import tools.jackson.databind.json.JsonMapper;

/** SensorMeasurementEvent Kafka producer */
@Component
@RequiredArgsConstructor
public class SensorMeasurementProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;

    @Value("${conf.kafka.topic}")
    private String topic;

    /** Completes when Kafka has acknowledged the event. */
    public Mono<Void> send(SensorMeasurementEvent event) {
        String key = event.warehouseId() + ":" + event.sensorId();
        return Mono.fromCallable(() -> jsonMapper.writeValueAsString(event))
                .flatMap(json -> Mono.fromFuture(() -> kafkaTemplate.send(topic, key, json)))
                .subscribeOn(Schedulers.boundedElastic()) // send() blocks while Kafka is unreachable, not on the Netty thread
                .then();
    }
}
