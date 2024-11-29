package io.ten1010.aipub.projectcontroller.controller.cluster.namespace;

import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
import io.kubernetes.client.extended.event.legacy.EventRecorder;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.ten1010.aipub.projectcontroller.configuration.ProjectProperties;
import io.ten1010.aipub.projectcontroller.core.K8sApis;
import io.ten1010.aipub.projectcontroller.model.V1alpha1Project;

public class NamespaceControllerFactory {

    private SharedInformerFactory sharedInformerFactory;
    private Indexer<V1Namespace> namespaceIndexer;
    private Indexer<V1alpha1Project> projectIndexer;
    private CoreV1Api coreV1Api;
    private EventRecorder eventRecorder;
    private ProjectProperties projectProperties;

    public NamespaceControllerFactory(SharedInformerFactory sharedInformerFactory, Indexer<V1Namespace> namespaceIndexer, Indexer<V1alpha1Project> projectIndexer, K8sApis k8sApis, EventRecorder eventRecorder, ProjectProperties projectProperties) {
        this.sharedInformerFactory = sharedInformerFactory;
        this.namespaceIndexer = namespaceIndexer;
        this.projectIndexer = projectIndexer;
        this.coreV1Api = k8sApis.getCoreV1Api();
        this.eventRecorder = eventRecorder;
        this.projectProperties = projectProperties;
    }

    public Controller create() {
        return ControllerBuilder.defaultBuilder(this.sharedInformerFactory)
                .withName("namespace-controller")
                .withWorkerCount(1)
                .watch(ProjectWatch::new)
                .watch(workQueue -> new ImageNamespaceGroupWatch(workQueue, projectProperties.getRegistrySecretNamespace()))
                .withReconciler(new NamespaceReconciler(
                        this.namespaceIndexer,
                        this.projectIndexer,
                        this.coreV1Api,
                        this.eventRecorder,
                        this.projectProperties))
                .build();
    }

}
