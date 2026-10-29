package com.somecompany.producer;

import com.somecompany.dto.SensorMeasurementEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** SensorMeasurementEvent Kafka producer */
@Component
@RequiredArgsConstructor
public class SensorMeasurementProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;

    @Value("${conf.kafka.topic}")
    private String topic;

    /** Returns when Kafka has acknowledged the event. The wait blocks only the calling virtual thread. */
    public void send(SensorMeasurementEvent event) {
        String key = event.warehouseId() + ":" + event.sensorId();
        kafkaTemplate.send(topic, key, jsonMapper.writeValueAsString(event)).join();
    }
}
