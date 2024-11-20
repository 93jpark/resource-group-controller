package io.ten1010.aipub.projectcontroller.service;

import java.util.Optional;

public interface RegistryRobotService {

    String createRobot(RegistryRobot robot);

    RegistryRobot getRobot(String id);

    Optional<RegistryRobot> findByUsername(String username);

    void updateRobot(String id, RegistryRobot robot);

    void deleteRobot(String id);

}
