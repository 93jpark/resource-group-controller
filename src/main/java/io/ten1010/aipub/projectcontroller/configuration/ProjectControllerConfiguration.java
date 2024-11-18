package io.ten1010.aipub.projectcontroller.configuration;

import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.models.V1DaemonSet;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1Secret;
import io.ten1010.aipub.projectcontroller.service.RegistryRobotService;
import io.ten1010.aipub.projectcontroller.controller.cluster.imagenamespacegroup.ImageNamespaceGroupControllerFactory;
import io.ten1010.aipub.projectcontroller.controller.cluster.nodegroup.NodeGroupControllerFactory;
import io.ten1010.aipub.projectcontroller.controller.cluster.secret.SecretControllerFactory;
import io.ten1010.aipub.projectcontroller.core.K8sApis;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroup;
import io.ten1010.aipub.projectcontroller.model.V1alpha1NodeGroup;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProjectControllerConfiguration {

    @Bean
    public Controller nodeGroupController(
            SharedInformerFactory sharedInformerFactory,
            K8sApis k8sApis) {
        Indexer<V1alpha1NodeGroup> nodeGroupIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1alpha1NodeGroup.class)
                .getIndexer();
        Indexer<V1Node> nodeIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1Node.class)
                .getIndexer();
        Indexer<V1DaemonSet> daemonSetIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1DaemonSet.class)
                .getIndexer();
        Indexer<V1Namespace> namespaceIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1Namespace.class)
                .getIndexer();
        return new NodeGroupControllerFactory(sharedInformerFactory, nodeGroupIndexer, nodeIndexer, daemonSetIndexer, namespaceIndexer, k8sApis)
                .create();
    }

    @Bean
    public Controller imageNamespaceGroupController(
            SharedInformerFactory sharedInformerFactory,
            K8sApis k8sApis,
            RegistryRobotService registryRobotService) {
        Indexer<V1alpha1ImageNamespaceGroup> imageNamespaceGroupIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1alpha1ImageNamespaceGroup.class)
                .getIndexer();
        Indexer<V1Secret> secretIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1Secret.class)
                .getIndexer();
        return new ImageNamespaceGroupControllerFactory(sharedInformerFactory, imageNamespaceGroupIndexer, secretIndexer, k8sApis, registryRobotService)
                .create();
    }

    @Bean
    public Controller secretController(
            SharedInformerFactory sharedInformerFactory,
            K8sApis k8sApis,
            RegistryRobotService registryRobotService) {
        Indexer<V1alpha1ImageNamespaceGroup> imageNamespaceGroupIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1alpha1ImageNamespaceGroup.class)
                .getIndexer();
        Indexer<V1Secret> secretIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1Secret.class)
                .getIndexer();
        return new SecretControllerFactory(sharedInformerFactory, imageNamespaceGroupIndexer, secretIndexer, k8sApis, registryRobotService)
                .create();
    }

}
