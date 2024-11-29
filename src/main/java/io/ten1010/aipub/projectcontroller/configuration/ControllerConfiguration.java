package io.ten1010.aipub.projectcontroller.configuration;

import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
import io.kubernetes.client.extended.controller.builder.ControllerManagerBuilder;
import io.kubernetes.client.extended.event.legacy.EventRecorder;
import io.kubernetes.client.extended.event.legacy.LegacyEventBroadcaster;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.models.V1EventSource;
import io.kubernetes.client.openapi.models.V1Secret;
import io.ten1010.aipub.projectcontroller.controller.Reconciliation;
import io.ten1010.aipub.projectcontroller.controller.SharedInformerFactoryFactory;
import io.ten1010.aipub.projectcontroller.core.K8sApis;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroup;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroupBinding;
import io.ten1010.aipub.projectcontroller.model.V1alpha1NodeGroup;
import io.ten1010.aipub.projectcontroller.model.V1alpha1NodeGroupBinding;
import io.ten1010.aipub.projectcontroller.model.V1alpha1Project;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

@Configuration
public class ControllerConfiguration {

    public static final String COMPONENT_NAME = "resource-group-controller";

    private static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    @Profile("enable-controller-runner")
    @Bean
    public CommandLineRunner controllerRunner(SharedInformerFactory sharedInformerFactory, List<Controller> controllers) {
        ControllerManagerBuilder builder = ControllerBuilder.controllerManagerBuilder(sharedInformerFactory);
        controllers.forEach(builder::addController);
        return args -> builder.build().run();
    }

    @Bean
    public SharedInformerFactory sharedInformerFactory(K8sApis k8sApis) {
        return new SharedInformerFactoryFactory(k8sApis)
                .create();
    }

    @Bean
    public Reconciliation reconciliation(SharedInformerFactory sharedInformerFactory) {
        Indexer<V1alpha1Project> projectIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1alpha1Project.class)
                .getIndexer();
        Indexer<V1alpha1NodeGroup> nodeGroupIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1alpha1NodeGroup.class)
                .getIndexer();
        Indexer<V1alpha1NodeGroupBinding> nodeGroupBindingIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1alpha1NodeGroupBinding.class)
                .getIndexer();
        Indexer<V1alpha1ImageNamespaceGroup> imageNamespaceGroupIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1alpha1ImageNamespaceGroup.class)
                .getIndexer();
        Indexer<V1alpha1ImageNamespaceGroupBinding> imageNamespaceGroupBindingIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1alpha1ImageNamespaceGroupBinding.class)
                .getIndexer();
        Indexer<V1Secret> secretIndexer = sharedInformerFactory
                .getExistingSharedIndexInformer(V1Secret.class)
                .getIndexer();
        return new Reconciliation(
                projectIndexer,
                nodeGroupIndexer,
                nodeGroupBindingIndexer,
                imageNamespaceGroupIndexer,
                imageNamespaceGroupBindingIndexer,
                secretIndexer);
    }

    @Bean(initMethod = "startRecording", destroyMethod = "shutdown")
    public LegacyEventBroadcaster broadcaster(K8sApis k8sApis) {
        return new LegacyEventBroadcaster(k8sApis.getCoreV1Api());
    }

    @Bean
    public EventRecorder eventRecorder(LegacyEventBroadcaster broadcaster) {
        V1EventSource eventSource = new V1EventSource();
        eventSource.host(getHostName()).component(COMPONENT_NAME);
        return broadcaster.newRecorder(eventSource);
    }

}
