package io.ten1010.aipub.projectcontroller.controller.cluster;

import org.springframework.util.Assert;

public class RegistryRobotResolver {

    private static final String ROBOT_PREFIX = "aipub-image-namespace-group-";

    private static String buildRobotUsername(String imageNamespaceGroupName) {
        return ROBOT_PREFIX + imageNamespaceGroupName;
    }

    public String resolveRobotUsername(String imageNamespaceGroupName) {
        Assert.hasText(imageNamespaceGroupName, "imageNamespaceGroupName must not be empty");
        return buildRobotUsername(imageNamespaceGroupName);
    }

    public String resolveImageNamespaceGroupName(String robotUsername) {
        if (!robotUsername.startsWith(ROBOT_PREFIX)) {
            throw new IllegalArgumentException("Invalid registry robot username format");
        }
        return robotUsername.substring(ROBOT_PREFIX.length());
    }

}
