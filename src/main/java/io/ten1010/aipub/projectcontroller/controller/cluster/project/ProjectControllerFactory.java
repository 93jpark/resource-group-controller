package io.ten1010.aipub.projectcontroller.controller.cluster.project;

import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
import io.kubernetes.client.extended.event.legacy.EventRecorder;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.ten1010.aipub.projectcontroller.core.K8sApis;
import io.ten1010.aipub.projectcontroller.model.V1alpha1Project;

public class ProjectControllerFactory {

    private SharedInformerFactory sharedInformerFactory;
    private Indexer<V1alpha1Project> projectIndexer;
    private Indexer<V1Namespace> namespaceIndexer;
    private K8sApis k8sApis;
    private EventRecorder eventRecorder;

    public ProjectControllerFactory(
            SharedInformerFactory sharedInformerFactory,
            Indexer<V1alpha1Project> projectIndexer,
            Indexer<V1Namespace> namespaceIndexer,
            K8sApis k8sApis,
            EventRecorder eventRecorder) {
        this.sharedInformerFactory = sharedInformerFactory;
        this.projectIndexer = projectIndexer;
        this.namespaceIndexer = namespaceIndexer;
        this.k8sApis = k8sApis;
        this.eventRecorder = eventRecorder;
    }

    public Controller create() {
        return ControllerBuilder.defaultBuilder(this.sharedInformerFactory)
                .withName("project-controller")
                .withWorkerCount(1)
                .watch(workQueue -> new ProjectWatch(workQueue, this.namespaceIndexer))
                .withReconciler(new ProjectReconciler(
                        this.projectIndexer,
                        this.namespaceIndexer,
                        this.k8sApis.getProjectApi(),
                        this.eventRecorder))
                .build();
    }

}
