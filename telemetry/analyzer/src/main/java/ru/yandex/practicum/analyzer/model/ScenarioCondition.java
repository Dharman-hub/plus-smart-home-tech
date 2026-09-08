package ru.yandex.practicum.analyzer.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "scenario_conditions")
@Getter
@Setter
@NoArgsConstructor
public class ScenarioCondition {

    @EmbeddedId
    private ScenarioConditionId id;

    @MapsId("scenarioId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id")
    private Scenario scenario;

    @MapsId("sensorId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_id")
    private Sensor sensor;

    @MapsId("conditionId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condition_id")
    private Condition condition;
}