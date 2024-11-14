package io.ten1010.aipub.projectcontroller.controller.cluster;

import java.util.List;
import java.util.Optional;

public interface RobotAccountService {

    Optional<RegistryRobotAccount> getRobotAccount(String imageNamespaceGroupName);

    RegistryRobotAccount createRobotAccount(String robotAccountName, List<String> aipubImageNamespaces);

    RegistryRobotAccount updateRobotAccount(Long imageNamespaceGroupId, List<String> aipubImageNamespaces);

    void deleteRobotAccount(Long imageNamespaceGroupId);

}
