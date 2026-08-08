package ru.yandex.practicum.collector.mapper;

import com.google.protobuf.Timestamp;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.event.ScenarioConditionProto;
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

import java.time.Instant;
import java.util.List;

@Component
public class HubEventMapper {

    public HubEventAvro toAvro(HubEventProto event) {
        return HubEventAvro.newBuilder()
                .setHubId(event.getHubId())
                .setTimestamp(toInstant(event.getTimestamp()))
                .setPayload(mapPayload(event))
                .build();
    }

    private Object mapPayload(HubEventProto event) {
        return switch (event.getPayloadCase()) {
            case DEVICE_ADDED -> DeviceAddedEventAvro.newBuilder()
                    .setId(event.getDeviceAdded().getId())
                    .setType(DeviceTypeAvro.valueOf(
                            event.getDeviceAdded().getType().name()
                    ))
                    .build();

            case DEVICE_REMOVED -> DeviceRemovedEventAvro.newBuilder()
                    .setId(event.getDeviceRemoved().getId())
                    .build();

            case SCENARIO_ADDED -> ScenarioAddedEventAvro.newBuilder()
                    .setName(event.getScenarioAdded().getName())
                    .setConditions(mapConditions(
                            event.getScenarioAdded().getConditionList()
                    ))
                    .setActions(mapActions(
                            event.getScenarioAdded().getActionList()
                    ))
                    .build();

            case SCENARIO_REMOVED -> ScenarioRemovedEventAvro.newBuilder()
                    .setName(event.getScenarioRemoved().getName())
                    .build();

            case PAYLOAD_NOT_SET -> throw new IllegalArgumentException(
                    "Hub event payload is not set"
            );
        };
    }

    private List<ScenarioConditionAvro> mapConditions(
            List<ScenarioConditionProto> conditions) {

        return conditions.stream()
                .map(this::mapCondition)
                .toList();
    }

    private ScenarioConditionAvro mapCondition(
            ScenarioConditionProto condition) {

        return ScenarioConditionAvro.newBuilder()
                .setSensorId(condition.getSensorId())
                .setType(ConditionTypeAvro.valueOf(
                        condition.getType().name()
                ))
                .setOperation(ConditionOperationAvro.valueOf(
                        condition.getOperation().name()
                ))
                .setValue(mapConditionValue(condition))
                .build();
    }

    private Object mapConditionValue(ScenarioConditionProto condition) {
        return switch (condition.getValueCase()) {
            case BOOL_VALUE -> condition.getBoolValue();
            case INT_VALUE -> condition.getIntValue();
            case VALUE_NOT_SET -> null;
        };
    }

    private List<DeviceActionAvro> mapActions(
            List<DeviceActionProto> actions) {

        return actions.stream()
                .map(this::mapAction)
                .toList();
    }

    private DeviceActionAvro mapAction(DeviceActionProto action) {
        return DeviceActionAvro.newBuilder()
                .setSensorId(action.getSensorId())
                .setType(ActionTypeAvro.valueOf(
                        action.getType().name()
                ))
                .setValue(action.hasValue() ? action.getValue() : null)
                .build();
    }

    private Instant toInstant(Timestamp timestamp) {
        return Instant.ofEpochSecond(
                timestamp.getSeconds(),
                timestamp.getNanos()
        );
    }
}