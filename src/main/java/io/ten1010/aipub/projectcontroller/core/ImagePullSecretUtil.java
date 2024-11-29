package io.ten1010.aipub.projectcontroller.core;

import io.kubernetes.client.openapi.models.V1Secret;

import java.util.HashMap;
import java.util.Map;

public final class ImagePullSecretUtil {

    private static final String IMAGE_PULL_SECRET_KEY = ".dockerconfigjson";

    public static boolean hasPullSecretData(V1Secret secret) {
        return secret.getData() != null && secret.getData().get(IMAGE_PULL_SECRET_KEY) != null;
    }

    public static String getPullSecretValue(V1Secret secret) {
        return new String(secret.getData().get(IMAGE_PULL_SECRET_KEY));
    }

    public static Map<String, byte[]> applyNewSecretValue(V1Secret secret, String secretValue) {
        Map<String, byte[]> secretValueMap = new HashMap<>();
        if (secret.getData() != null) {
            secretValueMap.putAll(secret.getData());
        }
        secret.getData().put(IMAGE_PULL_SECRET_KEY, secretValue.getBytes());
        return secretValueMap;
    }

    private ImagePullSecretUtil() {
        throw new UnsupportedOperationException();
    }

}
