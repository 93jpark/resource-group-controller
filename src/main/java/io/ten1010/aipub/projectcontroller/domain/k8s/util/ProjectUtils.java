package io.ten1010.aipub.projectcontroller.domain.k8s.util;

import io.ten1010.aipub.projectcontroller.domain.k8s.ProjectRoleEnum;
import io.ten1010.aipub.projectcontroller.domain.k8s.dto.V1alpha1Project;
import io.ten1010.aipub.projectcontroller.domain.k8s.dto.V1alpha1ProjectBinding;
import io.ten1010.aipub.projectcontroller.domain.k8s.dto.V1alpha1ProjectMember;
import io.ten1010.aipub.projectcontroller.domain.k8s.dto.V1alpha1ProjectSpecQuota;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public abstract class ProjectUtils {

    public static List<V1alpha1ProjectMember> getSpecMembers(V1alpha1Project object) {
        if (object.getSpec() == null ||
                object.getSpec().getMembers() == null) {
            return List.of();
        }
        return object.getSpec().getMembers();
    }

    public static List<V1alpha1ProjectMember> getSpecMembers(V1alpha1Project object, ProjectRoleEnum roleEnum) {
        String targetRole = roleEnum.getStr();
        return getSpecMembers(object).stream()
                .filter(e -> targetRole.equalsIgnoreCase(e.getRole()))
                .toList();
    }

    public static Optional<V1alpha1ProjectSpecQuota> getSpecQuota(V1alpha1Project object) {
        if (object.getSpec() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(object.getSpec().getQuota());
    }

    public static Optional<String> getSpecPvcStorageQuota(V1alpha1Project object) {
        Optional<V1alpha1ProjectSpecQuota> quotaOpt = getSpecQuota(object);
        return quotaOpt.map(V1alpha1ProjectSpecQuota::getPvcStorage);
    }

    public static Optional<V1alpha1ProjectBinding> getSpecBinding(V1alpha1Project object) {
        if (object.getSpec() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(object.getSpec().getBinding());
    }

    public static List<String> getSpecBindingNodeGroups(V1alpha1Project object) {
        Optional<V1alpha1ProjectBinding> bindingOpt = getSpecBinding(object);
        return bindingOpt
                .map(V1alpha1ProjectBinding::getNodeGroups)
                .filter(Objects::nonNull)
                .orElseGet(List::of);
    }

    public static List<String> getSpecBindingNodes(V1alpha1Project object) {
        Optional<V1alpha1ProjectBinding> bindingOpt = getSpecBinding(object);
        return bindingOpt
                .map(V1alpha1ProjectBinding::getNodes)
                .filter(Objects::nonNull)
                .orElseGet(List::of);
    }

    public static List<String> getSpecBindingImageNamespaces(V1alpha1Project object) {
        Optional<V1alpha1ProjectBinding> bindingOpt = getSpecBinding(object);
        return bindingOpt
                .map(V1alpha1ProjectBinding::getImageNamespaces)
                .filter(Objects::nonNull)
                .orElseGet(List::of);
    }

    public static List<String> getStatusAllBoundAipubUsers(V1alpha1Project object) {
        if (object.getStatus() == null ||
                object.getStatus().getAllBoundAipubUsers() == null) {
            return List.of();
        }
        return object.getStatus().getAllBoundAipubUsers();
    }

    public static List<String> getStatusAllBoundImageNamespaces(V1alpha1Project object) {
        if (object.getStatus() == null ||
                object.getStatus().getAllBoundImageNamespaces() == null) {
            return List.of();
        }
        return object.getStatus().getAllBoundImageNamespaces();
    }

}
