package ru.yandex.practicum.analyzer.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.analyzer.model.Scenario;
import ru.yandex.practicum.analyzer.service.ActionService;
import ru.yandex.practicum.analyzer.service.ScenarioService;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class SnapshotProcessor {

    private final Consumer<String, SensorsSnapshotAvro> consumer;
    private final ScenarioService scenarioService;
    private final ActionService actionService;

    @Value("${analyzer.kafka.snapshots.topic}")
    private String topic;

    public SnapshotProcessor(
            @Qualifier("snapshotsConsumer")
            Consumer<String, SensorsSnapshotAvro> consumer,
            ScenarioService scenarioService,
            ActionService actionService) {

        this.consumer = consumer;
        this.scenarioService = scenarioService;
        this.actionService = actionService;
    }

    public void start() {
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

        try {
            consumer.subscribe(List.of(topic));

            while (true) {
                ConsumerRecords<String, SensorsSnapshotAvro> records =
                        consumer.poll(Duration.ofMillis(1000));

                for (ConsumerRecord<String, SensorsSnapshotAvro> record : records) {
                    SensorsSnapshotAvro snapshot = record.value();

                    List<Scenario> scenarios =
                            scenarioService.findMatchingScenarios(snapshot);

                    for (Scenario scenario : scenarios) {
                        actionService.execute(
                                scenario,
                                snapshot.getTimestamp()
                        );
                    }
                }

                consumer.commitSync();
            }
        } catch (WakeupException ignored) {
        } catch (Exception e) {
            log.error("Ошибка обработки снапшотов", e);
        } finally {
            try {
                consumer.commitSync();
            } finally {
                consumer.close();
            }
        }
    }
}