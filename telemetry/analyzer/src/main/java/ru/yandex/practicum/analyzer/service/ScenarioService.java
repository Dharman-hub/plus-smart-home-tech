package ru.yandex.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.analyzer.model.Condition;
import ru.yandex.practicum.analyzer.model.Scenario;
import ru.yandex.practicum.analyzer.model.ScenarioCondition;
import ru.yandex.practicum.analyzer.repository.ScenarioConditionRepository;
import ru.yandex.practicum.analyzer.repository.ScenarioRepository;
import ru.yandex.practicum.kafka.telemetry.event.ClimateSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.LightSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.MotionSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.kafka.telemetry.event.SwitchSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.TemperatureSensorAvro;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ScenarioService {

    private final ScenarioRepository scenarioRepository;
    private final ScenarioConditionRepository scenarioConditionRepository;

    @Transactional(readOnly = true)
    public List<Scenario> findMatchingScenarios(SensorsSnapshotAvro snapshot) {
        String hubId = snapshot.getHubId();

        return scenarioRepository.findByHubId(hubId).stream()
                .filter(scenario -> matches(scenario, snapshot))
                .toList();
    }

    private boolean matches(Scenario scenario, SensorsSnapshotAvro snapshot) {
        List<ScenarioCondition> conditions =
                scenarioConditionRepository.findByScenario_Id(scenario.getId());

        return conditions.stream()
                .allMatch(condition -> matches(
                        condition,
                        snapshot.getSensorsState()
                ));
    }

    private boolean matches(
            ScenarioCondition scenarioCondition,
            Map<String, SensorStateAvro> states) {

        String sensorId = scenarioCondition.getSensor().getId();

        SensorStateAvro state = states.get(sensorId);

        if (state == null) {
            return false;
        }

        Condition condition = scenarioCondition.getCondition();
        Integer actualValue = getValue(condition, state.getData());

        if (actualValue == null || condition.getValue() == null) {
            return false;
        }

        return switch (condition.getOperation()) {
            case EQUALS -> actualValue.equals(condition.getValue());
            case GREATER_THAN -> actualValue > condition.getValue();
            case LOWER_THAN -> actualValue < condition.getValue();
        };
    }

    private Integer getValue(Condition condition, Object data) {
        return switch (condition.getType()) {
            case MOTION -> data instanceof MotionSensorAvro motion
                    ? motion.getMotion() ? 1 : 0
                    : null;

            case LUMINOSITY -> data instanceof LightSensorAvro light
                    ? light.getLuminosity()
                    : null;

            case SWITCH -> data instanceof SwitchSensorAvro switchSensor
                    ? switchSensor.getState() ? 1 : 0
                    : null;

            case TEMPERATURE -> {
                if (data instanceof TemperatureSensorAvro temperature) {
                    yield temperature.getTemperatureC();
                }

                if (data instanceof ClimateSensorAvro climate) {
                    yield climate.getTemperatureC();
                }

                yield null;
            }

            case CO2LEVEL -> data instanceof ClimateSensorAvro climate
                    ? climate.getCo2Level()
                    : null;

            case HUMIDITY -> data instanceof ClimateSensorAvro climate
                    ? climate.getHumidity()
                    : null;
        };
    }
}