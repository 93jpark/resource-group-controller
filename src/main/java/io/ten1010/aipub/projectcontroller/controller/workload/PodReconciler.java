package io.ten1010.aipub.projectcontroller.controller.workload;

import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1Pod;
import io.ten1010.aipub.projectcontroller.controller.ReconcilerSupport;
import io.ten1010.aipub.projectcontroller.controller.RequestHelper;
import io.ten1010.aipub.projectcontroller.domain.k8s.K8sApiProvider;
import io.ten1010.aipub.projectcontroller.domain.k8s.KeyResolver;
import io.ten1010.aipub.projectcontroller.domain.k8s.NamespaceNameResolver;
import io.ten1010.aipub.projectcontroller.domain.k8s.dto.V1alpha1Project;
import io.ten1010.aipub.projectcontroller.domain.k8s.util.K8sObjectUtils;
import io.ten1010.aipub.projectcontroller.domain.k8s.util.NodeUtils;
import io.ten1010.aipub.projectcontroller.domain.k8s.util.WorkloadUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class PodReconciler extends ReconcilerSupport {

    private final KeyResolver keyResolver;
    private final NamespaceNameResolver namespaceNameResolver;
    private final Indexer<V1Pod> podIndexer;
    private final Indexer<V1Node> nodeIndexer;
    private final Indexer<V1alpha1Project> projectIndexer;
    private final CoreV1Api coreV1Api;
    private final PodNodesResolver podNodesResolver;

    public PodReconciler(
            SharedInformerFactory sharedInformerFactory,
            K8sApiProvider k8sApiProvider,
            PodNodesResolver podNodesResolver) {
        this.keyResolver = new KeyResolver();
        this.namespaceNameResolver = new NamespaceNameResolver();
        this.podIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1Pod.class)
                .getIndexer();
        this.nodeIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1Node.class)
                .getIndexer();
        this.projectIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1alpha1Project.class)
                .getIndexer();
        this.coreV1Api = new CoreV1Api(k8sApiProvider.getApiClient());
        this.podNodesResolver = podNodesResolver;
    }

    @Override
    protected Result reconcileInternal(Request request) throws ApiException {
        String podKey = new RequestHelper(this.keyResolver).resolveKey(request);
        Optional<V1Pod> podOpt = Optional.ofNullable(this.podIndexer.getByKey(podKey));
        if (podOpt.isEmpty()) {
            return new Result(false);
        }
        V1Pod pod = podOpt.get();

        Optional<String> nodeNameOpt = WorkloadUtils.getNodeName(pod);
        if (nodeNameOpt.isEmpty()) {
            return new Result(false);
        }
        String nodeKey = this.keyResolver.resolveKey(nodeNameOpt.get());
        V1Node node = this.nodeIndexer.getByKey(nodeKey);
        if (node == null) {
            return new Result(false);
        }

        if (NodeUtils.isProjectManaged(node)) {
            return processCaseThatProjectManagedNode(node, pod);
        }

        return processCaseThatNotProjectManagedNode(pod);
    }

    private void deletePod(V1Pod pod) throws ApiException {
        if (K8sObjectUtils.getDeletionTimestamp(pod).isEmpty()) {
            this.coreV1Api.deleteNamespacedPod(K8sObjectUtils.getName(pod), K8sObjectUtils.getNamespace(pod))
                    .execute();
        }
    }

    private Result processCaseThatProjectManagedNode(V1Node node, V1Pod pod) throws ApiException {
        if (!NodeUtils.isStrictIsolationMode(node)) {
            return new Result(false);
        }

        List<V1Node> allowedProjectNodeObjects;
        try {
            allowedProjectNodeObjects = this.podNodesResolver.getNodes(pod);
        } catch (UnsupportedControllerException e) {
            deletePod(pod);
            return new Result(false);
        }

        Set<String> allowedProjectNodes = allowedProjectNodeObjects.stream()
                .map(K8sObjectUtils::getName)
                .collect(Collectors.toSet());
        if (!allowedProjectNodes.contains(K8sObjectUtils.getName(node))) {
            deletePod(pod);
            return new Result(false);
        }
        return new Result(false);
    }

    private Result processCaseThatNotProjectManagedNode(V1Pod pod) throws ApiException {
        String projName = this.namespaceNameResolver.resolveProjectName(K8sObjectUtils.getNamespace(pod));
        String projKey = this.keyResolver.resolveKey(projName);
        V1alpha1Project project = this.projectIndexer.getByKey(projKey);

        if (project != null) {
            deletePod(pod);
        }
        return new Result(false);
    }

}
