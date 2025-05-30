package io.ten1010.aipub.projectcontroller.controller.cr;

import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1ResourceQuota;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import io.ten1010.aipub.projectcontroller.controller.AbstractReconciler;
import io.ten1010.aipub.projectcontroller.controller.BoundObjectResolver;
import io.ten1010.aipub.projectcontroller.controller.RequestHelper;
import io.ten1010.aipub.projectcontroller.domain.k8s.*;
import io.ten1010.aipub.projectcontroller.domain.k8s.dto.*;
import io.ten1010.aipub.projectcontroller.domain.k8s.util.*;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.*;

public class ProjectReconciler extends AbstractReconciler {

    private final ReconciliationService reconciliationService;
    private final Indexer<V1alpha1Project> projectIndexer;
    private final Indexer<V1Namespace> namespaceIndexer;
    private final Indexer<V1ResourceQuota> quotaIndexer;
    private final BoundObjectResolver boundObjectResolver;
    private final GenericKubernetesApi<V1alpha1Project, V1alpha1ProjectList> projectApi;
    private final ObjectPatchHelper<V1alpha1Project> objectPatchHelper;
    private final SpecPatchHelper<V1alpha1Project> specPatchHelper;
    private final KeyResolver keyResolver;
    private final NamespaceNameResolver namespaceNameResolver;

    public ProjectReconciler(
            ReconciliationService reconciliationService,
            SharedInformerFactory sharedInformerFactory,
            K8sApiProvider k8sApiProvider) {
        this.reconciliationService = reconciliationService;
        this.projectIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1alpha1Project.class)
                .getIndexer();
        this.namespaceIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1Namespace.class)
                .getIndexer();
        this.quotaIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1ResourceQuota.class)
                .getIndexer();
        this.boundObjectResolver = new BoundObjectResolver(sharedInformerFactory);
        this.projectApi = k8sApiProvider.getProjectApi();
        this.objectPatchHelper = new ObjectPatchHelper<>(
                k8sApiProvider.getApiClient(),
                K8sObjectTypeConstants.PROJECT_V1ALPHA1,
                ProjectApiConstants.PROJECT_RESOURCE_PLURAL);
        this.specPatchHelper = new SpecPatchHelper<>(
                k8sApiProvider.getApiClient(),
                K8sObjectTypeConstants.PROJECT_V1ALPHA1,
                ProjectApiConstants.PROJECT_RESOURCE_PLURAL);
        this.keyResolver = new KeyResolver();
        this.namespaceNameResolver = new NamespaceNameResolver();
    }

    @Override
    protected Result reconcileInternal(Request request) throws ApiException {
        String projectKey = new RequestHelper(this.keyResolver).resolveKey(request);
        Optional<V1alpha1Project> projectOpt = Optional.ofNullable(this.projectIndexer.getByKey(projectKey));
        if (projectOpt.isEmpty()) {
            return new Result(false);
        }
        V1alpha1Project project = projectOpt.get();

        if (K8sObjectUtils.isTerminating(project)) {
            String nsName = this.namespaceNameResolver.resolveNamespaceName(request.getName());
            String nsKey = this.keyResolver.resolveKey(nsName);
            Optional<V1Namespace> nsOpt = Optional.ofNullable(this.namespaceIndexer.getByKey(nsKey));
            return reconcileTerminatingProject(project, nsOpt.isEmpty());
        }

        List<V1alpha1AipubUser> boundUsers = this.boundObjectResolver.getAllBoundAipubUsers(project);
        List<V1alpha1ImageHub> boundImageHubs = this.boundObjectResolver.getAllBoundImageHubs(project);

        List<V1alpha1ProjectMember> reconciledSpecProjectMembers = this.reconciliationService.reconcileProjectMembers(boundUsers, project);
        List<V1alpha1AipubUser> reconciledAipubUsers = this.reconciliationService.reconcileBoundAipubUsers(boundUsers, reconciledSpecProjectMembers);

        List<V1alpha1ProjectImageHub> reconciledSpecProjectImageHubs = this.reconciliationService.reconcileProjectImageHubs(boundImageHubs, project);
        List<V1alpha1ImageHub> reconcileImageHubs = this.reconciliationService.reconcileBoundImageHubs(boundImageHubs, reconciledSpecProjectImageHubs);

        V1ResourceQuota boundQuota = getBoundResourceQuota(project);
        List<V1alpha1NodeGroup> boundNodeGroups = this.boundObjectResolver.getAllBoundNodeGroups(project);
        List<V1Node> boundNodes = this.boundObjectResolver.getAllBoundNodes(project);
        boundNodes = NodeUtils.getProjectManagedNodes(boundNodes);
        V1alpha1ProjectSpec reconcileProjectSpec = this.reconciliationService.reconcileProjectSpec(project, reconciledSpecProjectMembers, reconciledSpecProjectImageHubs);
        V1alpha1ProjectStatus reconciledStatus = this.reconciliationService.reconcileProjectStatus(
                project, boundUsers, boundQuota, boundNodeGroups, boundNodes, reconcileImageHubs);

        return reconcileExistingProject(project, reconcileProjectSpec, reconciledStatus);
    }

    private Result reconcileTerminatingProject(V1alpha1Project project, boolean namespaceRemoved) throws ApiException {
        if (namespaceRemoved) {
            V1alpha1Project clone = ProjectUtils.clone(project);
            Objects.requireNonNull(clone.getMetadata());
            Objects.requireNonNull(clone.getMetadata().getFinalizers());
            List<String> finalizers = clone.getMetadata().getFinalizers();
            finalizers.remove(FinalizersConstants.PROJECT_FINALIZER);
            finalizers.remove(FinalizersConstants.FOREGROUND_DELETION);
            this.projectApi.update(clone);
        }
        return new Result(false);
    }

    private Result reconcileExistingProject(V1alpha1Project project, V1alpha1ProjectSpec reconciledSpec, V1alpha1ProjectStatus reconciledStatus) throws ApiException {
        if (Objects.equals(project.getSpec(), reconciledSpec) && Objects.equals(project.getStatus(), reconciledStatus)) {
            return new Result(false);
        }

        V1alpha1Project edited = new V1alpha1Project();
        edited.setApiVersion(project.getApiVersion());
        edited.setKind(project.getKind());
        edited.setMetadata(project.getMetadata());
        edited.setSpec(reconciledSpec);
        edited.setStatus(reconciledStatus);

        if (!Objects.equals(project.getSpec(), reconciledSpec)) {
            updateProjectSpec(edited);
            return new Result(true, Duration.ofSeconds(1));
        }

        if (!Objects.equals(project.getStatus(), reconciledStatus)) {
            updateProjectStatus(edited);
            return new Result(true, Duration.ofSeconds(1));
        }

        return new Result(false);
    }

    private KubernetesObject updateProjectSpec(V1alpha1Project project) throws ApiException {
        Objects.requireNonNull(project.getStatus());
        Objects.requireNonNull(project.getSpec());
        return this.objectPatchHelper.patchSpec(null, K8sObjectUtils.getName(project), project.getSpec());
    }

    private KubernetesObject updateProjectStatus(V1alpha1Project project) throws ApiException {
        Objects.requireNonNull(project.getStatus());
        Objects.requireNonNull(project.getSpec());
        return this.objectPatchHelper.patchStatus(null, K8sObjectUtils.getName(project), project.getStatus());
    }

    @Nullable
    private V1ResourceQuota getBoundResourceQuota(V1alpha1Project project) {
        String projName = K8sObjectUtils.getName(project);
        String quotaName = new ResourceQuotaNameResolver().resolveQuotaName(projName);
        String quotaKey = this.keyResolver.resolveKey(projName, quotaName);
        return this.quotaIndexer.getByKey(quotaKey);
    }

}
