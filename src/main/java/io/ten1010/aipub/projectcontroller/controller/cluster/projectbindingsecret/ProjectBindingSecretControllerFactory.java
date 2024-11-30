package io.ten1010.aipub.projectcontroller.controller.cluster.projectbindingsecret;

import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.models.V1Secret;
import io.ten1010.aipub.projectcontroller.configuration.ProjectProperties;
import io.ten1010.aipub.projectcontroller.controller.cluster.registrysecret.RegistrySecretReconciler;
import io.ten1010.aipub.projectcontroller.core.K8sApis;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroup;
import io.ten1010.aipub.projectcontroller.model.V1alpha1Project;
import io.ten1010.aipub.projectcontroller.service.RegistryRobotService;

public class ProjectBindingSecretControllerFactory {

    private SharedInformerFactory sharedInformerFactory;
    private Indexer<V1alpha1ImageNamespaceGroup> imageNamespaceGroupIndexer;
    private Indexer<V1alpha1Project> projectIndexer;
    private Indexer<V1Secret> secretIndexer;
    private K8sApis k8sApis;
    private ProjectProperties projectProperties;

    public ProjectBindingSecretControllerFactory(
            SharedInformerFactory sharedInformerFactory,
            Indexer<V1alpha1ImageNamespaceGroup> imageNamespaceGroupIndexer,
            Indexer<V1alpha1Project> projectIndexer,
            Indexer<V1Secret> secretIndexer,
            K8sApis k8sApis,
            ProjectProperties projectProperties) {
        this.sharedInformerFactory = sharedInformerFactory;
        this.imageNamespaceGroupIndexer = imageNamespaceGroupIndexer;
        this.projectIndexer = projectIndexer;
        this.secretIndexer = secretIndexer;
        this.k8sApis = k8sApis;
        this.projectProperties = projectProperties;
    }

    public Controller create() {
        return ControllerBuilder.defaultBuilder(this.sharedInformerFactory)
                .withName("project-binding-secret-controller")
                .withWorkerCount(1)
                .watch(workQueue -> new ImageNamespaceGroupBindingWatch(workQueue, this.projectIndexer))
                .withReconciler(new ProjectBindingSecretReconciler(
                        this.imageNamespaceGroupIndexer,
                        this.secretIndexer,
                        this.k8sApis.getCoreV1Api(),
                        this.projectProperties.getRegistrySecretNamespace()
                ))
                .build();
    }

}
