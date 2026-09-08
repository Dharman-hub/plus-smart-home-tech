package ru.yandex.practicum.analyzer.config;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.practicum.analyzer.deserializer.HubEventDeserializer;
import ru.yandex.practicum.analyzer.deserializer.SensorsSnapshotDeserializer;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.util.Properties;

@Configuration
public class KafkaConfig {

    @Value("${analyzer.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${analyzer.kafka.snapshots.group-id}")
    private String snapshotsGroupId;

    @Value("${analyzer.kafka.hubs.group-id}")
    private String hubsGroupId;

    @Bean("snapshotsConsumer")
    public Consumer<String, SensorsSnapshotAvro> snapshotsConsumer() {
        Properties properties = new Properties();

        properties.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
        );
        properties.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                snapshotsGroupId
        );
        properties.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );
        properties.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                SensorsSnapshotDeserializer.class
        );
        properties.put(
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                false
        );
        properties.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
        );

        return new KafkaConsumer<>(properties);
    }

    @Bean("hubEventsConsumer")
    public Consumer<String, HubEventAvro> hubEventsConsumer() {
        Properties properties = new Properties();

        properties.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
        );
        properties.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                hubsGroupId
        );
        properties.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );
        properties.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                HubEventDeserializer.class
        );
        properties.put(
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                false
        );
        properties.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
        );

        return new KafkaConsumer<>(properties);
    }
}