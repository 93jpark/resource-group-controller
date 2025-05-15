package io.ten1010.aipub.projectcontroller.domain.k8s.util;

import io.kubernetes.client.openapi.models.V1ConfigMap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ConfigMapUtils {

    public static HashMap<String, String> getData(V1ConfigMap configMap) {
        if (configMap.getData() == null) {
            return new HashMap<>();
        }
        return new HashMap<>(configMap.getData());
    }

    public static HashMap<String, String> getData(V1ConfigMap configMap, String key) {
        if (configMap.getData() != null && configMap.getData().containsKey(key)) {
            return new HashMap<>(Map.of(key, configMap.getData().get(key)));
        }
        return new HashMap<>();
    }

}
