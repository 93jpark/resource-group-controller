package io.ten1010.aipub.projectcontroller.controller.cluster.secret;

import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.models.V1Secret;
import io.ten1010.aipub.projectcontroller.configuration.ProjectProperties;
import io.ten1010.aipub.projectcontroller.model.V1alpha1Project;
import io.ten1010.aipub.projectcontroller.service.RegistryRobotService;
import io.ten1010.aipub.projectcontroller.core.K8sApis;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroup;

public class SecretControllerFactory {

    private SharedInformerFactory sharedInformerFactory;
    private Indexer<V1alpha1ImageNamespaceGroup> imageNamespaceGroupIndexer;
    private Indexer<V1alpha1Project> projectIndexer;
    private Indexer<V1Secret> secretIndexer;
    private K8sApis k8sApis;
    private RegistryRobotService registryRobotService;
    private ProjectProperties projectProperties;

    public SecretControllerFactory(
            SharedInformerFactory sharedInformerFactory,
            Indexer<V1alpha1ImageNamespaceGroup> imageNamespaceGroupIndexer,
            Indexer<V1alpha1Project> projectIndexer,
            Indexer<V1Secret> secretIndexer,
            K8sApis k8sApis,
            RegistryRobotService registryRobotService,
            ProjectProperties projectProperties) {
        this.sharedInformerFactory = sharedInformerFactory;
        this.imageNamespaceGroupIndexer = imageNamespaceGroupIndexer;
        this.projectIndexer = projectIndexer;
        this.secretIndexer = secretIndexer;
        this.k8sApis = k8sApis;
        this.registryRobotService = registryRobotService;
        this.projectProperties = projectProperties;
    }

    public Controller create() {
        return ControllerBuilder.defaultBuilder(this.sharedInformerFactory)
                .withName("secret-controller")
                .withWorkerCount(1)
                .watch(workQueue -> new SecretWatch(workQueue, this.imageNamespaceGroupIndexer))
                .watch(workQueue -> new ImageNamespaceGroupWatch(workQueue, this.secretIndexer, this.projectProperties.getSecretNamespace()))
                .watch(workQueue -> new ImageNamespaceGroupBindingWatch(workQueue, this.projectIndexer))
                .withReconciler(new SecretReconciler(
                        this.imageNamespaceGroupIndexer,
                        this.secretIndexer,
                        this.k8sApis.getCoreV1Api(),
                        this.registryRobotService,
                        this.projectProperties.getSecretNamespace()
                ))
                .build();
    }

}
