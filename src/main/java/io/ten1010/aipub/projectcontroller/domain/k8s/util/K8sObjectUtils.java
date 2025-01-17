package io.ten1010.aipub.projectcontroller.domain.k8s.util;

import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1OwnerReference;
import io.kubernetes.client.openapi.models.V1OwnerReferenceBuilder;
import io.ten1010.aipub.projectcontroller.domain.k8s.KeyResolver;

import java.time.OffsetDateTime;
import java.util.*;

public abstract class K8sObjectUtils {

    public static String getApiVersion(KubernetesObject object) {
        Objects.requireNonNull(object.getApiVersion());
        return object.getApiVersion();
    }

    public static String getKind(KubernetesObject object) {
        Objects.requireNonNull(object.getKind());
        return object.getKind();
    }

    public static String getName(KubernetesObject object) {
        Objects.requireNonNull(object.getMetadata());
        Objects.requireNonNull(object.getMetadata().getName());
        return object.getMetadata().getName();
    }

    public static List<String> getNames(List<? extends KubernetesObject> objects) {
        return objects.stream()
                .map(K8sObjectUtils::getName)
                .toList();
    }

    public static String getNamespace(KubernetesObject object) {
        Objects.requireNonNull(object.getMetadata());
        Objects.requireNonNull(object.getMetadata().getNamespace());
        return object.getMetadata().getNamespace();
    }

    public static Map<String, String> getLabels(KubernetesObject object) {
        V1ObjectMeta metadata = object.getMetadata();
        Objects.requireNonNull(metadata);
        if (metadata.getLabels() == null) {
            return Map.of();
        }
        return metadata.getLabels();
    }

    public static Map<String, String> getAnnotations(KubernetesObject object) {
        V1ObjectMeta metadata = object.getMetadata();
        Objects.requireNonNull(metadata);
        if (metadata.getAnnotations() == null) {
            return Map.of();
        }
        return metadata.getAnnotations();
    }

    public static Optional<OffsetDateTime> getDeletionTimestamp(KubernetesObject object) {
        return Optional.ofNullable(object.getMetadata().getDeletionTimestamp());
    }

    public static List<V1OwnerReference> getOwnerReferences(KubernetesObject object) {
        V1ObjectMeta metadata = object.getMetadata();
        Objects.requireNonNull(metadata);
        if (metadata.getOwnerReferences() == null) {
            return List.of();
        }
        return metadata.getOwnerReferences();
    }

    public static V1OwnerReference buildV1OwnerReference(KubernetesObject parentObject, boolean controller, boolean blockOwnerDeletion) {
        Objects.requireNonNull(parentObject.getApiVersion());
        Objects.requireNonNull(parentObject.getKind());
        V1ObjectMeta metadata = parentObject.getMetadata();
        Objects.requireNonNull(metadata);
        Objects.requireNonNull(metadata.getName());
        Objects.requireNonNull(metadata.getUid());

        return new V1OwnerReferenceBuilder()
                .withApiVersion(parentObject.getApiVersion())
                .withKind(parentObject.getKind())
                .withName(metadata.getName())
                .withUid(metadata.getUid())
                .withController(controller)
                .withBlockOwnerDeletion(blockOwnerDeletion)
                .build();
    }

    public static Optional<V1OwnerReference> findControllerOwnerReference(KubernetesObject object) {
        return getOwnerReferences(object).stream()
                .filter(ref -> ref.getController() != null && ref.getController())
                .findAny();
    }

    public static <T extends KubernetesObject> List<T> distinctByKey(KeyResolver keyResolver, List<T> objects) {
        Map<String, T> objectMap = new HashMap<>();
        for (T object : objects) {
            Objects.requireNonNull(object.getMetadata());
            String namespace = object.getMetadata().getNamespace();
            String name = K8sObjectUtils.getName(object);
            String key;
            if (namespace == null) {
                key = keyResolver.resolveKey(name);
            } else {
                key = keyResolver.resolveKey(namespace, name);
            }
            objectMap.put(key, object);
        }
        return objectMap.values().stream().toList();
    }

}
