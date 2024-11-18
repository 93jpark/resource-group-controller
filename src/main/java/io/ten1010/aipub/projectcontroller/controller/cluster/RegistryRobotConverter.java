package io.ten1010.aipub.projectcontroller.controller.cluster;

import org.springframework.util.Assert;

public class RegistryRobotConverter {

    private static final String ROBOT_SUFFIX = "-aipub-default-robot";

    public static String toRegistryRobotUsername(String imageNamespaceGroupName) {
        Assert.hasText(imageNamespaceGroupName, "imageNamespaceGroupName must not be empty");
        return buildRobotUsername(imageNamespaceGroupName);
    }

    public static String toImageNamespaceGroupName(String registryRobotUsername) {
        return resolveImageNamespaceGroupName(registryRobotUsername);
    }

    private static String buildRobotUsername(String imageNamespaceGroupName) {
        return imageNamespaceGroupName + ROBOT_SUFFIX;
    }

    public static String resolveImageNamespaceGroupName(String registryRobotUsername) {
        if (!registryRobotUsername.endsWith(ROBOT_SUFFIX)) {
            throw new IllegalArgumentException("Invalid registry robot username format");
        }
        return registryRobotUsername.substring(0, registryRobotUsername.length() - ROBOT_SUFFIX.length());
    }

}
