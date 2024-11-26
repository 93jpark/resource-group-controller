package io.ten1010.aipub.projectcontroller.controller.cluster.nodegroupbinding;

import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroup;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroupList;
import io.ten1010.aipub.projectcontroller.model.V1alpha1NodeGroup;
import io.ten1010.aipub.projectcontroller.model.V1alpha1NodeGroupBinding;

public class NodeGroupBindingControllerFactory {

    private SharedInformerFactory sharedInformerFactory;
    private Indexer<V1alpha1NodeGroup> nodeGroupIndexer;
    private Indexer<V1alpha1NodeGroupBinding> nodeGroupBindingIndexer;
    private GenericKubernetesApi<V1alpha1ImageNamespaceGroup, V1alpha1ImageNamespaceGroupList> imageNamespaceGroupApi;
    private CoreV1Api coreV1Api;

    public NodeGroupBindingControllerFactory(
            SharedInformerFactory sharedInformerFactory,
            Indexer<V1alpha1NodeGroup> nodeGroupIndexer,
            Indexer<V1alpha1NodeGroupBinding> nodeGroupBindingIndexer,
            GenericKubernetesApi<V1alpha1ImageNamespaceGroup, V1alpha1ImageNamespaceGroupList> imageNamespaceGroupApi,
            CoreV1Api coreV1Api) {
        this.sharedInformerFactory = sharedInformerFactory;
        this.nodeGroupIndexer = nodeGroupIndexer;
        this.nodeGroupBindingIndexer = nodeGroupBindingIndexer;
        this.imageNamespaceGroupApi = imageNamespaceGroupApi;
        this.coreV1Api = coreV1Api;
    }

    public Controller create() {
        return ControllerBuilder.defaultBuilder(this.sharedInformerFactory)
                .withName("node-group-binding-controller")
                .withWorkerCount(1)
                .watch(NodeGroupBindingWatch::new)
                .withReconciler(new NodeGroupBindingReconciler(
                        this.nodeGroupIndexer,
                        this.nodeGroupBindingIndexer,
                        this.coreV1Api)
                ).build();
    }

}
