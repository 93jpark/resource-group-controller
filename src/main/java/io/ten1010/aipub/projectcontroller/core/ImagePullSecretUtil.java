package io.ten1010.aipub.projectcontroller.core;

import io.kubernetes.client.openapi.models.V1LocalObjectReference;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1PodTemplateSpec;
import io.kubernetes.client.openapi.models.V1Secret;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ImagePullSecretUtil {

    private static final String PULL_SECRET_KEY = "pullSecret";

    public static boolean hasPullSecretData(V1Secret secret) {
        return secret.getData() != null && secret.getData().get(PULL_SECRET_KEY) != null;
    }

    public static String getPullSecretValue(V1Secret secret) {
        return new String(secret.getData().get(PULL_SECRET_KEY));
    }

    public static List<V1LocalObjectReference> getPodTemplateImagePullSecrets(V1PodTemplateSpec podTemplateSpec) {
        return Optional.ofNullable(podTemplateSpec)
                .map(V1PodTemplateSpec::getSpec)
                .map(V1PodSpec::getImagePullSecrets)
                .orElseGet(ArrayList::new);
    }

    public static Map<String, byte[]> applyNewSecretValue(V1Secret secret, String secretValue) {
        Map<String, byte[]> secretValueMap = new HashMap<>();
        if (secret.getData() != null) {
            secretValueMap.putAll(secret.getData());
        }
        secret.getData().put(PULL_SECRET_KEY, secretValue.getBytes());
        return secretValueMap;
    }

    private ImagePullSecretUtil() {
        throw new UnsupportedOperationException();
    }

}
