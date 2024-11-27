package io.ten1010.aipub.projectcontroller.configuration;

import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.models.*;
import io.ten1010.aipub.projectcontroller.controller.workload.replicaset.ReplicaSetControllerFactory;
import io.ten1010.aipub.projectcontroller.controller.workload.statefulset.StatefulSetControllerFactory;
import io.ten1010.aipub.projectcontroller.controller.Reconciliation;
import io.ten1010.aipub.projectcontroller.controller.workload.cronjob.CronJobControllerFactory;
import io.ten1010.aipub.projectcontroller.controller.workload.daemonset.DaemonSetControllerFactory;
import io.ten1010.aipub.projectcontroller.controller.workload.deployment.DeploymentControllerFactory;
import io.ten1010.aipub.projectcontroller.controller.workload.job.JobControllerFactory;
import io.ten1010.aipub.projectcontroller.controller.workload.pod.PodControllerFactory;
import io.ten1010.aipub.projectcontroller.controller.workload.replicationcontroller.ReplicationControllerControllerFactory;
import io.ten1010.aipub.projectcontroller.core.K8sApis;
import io.ten1010.aipub.projectcontroller.model.V1alpha1Project;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkloadControllerConfiguration {

    @Bean
    public Controller cronJobController(
            SharedInformerFactory sharedInformerFactory,
            Reconciliation reconciliation,
            K8sApis k8sApis) {
        Indexer<V1CronJob> cronJobIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1CronJob.class)
                .getIndexer();
        Indexer<V1alpha1Project> projectIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1alpha1Project.class)
                .getIndexer();
        return new CronJobControllerFactory(sharedInformerFactory, cronJobIndexer, projectIndexer, reconciliation, k8sApis)
                .create();
    }

    @Bean
    public Controller daemonSetController(
            SharedInformerFactory sharedInformerFactory,
            Reconciliation reconciliation,
            K8sApis k8sApis) {
        Indexer<V1DaemonSet> daemonSetIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1DaemonSet.class)
                .getIndexer();
        Indexer<V1alpha1Project> projectIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1alpha1Project.class)
                .getIndexer();
        return new DaemonSetControllerFactory(sharedInformerFactory, daemonSetIndexer, projectIndexer, reconciliation, k8sApis)
                .create();
    }

    @Bean
    public Controller deploymentController(
            SharedInformerFactory sharedInformerFactory,
            Reconciliation reconciliation,
            K8sApis k8sApis) {
        Indexer<V1Deployment> deploymentIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1Deployment.class)
                .getIndexer();
        Indexer<V1alpha1Project> projectIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1alpha1Project.class)
                .getIndexer();
        return new DeploymentControllerFactory(sharedInformerFactory, deploymentIndexer, projectIndexer, reconciliation, k8sApis)
                .create();
    }

    @Bean
    public Controller jobController(
            SharedInformerFactory sharedInformerFactory,
            Reconciliation reconciliation,
            K8sApis k8sApis) {
        Indexer<V1Job> jobIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1Job.class)
                .getIndexer();
        Indexer<V1alpha1Project> projectIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1alpha1Project.class)
                .getIndexer();
        return new JobControllerFactory(sharedInformerFactory, jobIndexer, projectIndexer, reconciliation, k8sApis)
                .create();
    }

    @Bean
    public Controller podController(
            SharedInformerFactory sharedInformerFactory,
            Reconciliation reconciliation,
            K8sApis k8sApis) {
        Indexer<V1Pod> podIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1Pod.class)
                .getIndexer();
        Indexer<V1alpha1Project> projectIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1alpha1Project.class)
                .getIndexer();
        return new PodControllerFactory(sharedInformerFactory, podIndexer, projectIndexer, reconciliation, k8sApis)
                .create();
    }

    @Bean
    public Controller replicaSetController(
            SharedInformerFactory sharedInformerFactory,
            Reconciliation reconciliation,
            K8sApis k8sApis) {
        Indexer<V1ReplicaSet> replicaSetIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1ReplicaSet.class)
                .getIndexer();
        Indexer<V1alpha1Project> projectIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1alpha1Project.class)
                .getIndexer();
        return new ReplicaSetControllerFactory(sharedInformerFactory, replicaSetIndexer, projectIndexer, reconciliation, k8sApis)
                .create();
    }

    @Bean
    public Controller replicationControllerController(
            SharedInformerFactory sharedInformerFactory,
            Reconciliation reconciliation,
            K8sApis k8sApis) {
        Indexer<V1ReplicationController> replicationControllerIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1ReplicationController.class)
                .getIndexer();
        Indexer<V1alpha1Project> projectIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1alpha1Project.class)
                .getIndexer();
        return new ReplicationControllerControllerFactory(sharedInformerFactory, replicationControllerIndexer, projectIndexer, reconciliation, k8sApis)
                .create();
    }

    @Bean
    public Controller statefulSetController(
            SharedInformerFactory sharedInformerFactory,
            Reconciliation reconciliation,
            K8sApis k8sApis) {
        Indexer<V1StatefulSet> statefulSetIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1StatefulSet.class)
                .getIndexer();
        Indexer<V1alpha1Project> projectIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1alpha1Project.class)
                .getIndexer();
        return new StatefulSetControllerFactory(sharedInformerFactory, statefulSetIndexer, projectIndexer, reconciliation, k8sApis)
                .create();
    }

}
