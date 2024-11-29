package io.ten1010.aipub.projectcontroller.core;

import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1TypedObjectReference;

import java.util.Objects;

public final class KeyUtil {

    public static String buildKey(String namespace, String name) {
        return namespace + "/" + name;
    }

    public static String buildKey(String name) {
        return name;
    }

    public static String getKey(KubernetesObject object) {
        Objects.requireNonNull(object.getMetadata());
        Objects.requireNonNull(object.getMetadata().getName());
        if (object.getMetadata().getNamespace() == null) {
            return buildKey(object.getMetadata().getName());
        }
        return buildKey(object.getMetadata().getNamespace(), object.getMetadata().getName());
    }

    public static String getKey(V1TypedObjectReference reference) {
        Objects.requireNonNull(reference.getName());
        if (reference.getNamespace() == null) {
            return buildKey(reference.getName());
        }
        return buildKey(reference.getNamespace(), reference.getName());
    }

    private KeyUtil() {
        throw new UnsupportedOperationException();
    }

}
