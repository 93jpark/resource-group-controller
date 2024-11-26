package io.ten1010.aipub.projectcontroller.configuration;

import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.models.V1DaemonSet;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1Secret;
import io.ten1010.aipub.projectcontroller.controller.cluster.imagenamespacegroup.ImageNamespaceGroupControllerFactory;
import io.ten1010.aipub.projectcontroller.controller.cluster.imagenamespacegroupbinding.ImageNamespaceGroupBindingControllerFactory;
import io.ten1010.aipub.projectcontroller.controller.cluster.namespace.NamespaceControllerFactory;
import io.ten1010.aipub.projectcontroller.controller.cluster.nodegroup.NodeGroupControllerFactory;
import io.ten1010.aipub.projectcontroller.controller.cluster.nodegroupbinding.NodeGroupBindingControllerFactory;
import io.ten1010.aipub.projectcontroller.controller.cluster.secret.SecretControllerFactory;
import io.ten1010.aipub.projectcontroller.core.K8sApis;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroup;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroupBinding;
import io.ten1010.aipub.projectcontroller.model.V1alpha1NodeGroup;
import io.ten1010.aipub.projectcontroller.model.V1alpha1NodeGroupBinding;
import io.ten1010.aipub.projectcontroller.model.V1alpha1Project;
import io.ten1010.aipub.projectcontroller.service.RegistryRobotService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties(ProjectProperties.class)
@Configuration
public class ProjectControllerConfiguration {

    private ProjectProperties projectProperties;

    @Bean
    public Controller namespaceController(
            SharedInformerFactory sharedInformerFactory,
            K8sApis k8sApis) {
        Indexer<V1Namespace> namespaceIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1Namespace.class)
                .getIndexer();
        Indexer<V1alpha1Project> projectIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1alpha1Project.class)
                .getIndexer();
        return new NamespaceControllerFactory(sharedInformerFactory, namespaceIndexer, projectIndexer, k8sApis).create();
    }
 
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
    public Controller nodeGroupBindingController(
            SharedInformerFactory sharedInformerFactory,
            K8sApis k8sApis) {
        Indexer<V1alpha1NodeGroup> nodeGroupIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1alpha1NodeGroup.class)
                .getIndexer();
        Indexer<V1alpha1NodeGroupBinding> nodeGroupBindingIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1alpha1NodeGroupBinding.class)
                .getIndexer();
        Indexer<V1alpha1Project> projectIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1alpha1Project.class)
                .getIndexer();

        return new NodeGroupBindingControllerFactory(sharedInformerFactory, k8sApis)
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
        return new ImageNamespaceGroupControllerFactory(sharedInformerFactory, imageNamespaceGroupIndexer, secretIndexer, k8sApis, registryRobotService, this.projectProperties)
                .create();
    }

    @Bean
    public Controller imageNamespaceGroupBindingController(
            SharedInformerFactory sharedInformerFactory,
            K8sApis k8sApis) {
        Indexer<V1alpha1ImageNamespaceGroup> imageNamespaceGroupIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1alpha1ImageNamespaceGroup.class)
                .getIndexer();
        Indexer<V1alpha1ImageNamespaceGroupBinding> imageNamespaceGroupBindingIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1alpha1ImageNamespaceGroupBinding.class)
                .getIndexer();
        Indexer<V1alpha1Project> projectIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1alpha1Project.class)
                .getIndexer();
        return new ImageNamespaceGroupBindingControllerFactory(sharedInformerFactory, imageNamespaceGroupIndexer, imageNamespaceGroupBindingIndexer, projectIndexer, k8sApis)
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
        Indexer<V1alpha1Project> projectIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1alpha1Project.class)
                .getIndexer();
        Indexer<V1Secret> secretIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1Secret.class)
                .getIndexer();
        return new SecretControllerFactory(sharedInformerFactory, imageNamespaceGroupIndexer, projectIndexer, secretIndexer, k8sApis, registryRobotService, this.projectProperties)
                .create();
    }

}
