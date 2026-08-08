package ru.yandex.practicum.collector.mapper;

import com.google.protobuf.Timestamp;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.kafka.telemetry.event.ClimateSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.LightSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.MotionSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SwitchSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.TemperatureSensorAvro;

import java.time.Instant;

@Component
public class SensorEventMapper {

    public SensorEventAvro toAvro(SensorEventProto event) {
        return SensorEventAvro.newBuilder()
                .setId(event.getId())
                .setHubId(event.getHubId())
                .setTimestamp(toInstant(event.getTimestamp()))
                .setPayload(mapPayload(event))
                .build();
    }

    private Object mapPayload(SensorEventProto event) {
        return switch (event.getPayloadCase()) {
            case CLIMATE_SENSOR -> ClimateSensorAvro.newBuilder()
                    .setTemperatureC(event.getClimateSensor().getTemperatureC())
                    .setHumidity(event.getClimateSensor().getHumidity())
                    .setCo2Level(event.getClimateSensor().getCo2Level())
                    .build();

            case LIGHT_SENSOR -> LightSensorAvro.newBuilder()
                    .setLinkQuality(event.getLightSensor().getLinkQuality())
                    .setLuminosity(event.getLightSensor().getLuminosity())
                    .build();

            case MOTION_SENSOR -> MotionSensorAvro.newBuilder()
                    .setLinkQuality(event.getMotionSensor().getLinkQuality())
                    .setMotion(event.getMotionSensor().getMotion())
                    .setVoltage(event.getMotionSensor().getVoltage())
                    .build();

            case SWITCH_SENSOR -> SwitchSensorAvro.newBuilder()
                    .setState(event.getSwitchSensor().getState())
                    .build();

            case TEMPERATURE_SENSOR -> TemperatureSensorAvro.newBuilder()
                    .setId(event.getId())
                    .setHubId(event.getHubId())
                    .setTimestamp(toInstant(event.getTimestamp()))
                    .setTemperatureC(event.getTemperatureSensor().getTemperatureC())
                    .setTemperatureF(event.getTemperatureSensor().getTemperatureF())
                    .build();

            case PAYLOAD_NOT_SET -> throw new IllegalArgumentException(
                    "Sensor event payload is not set"
            );
        };
    }

    private Instant toInstant(Timestamp timestamp) {
        return Instant.ofEpochSecond(
                timestamp.getSeconds(),
                timestamp.getNanos()
        );
    }
}