package io.ten1010.aipub.projectcontroller.core;

import io.kubernetes.client.openapi.models.V1Secret;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public final class ImagePullSecretUtil {

    private static final String IMAGE_PULL_SECRET_KEY = ".dockerconfigjson";

    public static boolean hasPullSecretData(V1Secret secret) {
        return secret.getData() != null && secret.getData().get(IMAGE_PULL_SECRET_KEY) != null;
    }

    public static void applyNewPullSecretValue(V1Secret secret, String robotUsername, String secretValue) {
        Map<String, byte[]> secretValueMap = new HashMap<>();
        if (secret.getData() != null) {
            secretValueMap.putAll(secret.getData());
        }

        // Harbor robot account 정보로 Docker config JSON 생성
        String dockerConfig = String.format(
                "{\"auths\":{\"%s\":{\"username\":\"%s\",\"password\":\"%s\"}}}",
                "https://harbor.cluster9.idc1.ten1010.io",   // Harbor 레지스트리 주소
                "robot$" + robotUsername, // Harbor robot account 이름
                secretValue                // Harbor에서 발급받은 시크릿
        );

        secretValueMap.put(IMAGE_PULL_SECRET_KEY, dockerConfig.getBytes());
        secret.setData(secretValueMap);
    }

    private ImagePullSecretUtil() {
        throw new UnsupportedOperationException();
    }

}
