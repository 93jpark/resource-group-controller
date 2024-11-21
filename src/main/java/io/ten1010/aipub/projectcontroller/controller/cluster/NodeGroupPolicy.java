package io.ten1010.aipub.projectcontroller.controller.cluster;

import io.ten1010.aipub.projectcontroller.core.K8sObjectUtil;
import io.ten1010.aipub.projectcontroller.core.KeyUtil;
import io.ten1010.aipub.projectcontroller.model.V1alpha1NodeGroup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter
@Setter
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class NodeGroupPolicy {

    private String nodeGroupName;
    private String nodeGroupNamespace;
    private List<String> groupNodes;
    private boolean allowAllDaemonSets;
    private List<String> allowedDaemonSetNamespaces;
    private List<String> allowedDaemonSetKeys;

    public static NodeGroupPolicy from(V1alpha1NodeGroup nodeGroup) {
        return NodeGroupPolicy.builder()
                .nodeGroupName(K8sObjectUtil.getName(nodeGroup))
                .nodeGroupNamespace(K8sObjectUtil.getNamespace(nodeGroup))
                .groupNodes(nodeGroup.getNodes())
                .allowAllDaemonSets(isAllDaemonSetAllowed(nodeGroup))
                .allowedDaemonSetKeys(getAllowedDaemonSets(nodeGroup))
                .allowedDaemonSetNamespaces(getAllowedNamespaces(nodeGroup))
                .build();
    }

    private static List<String> getAllowedDaemonSets(V1alpha1NodeGroup nodeGroup) {
        if (nodeGroup.getPolicy() == null) {
            return new ArrayList<>();
        }
        if (nodeGroup.getPolicy().getDaemonSet() == null) {
            return new ArrayList<>();
        }
        if (nodeGroup.getPolicy().getDaemonSet().getAllowedDaemonSets() == null) {
            return new ArrayList<>();
        }
        return nodeGroup.getPolicy().getDaemonSet().getAllowedDaemonSets().stream()
                .map(allowedDaemonSet -> {
                    Objects.requireNonNull(allowedDaemonSet.getName(), "allowedDaemonSet.name must not be null");
                    Objects.requireNonNull(allowedDaemonSet.getNamespace(), "allowedDaemonSet.namespace must not be null");
                    return KeyUtil.buildKey(allowedDaemonSet.getNamespace(), allowedDaemonSet.getName());
                })
                .collect(Collectors.toList());
    }

    private static boolean isAllDaemonSetAllowed(V1alpha1NodeGroup nodeGroup) {
        if (nodeGroup.getPolicy() == null) {
            return false;
        }
        if (nodeGroup.getPolicy().getDaemonSet() == null) {
            return false;
        }
        if (nodeGroup.getPolicy().getDaemonSet().getAllowedDaemonSets() == null) {
            return false;
        }
        return nodeGroup.getPolicy().getDaemonSet().getAllowAllDaemonSets();
    }

    private static List<String> getAllowedNamespaces(V1alpha1NodeGroup nodeGroup) {
        if (nodeGroup.getPolicy() == null) {
            return new ArrayList<>();
        }
        if (nodeGroup.getPolicy().getDaemonSet() == null) {
            return new ArrayList<>();
        }
        if (nodeGroup.getPolicy().getDaemonSet().getAllowedNamespaces() == null) {
            return new ArrayList<>();
        }
        return nodeGroup.getPolicy().getDaemonSet().getAllowedNamespaces();
    }

}
