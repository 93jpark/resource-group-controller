package io.ten1010.aipub.projectcontroller.controller.cluster.rolebinding;

import io.kubernetes.client.extended.controller.reconciler.Reconciler;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.RbacAuthorizationV1Api;
import io.kubernetes.client.openapi.models.*;
import io.ten1010.aipub.projectcontroller.controller.KubernetesApiReconcileExceptionHandlingTemplate;
import io.ten1010.aipub.projectcontroller.controller.cluster.role.ResourceGroupRoleName;
import io.ten1010.aipub.projectcontroller.core.KeyUtil;
import io.ten1010.aipub.projectcontroller.core.NodeGroupUtil;
import io.ten1010.aipub.projectcontroller.model.V1alpha1NodeGroup;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class RoleBindingReconciler implements Reconciler {

    public static final Duration API_CONFLICT_REQUEUE_DURATION = Duration.ofSeconds(5);
    public static final Duration API_FAIL_REQUEUE_DURATION = Duration.ofSeconds(60);

    private static V1RoleRef buildRoleRef(String roleName) {
        return new V1RoleRefBuilder()
                .withApiGroup("rbac.authorization.k8s.io")
                .withKind("Role")
                .withName(roleName)
                .build();
    }

    private static Optional<V1RoleRef> getRoleRef(V1RoleBinding roleBinding) {
        return Optional.ofNullable(roleBinding.getRoleRef());
    }

    private static List<RbacV1Subject> getSubjects(V1RoleBinding roleBinding) {
        return roleBinding.getSubjects() == null ? new ArrayList<>() : roleBinding.getSubjects();
    }

    private static V1OwnerReference buildOwnerReference(String groupName, String groupUid) {
        V1OwnerReferenceBuilder builder = new V1OwnerReferenceBuilder();
        return builder.withApiVersion("project.aipub/ten1010.io/v1alpha1")
                .withBlockOwnerDeletion(true)
                .withController(true)
                .withKind("nodegroup")
                .withName(groupName)
                .withUid(groupUid)
                .build();
    }

    private static V1OwnerReference buildOwnerReference(V1alpha1NodeGroup group) {
        Objects.requireNonNull(group.getMetadata());
        Objects.requireNonNull(group.getMetadata().getName());
        Objects.requireNonNull(group.getMetadata().getUid());
        return buildOwnerReference(group.getMetadata().getName(), group.getMetadata().getUid());
    }

    private KubernetesApiReconcileExceptionHandlingTemplate template;
    private Indexer<V1Namespace> namespaceIndexer;
    private Indexer<V1alpha1NodeGroup> groupIndexer;
    private Indexer<V1RoleBinding> roleBindingIndexer;
    private Indexer<V1Role> roleIndexer;
    private RbacAuthorizationV1Api rbacAuthorizationV1Api;

    public RoleBindingReconciler(
            Indexer<V1Namespace> namespaceIndexer,
            Indexer<V1alpha1NodeGroup> groupIndexer,
            Indexer<V1RoleBinding> roleBindingIndexer,
            Indexer<V1Role> roleIndexer,
            RbacAuthorizationV1Api rbacAuthorizationV1Api) {
        this.template = new KubernetesApiReconcileExceptionHandlingTemplate(API_CONFLICT_REQUEUE_DURATION, API_FAIL_REQUEUE_DURATION);
        this.namespaceIndexer = namespaceIndexer;
        this.groupIndexer = groupIndexer;
        this.roleBindingIndexer = roleBindingIndexer;
        this.roleIndexer = roleIndexer;
        this.rbacAuthorizationV1Api = rbacAuthorizationV1Api;
    }

    @Override
    public Result reconcile(Request request) {
        return this.template.execute(
                () -> {
                    if (!ResourceGroupRoleBindingName.isResourceGroupRoleBindingName(request.getName())) {
                        return new Result(false);
                    }
                    V1Namespace namespace = this.namespaceIndexer.getByKey(KeyUtil.buildKey(request.getNamespace()));
                    if (namespace == null) {
                        return new Result(false);
                    }
                    String groupName = ResourceGroupRoleBindingName.fromRoleBindingName(request.getName()).getResourceGroupName();
                    V1alpha1NodeGroup group = this.groupIndexer.getByKey(groupName);
                    if (group == null) {
                        deleteRoleBindingIfExist(request.getNamespace(), request.getName());
                        return new Result(false);
                    }
//                    Objects.requireNonNull(group.getSpec());
//                    List<String> namespacesInGroup = group.getSpec().getNamespaces();
                    List<String> namespacesInGroup = NodeGroupUtil.getNamespaces(group);
                    if (!namespacesInGroup.contains(request.getNamespace())) {
                        deleteRoleBindingIfExist(request.getNamespace(), request.getName());
                        return new Result(false);
                    }
                    String roleName = new ResourceGroupRoleName(groupName).getName();
                    V1RoleRef roleRef = buildRoleRef(roleName);
//                    List<RbacV1Subject> subjects = group.getSpec().getSubjects();
                    List<RbacV1Subject> subjects = new ArrayList<>();
                    V1RoleBinding roleBinding = this.roleBindingIndexer.getByKey(KeyUtil.buildKey(request.getNamespace(), request.getName()));
                    if (roleBinding == null) {
                        String roleKey = KeyUtil.buildKey(request.getNamespace(), roleRef.getName());
                        V1Role role = this.roleIndexer.getByKey(roleKey);
                        if (role == null) {
                            return new Result(true, Duration.ofSeconds(3));
                        }
                        Objects.requireNonNull(group.getMetadata());
                        createRoleBinding(request.getNamespace(), request.getName(), roleRef, subjects, buildOwnerReference(group));
                        return new Result(false);
                    }
                    Optional<V1RoleRef> roleRefOpt = getRoleRef(roleBinding);
                    if (roleRefOpt.isPresent() && roleRefOpt.get().equals(roleRef) && getSubjects(roleBinding).equals(subjects)) {
                        return new Result(false);
                    }
                    updateRoleBinding(roleBinding, roleRef, subjects);
                    return new Result(false);
                },
                request);
    }

    private void createRoleBinding(String namespace, String name, V1RoleRef roleRef, List<RbacV1Subject> subjects, V1OwnerReference ownerReference) throws ApiException {
        V1RoleBindingBuilder builder = new V1RoleBindingBuilder();
        V1RoleBinding roleBinding = builder.withNewMetadata()
                .withNamespace(namespace)
                .withName(name)
                .withOwnerReferences(ownerReference)
                .endMetadata()
                .withRoleRef(roleRef)
                .withSubjects(subjects)
                .build();
        this.rbacAuthorizationV1Api.createNamespacedRoleBinding(namespace, roleBinding).execute();
    }

    private void updateRoleBinding(V1RoleBinding target, V1RoleRef roleRef, List<RbacV1Subject> subjects) throws ApiException {
        V1RoleBindingBuilder builder = new V1RoleBindingBuilder(target);
        V1RoleBinding updated = builder
                .withRoleRef(roleRef)
                .withSubjects(subjects)
                .build();
        V1ObjectMeta meta = target.getMetadata();
        Objects.requireNonNull(meta);
        Objects.requireNonNull(meta.getNamespace());
        Objects.requireNonNull(meta.getName());
        this.rbacAuthorizationV1Api.replaceNamespacedRoleBinding(meta.getName(), meta.getNamespace(), updated).execute();
    }

    private void deleteRoleBinding(String namespace, String name) throws ApiException {
        this.rbacAuthorizationV1Api.deleteNamespacedRoleBinding(name, namespace)
                .execute();
    }

    private void deleteRoleBindingIfExist(String namespace, String name) throws ApiException {
        V1RoleBinding roleBinding = this.roleBindingIndexer.getByKey(KeyUtil.buildKey(namespace, name));
        if (roleBinding == null) {
            return;
        }
        deleteRoleBinding(namespace, name);
    }

}
