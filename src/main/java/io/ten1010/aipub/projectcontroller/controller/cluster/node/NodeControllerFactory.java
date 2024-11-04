package io.ten1010.aipub.projectcontroller.controller.cluster.node;

import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
import io.kubernetes.client.extended.event.legacy.EventRecorder;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.models.V1Node;
import io.ten1010.aipub.projectcontroller.core.K8sApis;
import io.ten1010.aipub.projectcontroller.model.V1alpha1NodeGroup;

public class NodeControllerFactory {

    private SharedInformerFactory informerFactory;
    private Indexer<V1Node> nodeIndexer;
    private Indexer<V1alpha1NodeGroup> groupIndexer;
    private K8sApis k8sApis;
    private EventRecorder eventRecorder;

    public NodeControllerFactory(
            SharedInformerFactory informerFactory,
            Indexer<V1Node> nodeIndexer,
            Indexer<V1alpha1NodeGroup> groupIndexer,
            K8sApis k8sApis,
            EventRecorder eventRecorder) {
        this.informerFactory = informerFactory;
        this.nodeIndexer = nodeIndexer;
        this.groupIndexer = groupIndexer;
        this.k8sApis = k8sApis;
        this.eventRecorder = eventRecorder;
    }

    public Controller create() {
        return ControllerBuilder.defaultBuilder(this.informerFactory)
                .withName("node-controller")
                .withWorkerCount(1)
                .watch(NodeGroupWatch::new)
                .watch(NodeWatch::new)
                .withReconciler(new NodeReconciler(this.nodeIndexer, this.groupIndexer, this.k8sApis.getCoreV1Api(), this.eventRecorder))
                .build();
    }

}
