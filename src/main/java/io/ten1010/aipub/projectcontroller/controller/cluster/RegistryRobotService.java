package io.ten1010.aipub.projectcontroller.controller.cluster;

import java.util.List;
import java.util.Optional;

public interface RegistryRobotService {

    Optional<RegistryRobot> getRegistryRobot(String imageNamespaceGroupName);

    RegistryRobot createRegistryRobot(String registryRobotName, List<String> aipubImageNamespaces);

    RegistryRobot updateRegistryRobot(Long imageNamespaceGroupId, List<String> aipubImageNamespaces);

    void deleteRegistryRobot(Long imageNamespaceGroupId);

}
