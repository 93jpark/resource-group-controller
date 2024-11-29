package io.ten1010.aipub.projectcontroller.controller.cluster.nodegroupbinding;

import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Indexer;
import io.ten1010.aipub.projectcontroller.core.K8sApis;
import io.ten1010.aipub.projectcontroller.model.V1alpha1NodeGroup;
import io.ten1010.aipub.projectcontroller.model.V1alpha1NodeGroupBinding;
import io.ten1010.aipub.projectcontroller.model.V1alpha1Project;

public class NodeGroupBindingControllerFactory {

    private SharedInformerFactory sharedInformerFactory;
    private Indexer<V1alpha1NodeGroup> nodeGroupIndexer;
    private Indexer<V1alpha1NodeGroupBinding> nodeGroupBindingIndexer;
    private Indexer<V1alpha1Project> projectIndexer;
    private K8sApis k8sApis;

    public NodeGroupBindingControllerFactory(
            SharedInformerFactory sharedInformerFactory,
            Indexer<V1alpha1NodeGroup> nodeGroupIndexer,
            Indexer<V1alpha1NodeGroupBinding> nodeGroupBindingIndexer,
            Indexer<V1alpha1Project> projectIndexer,
            K8sApis k8sApis) {
        this.sharedInformerFactory = sharedInformerFactory;
        this.nodeGroupIndexer = nodeGroupIndexer;
        this.nodeGroupBindingIndexer = nodeGroupBindingIndexer;
        this.projectIndexer = projectIndexer;
        this.k8sApis = k8sApis;
    }

    public Controller create() {
        return ControllerBuilder.defaultBuilder(this.sharedInformerFactory)
                .withName("node-group-binding-controller")
                .withWorkerCount(1)
                .watch(NodeGroupBindingWatch::new)
                .withReconciler(new NodeGroupBindingReconciler(
                        this.nodeGroupIndexer,
                        this.nodeGroupBindingIndexer,
                        this.projectIndexer,
                        this.k8sApis.getCoreV1Api())
                ).build();
    }

}
