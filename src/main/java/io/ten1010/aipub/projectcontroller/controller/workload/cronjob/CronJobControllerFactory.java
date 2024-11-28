package io.ten1010.aipub.projectcontroller.controller.workload.cronjob;

import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.models.V1CronJob;
import io.ten1010.aipub.projectcontroller.controller.Reconciliation;
import io.ten1010.aipub.projectcontroller.core.K8sApis;
import io.ten1010.aipub.projectcontroller.model.V1alpha1Project;

public class CronJobControllerFactory {

    private SharedInformerFactory informerFactory;
    private Indexer<V1CronJob> cronJobIndexer;
    private Indexer<V1alpha1Project> projectIndexer;
    private Reconciliation reconciliation;
    private K8sApis k8sApis;

    public CronJobControllerFactory(
            SharedInformerFactory informerFactory,
            Indexer<V1CronJob> cronJobIndexer,
            Indexer<V1alpha1Project> projectIndexer,
            Reconciliation reconciliation,
            K8sApis k8sApis) {
        this.informerFactory = informerFactory;
        this.cronJobIndexer = cronJobIndexer;
        this.projectIndexer = projectIndexer;
        this.reconciliation = reconciliation;
        this.k8sApis = k8sApis;
    }

    public Controller create() {
        return ControllerBuilder.defaultBuilder(this.informerFactory)
                .withName("cron-job-controller")
                .withWorkerCount(1)
                .watch(workQueue -> new ImageNamespaceGroupBindingWatch(workQueue, this.cronJobIndexer, this.projectIndexer))
                .watch(workQueue -> new NodeGroupBindingWatch(workQueue, this.cronJobIndexer, this.projectIndexer))
                .watch(CronJobWatch::new)
                .withReconciler(new CronJobReconciler(this.cronJobIndexer, this.reconciliation, this.k8sApis.getBatchV1Api()))
                .build();
    }

}
