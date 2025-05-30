package io.ten1010.aipub.projectcontroller.domain.k8s.util;

import io.ten1010.aipub.projectcontroller.domain.k8s.dto.V1alpha1Project;
import io.ten1010.aipub.projectcontroller.domain.k8s.dto.V1alpha1ProjectImageHub;

import java.util.List;

public abstract class ProjectImageHubUtils {

    public static V1alpha1ProjectImageHub copyOf(V1alpha1ProjectImageHub object) {
        V1alpha1ProjectImageHub copy = new V1alpha1ProjectImageHub();
        copy.setId(object.getId());
        copy.setName(object.getName());
        return copy;
    }

    public static List<V1alpha1ProjectImageHub> getProjectImageHubs(V1alpha1Project project) {
        return ProjectUtils.getSpecBindingImageHubs(project)
                .stream()
                .filter(e -> e.getName() != null)
                .toList();
    }

    public static List<V1alpha1ProjectImageHub> getProjectImageHubsWithId(V1alpha1Project project) {
        return ProjectUtils.getSpecBindingImageHubs(project)
                .stream()
                .filter(e -> e.getName() != null)
                .filter(e -> e.getId() != null)
                .toList();
    }

    public static List<V1alpha1ProjectImageHub> getProjectImageHubsWithoutId(V1alpha1Project project) {
        return ProjectUtils.getSpecBindingImageHubs(project)
                .stream()
                .filter(e -> e.getName() != null)
                .filter(e -> e.getId() == null)
                .toList();
    }

}
