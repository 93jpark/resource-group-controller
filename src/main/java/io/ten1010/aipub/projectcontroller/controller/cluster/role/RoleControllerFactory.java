package io.ten1010.aipub.projectcontroller.controller.cluster.role;

import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1Role;
import io.ten1010.aipub.projectcontroller.core.K8sApis;
import io.ten1010.aipub.projectcontroller.model.V1alpha1NodeGroup;

public class RoleControllerFactory {

    private SharedInformerFactory informerFactory;
    private Indexer<V1Namespace> namespaceIndexer;
    private Indexer<V1alpha1NodeGroup> groupIndexer;
    private Indexer<V1Role> roleIndexer;
    private K8sApis k8sApis;

    public RoleControllerFactory(
            SharedInformerFactory informerFactory,
            Indexer<V1Namespace> namespaceIndexer,
            Indexer<V1alpha1NodeGroup> groupIndexer,
            Indexer<V1Role> roleIndexer,
            K8sApis k8sApis) {
        this.informerFactory = informerFactory;
        this.namespaceIndexer = namespaceIndexer;
        this.groupIndexer = groupIndexer;
        this.roleIndexer = roleIndexer;
        this.k8sApis = k8sApis;
    }

    public Controller create() {
        return ControllerBuilder.defaultBuilder(this.informerFactory)
                .withName("role-controller")
                .withWorkerCount(1)
                .watch(ResourceGroupWatch::new)
                .watch(RoleWatch::new)
                .watch(workQueue -> new NamespaceWatch(workQueue, this.groupIndexer))
                .withReconciler(new RoleReconciler(this.namespaceIndexer, this.groupIndexer, this.roleIndexer, this.k8sApis.getRbacAuthorizationV1Api()))
                .build();
    }

}
