package io.ten1010.aipub.projectcontroller.configuration;

import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.ControllerManager;
import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
import io.kubernetes.client.extended.controller.builder.ControllerManagerBuilder;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.ten1010.aipub.projectcontroller.controller.cluster.NamespaceControllerFactory;
import io.ten1010.aipub.projectcontroller.controller.cluster.NodeControllerFactory;
import io.ten1010.aipub.projectcontroller.controller.cr.AipubUserControllerFactory;
import io.ten1010.aipub.projectcontroller.controller.cr.ImageHubControllerFactory;
import io.ten1010.aipub.projectcontroller.controller.cr.NodeGroupControllerFactory;
import io.ten1010.aipub.projectcontroller.controller.cr.ProjectControllerFactory;
import io.ten1010.aipub.projectcontroller.controller.namespaced.ImagePullSecretReconcilerFactory;
import io.ten1010.aipub.projectcontroller.controller.namespaced.ResourceQuotaControllerFactory;
import io.ten1010.aipub.projectcontroller.controller.rbac.aipub.AipubUserClusterRoleBindingControllerFactory;
import io.ten1010.aipub.projectcontroller.controller.rbac.aipub.AipubUserClusterRoleControllerFactory;
import io.ten1010.aipub.projectcontroller.controller.rbac.member.ClusterRoleBindingControllerFactory;
import io.ten1010.aipub.projectcontroller.controller.rbac.member.ClusterRoleControllerFactory;
import io.ten1010.aipub.projectcontroller.controller.rbac.member.RoleBindingControllerFactory;
import io.ten1010.aipub.projectcontroller.controller.rbac.member.RoleControllerFactory;
import io.ten1010.aipub.projectcontroller.controller.workload.*;
import io.ten1010.aipub.projectcontroller.domain.k8s.K8sApiProvider;
import io.ten1010.aipub.projectcontroller.domain.k8s.K8sObjectType;
import io.ten1010.aipub.projectcontroller.domain.k8s.ReconciliationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ControllerConfiguration {

    @Bean
    public ControllerManager controllerManager(
            SharedInformerFactory globalSharedInformerFactory, List<Controller> controllers, List<WorkloadControllerFactory<?>> workloadControllerFactories) {
        System.out.println(controllers);
        ControllerManagerBuilder builder = ControllerBuilder.controllerManagerBuilder(globalSharedInformerFactory);
        controllers.forEach(builder::addController);
        workloadControllerFactories.forEach(f -> builder.addController(f.createController()));
        ControllerManager controllerManager = builder.build();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(controllerManager);

        return controllerManager;
    }

    @Bean
    public Controller projectController(SharedInformerFactory globalSharedInformerFactory,
                                        K8sApiProvider k8sApiProvider,
                                        ReconciliationService reconciliationService) {
        return new ProjectControllerFactory(globalSharedInformerFactory, k8sApiProvider, reconciliationService)
                .createController();
    }

    @Bean
    public Controller aipubUserController(SharedInformerFactory globalSharedInformerFactory,
                                          K8sApiProvider k8sApiProvider,
                                          ReconciliationService reconciliationService) {
        return new AipubUserControllerFactory(globalSharedInformerFactory, k8sApiProvider, reconciliationService)
                .createController();
    }

    @Bean
    public Controller aipubUserClusterRoleController(SharedInformerFactory globalSharedInformerFactory,
                                                     K8sApiProvider k8sApiProvider,
                                                     ReconciliationService reconciliationService) {
        return new AipubUserClusterRoleControllerFactory(globalSharedInformerFactory, k8sApiProvider, reconciliationService)
                .createController();
    }

    @Bean
    public Controller aipubUserClusterRoleBindingController(SharedInformerFactory globalSharedInformerFactory,
                                                            K8sApiProvider k8sApiProvider,
                                                            ReconciliationService reconciliationService) {
        return new AipubUserClusterRoleBindingControllerFactory(globalSharedInformerFactory, k8sApiProvider, reconciliationService)
                .createController();
    }

    @Bean
    public Controller aodeGroupController(SharedInformerFactory globalSharedInformerFactory,
                                          K8sApiProvider k8sApiProvider,
                                          ReconciliationService reconciliationService) {
        return new NodeGroupControllerFactory(globalSharedInformerFactory, k8sApiProvider, reconciliationService)
                .createController();
    }

    @Bean
    public Controller imageHubController(SharedInformerFactory globalSharedInformerFactory,
                                               K8sApiProvider k8sApiProvider,
                                               ReconciliationService reconciliationService) {
        return new ImageHubControllerFactory(globalSharedInformerFactory, k8sApiProvider, reconciliationService)
                .createController();
    }

    @Bean
    public Controller namespaceController(SharedInformerFactory globalSharedInformerFactory,
                                          K8sApiProvider k8sApiProvider,
                                          ReconciliationService reconciliationService) {
        return new NamespaceControllerFactory(globalSharedInformerFactory, k8sApiProvider, reconciliationService)
                .createController();
    }

    @Bean
    public Controller nodeController(SharedInformerFactory globalSharedInformerFactory,
                                     K8sApiProvider k8sApiProvider,
                                     ReconciliationService reconciliationService) {
        return new NodeControllerFactory(globalSharedInformerFactory, k8sApiProvider, reconciliationService)
                .createController();
    }

    @Bean
    public Controller clusterRoleController(SharedInformerFactory globalSharedInformerFactory,
                                            K8sApiProvider k8sApiProvider,
                                            ReconciliationService reconciliationService) {
        return new ClusterRoleControllerFactory(globalSharedInformerFactory, k8sApiProvider, reconciliationService)
                .createController();
    }

    @Bean
    public Controller clusterRoleBindingController(SharedInformerFactory globalSharedInformerFactory,
                                                   K8sApiProvider k8sApiProvider,
                                                   ReconciliationService reconciliationService) {
        return new ClusterRoleBindingControllerFactory(globalSharedInformerFactory, k8sApiProvider, reconciliationService)
                .createController();
    }

    @Bean
    public Controller roleController(SharedInformerFactory globalSharedInformerFactory,
                                     K8sApiProvider k8sApiProvider,
                                     ReconciliationService reconciliationService) {
        return new RoleControllerFactory(globalSharedInformerFactory, k8sApiProvider, reconciliationService)
                .createController();
    }

    @Bean
    public Controller roleBindingController(SharedInformerFactory globalSharedInformerFactory,
                                            K8sApiProvider k8sApiProvider,
                                            ReconciliationService reconciliationService) {
        return new RoleBindingControllerFactory(globalSharedInformerFactory, k8sApiProvider, reconciliationService)
                .createController();
    }

    @Bean
    public Controller resourceQuotaController(SharedInformerFactory globalSharedInformerFactory,
                                              K8sApiProvider k8sApiProvider,
                                              ReconciliationService reconciliationService) {
        return new ResourceQuotaControllerFactory(globalSharedInformerFactory, k8sApiProvider, reconciliationService)
                .createController();
    }

    @Bean
    public Controller imagePullSecretReconcilerFactory(SharedInformerFactory globalSharedInformerFactory,
                                                       K8sApiProvider k8sApiProvider,
                                                       ReconciliationService reconciliationService) {
        return new ImagePullSecretReconcilerFactory(globalSharedInformerFactory, k8sApiProvider, reconciliationService)
                .createController();
    }

    @Bean
    public Controller podController(SharedInformerFactory globalSharedInformerFactory,
                                    K8sApiProvider k8sApiProvider,
                                    PodNodesResolver podNodesResolver) {
        return new PodControllerFactory(globalSharedInformerFactory, k8sApiProvider, podNodesResolver)
                .createController();
    }

    @Bean
    public RootWorkloadControllerResolver rootControllerResolver(
            SharedInformerFactory globalSharedInformerFactory,
            List<WorkloadControllerFactory<?>> workloadControllerFactories) {
        List<? extends K8sObjectType<?>> supportedTypes = workloadControllerFactories.stream()
                .map(WorkloadControllerFactory::getObjectType)
                .toList();
        return new RootWorkloadControllerResolver(supportedTypes, globalSharedInformerFactory);
    }

    @Bean
    public CompositeWorkloadControllerNodesResolver compositeWorkloadControllerNodesResolver(List<WorkloadControllerFactory<?>> workloadControllerFactories) {
        Map<Class<? extends KubernetesObject>, WorkloadControllerNodesResolver> resolvers = new HashMap<>();
        for (WorkloadControllerFactory<?> factory : workloadControllerFactories) {
            resolvers.put(factory.getObjectType().getObjClass(), factory.getWorkloadNodesResolver());
        }
        return new CompositeWorkloadControllerNodesResolver(resolvers);
    }

    @Bean
    public PodNodesResolver podNodesResolver(
            RootWorkloadControllerResolver rootWorkloadControllerResolver,
            CompositeWorkloadControllerNodesResolver workloadControllerNodesResolver,
            SharedInformerFactory globalSharedInformerFactory) {
        return new PodNodesResolver(rootWorkloadControllerResolver, workloadControllerNodesResolver, globalSharedInformerFactory);
    }

    @Bean
    public CronJobInformerRegistrar cronJobInformerRegistrar(K8sApiProvider k8sApiProvider) {
        return new CronJobInformerRegistrar(k8sApiProvider);
    }

    @Bean
    public CronJobWorkloadControllerFactory cronJobWorkloadControllerFactory(
            SharedInformerFactory globalSharedInformerFactory,
            ReconciliationService reconciliationService,
            K8sApiProvider k8sApiProvider) {
        return new CronJobWorkloadControllerFactory(globalSharedInformerFactory, reconciliationService, k8sApiProvider);
    }

    @Bean
    public DaemonSetInformerRegistrar daemonSetInformerRegistrar(K8sApiProvider k8sApiProvider) {
        return new DaemonSetInformerRegistrar(k8sApiProvider);
    }

    @Bean
    public DaemonSetWorkloadControllerFactory daemonSetWorkloadControllerFactory(
            SharedInformerFactory globalSharedInformerFactory,
            ReconciliationService reconciliationService,
            K8sApiProvider k8sApiProvider) {
        return new DaemonSetWorkloadControllerFactory(globalSharedInformerFactory, reconciliationService, k8sApiProvider);
    }

    @Bean
    public DeploymentInformerRegistrar deploymentInformerRegistrar(K8sApiProvider k8sApiProvider) {
        return new DeploymentInformerRegistrar(k8sApiProvider);
    }

    @Bean
    public DeploymentWorkloadControllerFactory deploymentWorkloadControllerFactory(
            SharedInformerFactory globalSharedInformerFactory,
            ReconciliationService reconciliationService,
            K8sApiProvider k8sApiProvider) {
        return new DeploymentWorkloadControllerFactory(globalSharedInformerFactory, reconciliationService, k8sApiProvider);
    }

    @Bean
    public JobInformerRegistrar jobInformerRegistrar(K8sApiProvider k8sApiProvider) {
        return new JobInformerRegistrar(k8sApiProvider);
    }

    @Bean
    public JobWorkloadControllerFactory jobWorkloadControllerFactory(
            SharedInformerFactory globalSharedInformerFactory,
            ReconciliationService reconciliationService,
            K8sApiProvider k8sApiProvider) {
        return new JobWorkloadControllerFactory(globalSharedInformerFactory, reconciliationService, k8sApiProvider);
    }

    @Bean
    public ReplicaSetInformerRegistrar replicaSetInformerRegistrar(K8sApiProvider k8sApiProvider) {
        return new ReplicaSetInformerRegistrar(k8sApiProvider);
    }

    @Bean
    public ReplicaSetWorkloadControllerFactory replicaSetWorkloadControllerFactory(
            SharedInformerFactory globalSharedInformerFactory,
            ReconciliationService reconciliationService,
            K8sApiProvider k8sApiProvider) {
        return new ReplicaSetWorkloadControllerFactory(globalSharedInformerFactory, reconciliationService, k8sApiProvider);
    }

    @Bean
    public StatefulSetInformerRegistrar statefulSetInformerRegistrar(K8sApiProvider k8sApiProvider) {
        return new StatefulSetInformerRegistrar(k8sApiProvider);
    }

    @Bean
    public StatefulSetWorkloadControllerFactory statefulSetWorkloadControllerFactory(
            SharedInformerFactory globalSharedInformerFactory,
            ReconciliationService reconciliationService,
            K8sApiProvider k8sApiProvider) {
        return new StatefulSetWorkloadControllerFactory(globalSharedInformerFactory, reconciliationService, k8sApiProvider);
    }

}
