package io.ten1010.aipub.projectcontroller.controller.workload.job;

import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.models.V1Job;
import io.ten1010.aipub.projectcontroller.controller.Reconciliation;
import io.ten1010.aipub.projectcontroller.core.K8sApis;
import io.ten1010.aipub.projectcontroller.model.V1alpha1Project;

public class JobControllerFactory {

    private SharedInformerFactory informerFactory;
    private Indexer<V1Job> jobIndexer;
    private Indexer<V1alpha1Project> projectIndexer;
    private Reconciliation reconciliation;
    private K8sApis k8sApis;

    public JobControllerFactory(
            SharedInformerFactory informerFactory,
            Indexer<V1Job> jobIndexer,
            Indexer<V1alpha1Project> projectIndexer,
            Reconciliation reconciliation,
            K8sApis k8sApis) {
        this.informerFactory = informerFactory;
        this.jobIndexer = jobIndexer;
        this.projectIndexer = projectIndexer;
        this.reconciliation = reconciliation;
        this.k8sApis = k8sApis;
    }

    public Controller create() {
        return ControllerBuilder.defaultBuilder(this.informerFactory)
                .withName("job-controller")
                .withWorkerCount(1)
                .watch(workQueue -> new ImageNamespaceGroupBindingWatch(workQueue, this.jobIndexer, this.projectIndexer))
                .watch(workQueue -> new NodeGroupBindingWatch(workQueue, this.jobIndexer, this.projectIndexer))
                .watch(JobWatch::new)
                .withReconciler(new JobReconciler(this.jobIndexer, this.reconciliation, this.k8sApis.getBatchV1Api()))
                .build();
    }

}
