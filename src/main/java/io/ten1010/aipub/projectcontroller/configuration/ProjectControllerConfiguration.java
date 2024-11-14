package io.ten1010.aipub.projectcontroller.configuration;

import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.models.V1Secret;
import io.ten1010.aipub.projectcontroller.controller.cluster.RobotAccountService;
import io.ten1010.aipub.projectcontroller.controller.cluster.imagenamespacegroup.ImageNamespaceGroupControllerFactory;
import io.ten1010.aipub.projectcontroller.controller.cluster.secret.SecretControllerFactory;
import io.ten1010.aipub.projectcontroller.core.K8sApis;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroup;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProjectControllerConfiguration {

    @Bean
    public Controller imageNamespaceGroupController(
            SharedInformerFactory sharedInformerFactory,
            K8sApis k8sApis,
            RobotAccountService robotAccountService) {
        Indexer<V1alpha1ImageNamespaceGroup> imageNamespaceGroupIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1alpha1ImageNamespaceGroup.class)
                .getIndexer();
        Indexer<V1Secret> secretIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1Secret.class)
                .getIndexer();
        return new ImageNamespaceGroupControllerFactory(sharedInformerFactory, imageNamespaceGroupIndexer, secretIndexer, k8sApis, robotAccountService)
                .create();
    }

    @Bean
    public Controller secretController(
            SharedInformerFactory sharedInformerFactory,
            K8sApis k8sApis,
            RobotAccountService robotAccountService) {
        Indexer<V1alpha1ImageNamespaceGroup> imageNamespaceGroupIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1alpha1ImageNamespaceGroup.class)
                .getIndexer();
        Indexer<V1Secret> secretIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1Secret.class)
                .getIndexer();
        return new SecretControllerFactory(sharedInformerFactory, imageNamespaceGroupIndexer, secretIndexer, k8sApis, robotAccountService)
                .create();
    }

}
