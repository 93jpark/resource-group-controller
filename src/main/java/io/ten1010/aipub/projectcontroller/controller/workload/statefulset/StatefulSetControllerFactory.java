package io.ten1010.aipub.projectcontroller.controller.workload.statefulset;

import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.ten1010.aipub.projectcontroller.controller.Reconciliation;
import io.ten1010.aipub.projectcontroller.core.K8sApis;
import io.ten1010.aipub.projectcontroller.model.V1alpha1Project;

public class StatefulSetControllerFactory {

    private SharedInformerFactory informerFactory;
    private Indexer<V1StatefulSet> statefulSetIndexer;
    private Indexer<V1alpha1Project> projectIndexer;
    private Reconciliation reconciliation;
    private K8sApis k8sApis;

    public StatefulSetControllerFactory(
            SharedInformerFactory informerFactory,
            Indexer<V1StatefulSet> statefulSetIndexer,
            Indexer<V1alpha1Project> projectIndexer,
            Reconciliation reconciliation,
            K8sApis k8sApis) {
        this.informerFactory = informerFactory;
        this.statefulSetIndexer = statefulSetIndexer;
        this.projectIndexer = projectIndexer;
        this.reconciliation = reconciliation;
        this.k8sApis = k8sApis;
    }

    public Controller create() {
        return ControllerBuilder.defaultBuilder(this.informerFactory)
                .withName("stateful-set-controller")
                .withWorkerCount(1)
                .watch(workQueue -> new ImageNamespaceGroupBindingWatch(workQueue, this.statefulSetIndexer, this.projectIndexer))
                .watch(workQueue -> new NodeGroupBindingWatch(workQueue, this.statefulSetIndexer, this.projectIndexer))
                .watch(StatefulSetWatch::new)
                .withReconciler(new StatefulSetReconciler(this.statefulSetIndexer, this.reconciliation, this.k8sApis.getAppsV1Api()))
                .build();
    }

}
