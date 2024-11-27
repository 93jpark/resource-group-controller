package io.ten1010.aipub.projectcontroller.controller.workload.deployment;

import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.ten1010.aipub.projectcontroller.controller.Reconciliation;
import io.ten1010.aipub.projectcontroller.core.K8sApis;
import io.ten1010.aipub.projectcontroller.model.V1alpha1Project;

public class DeploymentControllerFactory {

    private SharedInformerFactory informerFactory;
    private Indexer<V1Deployment> deploymentIndexer;
    private Indexer<V1alpha1Project> projectIndexer;
    private Reconciliation reconciliation;
    private K8sApis k8sApis;

    public DeploymentControllerFactory(
            SharedInformerFactory informerFactory,
            Indexer<V1Deployment> deploymentIndexer,
            Indexer<V1alpha1Project> projectIndexer,
            Reconciliation reconciliation,
            K8sApis k8sApis) {
        this.informerFactory = informerFactory;
        this.deploymentIndexer = deploymentIndexer;
        this.projectIndexer = projectIndexer;
        this.reconciliation = reconciliation;
        this.k8sApis = k8sApis;
    }

    public Controller create() {
        return ControllerBuilder.defaultBuilder(this.informerFactory)
                .withName("deployment-controller")
                .withWorkerCount(1)
                .watch(workQueue -> new ResourceGroupWatch(workQueue, this.deploymentIndexer))
                .watch(workQueue -> new ImageNamespaceGroupBindingWatch(workQueue, this.deploymentIndexer, this.projectIndexer))
                .watch(DeploymentWatch::new)
                .withReconciler(new DeploymentReconciler(this.deploymentIndexer, this.reconciliation, this.k8sApis.getAppsV1Api()))
                .build();
    }

}
