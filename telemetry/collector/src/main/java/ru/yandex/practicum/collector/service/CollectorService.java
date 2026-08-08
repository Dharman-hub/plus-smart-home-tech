package ru.yandex.practicum.collector.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.collector.kafka.KafkaEventProducer;
import ru.yandex.practicum.collector.mapper.HubEventMapper;
import ru.yandex.practicum.collector.mapper.SensorEventMapper;
import ru.yandex.practicum.collector.model.hub.HubEvent;
import ru.yandex.practicum.collector.model.sensor.SensorEvent;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

@Service
@RequiredArgsConstructor
public class CollectorService {

    private final SensorEventMapper sensorEventMapper;
    private final HubEventMapper hubEventMapper;
    private final KafkaEventProducer kafkaEventProducer;

    public void collectSensorEvent(SensorEvent event) {
        SensorEventAvro avroEvent = sensorEventMapper.toAvro(event);

        kafkaEventProducer.sendSensorEvent(avroEvent);
    }

    public void collectHubEvent(HubEvent event) {
        HubEventAvro avroEvent = hubEventMapper.toAvro(event);

        kafkaEventProducer.sendHubEvent(avroEvent);
    }
}