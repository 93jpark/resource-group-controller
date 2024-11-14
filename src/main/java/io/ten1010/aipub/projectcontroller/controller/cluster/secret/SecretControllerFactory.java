package io.ten1010.aipub.projectcontroller.controller.cluster.secret;

import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.models.V1Secret;
import io.ten1010.aipub.projectcontroller.controller.cluster.RobotAccountService;
import io.ten1010.aipub.projectcontroller.core.K8sApis;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroup;

public class SecretControllerFactory {

    private SharedInformerFactory sharedInformerFactory;
    private Indexer<V1alpha1ImageNamespaceGroup> imageNamespaceGroupIndexer;
    private Indexer<V1Secret> secretIndexer;
    private K8sApis k8sApis;
    private RobotAccountService robotAccountService;

    public SecretControllerFactory(
            SharedInformerFactory sharedInformerFactory,
            Indexer<V1alpha1ImageNamespaceGroup> imageNamespaceGroupIndexer,
            Indexer<V1Secret> secretIndexer,
            K8sApis k8sApis,
            RobotAccountService robotAccountService) {
        this.sharedInformerFactory = sharedInformerFactory;
        this.imageNamespaceGroupIndexer = imageNamespaceGroupIndexer;
        this.secretIndexer = secretIndexer;
        this.k8sApis = k8sApis;
        this.robotAccountService = robotAccountService;
    }

    public Controller create() {
        return ControllerBuilder.defaultBuilder(this.sharedInformerFactory)
                .withName("secret-controller")
                .withWorkerCount(1)
                .watch(workQueue -> new SecretWatch(workQueue, this.imageNamespaceGroupIndexer))
                .watch(workQueue -> new ImageNamespaceGroupWatch(workQueue, this.secretIndexer))
                .withReconciler(new SecretReconciler(
                        this.imageNamespaceGroupIndexer,
                        this.secretIndexer,
                        this.k8sApis.getCoreV1Api(),
                        this.robotAccountService
                ))
                .build();
    }

}
