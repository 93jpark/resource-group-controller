package io.ten1010.aipub.projectcontroller.controller.workload.replicaset;

import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.models.V1ReplicaSet;
import io.ten1010.aipub.projectcontroller.controller.Reconciliation;
import io.ten1010.aipub.projectcontroller.core.K8sApis;
import io.ten1010.aipub.projectcontroller.model.V1alpha1Project;

public class ReplicaSetControllerFactory {

    private SharedInformerFactory informerFactory;
    private Indexer<V1ReplicaSet> replicaSetIndexer;
    private Indexer<V1alpha1Project> projectIndexer;
    private Reconciliation reconciliation;
    private K8sApis k8sApis;

    public ReplicaSetControllerFactory(
            SharedInformerFactory informerFactory,
            Indexer<V1ReplicaSet> replicaSetIndexer,
            Indexer<V1alpha1Project> projectIndexer,
            Reconciliation reconciliation,
            K8sApis k8sApis) {
        this.informerFactory = informerFactory;
        this.replicaSetIndexer = replicaSetIndexer;
        this.projectIndexer = projectIndexer;
        this.reconciliation = reconciliation;
        this.k8sApis = k8sApis;
    }

    public Controller create() {
        return ControllerBuilder.defaultBuilder(this.informerFactory)
                .withName("replica-set-controller")
                .withWorkerCount(1)
                .watch(workQueue -> new ImageNamespaceGroupBindingWatch(workQueue, this.replicaSetIndexer, this.projectIndexer))
                .watch(workQueue -> new NodeGroupBindingWatch(workQueue, this.replicaSetIndexer, this.projectIndexer))
                .watch(ReplicaSetWatch::new)
                .withReconciler(new ReplicaSetReconciler(this.replicaSetIndexer, this.reconciliation, this.k8sApis.getAppsV1Api()))
                .build();
    }

}
