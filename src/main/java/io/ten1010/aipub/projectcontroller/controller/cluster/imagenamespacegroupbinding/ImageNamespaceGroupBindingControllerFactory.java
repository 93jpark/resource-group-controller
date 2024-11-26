package io.ten1010.aipub.projectcontroller.controller.cluster.imagenamespacegroupbinding;

import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import io.ten1010.aipub.projectcontroller.controller.cluster.imagenamespacegroup.SecretWatch;
import io.ten1010.aipub.projectcontroller.core.K8sApis;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroup;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroupBinding;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroupBindingList;
import io.ten1010.aipub.projectcontroller.model.V1alpha1Project;

public class ImageNamespaceGroupBindingControllerFactory {

    private SharedInformerFactory sharedInformerFactory;
    private Indexer<V1alpha1ImageNamespaceGroup> imageNamespaceGroupIndexer;
    private Indexer<V1alpha1ImageNamespaceGroupBinding> imageNamespaceGroupBindingIndexer;
    private Indexer<V1alpha1Project> projectIndexer;
    private GenericKubernetesApi<V1alpha1ImageNamespaceGroupBinding, V1alpha1ImageNamespaceGroupBindingList> imageNamespaceGroupBindingApi;
    private CoreV1Api coreV1Api;

    public ImageNamespaceGroupBindingControllerFactory(
            SharedInformerFactory sharedInformerFactory,
            Indexer<V1alpha1ImageNamespaceGroup> imageNamespaceGroupIndexer,
            Indexer<V1alpha1ImageNamespaceGroupBinding> imageNamespaceGroupBindingIndexer,
            Indexer<V1alpha1Project> projectIndexer,
            K8sApis k8sApis) {
        this.sharedInformerFactory = sharedInformerFactory;
        this.imageNamespaceGroupIndexer = imageNamespaceGroupIndexer;
        this.imageNamespaceGroupBindingIndexer = imageNamespaceGroupBindingIndexer;
        this.projectIndexer = projectIndexer;
        this.imageNamespaceGroupBindingApi = k8sApis.getImageNamespaceGroupBindingApi();
        this.coreV1Api = k8sApis.getCoreV1Api();
    }

    public Controller create() {
        return ControllerBuilder.defaultBuilder(this.sharedInformerFactory)
                .withName("image-namespace-group-binding-controller")
                .withWorkerCount(1)
                .watch(ImageNamespaceGroupBindingWatch::new)
                .watch(workQueue -> new SecretWatch(workQueue, this.imageNamespaceGroupIndexer))
                .withReconciler(new ImageNamespaceGroupBindingReconciler(
                        this.imageNamespaceGroupIndexer,
                        this.imageNamespaceGroupBindingIndexer,
                        this.projectIndexer,
                        this.imageNamespaceGroupBindingApi,
                        this.coreV1Api)
                ).build();
    }

}
