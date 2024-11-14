package io.ten1010.aipub.projectcontroller.core;

import io.kubernetes.client.openapi.models.V1Secret;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class ImagePullSecretUtil {

    private static final String PULL_SECRET_KEY = "pullSecret";

    public static boolean hasPullSecretData(V1Secret secret, String secretValue) {
        if (secret.getData() == null || secret.getData().get(PULL_SECRET_KEY) == null)  {
            return false;
        }
        return Arrays.equals(secret.getData().get(PULL_SECRET_KEY), secretValue.getBytes());
    }

    public static byte[] castToBytes(String secretValue) {
        return secretValue.getBytes();
    }

    public static Map<String, byte[]> applyNewSecretValue(V1Secret secret, byte[] secretValue) {
        Map<String, byte[]> secretValueMap = new HashMap<>();
        if (secret.getData() != null) {
            secretValueMap.putAll(secret.getData());
        }
        secret.getData().put(PULL_SECRET_KEY, secretValue);
        return secretValueMap;
    }

    private ImagePullSecretUtil() {
        throw new UnsupportedOperationException();
    }

}
