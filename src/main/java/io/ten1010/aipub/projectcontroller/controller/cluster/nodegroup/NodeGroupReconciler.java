package io.ten1010.aipub.projectcontroller.controller.cluster.nodegroup;

import io.kubernetes.client.extended.controller.reconciler.Reconciler;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.models.V1DaemonSet;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import io.ten1010.aipub.projectcontroller.controller.KubernetesApiReconcileExceptionHandlingTemplate;
import io.ten1010.aipub.projectcontroller.controller.cluster.NodeGroupPolicy;
import io.ten1010.aipub.projectcontroller.core.KeyUtil;
import io.ten1010.aipub.projectcontroller.model.V1alpha1NodeGroup;
import io.ten1010.aipub.projectcontroller.model.V1alpha1NodeGroupList;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Optional;

@Slf4j
public class NodeGroupReconciler implements Reconciler {

    public static final Duration API_CONFLICT_REQUEUE_DURATION = Duration.ofSeconds(5);
    public static final Duration API_FAIL_REQUEUE_DURATION = Duration.ofSeconds(60);

    private KubernetesApiReconcileExceptionHandlingTemplate template;
    private Indexer<V1alpha1NodeGroup> nodeGroupIndexer;
    private Indexer<V1Node> nodeIndexer;
    private Indexer<V1Namespace> namespaceIndexer;
    private Indexer<V1DaemonSet> daemonSetIndexer;
    private GenericKubernetesApi<V1alpha1NodeGroup, V1alpha1NodeGroupList> nodeGroupApi;

    public NodeGroupReconciler(
            Indexer<V1alpha1NodeGroup> nodeGroupIndexer,
            Indexer<V1Node> nodeIndexer,
            Indexer<V1Namespace> namespaceIndexer,
            Indexer<V1DaemonSet> daemonSetIndexer,
            GenericKubernetesApi<V1alpha1NodeGroup, V1alpha1NodeGroupList> nodeGroupApi) {
        this.template = new KubernetesApiReconcileExceptionHandlingTemplate(API_CONFLICT_REQUEUE_DURATION, API_FAIL_REQUEUE_DURATION);
        this.nodeGroupIndexer = nodeGroupIndexer;
        this.nodeIndexer = nodeIndexer;
        this.namespaceIndexer = namespaceIndexer;
        this.daemonSetIndexer = daemonSetIndexer;
        this.nodeGroupApi = nodeGroupApi;
    }

    @Override
    public Result reconcile(Request request) {
        return this.template.execute(
                () -> {
                    String nodeGroupKey = KeyUtil.buildKey(request.getName());
                    Optional<V1alpha1NodeGroup> nodeGroupOpt = Optional.ofNullable(this.nodeGroupIndexer.getByKey(nodeGroupKey));
                    if (nodeGroupOpt.isEmpty()) {
                        log.debug("NodeGroup [{}] not founded while reconciling", nodeGroupKey);
                        return new Result(false);
                    }
                    V1alpha1NodeGroup nodeGroup = nodeGroupOpt.get();
                    log.debug("NodeGroup [{}] founded while reconciling\n{}", nodeGroupKey, nodeGroup);

                    return new Result(false);
                }, request);
    }

}
