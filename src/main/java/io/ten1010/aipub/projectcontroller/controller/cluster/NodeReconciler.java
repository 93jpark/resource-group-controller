package io.ten1010.aipub.projectcontroller.controller.cluster;

import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeBuilder;
import io.kubernetes.client.openapi.models.V1Taint;
import io.ten1010.aipub.projectcontroller.controller.BoundObjectResolver;
import io.ten1010.aipub.projectcontroller.controller.ReconcilerSupport;
import io.ten1010.aipub.projectcontroller.controller.RequestHelper;
import io.ten1010.aipub.projectcontroller.domain.k8s.K8sApiProvider;
import io.ten1010.aipub.projectcontroller.domain.k8s.KeyResolver;
import io.ten1010.aipub.projectcontroller.domain.k8s.ReconciliationService;
import io.ten1010.aipub.projectcontroller.domain.k8s.dto.V1alpha1NodeGroup;
import io.ten1010.aipub.projectcontroller.domain.k8s.dto.V1alpha1Project;
import io.ten1010.aipub.projectcontroller.domain.k8s.util.K8sObjectUtils;
import io.ten1010.aipub.projectcontroller.domain.k8s.util.NodeUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class NodeReconciler extends ReconcilerSupport {

    private final KeyResolver keyResolver;
    private final ReconciliationService reconciliationService;
    private final Indexer<V1Node> nodeIndexer;
    private final BoundObjectResolver boundObjectResolver;
    private final CoreV1Api coreV1Api;

    public NodeReconciler(
            SharedInformerFactory sharedInformerFactory,
            K8sApiProvider k8sApiProvider,
            ReconciliationService reconciliationService) {
        this.keyResolver = new KeyResolver();
        this.reconciliationService = reconciliationService;
        this.nodeIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1Node.class)
                .getIndexer();
        this.boundObjectResolver = new BoundObjectResolver(sharedInformerFactory);
        this.coreV1Api = new CoreV1Api(k8sApiProvider.getApiClient());
    }

    @Override
    protected Result reconcileInternal(Request request) throws ApiException {
        String nodeKey = new RequestHelper(this.keyResolver).resolveKey(request);
        Optional<V1Node> nodeOpt = Optional.ofNullable(this.nodeIndexer.getByKey(nodeKey));
        if (nodeOpt.isEmpty()) {
            return new Result(false);
        }
        V1Node node = nodeOpt.get();

        List<V1alpha1Project> boundProjects = this.boundObjectResolver.getAllBoundProjects(node);
        List<V1alpha1NodeGroup> boundNodeGroups = this.boundObjectResolver.getAllBoundNodeGroups(node);
        Map<String, String> reconciledLabels = this.reconciliationService.reconcileNodeLabels(node);
        Map<String, String> reconciledAnnotations = this.reconciliationService.reconcileNodeAnnotations(node, boundProjects, boundNodeGroups);
        List<V1Taint> reconciledTaints = this.reconciliationService.reconcileTaints(node);

        return reconcileExistingNode(nodeOpt.get(), reconciledLabels, reconciledAnnotations, reconciledTaints);
    }

    private Result reconcileExistingNode(
            V1Node node,
            Map<String, String> reconciledLabels,
            Map<String, String> reconciledAnnotations,
            List<V1Taint> reconciledTaints) throws ApiException {
        if (K8sObjectUtils.getLabels(node).equals(reconciledLabels) &&
                K8sObjectUtils.getAnnotations(node).equals(reconciledAnnotations) &&
                Set.copyOf(NodeUtils.getTaints(node)).equals(Set.copyOf(reconciledTaints))) {
            return new Result(false);
        }
        V1Node edited = new V1NodeBuilder(node)
                .editMetadata()
                .withLabels(reconciledLabels)
                .withAnnotations(reconciledAnnotations)
                .endMetadata()
                .editSpec()
                .withTaints(reconciledTaints)
                .endSpec()
                .build();
        updateNode(K8sObjectUtils.getName(node), edited);
        return new Result(false);
    }

    private void updateNode(String objName, V1Node node) throws ApiException {
        this.coreV1Api
                .replaceNode(objName, node)
                .execute();
    }

}
