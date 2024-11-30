package io.ten1010.aipub.projectcontroller.service;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class MockRegistryRobotService implements RegistryRobotService {

    private Map<String, RegistryRobot> robotMap;

    public MockRegistryRobotService() {
        this.robotMap = new HashMap<>();
    }

    @Override
    public String createRobot(RegistryRobot robot) {
        createMockRobot(robot);
        robotMap.put(robot.getUsername(), robot);
        return robot.getSecret();
    }

    @Override
    public RegistryRobot getRobot(String id) {
        return robotMap.get(id);
    }

    @Override
    public Optional<RegistryRobot> findByUsername(String username) {
        return Optional.ofNullable(robotMap.get(username));
    }

    @Override
    public void updateRobot(String id, RegistryRobot robot) {
        log.info("Updating robot with id: {}", id);
        this.robotMap.put(id, robot);
    }

    @Override
    public void deleteRobot(String id) {
        log.info("Deleting robot with id: {}", id);
        this.robotMap.remove(id);
    }

    private RegistryRobot createMockRobot(RegistryRobot robot) {
        robot.setId(robot.getUsername());
        robot.setSecret("1xmAN8M9lFeIR23THid2MmdhCctyXsTJ");
        robot.setCreatedTimestamp(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC));
        log.info("Created mock robot: {}", robot);
        return robot;
    }

}
