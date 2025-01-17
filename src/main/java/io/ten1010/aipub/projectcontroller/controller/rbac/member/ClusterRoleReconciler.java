package io.ten1010.aipub.projectcontroller.controller.rbac.member;

import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.RbacAuthorizationV1Api;
import io.kubernetes.client.openapi.models.*;
import io.ten1010.aipub.projectcontroller.controller.BoundObjectResolver;
import io.ten1010.aipub.projectcontroller.controller.ReconcilerSupport;
import io.ten1010.aipub.projectcontroller.controller.RequestHelper;
import io.ten1010.aipub.projectcontroller.domain.k8s.*;
import io.ten1010.aipub.projectcontroller.domain.k8s.dto.V1alpha1NodeGroup;
import io.ten1010.aipub.projectcontroller.domain.k8s.dto.V1alpha1Project;
import io.ten1010.aipub.projectcontroller.domain.k8s.util.K8sObjectUtils;
import io.ten1010.aipub.projectcontroller.domain.k8s.util.NodeUtils;
import io.ten1010.aipub.projectcontroller.domain.k8s.util.RoleUtils;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class ClusterRoleReconciler extends ReconcilerSupport {

    private final KeyResolver keyResolver;
    private final RoleNameResolver roleNameResolver;
    private final ReconciliationService reconciliationService;
    private final Indexer<V1ClusterRole> clusterRoleIndexer;
    private final Indexer<V1alpha1Project> projectIndexer;
    private final BoundObjectResolver boundObjectResolver;
    private final RbacAuthorizationV1Api rbacAuthorizationV1Api;

    public ClusterRoleReconciler(
            SharedInformerFactory sharedInformerFactory,
            K8sApiProvider k8sApiProvider,
            ReconciliationService reconciliationService) {
        this.keyResolver = new KeyResolver();
        this.roleNameResolver = new RoleNameResolver();
        this.reconciliationService = reconciliationService;
        this.clusterRoleIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1ClusterRole.class)
                .getIndexer();
        this.projectIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1alpha1Project.class)
                .getIndexer();
        this.boundObjectResolver = new BoundObjectResolver(sharedInformerFactory);
        this.rbacAuthorizationV1Api = new RbacAuthorizationV1Api(k8sApiProvider.getApiClient());
    }

    @Override
    protected Result reconcileInternal(Request request) throws ApiException {
        Optional<ProjectNameAndRole> projNameOpt = this.roleNameResolver.resolveProjectName(request.getName());
        if (projNameOpt.isEmpty()) {
            return new Result(false);
        }
        String projName = projNameOpt.get().getProjectName();
        ProjectRoleEnum projRoleEnum = projNameOpt.get().getProjectRoleEnum();

        String roleKey = new RequestHelper(this.keyResolver).resolveKey(request);
        Optional<V1ClusterRole> roleOpt = Optional.ofNullable(this.clusterRoleIndexer.getByKey(roleKey));
        String projKey = this.keyResolver.resolveKey(projName);
        Optional<V1alpha1Project> projectOpt = Optional.ofNullable(this.projectIndexer.getByKey(projKey));

        if (projectOpt.isEmpty()) {
            if (roleOpt.isPresent()) {
                V1ClusterRole role = roleOpt.get();
                deleteRole(K8sObjectUtils.getName(role));
                return new Result(false);
            }
            return new Result(false);
        }

        List<V1OwnerReference> reconciledReferences = this.reconciliationService.reconcileOwnerReferences(roleOpt.orElse(null), projectOpt.get());
        List<V1alpha1NodeGroup> boundNodeGroups = this.boundObjectResolver.getAllBoundNodeGroups(projectOpt.get());
        List<V1Node> boundNodes = this.boundObjectResolver.getAllBoundNodes(projectOpt.get());
        boundNodes = NodeUtils.getProjectManagedNodes(boundNodes);
        List<V1PolicyRule> reconciledRules = this.reconciliationService.reconcileClusterRoleRules(projectOpt.get(), projRoleEnum, boundNodeGroups, boundNodes);
        V1AggregationRule reconciledAggregationRule = this.reconciliationService.reconcileClusterRoleAggregationRule(projectOpt.get(), projRoleEnum);

        if (roleOpt.isPresent()) {
            return reconcileExistingRole(roleOpt.get(), reconciledReferences, reconciledRules, reconciledAggregationRule);
        }

        return reconcileNoExistingRole(request.getName(), reconciledReferences, reconciledRules, reconciledAggregationRule);
    }

    private Result reconcileNoExistingRole(
            String objName,
            List<V1OwnerReference> reconciledReferences,
            List<V1PolicyRule> reconciledRules,
            @Nullable V1AggregationRule reconciledAggregationRule) throws ApiException {
        V1ClusterRole role = new V1ClusterRoleBuilder()
                .withNewMetadata()
                .withName(objName)
                .withOwnerReferences(reconciledReferences)
                .endMetadata()
                .withRules(reconciledRules)
                .withAggregationRule(reconciledAggregationRule)
                .build();
        createRole(role);
        return new Result(false);
    }

    private Result reconcileExistingRole(
            V1ClusterRole clusterRole,
            List<V1OwnerReference> reconciledReferences,
            List<V1PolicyRule> reconciledRules,
            @Nullable V1AggregationRule reconciledAggregationRule) throws ApiException {
        if (Set.copyOf(K8sObjectUtils.getOwnerReferences(clusterRole)).equals(Set.copyOf(reconciledReferences)) &&
                RoleUtils.getRules(clusterRole).equals(reconciledRules) &&
                Objects.equals(clusterRole.getAggregationRule(), reconciledAggregationRule)) {
            return new Result(false);
        }
        V1ClusterRole edited = new V1ClusterRoleBuilder(clusterRole)
                .editMetadata()
                .withOwnerReferences(reconciledReferences)
                .endMetadata()
                .withRules(reconciledRules)
                .withAggregationRule(reconciledAggregationRule)
                .build();
        updateRole(K8sObjectUtils.getName(clusterRole), edited);
        return new Result(false);
    }

    private void createRole(V1ClusterRole clusterRole) throws ApiException {
        this.rbacAuthorizationV1Api
                .createClusterRole(clusterRole)
                .execute();
    }

    private void updateRole(String objName, V1ClusterRole clusterRole) throws ApiException {
        this.rbacAuthorizationV1Api
                .replaceClusterRole(objName, clusterRole)
                .execute();
    }

    private void deleteRole(String objName) throws ApiException {
        this.rbacAuthorizationV1Api
                .deleteClusterRole(objName)
                .execute();
    }

}
