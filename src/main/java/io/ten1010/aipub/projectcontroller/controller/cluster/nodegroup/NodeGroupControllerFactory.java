package io.ten1010.aipub.projectcontroller.controller.cluster.nodegroup;

import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.models.V1DaemonSet;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1Node;
import io.ten1010.aipub.projectcontroller.core.K8sApis;
import io.ten1010.aipub.projectcontroller.model.V1alpha1NodeGroup;

public class NodeGroupControllerFactory {

    private SharedInformerFactory sharedInformerFactory;
    private Indexer<V1alpha1NodeGroup> nodeGroupIndexer;
    private Indexer<V1Node> nodeIndexer;
    private Indexer<V1DaemonSet> daemonSetIndexer;
    private Indexer<V1Namespace> namespaceIndexer;
    private K8sApis k8sApis;

    public NodeGroupControllerFactory(
            SharedInformerFactory sharedInformerFactory,
            Indexer<V1alpha1NodeGroup> nodeGroupIndexer,
            Indexer<V1Node> nodeIndexer,
            Indexer<V1DaemonSet> daemonSetIndexer,
            Indexer<V1Namespace> namespaceIndexer,
            K8sApis k8sApis) {
        this.sharedInformerFactory = sharedInformerFactory;
        this.nodeGroupIndexer = nodeGroupIndexer;
        this.nodeIndexer = nodeIndexer;
        this.daemonSetIndexer = daemonSetIndexer;
        this.namespaceIndexer = namespaceIndexer;
        this.k8sApis = k8sApis;
    }

    public Controller create() {
        return ControllerBuilder.defaultBuilder(this.sharedInformerFactory)
                .withName("node-group-controller")
                .withWorkerCount(1)
                .watch(workQueue -> new NodeGroupWatch(workQueue, this.nodeIndexer))
                .withReconciler(new NodeGroupReconciler(
                        this.nodeGroupIndexer,
                        this.nodeIndexer,
                        this.namespaceIndexer,
                        this.daemonSetIndexer,
                        this.k8sApis.getNodeGroupApi()))
                .build();
    }


}
