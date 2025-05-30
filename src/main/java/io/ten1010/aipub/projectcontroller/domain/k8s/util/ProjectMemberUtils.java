package io.ten1010.aipub.projectcontroller.domain.k8s.util;

import io.ten1010.aipub.projectcontroller.domain.k8s.dto.V1alpha1Project;
import io.ten1010.aipub.projectcontroller.domain.k8s.dto.V1alpha1ProjectMember;

import java.util.List;

public abstract class ProjectMemberUtils {

    public static V1alpha1ProjectMember copyOf(V1alpha1ProjectMember object) {
        V1alpha1ProjectMember copy = new V1alpha1ProjectMember();
        copy.setId(object.getId());
        copy.setAipubUser(object.getAipubUser());
        copy.setSubject(object.getSubject());
        copy.setRole(object.getRole());
        return copy;
    }

    public static List<V1alpha1ProjectMember> getAipubUserMembers(V1alpha1Project project) {
        return ProjectUtils.getSpecMembers(project)
                .stream()
                .filter(e -> e.getSubject() == null)
                .filter(e -> e.getAipubUser() != null)
                .toList();
    }

    public static List<V1alpha1ProjectMember> getAipubUserMembersWithId(V1alpha1Project project) {
        return ProjectUtils.getSpecMembers(project)
                .stream()
                .filter(e -> e.getSubject() == null)
                .filter(e -> e.getAipubUser() != null)
                .filter(e -> e.getId() != null)
                .toList();
    }

    public static List<V1alpha1ProjectMember> getAipubUserMembersWithoutId(V1alpha1Project project) {
        return ProjectUtils.getSpecMembers(project)
                .stream()
                .filter(e -> e.getSubject() == null)
                .filter(e -> e.getAipubUser() != null)
                .filter(e -> e.getId() == null)
                .toList();
    }

    public static List<V1alpha1ProjectMember> getSubjectMembers(V1alpha1Project project) {
        return ProjectUtils.getSpecMembers(project)
                .stream()
                .filter(e -> e.getAipubUser() == null)
                .filter(e -> e.getSubject() != null)
                .toList();
    }

}
