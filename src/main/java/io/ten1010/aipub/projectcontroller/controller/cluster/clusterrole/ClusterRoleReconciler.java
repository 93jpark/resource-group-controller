package io.ten1010.aipub.projectcontroller.controller.cluster.clusterrole;

import io.kubernetes.client.extended.controller.reconciler.Reconciler;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.RbacAuthorizationV1Api;
import io.kubernetes.client.openapi.models.*;
import io.ten1010.aipub.projectcontroller.controller.KubernetesApiReconcileExceptionHandlingTemplate;
import io.ten1010.aipub.projectcontroller.core.K8sObjectUtil;
import io.ten1010.aipub.projectcontroller.core.KeyUtil;
import io.ten1010.aipub.projectcontroller.model.V1alpha1NodeGroup;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
public class ClusterRoleReconciler implements Reconciler {

    public static final Duration API_CONFLICT_REQUEUE_DURATION = Duration.ofSeconds(5);
    public static final Duration API_FAIL_REQUEUE_DURATION = Duration.ofSeconds(60);

    private static List<V1PolicyRule> buildRules(List<String> groups, List<String> nodes, List<String> namespaces) {
        V1PolicyRule groupApiRule = new V1PolicyRuleBuilder().withApiGroups("project.ten1010.io")
                .withResources("projects")
                .withResourceNames(groups)
                .withVerbs("get")
                .build();
        V1PolicyRule nodeApiRule = new V1PolicyRuleBuilder().withApiGroups("")
                .withResources("nodes")
                .withResourceNames(nodes)
                .withVerbs("get")
                .build();
        V1PolicyRule namespaceApiRule = new V1PolicyRuleBuilder().withApiGroups("")
                .withResources("namespaces")
                .withResourceNames(namespaces)
                .withVerbs("get")
                .build();

        return List.of(groupApiRule, nodeApiRule, namespaceApiRule);
    }

    private static List<V1PolicyRule> buildRules(V1alpha1NodeGroup group) {
        String groupName = K8sObjectUtil.getName(group);
        return buildRules(List.of(groupName), group.getNodes(), List.of());
    }

    private static List<V1PolicyRule> getRules(V1ClusterRole clusterRole) {
        return clusterRole.getRules() == null ? new ArrayList<>() : clusterRole.getRules();
    }

    private static V1OwnerReference buildOwnerReference(String groupName, String groupUid) {
        V1OwnerReferenceBuilder builder = new V1OwnerReferenceBuilder();
        return builder.withApiVersion("project.aipub/ten1010.io/v1alpha1")
                .withBlockOwnerDeletion(true)
                .withController(true)
                .withKind("NodeGroup")
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
    private Indexer<V1alpha1NodeGroup> groupIndexer;
    private Indexer<V1ClusterRole> clusterRoleIndexer;
    private RbacAuthorizationV1Api rbacAuthorizationV1Api;

    public ClusterRoleReconciler(
            Indexer<V1alpha1NodeGroup> groupIndexer,
            Indexer<V1ClusterRole> clusterRoleIndexer,
            RbacAuthorizationV1Api rbacAuthorizationV1Api) {
        this.template = new KubernetesApiReconcileExceptionHandlingTemplate(API_CONFLICT_REQUEUE_DURATION, API_FAIL_REQUEUE_DURATION);
        this.groupIndexer = groupIndexer;
        this.clusterRoleIndexer = clusterRoleIndexer;
        this.rbacAuthorizationV1Api = rbacAuthorizationV1Api;
    }

    @Override
    public Result reconcile(Request request) {
        return this.template.execute(
                () -> {
                    if (!ResourceGroupClusterRoleName.isResourceGroupClusterRoleName(request.getName())) {
                        return new Result(false);
                    }
                    String groupName = ResourceGroupClusterRoleName.fromClusterRoleName(request.getName()).getResourceGroupName();
                    V1alpha1NodeGroup group = this.groupIndexer.getByKey(groupName);
                    if (group == null) {
                        deleteClusterRoleIfExist(request.getName());
                        return new Result(false);
                    }
                    V1ClusterRole clusterRole = this.clusterRoleIndexer.getByKey(KeyUtil.buildKey(request.getName()));
                    List<V1PolicyRule> rules = buildRules(group);
                    if (clusterRole == null) {
                        createClusterRole(request.getName(), rules, buildOwnerReference(group));
                        return new Result(false);
                    }
                    if (getRules(clusterRole).equals(rules)) {
                        return new Result(false);
                    }
                    updateClusterRole(clusterRole, rules);
                    return new Result(false);
                },
                request);
    }

    private void createClusterRole(String name, List<V1PolicyRule> rules, V1OwnerReference ownerReference) throws ApiException {
        V1ClusterRoleBuilder builder = new V1ClusterRoleBuilder();
        V1ClusterRole clusterRole = builder.withNewMetadata()
                .withName(name)
                .withOwnerReferences(ownerReference)
                .endMetadata()
                .withRules(rules)
                .build();
        this.rbacAuthorizationV1Api.createClusterRole(clusterRole).execute();
    }

    private void updateClusterRole(V1ClusterRole target, List<V1PolicyRule> rules) throws ApiException {
        V1ClusterRoleBuilder builder = new V1ClusterRoleBuilder(target);
        V1ClusterRole updated = builder.withRules(rules)
                .build();
        V1ObjectMeta meta = target.getMetadata();
        Objects.requireNonNull(meta);
        Objects.requireNonNull(meta.getName());
        this.rbacAuthorizationV1Api.replaceClusterRole(meta.getName(), updated).execute();
    }

    private void deleteClusterRole(String name) throws ApiException {
        this.rbacAuthorizationV1Api.deleteClusterRole(name).execute();
    }

    private void deleteClusterRoleIfExist(String name) throws ApiException {
        V1ClusterRole clusterRole = this.clusterRoleIndexer.getByKey(KeyUtil.buildKey(name));
        if (clusterRole == null) {
            return;
        }
        deleteClusterRole(name);
    }

}
