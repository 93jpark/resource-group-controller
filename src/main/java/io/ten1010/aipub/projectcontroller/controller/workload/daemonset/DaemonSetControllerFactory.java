package io.ten1010.aipub.projectcontroller.controller.workload.daemonset;

import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.models.V1DaemonSet;
import io.ten1010.aipub.projectcontroller.controller.Reconciliation;
import io.ten1010.aipub.projectcontroller.core.K8sApis;
import io.ten1010.aipub.projectcontroller.model.V1alpha1Project;

public class DaemonSetControllerFactory {

    private SharedInformerFactory informerFactory;
    private Indexer<V1DaemonSet> daemonSetIndexer;
    private Indexer<V1alpha1Project> projectIndexer;
    private Reconciliation reconciliation;
    private K8sApis k8sApis;

    public DaemonSetControllerFactory(
            SharedInformerFactory informerFactory,
            Indexer<V1DaemonSet> daemonSetIndexer,
            Indexer<V1alpha1Project> projectIndexer,
            Reconciliation reconciliation,
            K8sApis k8sApis) {
        this.informerFactory = informerFactory;
        this.daemonSetIndexer = daemonSetIndexer;
        this.projectIndexer = projectIndexer;
        this.reconciliation = reconciliation;
        this.k8sApis = k8sApis;
    }

    public Controller create() {
        return ControllerBuilder.defaultBuilder(this.informerFactory)
                .withName("daemon-set-controller")
                .withWorkerCount(1)
                .watch(workQueue -> new ResourceGroupWatch(workQueue, this.daemonSetIndexer))
                .watch(workQueue -> new ImageNamespaceGroupBindingWatch(workQueue, this.daemonSetIndexer, this.projectIndexer))
                .watch(DaemonSetWatch::new)
                .withReconciler(new DaemonSetReconciler(this.daemonSetIndexer, this.reconciliation, this.k8sApis.getAppsV1Api()))
                .build();
    }

}
