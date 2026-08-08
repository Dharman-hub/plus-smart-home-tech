package ru.yandex.practicum.collector.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventProducer {

    private final Producer<String, SpecificRecordBase> producer;

    @Value("${collector.kafka.sensors-topic}")
    private String sensorsTopic;

    @Value("${collector.kafka.hubs-topic}")
    private String hubsTopic;

    public void sendSensorEvent(SensorEventAvro event) {
        send(
                sensorsTopic,
                event.getHubId(),
                event
        );
    }

    public void sendHubEvent(HubEventAvro event) {
        send(
                hubsTopic,
                event.getHubId(),
                event
        );
    }

    private void send(
            String topic,
            String key,
            SpecificRecordBase event) {

        ProducerRecord<String, SpecificRecordBase> record =
                new ProducerRecord<>(topic, key, event);

        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                log.error(
                        "Failed to send event to topic {}",
                        topic,
                        exception
                );
            }
        });
    }
}