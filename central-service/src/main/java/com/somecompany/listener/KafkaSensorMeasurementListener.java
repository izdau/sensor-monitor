package com.somecompany.listener;

import com.somecompany.dto.SensorMeasurementEvent;
import com.somecompany.service.SensorAlarmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Receives the sensor measurements from Kafka. */
@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaSensorMeasurementListener {

    private final JsonMapper jsonMapper;
    private final SensorAlarmService sensorAlarmService;

    /** The offset is committed after the method returns. A message that cannot be processed is logged and skipped. */
    @KafkaListener(topics = "${conf.kafka.topic}")
    public void onMessage(String message) {
        try {
            sensorAlarmService.check(jsonMapper.readValue(message, SensorMeasurementEvent.class));
        } catch (RuntimeException e) {
            log.warn("dropped sensor measurement: {}", e.getMessage());
        }
    }
}
