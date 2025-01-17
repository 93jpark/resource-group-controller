package io.ten1010.aipub.projectcontroller.controller.watch;

import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.*;
import io.ten1010.aipub.projectcontroller.domain.k8s.dto.V1alpha1AipubUser;
import io.ten1010.aipub.projectcontroller.domain.k8s.dto.V1alpha1ImageNamespace;
import io.ten1010.aipub.projectcontroller.domain.k8s.dto.V1alpha1NodeGroup;
import io.ten1010.aipub.projectcontroller.domain.k8s.dto.V1alpha1Project;
import io.ten1010.aipub.projectcontroller.domain.k8s.util.*;

import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;

public class OnUpdateFilterFactory {

    /**
     * 항상 true 리턴
     * @return
     * @param <T>
     */
    public <T extends KubernetesObject> BiPredicate<T, T> alwaysTrueFilter() {
        return (oldObj, newObj) -> true;
    }

    public <T extends KubernetesObject> BiPredicate<T, T> alwaysFalseFilter() {
        return (oldObj, newObj) -> true;
    }

    /**
     * OwnerReference가 다른 경우 true
     * @return
     * @param <T>
     */
    public <T extends KubernetesObject> BiPredicate<T, T> ownerReferencesFilter() {
        return (oldObj, newObj) -> !K8sObjectUtils.getOwnerReferences(oldObj).equals(K8sObjectUtils.getOwnerReferences(newObj));
    }

    /**
     * project의 spec이 다른 경우 true. 즉, 사용자가 project에 대한 정보를 변경한 경우.
     * @return
     */
    public BiPredicate<V1alpha1Project, V1alpha1Project> projectSpecFieldFilter() {
        return (oldObj, newObj) -> !Objects.equals(oldObj.getSpec(), newObj.getSpec());
    }

    /**
     * project의 quota 다른 경우 true. 즉, project의 상태가 변경된 경우.
     * @return
     */
    public BiPredicate<V1alpha1Project, V1alpha1Project> projectSpecQuotaFieldFilter() {
        return (oldObj, newObj) -> !ProjectUtils.getSpecQuota(oldObj).equals(ProjectUtils.getSpecQuota(newObj));
    }

    /**
     * project의 binding(node, imageNs, nodeGroup)이 변경된 경우
     * @return
     */
    public BiPredicate<V1alpha1Project, V1alpha1Project> projectSpecBindingFieldFilter() {
        return (oldObj, newObj) -> !ProjectUtils.getSpecBinding(oldObj).equals(ProjectUtils.getSpecBinding(newObj));
    }

    /**
     * bound된 AipubUser가 변경된 경우
     * @return
     */
    public BiPredicate<V1alpha1Project, V1alpha1Project> projectStatusAllBoundAipubUsersFieldFilter() {
        return (oldObj, newObj) -> !ProjectUtils.getStatusAllBoundAipubUsers(oldObj).equals(ProjectUtils.getStatusAllBoundAipubUsers(newObj));
    }

    /**
     * bound된 ImageNamespace가 변경된 경우
     * @return
     */
    public BiPredicate<V1alpha1Project, V1alpha1Project> projectStatusAllBoundImageNamespacesFieldFilter() {
        return (oldObj, newObj) -> !ProjectUtils.getStatusAllBoundImageNamespaces(oldObj).equals(ProjectUtils.getStatusAllBoundImageNamespaces(newObj));
    }

    /**
     * AipubUser의 spec이 변경된 경우. AipubUserSpec은 id임
     * @return
     */
    public BiPredicate<V1alpha1AipubUser, V1alpha1AipubUser> aipubUserSpecFieldFilter() {
        return (oldObj, newObj) -> !Objects.equals(oldObj.getSpec(), newObj.getSpec());
    }

    /**
     * nodeGroup의 spec(policy, nodes, 등)이 변경된 경우
     * @return
     */
    public BiPredicate<V1alpha1NodeGroup, V1alpha1NodeGroup> nodeGroupSpecFieldFilter() {
        return (oldObj, newObj) -> !Objects.equals(oldObj.getSpec(), newObj.getSpec());
    }

    /**
     * imageNamespace id가 변경된 경우
     * @return
     */
    public BiPredicate<V1alpha1ImageNamespace, V1alpha1ImageNamespace> imageNamespaceSpecFieldFilter() {
        return (oldObj, newObj) -> !Objects.equals(oldObj.getSpec(), newObj.getSpec());
    }

    public BiPredicate<V1Node, V1Node> nodeFilter() {
        return (oldObj, newObj) -> !K8sObjectUtils.getLabels(oldObj).equals(K8sObjectUtils.getLabels(newObj)) ||
                !K8sObjectUtils.getAnnotations(oldObj).equals(K8sObjectUtils.getAnnotations(newObj)) ||
                !Set.copyOf(NodeUtils.getTaints(oldObj)).equals(Set.copyOf(NodeUtils.getTaints(newObj)));
    }

    public BiPredicate<V1ClusterRole, V1ClusterRole> clusterRoleFilter() {
        return (oldObj, newObj) -> !Set.copyOf(K8sObjectUtils.getOwnerReferences(oldObj)).equals(Set.copyOf(K8sObjectUtils.getOwnerReferences(newObj))) ||
                !Set.copyOf(RoleUtils.getRules(oldObj)).equals(Set.copyOf(RoleUtils.getRules(newObj))) ||
                !Objects.equals(oldObj.getAggregationRule(), newObj.getAggregationRule());
    }

    public BiPredicate<V1ClusterRoleBinding, V1ClusterRoleBinding> clusterRoleBindingFilter() {
        return (oldObj, newObj) -> !Set.copyOf(K8sObjectUtils.getOwnerReferences(oldObj)).equals(Set.copyOf(K8sObjectUtils.getOwnerReferences(newObj))) ||
                !oldObj.getRoleRef().equals(newObj.getRoleRef()) ||
                !Set.copyOf(RoleUtils.getSubjects(oldObj)).equals(Set.copyOf(RoleUtils.getSubjects(newObj)));
    }

    public BiPredicate<V1Role, V1Role> roleFilter() {
        return (oldObj, newObj) -> !Set.copyOf(K8sObjectUtils.getOwnerReferences(oldObj)).equals(Set.copyOf(K8sObjectUtils.getOwnerReferences(newObj))) ||
                !Set.copyOf(RoleUtils.getRules(oldObj)).equals(Set.copyOf(RoleUtils.getRules(newObj)));
    }

    public BiPredicate<V1RoleBinding, V1RoleBinding> roleBindingFilter() {
        return (oldObj, newObj) -> !Set.copyOf(K8sObjectUtils.getOwnerReferences(oldObj)).equals(Set.copyOf(K8sObjectUtils.getOwnerReferences(newObj))) ||
                !oldObj.getRoleRef().equals(newObj.getRoleRef()) ||
                !Set.copyOf(RoleUtils.getSubjects(oldObj)).equals(Set.copyOf(RoleUtils.getSubjects(newObj)));
    }

    public BiPredicate<V1ResourceQuota, V1ResourceQuota> resourceQuotaFilter() {
        return (oldObj, newObj) -> !Set.copyOf(K8sObjectUtils.getOwnerReferences(oldObj)).equals(Set.copyOf(K8sObjectUtils.getOwnerReferences(newObj))) ||
                !Objects.equals(oldObj.getSpec(), newObj.getSpec());
    }

    public BiPredicate<V1ResourceQuota, V1ResourceQuota> resourceQuotaStatusFieldFilter() {
        return (oldObj, newObj) -> !Objects.equals(oldObj.getStatus(), newObj.getStatus());
    }

    public BiPredicate<V1Pod, V1Pod> podNodeNameFieldFilter() {
        return (oldObj, newObj) -> !Set.copyOf(K8sObjectUtils.getOwnerReferences(oldObj)).equals(Set.copyOf(K8sObjectUtils.getOwnerReferences(newObj))) ||
                !WorkloadUtils.getNodeName(oldObj).equals(WorkloadUtils.getNodeName(newObj));
    }

    public BiPredicate<V1CronJob, V1CronJob> cronJobFilter() {
        return (oldObj, newObj) -> !K8sObjectUtils.getOwnerReferences(oldObj).equals(K8sObjectUtils.getOwnerReferences(newObj)) ||
                !WorkloadUtils.getPodTemplateSpec(oldObj).equals(WorkloadUtils.getPodTemplateSpec(newObj));
    }

    public BiPredicate<V1DaemonSet, V1DaemonSet> daemonSetFilter() {
        return (oldObj, newObj) -> !K8sObjectUtils.getOwnerReferences(oldObj).equals(K8sObjectUtils.getOwnerReferences(newObj)) ||
                !WorkloadUtils.getPodTemplateSpec(oldObj).equals(WorkloadUtils.getPodTemplateSpec(newObj));
    }

    public BiPredicate<V1Deployment, V1Deployment> deploymentFilter() {
        return (oldObj, newObj) -> !K8sObjectUtils.getOwnerReferences(oldObj).equals(K8sObjectUtils.getOwnerReferences(newObj)) ||
                !WorkloadUtils.getPodTemplateSpec(oldObj).equals(WorkloadUtils.getPodTemplateSpec(newObj));
    }

    public BiPredicate<V1Job, V1Job> jobFilter() {
        return (oldObj, newObj) -> !K8sObjectUtils.getOwnerReferences(oldObj).equals(K8sObjectUtils.getOwnerReferences(newObj)) ||
                !WorkloadUtils.getPodTemplateSpec(oldObj).equals(WorkloadUtils.getPodTemplateSpec(newObj));
    }

    public BiPredicate<V1ReplicaSet, V1ReplicaSet> replicaSetFilter() {
        return (oldObj, newObj) -> !K8sObjectUtils.getOwnerReferences(oldObj).equals(K8sObjectUtils.getOwnerReferences(newObj)) ||
                !WorkloadUtils.getPodTemplateSpec(oldObj).equals(WorkloadUtils.getPodTemplateSpec(newObj));
    }

    public BiPredicate<V1StatefulSet, V1StatefulSet> statefulSetFilter() {
        return (oldObj, newObj) -> !K8sObjectUtils.getOwnerReferences(oldObj).equals(K8sObjectUtils.getOwnerReferences(newObj)) ||
                !WorkloadUtils.getPodTemplateSpec(oldObj).equals(WorkloadUtils.getPodTemplateSpec(newObj));
    }

}
