package ru.yandex.practicum.analyzer.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.analyzer.service.HubEventService;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class HubEventProcessor implements Runnable {

    private final Consumer<String, HubEventAvro> consumer;
    private final HubEventService hubEventService;

    @Value("${analyzer.kafka.hubs.topic}")
    private String topic;

    public HubEventProcessor(
            @Qualifier("hubEventsConsumer")
            Consumer<String, HubEventAvro> consumer,
            HubEventService hubEventService) {

        this.consumer = consumer;
        this.hubEventService = hubEventService;
    }

    @Override
    public void run() {
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

        try {
            consumer.subscribe(List.of(topic));

            while (true) {
                ConsumerRecords<String, HubEventAvro> records =
                        consumer.poll(Duration.ofMillis(1000));

                for (ConsumerRecord<String, HubEventAvro> record : records) {
                    hubEventService.handle(record.value());
                }

                consumer.commitSync();
            }
        } catch (WakeupException ignored) {
        } catch (Exception e) {
            log.error("Ошибка обработки событий хаба", e);
        } finally {
            try {
                consumer.commitSync();
            } finally {
                consumer.close();
            }
        }
    }
}