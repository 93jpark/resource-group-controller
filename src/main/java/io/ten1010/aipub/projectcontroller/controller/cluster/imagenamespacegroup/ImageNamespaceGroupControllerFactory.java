package io.ten1010.aipub.projectcontroller.controller.cluster.imagenamespacegroup;

import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import io.ten1010.aipub.projectcontroller.controller.cluster.RegistryRobotService;
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

    public ImageNamespaceGroupControllerFactory(
            SharedInformerFactory sharedInformerFactory,
            Indexer<V1alpha1ImageNamespaceGroup> imageNamespaceGroupIndexer,
            Indexer<V1Secret> secretIndexer,
            K8sApis k8sApis,
            RegistryRobotService registryRobotService) {
        this.sharedInformerFactory = sharedInformerFactory;
        this.imageNamespaceGroupIndexer = imageNamespaceGroupIndexer;
        this.secretIndexer = secretIndexer;
        this.imageNamespaceGroupApi = k8sApis.getImageNamespaceGroupApi();
        this.registryRobotService = registryRobotService;
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
                        this.registryRobotService))
                .build();
    }

}
