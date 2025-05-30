package io.ten1010.aipub.projectcontroller.domain.k8s.util;

import io.ten1010.aipub.projectcontroller.domain.k8s.dto.V1alpha1ImageHub;
import io.ten1010.aipub.projectcontroller.domain.k8s.dto.V1alpha1ImageHubSpec;
import io.ten1010.aipub.projectcontroller.domain.k8s.dto.V1alpha1ImageHubStatus;

import java.util.Objects;

public abstract class ImageHubUtils {

    public static String getSpecId(V1alpha1ImageHub object) {
        Objects.requireNonNull(object.getSpec());
        Objects.requireNonNull(object.getSpec().getId());

        return object.getSpec().getId();
    }

    public static V1alpha1ImageHub copyOf(V1alpha1ImageHub object) {
        V1alpha1ImageHub imageHub = new V1alpha1ImageHub();
        V1alpha1ImageHubSpec spec = new V1alpha1ImageHubSpec();
        spec.setId(getSpecId(object));
        V1alpha1ImageHubStatus status = new V1alpha1ImageHubStatus();
        status.setAllBoundAipubUsers(object.getStatus().getAllBoundAipubUsers());
        status.setAllBoundProjects(object.getStatus().getAllBoundProjects());
        imageHub.setApiVersion(object.getApiVersion());
        imageHub.setKind(object.getKind());
        imageHub.setMetadata(object.getMetadata());
        imageHub.setSpec(spec);
        imageHub.setStatus(status);
        return imageHub;
    }

}
