package ru.yandex.practicum.analyzer.service;

import com.google.protobuf.Timestamp;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.analyzer.model.Action;
import ru.yandex.practicum.analyzer.model.Scenario;
import ru.yandex.practicum.analyzer.model.ScenarioAction;
import ru.yandex.practicum.analyzer.repository.ScenarioActionRepository;
import ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;

import java.time.Instant;
import java.util.List;

@Service
public class ActionService {

    private final ScenarioActionRepository scenarioActionRepository;
    private final HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient;

    public ActionService(
            ScenarioActionRepository scenarioActionRepository,
            @GrpcClient("hub-router")
            HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient) {

        this.scenarioActionRepository = scenarioActionRepository;
        this.hubRouterClient = hubRouterClient;
    }

    @Transactional(readOnly = true)
    public void execute(Scenario scenario, Instant timestamp) {
        List<ScenarioAction> actions =
                scenarioActionRepository.findByScenario_Id(scenario.getId());

        for (ScenarioAction scenarioAction : actions) {
            DeviceActionProto action = toProto(scenarioAction);

            DeviceActionRequest request = DeviceActionRequest.newBuilder()
                    .setHubId(scenario.getHubId())
                    .setScenarioName(scenario.getName())
                    .setAction(action)
                    .setTimestamp(toTimestamp(timestamp))
                    .build();

            hubRouterClient.handleDeviceAction(request);
        }
    }

    private DeviceActionProto toProto(ScenarioAction scenarioAction) {
        Action action = scenarioAction.getAction();

        DeviceActionProto.Builder builder = DeviceActionProto.newBuilder()
                .setSensorId(scenarioAction.getSensor().getId())
                .setType(ActionTypeProto.valueOf(action.getType().name()));

        if (action.getValue() != null) {
            builder.setValue(action.getValue());
        }

        return builder.build();
    }

    private Timestamp toTimestamp(Instant instant) {
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }
}