package ru.yandex.practicum.collector.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.collector.model.sensor.ClimateSensorEvent;
import ru.yandex.practicum.collector.model.sensor.LightSensorEvent;
import ru.yandex.practicum.collector.model.sensor.MotionSensorEvent;
import ru.yandex.practicum.collector.model.sensor.SensorEvent;
import ru.yandex.practicum.collector.model.sensor.SwitchSensorEvent;
import ru.yandex.practicum.collector.model.sensor.TemperatureSensorEvent;
import ru.yandex.practicum.kafka.telemetry.event.ClimateSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.LightSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.MotionSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SwitchSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.TemperatureSensorAvro;

@Component
public class SensorEventMapper {

    public SensorEventAvro toAvro(SensorEvent event) {
        return SensorEventAvro.newBuilder()
                .setId(event.getId())
                .setHubId(event.getHubId())
                .setTimestamp(event.getTimestamp())
                .setPayload(mapPayload(event))
                .build();
    }

    private Object mapPayload(SensorEvent event) {
        if (event instanceof ClimateSensorEvent climateEvent) {
            return ClimateSensorAvro.newBuilder()
                    .setTemperatureC(climateEvent.getTemperatureC())
                    .setHumidity(climateEvent.getHumidity())
                    .setCo2Level(climateEvent.getCo2Level())
                    .build();
        }

        if (event instanceof LightSensorEvent lightEvent) {
            return LightSensorAvro.newBuilder()
                    .setLinkQuality(lightEvent.getLinkQuality())
                    .setLuminosity(lightEvent.getLuminosity())
                    .build();
        }

        if (event instanceof MotionSensorEvent motionEvent) {
            return MotionSensorAvro.newBuilder()
                    .setLinkQuality(motionEvent.getLinkQuality())
                    .setMotion(motionEvent.getMotion())
                    .setVoltage(motionEvent.getVoltage())
                    .build();
        }

        if (event instanceof SwitchSensorEvent switchEvent) {
            return SwitchSensorAvro.newBuilder()
                    .setState(switchEvent.getState())
                    .build();
        }

        if (event instanceof TemperatureSensorEvent temperatureEvent) {
            return TemperatureSensorAvro.newBuilder()
                    .setId(temperatureEvent.getId())
                    .setHubId(temperatureEvent.getHubId())
                    .setTimestamp(temperatureEvent.getTimestamp())
                    .setTemperatureC(temperatureEvent.getTemperatureC())
                    .setTemperatureF(temperatureEvent.getTemperatureF())
                    .build();
        }

        throw new IllegalArgumentException(
                "Unknown sensor event type: " + event.getClass().getName()
        );
    }
}