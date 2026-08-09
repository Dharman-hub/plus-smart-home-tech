package ru.yandex.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.analyzer.model.Action;
import ru.yandex.practicum.analyzer.model.ActionType;
import ru.yandex.practicum.analyzer.model.Condition;
import ru.yandex.practicum.analyzer.model.ConditionOperation;
import ru.yandex.practicum.analyzer.model.ConditionType;
import ru.yandex.practicum.analyzer.model.Scenario;
import ru.yandex.practicum.analyzer.model.ScenarioAction;
import ru.yandex.practicum.analyzer.model.ScenarioActionId;
import ru.yandex.practicum.analyzer.model.ScenarioCondition;
import ru.yandex.practicum.analyzer.model.ScenarioConditionId;
import ru.yandex.practicum.analyzer.model.Sensor;
import ru.yandex.practicum.analyzer.repository.ActionRepository;
import ru.yandex.practicum.analyzer.repository.ConditionRepository;
import ru.yandex.practicum.analyzer.repository.ScenarioActionRepository;
import ru.yandex.practicum.analyzer.repository.ScenarioConditionRepository;
import ru.yandex.practicum.analyzer.repository.ScenarioRepository;
import ru.yandex.practicum.analyzer.repository.SensorRepository;
import ru.yandex.practicum.kafka.telemetry.event.DeviceActionAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceRemovedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioConditionAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioRemovedEventAvro;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HubEventService {

    private final SensorRepository sensorRepository;
    private final ScenarioRepository scenarioRepository;
    private final ConditionRepository conditionRepository;
    private final ActionRepository actionRepository;
    private final ScenarioConditionRepository scenarioConditionRepository;
    private final ScenarioActionRepository scenarioActionRepository;

    @Transactional
    public void handle(HubEventAvro event) {
        Object payload = event.getPayload();
        String hubId = event.getHubId().toString();

        if (payload instanceof DeviceAddedEventAvro added) {
            addSensor(hubId, added);
        } else if (payload instanceof DeviceRemovedEventAvro removed) {
            removeSensor(hubId, removed);
        } else if (payload instanceof ScenarioAddedEventAvro added) {
            addScenario(hubId, added);
        } else if (payload instanceof ScenarioRemovedEventAvro removed) {
            removeScenario(hubId, removed.getName().toString());
        } else {
            throw new IllegalArgumentException(
                    "Unknown hub event payload: " + payload.getClass().getName()
            );
        }
    }

    private void addSensor(String hubId, DeviceAddedEventAvro event) {
        String sensorId = event.getId().toString();

        Sensor sensor = sensorRepository.findById(sensorId)
                .orElseGet(Sensor::new);

        sensor.setId(sensorId);
        sensor.setHubId(hubId);

        sensorRepository.save(sensor);
    }

    private void removeSensor(String hubId, DeviceRemovedEventAvro event) {
        sensorRepository.findByIdAndHubId(event.getId().toString(), hubId)
                .ifPresent(this::deleteSensor);
    }

    private void addScenario(String hubId, ScenarioAddedEventAvro event) {
        String name = event.getName().toString();

        scenarioRepository.findByHubIdAndName(hubId, name)
                .ifPresent(this::deleteScenario);

        Scenario scenario = new Scenario();
        scenario.setHubId(hubId);
        scenario.setName(name);
        scenario = scenarioRepository.save(scenario);

        for (ScenarioConditionAvro source : event.getConditions()) {
            Sensor sensor = getSensor(source.getSensorId().toString(), hubId);

            Condition condition = new Condition();
            condition.setType(ConditionType.valueOf(source.getType().name()));
            condition.setOperation(
                    ConditionOperation.valueOf(source.getOperation().name())
            );
            condition.setValue(toInteger(source.getValue()));
            condition = conditionRepository.save(condition);

            ScenarioCondition relation = new ScenarioCondition();
            relation.setId(new ScenarioConditionId(
                    scenario.getId(),
                    sensor.getId(),
                    condition.getId()
            ));
            relation.setScenario(scenario);
            relation.setSensor(sensor);
            relation.setCondition(condition);

            scenarioConditionRepository.save(relation);
        }

        for (DeviceActionAvro source : event.getActions()) {
            Sensor sensor = getSensor(source.getSensorId().toString(), hubId);

            Action action = new Action();
            action.setType(ActionType.valueOf(source.getType().name()));
            action.setValue(source.getValue());
            action = actionRepository.save(action);

            ScenarioAction relation = new ScenarioAction();
            relation.setId(new ScenarioActionId(
                    scenario.getId(),
                    sensor.getId(),
                    action.getId()
            ));
            relation.setScenario(scenario);
            relation.setSensor(sensor);
            relation.setAction(action);

            scenarioActionRepository.save(relation);
        }
    }

    private void removeScenario(String hubId, String name) {
        scenarioRepository.findByHubIdAndName(hubId, name)
                .ifPresent(this::deleteScenario);
    }

    private Sensor getSensor(String sensorId, String hubId) {
        return sensorRepository.findByIdAndHubId(sensorId, hubId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Sensor not found: " + sensorId
                ));
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Boolean bool) {
            return bool ? 1 : 0;
        }

        if (value instanceof Number number) {
            return number.intValue();
        }

        throw new IllegalArgumentException(
                "Unsupported condition value: " + value
        );
    }

    private void deleteSensor(Sensor sensor) {
        List<ScenarioCondition> conditions =
                scenarioConditionRepository.findBySensor_Id(sensor.getId());

        List<ScenarioAction> actions =
                scenarioActionRepository.findBySensor_Id(sensor.getId());

        List<Long> conditionIds = new ArrayList<>();
        for (ScenarioCondition relation : conditions) {
            conditionIds.add(relation.getCondition().getId());
        }

        List<Long> actionIds = new ArrayList<>();
        for (ScenarioAction relation : actions) {
            actionIds.add(relation.getAction().getId());
        }

        scenarioConditionRepository.deleteAll(conditions);
        scenarioActionRepository.deleteAll(actions);

        conditionRepository.deleteAllById(conditionIds);
        actionRepository.deleteAllById(actionIds);

        sensorRepository.delete(sensor);
    }

    private void deleteScenario(Scenario scenario) {
        List<ScenarioCondition> conditions =
                scenarioConditionRepository.findByScenario_Id(scenario.getId());

        List<ScenarioAction> actions =
                scenarioActionRepository.findByScenario_Id(scenario.getId());

        List<Long> conditionIds = new ArrayList<>();
        for (ScenarioCondition relation : conditions) {
            conditionIds.add(relation.getCondition().getId());
        }

        List<Long> actionIds = new ArrayList<>();
        for (ScenarioAction relation : actions) {
            actionIds.add(relation.getAction().getId());
        }

        scenarioConditionRepository.deleteAll(conditions);
        scenarioActionRepository.deleteAll(actions);

        scenarioRepository.delete(scenario);

        conditionRepository.deleteAllById(conditionIds);
        actionRepository.deleteAllById(actionIds);
    }
}