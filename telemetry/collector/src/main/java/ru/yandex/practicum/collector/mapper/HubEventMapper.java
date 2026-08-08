package ru.yandex.practicum.collector.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.collector.model.hub.DeviceAction;
import ru.yandex.practicum.collector.model.hub.DeviceAddedEvent;
import ru.yandex.practicum.collector.model.hub.DeviceRemovedEvent;
import ru.yandex.practicum.collector.model.hub.HubEvent;
import ru.yandex.practicum.collector.model.hub.ScenarioAddedEvent;
import ru.yandex.practicum.collector.model.hub.ScenarioCondition;
import ru.yandex.practicum.collector.model.hub.ScenarioRemovedEvent;
import ru.yandex.practicum.kafka.telemetry.event.ActionTypeAvro;
import ru.yandex.practicum.kafka.telemetry.event.ConditionOperationAvro;
import ru.yandex.practicum.kafka.telemetry.event.ConditionTypeAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceActionAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceRemovedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceTypeAvro;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioConditionAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioRemovedEventAvro;

import java.util.List;

@Component
public class HubEventMapper {

    public HubEventAvro toAvro(HubEvent event) {
        return HubEventAvro.newBuilder()
                .setHubId(event.getHubId())
                .setTimestamp(event.getTimestamp())
                .setPayload(mapPayload(event))
                .build();
    }

    private Object mapPayload(HubEvent event) {
        if (event instanceof DeviceAddedEvent deviceAddedEvent) {
            return DeviceAddedEventAvro.newBuilder()
                    .setId(deviceAddedEvent.getId())
                    .setType(DeviceTypeAvro.valueOf(
                            deviceAddedEvent.getDeviceType().name()
                    ))
                    .build();
        }

        if (event instanceof DeviceRemovedEvent deviceRemovedEvent) {
            return DeviceRemovedEventAvro.newBuilder()
                    .setId(deviceRemovedEvent.getId())
                    .build();
        }

        if (event instanceof ScenarioAddedEvent scenarioAddedEvent) {
            return ScenarioAddedEventAvro.newBuilder()
                    .setName(scenarioAddedEvent.getName())
                    .setConditions(mapConditions(
                            scenarioAddedEvent.getConditions()
                    ))
                    .setActions(mapActions(
                            scenarioAddedEvent.getActions()
                    ))
                    .build();
        }

        if (event instanceof ScenarioRemovedEvent scenarioRemovedEvent) {
            return ScenarioRemovedEventAvro.newBuilder()
                    .setName(scenarioRemovedEvent.getName())
                    .build();
        }

        throw new IllegalArgumentException(
                "Unknown hub event type: " + event.getClass().getName()
        );
    }

    private List<ScenarioConditionAvro> mapConditions(
            List<ScenarioCondition> conditions) {

        return conditions.stream()
                .map(this::mapCondition)
                .toList();
    }

    private ScenarioConditionAvro mapCondition(
            ScenarioCondition condition) {

        return ScenarioConditionAvro.newBuilder()
                .setSensorId(condition.getSensorId())
                .setType(ConditionTypeAvro.valueOf(
                        condition.getType().name()
                ))
                .setOperation(ConditionOperationAvro.valueOf(
                        condition.getOperation().name()
                ))
                .setValue(condition.getValue())
                .build();
    }

    private List<DeviceActionAvro> mapActions(
            List<DeviceAction> actions) {

        return actions.stream()
                .map(this::mapAction)
                .toList();
    }

    private DeviceActionAvro mapAction(DeviceAction action) {
        return DeviceActionAvro.newBuilder()
                .setSensorId(action.getSensorId())
                .setType(ActionTypeAvro.valueOf(
                        action.getType().name()
                ))
                .setValue(action.getValue())
                .build();
    }
}