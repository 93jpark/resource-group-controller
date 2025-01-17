package io.ten1010.aipub.projectcontroller.controller.rbac.aipubuser;

import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.ControllerWatch;
import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.workqueue.WorkQueue;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.models.V1ClusterRole;
import io.ten1010.aipub.projectcontroller.controller.ControllerFactory;
import io.ten1010.aipub.projectcontroller.controller.watch.DefaultControllerWatch;
import io.ten1010.aipub.projectcontroller.controller.watch.OnUpdateFilterFactory;
import io.ten1010.aipub.projectcontroller.controller.watch.RequestBuilderFactory;
import io.ten1010.aipub.projectcontroller.domain.k8s.K8sApiProvider;
import io.ten1010.aipub.projectcontroller.domain.k8s.ReconciliationService;
import io.ten1010.aipub.projectcontroller.domain.k8s.dto.V1alpha1AipubUser;
import io.ten1010.aipub.projectcontroller.domain.k8s.dto.V1alpha1Project;

public class AipubUserClusterRoleControllerFactory implements ControllerFactory {

    private final SharedInformerFactory sharedInformerFactory;
    private final OnUpdateFilterFactory onUpdateFilterFactory;
    private final RequestBuilderFactory requestBuilderFactory;
    private final K8sApiProvider k8sApiProvider;
    private final ReconciliationService reconciliationService;

    public AipubUserClusterRoleControllerFactory(
            SharedInformerFactory sharedInformerFactory,
            K8sApiProvider k8sApiProvider,
            ReconciliationService reconciliationService) {
        this.sharedInformerFactory = sharedInformerFactory;
        this.onUpdateFilterFactory = new OnUpdateFilterFactory();
        this.requestBuilderFactory = new RequestBuilderFactory(sharedInformerFactory);
        this.k8sApiProvider = k8sApiProvider;
        this.reconciliationService = reconciliationService;
    }

    @Override
    public Controller createController() {
        return ControllerBuilder.defaultBuilder(this.sharedInformerFactory)
                .withName("aipub-user-cluster-role-controller")
                .withWorkerCount(1)
                .withReadyFunc(this.sharedInformerFactory.getExistingSharedIndexInformer(V1ClusterRole.class)::hasSynced)
                .withReadyFunc(this.sharedInformerFactory.getExistingSharedIndexInformer(V1alpha1Project.class)::hasSynced)
                .watch(this::createClusterRoleWatch)
                .watch(this::createAipubUserWatch)
                .withReconciler(new AipubUserClusterRoleReconciler(this.sharedInformerFactory, this.k8sApiProvider, this.reconciliationService))
                .build();
    }

    private ControllerWatch<V1ClusterRole> createClusterRoleWatch(WorkQueue<Request> workQueue) {
        DefaultControllerWatch<V1ClusterRole> watch = new DefaultControllerWatch<>(workQueue, V1ClusterRole.class);
        watch.setOnUpdateFilter(this.onUpdateFilterFactory.clusterRoleFilter());
        return watch;
    }

    private ControllerWatch<V1alpha1AipubUser> createAipubUserWatch(WorkQueue<Request> workQueue) {
        DefaultControllerWatch<V1alpha1AipubUser> watch = new DefaultControllerWatch<>(workQueue, V1alpha1AipubUser.class);
        watch.setOnUpdateFilter(this.onUpdateFilterFactory.alwaysFalseFilter());
        watch.setRequestBuilder(this.requestBuilderFactory.aipubUserToClusterRoles());
        return watch;
    }

}
