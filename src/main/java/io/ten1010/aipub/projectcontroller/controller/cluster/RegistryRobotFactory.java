package io.ten1010.aipub.projectcontroller.controller.cluster;

import io.ten1010.aipub.projectcontroller.service.RegistryAccess;
import io.ten1010.aipub.projectcontroller.service.RegistryRobot;
import io.ten1010.aipub.projectcontroller.service.RobotPermission;

import java.util.ArrayList;
import java.util.List;

public final class RegistryRobotFactory {

    public static RegistryRobot create(String robotUsername, List<String> aipubImageNamespaces) {
        RegistryRobot robot = new RegistryRobot();
        robot.setUsername(robotUsername);
        List<RobotPermission> robotPermissions = new ArrayList<>();
        for (String imageNamespace : aipubImageNamespaces) {
            List<RegistryAccess> registryAccesses = new ArrayList<>();
            RegistryAccess registryAccess = new RegistryAccess();
            registryAccess.setResource("repository");
            registryAccess.setAction("pull");
            registryAccesses.add(registryAccess);

            RobotPermission robotPermission = new RobotPermission();
            robotPermission.setAccesses(registryAccesses);
            robotPermission.setNamespace(imageNamespace);
            robotPermissions.add(robotPermission);
        }
        robot.setPermissions(robotPermissions);
        return robot;
    }

    private RegistryRobotFactory() {
        throw new UnsupportedOperationException();
    }

}
