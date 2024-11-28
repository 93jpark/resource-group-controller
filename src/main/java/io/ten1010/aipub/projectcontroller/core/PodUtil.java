package io.ten1010.aipub.projectcontroller.core;

import io.kubernetes.client.openapi.models.V1Affinity;
import io.kubernetes.client.openapi.models.V1LocalObjectReference;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1Toleration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class PodUtil {

    public static List<V1Toleration> getTolerations(V1Pod pod) {
        Objects.requireNonNull(pod.getSpec());
        List<V1Toleration> tolerations = pod.getSpec().getTolerations();
        return tolerations == null ? new ArrayList<>() : tolerations;
    }

    public static Optional<V1Affinity> getAffinity(V1Pod pod) {
        Objects.requireNonNull(pod.getSpec());
        V1Affinity affinity = pod.getSpec().getAffinity();
        if (affinity == null) {
            return Optional.empty();
        }
        return Optional.of(affinity);
    }

    public static List<V1LocalObjectReference> getImagePullSecrets(V1Pod pod) {
        if (pod.getSpec() == null ||
                pod.getSpec().getImagePullSecrets() == null) {
            return new ArrayList<>();
        }

        return pod.getSpec().getImagePullSecrets();
    }

    private PodUtil() {
        throw new UnsupportedOperationException();
    }

}
