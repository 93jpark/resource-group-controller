package io.ten1010.aipub.projectcontroller.controller.cluster.clusterrolebinding;

import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.models.V1ClusterRole;
import io.kubernetes.client.openapi.models.V1ClusterRoleBinding;
import io.ten1010.aipub.projectcontroller.core.K8sApis;
import io.ten1010.aipub.projectcontroller.model.V1alpha1NodeGroup;

public class ClusterRoleBindingControllerFactory {

    private SharedInformerFactory informerFactory;
    private Indexer<V1alpha1NodeGroup> groupIndexer;
    private Indexer<V1ClusterRoleBinding> clusterRoleBindingIndexer;
    private Indexer<V1ClusterRole> clusterRoleIndexer;
    private K8sApis k8sApis;

    public ClusterRoleBindingControllerFactory(
            SharedInformerFactory informerFactory,
            Indexer<V1alpha1NodeGroup> groupIndexer,
            Indexer<V1ClusterRoleBinding> clusterRoleBindingIndexer,
            Indexer<V1ClusterRole> clusterRoleIndexer,
            K8sApis k8sApis) {
        this.informerFactory = informerFactory;
        this.groupIndexer = groupIndexer;
        this.clusterRoleBindingIndexer = clusterRoleBindingIndexer;
        this.clusterRoleIndexer = clusterRoleIndexer;
        this.k8sApis = k8sApis;
    }

    public Controller create() {
        return ControllerBuilder.defaultBuilder(this.informerFactory)
                .withName("cluster-role-binding-controller")
                .withWorkerCount(1)
                .watch(ResourceGroupWatch::new)
                .watch(ClusterRoleBindingWatch::new)
                .withReconciler(new ClusterRoleBindingReconciler(
                        this.groupIndexer,
                        this.clusterRoleBindingIndexer,
                        this.clusterRoleIndexer,
                        this.k8sApis.getRbacAuthorizationV1Api()))
                .build();
    }

}
