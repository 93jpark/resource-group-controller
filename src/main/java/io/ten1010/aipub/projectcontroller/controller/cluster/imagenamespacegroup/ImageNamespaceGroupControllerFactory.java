package io.ten1010.aipub.projectcontroller.controller.cluster.imagenamespacegroup;

import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import io.ten1010.aipub.projectcontroller.configuration.ProjectProperties;
import io.ten1010.aipub.projectcontroller.service.RegistryRobotService;
import io.ten1010.aipub.projectcontroller.core.K8sApis;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroup;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroupList;

public class ImageNamespaceGroupControllerFactory {

    private SharedInformerFactory sharedInformerFactory;
    private Indexer<V1alpha1ImageNamespaceGroup> imageNamespaceGroupIndexer;
    private Indexer<V1Secret> secretIndexer;
    private GenericKubernetesApi<V1alpha1ImageNamespaceGroup, V1alpha1ImageNamespaceGroupList> imageNamespaceGroupApi;
    private CoreV1Api coreV1Api;
    private RegistryRobotService registryRobotService;
    private ProjectProperties projectProperties;

    public ImageNamespaceGroupControllerFactory(
            SharedInformerFactory sharedInformerFactory,
            Indexer<V1alpha1ImageNamespaceGroup> imageNamespaceGroupIndexer,
            Indexer<V1Secret> secretIndexer,
            K8sApis k8sApis,
            RegistryRobotService registryRobotService,
            ProjectProperties projectProperties) {
        this.sharedInformerFactory = sharedInformerFactory;
        this.imageNamespaceGroupIndexer = imageNamespaceGroupIndexer;
        this.secretIndexer = secretIndexer;
        this.imageNamespaceGroupApi = k8sApis.getImageNamespaceGroupApi();
        this.coreV1Api = k8sApis.getCoreV1Api();
        this.registryRobotService = registryRobotService;
        this.projectProperties = projectProperties;
    }

    public Controller create() {
        return ControllerBuilder.defaultBuilder(this.sharedInformerFactory)
                .withName("image-namespace-group-controller")
                .withWorkerCount(1)
                .watch(ImageNamespaceGroupWatch::new)
                .watch(workQueue -> new SecretWatch(workQueue, this.imageNamespaceGroupIndexer))
                .withReconciler(new ImageNamespaceGroupReconciler(
                        this.imageNamespaceGroupIndexer,
                        this.secretIndexer,
                        this.imageNamespaceGroupApi,
                        this.coreV1Api,
                        this.registryRobotService,
                        this.projectProperties.getRegistrySecretNamespace()))
                .build();
    }

}
