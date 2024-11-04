package io.ten1010.aipub.projectcontroller.configuration;

import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.ControllerManager;
import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
import io.kubernetes.client.extended.controller.builder.ControllerManagerBuilder;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.ten1010.aipub.projectcontroller.controller.ControllerFactory;
import io.ten1010.aipub.projectcontroller.controller.cluster.NamespaceControllerFactory;
import io.ten1010.aipub.projectcontroller.controller.cluster.NodeControllerFactory;
import io.ten1010.aipub.projectcontroller.controller.cr.AipubUserControllerFactory;
import io.ten1010.aipub.projectcontroller.controller.cr.ImageNamespaceControllerFactory;
import io.ten1010.aipub.projectcontroller.controller.cr.NodeGroupControllerFactory;
import io.ten1010.aipub.projectcontroller.controller.cr.ProjectControllerFactory;
import io.ten1010.aipub.projectcontroller.controller.namespaced.ResourceQuotaControllerFactory;
import io.ten1010.aipub.projectcontroller.controller.rbac.aipubuser.AipubUserClusterRoleBindingControllerFactory;
import io.ten1010.aipub.projectcontroller.controller.rbac.aipubuser.AipubUserClusterRoleControllerFactory;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ControllerConfiguration {

    @Bean
    public ControllerManager controllerManager(SharedInformerFactory sharedInformerFactory, List<Controller> controllers) {
        ControllerManagerBuilder builder = ControllerBuilder.controllerManagerBuilder(sharedInformerFactory);
        controllers.forEach(builder::addController);
        ControllerManager controllerManager = builder.build();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(controllerManager);

        return controllerManager;
    }

    @Bean
    public List<Controller> controllers(
            SharedInformerFactory sharedInformerFactory,
            K8sApiProvider k8sApiProvider,
            ReconciliationService reconciliationService,
            List<WorkloadControllerFactory<?>> workloadControllerFactories,
            PodNodesResolver podNodesResolver) {
        List<ControllerFactory> factories = new ArrayList<>();
        factories.add(new ProjectControllerFactory(sharedInformerFactory, k8sApiProvider, reconciliationService));
        factories.add(new AipubUserControllerFactory(sharedInformerFactory, k8sApiProvider, reconciliationService));
        factories.add(new AipubUserClusterRoleControllerFactory(sharedInformerFactory, k8sApiProvider, reconciliationService));
        factories.add(new AipubUserClusterRoleBindingControllerFactory(sharedInformerFactory, k8sApiProvider, reconciliationService));
        factories.add(new NodeGroupControllerFactory(sharedInformerFactory, k8sApiProvider, reconciliationService));
        factories.add(new ImageNamespaceControllerFactory(sharedInformerFactory, k8sApiProvider, reconciliationService));
        factories.add(new NamespaceControllerFactory(sharedInformerFactory, k8sApiProvider, reconciliationService));
        factories.add(new NodeControllerFactory(sharedInformerFactory, k8sApiProvider, reconciliationService));
        factories.add(new ClusterRoleControllerFactory(sharedInformerFactory, k8sApiProvider, reconciliationService));
        factories.add(new ClusterRoleBindingControllerFactory(sharedInformerFactory, k8sApiProvider, reconciliationService));
        factories.add(new RoleControllerFactory(sharedInformerFactory, k8sApiProvider, reconciliationService));
        factories.add(new RoleBindingControllerFactory(sharedInformerFactory, k8sApiProvider, reconciliationService));
        factories.add(new ResourceQuotaControllerFactory(sharedInformerFactory, k8sApiProvider, reconciliationService));
        factories.add(new PodControllerFactory(sharedInformerFactory, k8sApiProvider, podNodesResolver));
        factories.addAll(workloadControllerFactories);

        return factories.stream()
                .map(ControllerFactory::createController)
                .toList();
    }

    @Bean
    public RootWorkloadControllerResolver rootControllerResolver(
            SharedInformerFactory sharedInformerFactory,
            List<WorkloadControllerFactory<?>> workloadControllerFactories) {
        List<? extends K8sObjectType<?>> supportedTypes = workloadControllerFactories.stream()
                .map(WorkloadControllerFactory::getObjectType)
                .toList();
        return new RootWorkloadControllerResolver(supportedTypes, sharedInformerFactory);
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
            SharedInformerFactory sharedInformerFactory) {
        return new PodNodesResolver(rootWorkloadControllerResolver, workloadControllerNodesResolver, sharedInformerFactory);
    }

    @Bean
    public CronJobInformerRegistrar cronJobInformerRegistrar(K8sApiProvider k8sApiProvider) {
        return new CronJobInformerRegistrar(k8sApiProvider);
    }

    @Bean
    public CronJobWorkloadControllerFactory cronJobWorkloadControllerFactory(
            SharedInformerFactory sharedInformerFactory,
            ReconciliationService reconciliationService,
            K8sApiProvider k8sApiProvider) {
        return new CronJobWorkloadControllerFactory(sharedInformerFactory, reconciliationService, k8sApiProvider);
    }

    @Bean
    public DaemonSetInformerRegistrar daemonSetInformerRegistrar(K8sApiProvider k8sApiProvider) {
        return new DaemonSetInformerRegistrar(k8sApiProvider);
    }

    @Bean
    public DaemonSetWorkloadControllerFactory daemonSetWorkloadControllerFactory(
            SharedInformerFactory sharedInformerFactory,
            ReconciliationService reconciliationService,
            K8sApiProvider k8sApiProvider) {
        return new DaemonSetWorkloadControllerFactory(sharedInformerFactory, reconciliationService, k8sApiProvider);
    }

    @Bean
    public DeploymentInformerRegistrar deploymentInformerRegistrar(K8sApiProvider k8sApiProvider) {
        return new DeploymentInformerRegistrar(k8sApiProvider);
    }

    @Bean
    public DeploymentWorkloadControllerFactory deploymentWorkloadControllerFactory(
            SharedInformerFactory sharedInformerFactory,
            ReconciliationService reconciliationService,
            K8sApiProvider k8sApiProvider) {
        return new DeploymentWorkloadControllerFactory(sharedInformerFactory, reconciliationService, k8sApiProvider);
    }

    @Bean
    public JobInformerRegistrar jobInformerRegistrar(K8sApiProvider k8sApiProvider) {
        return new JobInformerRegistrar(k8sApiProvider);
    }

    @Bean
    public JobWorkloadControllerFactory jobWorkloadControllerFactory(
            SharedInformerFactory sharedInformerFactory,
            ReconciliationService reconciliationService,
            K8sApiProvider k8sApiProvider) {
        return new JobWorkloadControllerFactory(sharedInformerFactory, reconciliationService, k8sApiProvider);
    }

    @Bean
    public ReplicaSetInformerRegistrar replicaSetInformerRegistrar(K8sApiProvider k8sApiProvider) {
        return new ReplicaSetInformerRegistrar(k8sApiProvider);
    }

    @Bean
    public ReplicaSetWorkloadControllerFactory replicaSetWorkloadControllerFactory(
            SharedInformerFactory sharedInformerFactory,
            ReconciliationService reconciliationService,
            K8sApiProvider k8sApiProvider) {
        return new ReplicaSetWorkloadControllerFactory(sharedInformerFactory, reconciliationService, k8sApiProvider);
    }

    @Bean
    public StatefulSetInformerRegistrar statefulSetInformerRegistrar(K8sApiProvider k8sApiProvider) {
        return new StatefulSetInformerRegistrar(k8sApiProvider);
    }

    @Bean
    public StatefulSetWorkloadControllerFactory statefulSetWorkloadControllerFactory(
            SharedInformerFactory sharedInformerFactory,
            ReconciliationService reconciliationService,
            K8sApiProvider k8sApiProvider) {
        return new StatefulSetWorkloadControllerFactory(sharedInformerFactory, reconciliationService, k8sApiProvider);
    }

}
