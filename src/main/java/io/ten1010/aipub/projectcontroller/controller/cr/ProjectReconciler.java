package io.ten1010.aipub.projectcontroller.controller.cr;

import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1ResourceQuota;
import io.ten1010.aipub.projectcontroller.controller.BoundObjectResolver;
import io.ten1010.aipub.projectcontroller.controller.ReconcilerSupport;
import io.ten1010.aipub.projectcontroller.controller.RequestHelper;
import io.ten1010.aipub.projectcontroller.domain.k8s.*;
import io.ten1010.aipub.projectcontroller.domain.k8s.dto.*;
import io.ten1010.aipub.projectcontroller.domain.k8s.util.K8sObjectUtils;
import io.ten1010.aipub.projectcontroller.domain.k8s.util.NodeUtils;
import io.ten1010.aipub.projectcontroller.domain.k8s.util.StatusPatchHelper;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ProjectReconciler extends ReconcilerSupport {

    private final KeyResolver keyResolver;
    private final ReconciliationService reconciliationService;
    private final Indexer<V1alpha1Project> projectIndexer;
    private final Indexer<V1ResourceQuota> quotaIndexer;
    private final BoundObjectResolver boundObjectResolver;
    private final StatusPatchHelper statusPatchHelper;

    public ProjectReconciler(
            SharedInformerFactory sharedInformerFactory,
            K8sApiProvider k8sApiProvider,
            ReconciliationService reconciliationService) {
        this.keyResolver = new KeyResolver();
        this.reconciliationService = reconciliationService;
        this.projectIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1alpha1Project.class)
                .getIndexer();
        this.quotaIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1ResourceQuota.class)
                .getIndexer();
        this.boundObjectResolver = new BoundObjectResolver(sharedInformerFactory);
        this.statusPatchHelper = new StatusPatchHelper<>(
                k8sApiProvider.getApiClient(),
                K8sObjectTypeConstants.PROJECT_V1ALPHA1,
                ProjectApiConstants.PROJECT_RESOURCE_PLURAL);
    }

    @Override
    protected Result reconcileInternal(Request request) throws ApiException {
        String projectKey = new RequestHelper(this.keyResolver).resolveKey(request);
        Optional<V1alpha1Project> projectOpt = Optional.ofNullable(this.projectIndexer.getByKey(projectKey));
        if (projectOpt.isEmpty()) {
            return new Result(false);
        }
        V1alpha1Project project = projectOpt.get();

        V1ResourceQuota boundQuota = getBoundResourceQuota(project);
        List<V1alpha1AipubUser> boundAipubUsers = this.boundObjectResolver.getAllBoundAipubUsers(project);
        List<V1alpha1NodeGroup> boundNodeGroups = this.boundObjectResolver.getAllBoundNodeGroups(project);
        List<V1Node> boundNodes = this.boundObjectResolver.getAllBoundNodes(project);
        boundNodes = NodeUtils.getProjectManagedNodes(boundNodes);
        List<V1alpha1ImageNamespace> boundImageNamespaces = this.boundObjectResolver.getAllBoundImageNamespaces(project);
        V1alpha1ProjectStatus reconciledStatus = this.reconciliationService.reconcileProjectStatus(
                project, boundAipubUsers, boundQuota, boundNodeGroups, boundNodes, boundImageNamespaces);

        return reconcileExistingProject(project, reconciledStatus);
    }

    private Result reconcileExistingProject(V1alpha1Project project, V1alpha1ProjectStatus reconciledStatus) throws ApiException {
        if (Objects.equals(project.getStatus(), reconciledStatus)) {
            return new Result(false);
        }

        V1alpha1Project edited = new V1alpha1Project();
        edited.setApiVersion(project.getApiVersion());
        edited.setKind(project.getKind());
        edited.setMetadata(project.getMetadata());
        edited.setSpec(project.getSpec());
        edited.setStatus(reconciledStatus);
        updateProjectStatus(edited);
        return new Result(false);
    }

    private void updateProjectStatus(V1alpha1Project project) throws ApiException {
        Objects.requireNonNull(project.getStatus());
        this.statusPatchHelper.patchStatus(null, K8sObjectUtils.getName(project), project.getStatus());
    }

    @Nullable
    private V1ResourceQuota getBoundResourceQuota(V1alpha1Project project) {
        String projName = K8sObjectUtils.getName(project);
        String quotaName = new ResourceQuotaNameResolver().resolveQuotaName(projName);
        String quotaKey = this.keyResolver.resolveKey(projName, quotaName);
        return this.quotaIndexer.getByKey(quotaKey);
    }

}
