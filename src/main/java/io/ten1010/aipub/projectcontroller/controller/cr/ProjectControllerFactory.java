package io.ten1010.aipub.projectcontroller.controller.cr;

import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.ControllerWatch;
import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.workqueue.WorkQueue;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1ResourceQuota;
import io.ten1010.aipub.projectcontroller.controller.ControllerFactory;
import io.ten1010.aipub.projectcontroller.controller.watch.DefaultControllerWatch;
import io.ten1010.aipub.projectcontroller.controller.watch.OnUpdateFilterFactory;
import io.ten1010.aipub.projectcontroller.controller.watch.RequestBuilderFactory;
import io.ten1010.aipub.projectcontroller.domain.k8s.K8sApiProvider;
import io.ten1010.aipub.projectcontroller.domain.k8s.ReconciliationService;
import io.ten1010.aipub.projectcontroller.domain.k8s.dto.V1alpha1AipubUser;
import io.ten1010.aipub.projectcontroller.domain.k8s.dto.V1alpha1ImageNamespace;
import io.ten1010.aipub.projectcontroller.domain.k8s.dto.V1alpha1NodeGroup;
import io.ten1010.aipub.projectcontroller.domain.k8s.dto.V1alpha1Project;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ProjectControllerFactory implements ControllerFactory {

    private final SharedInformerFactory sharedInformerFactory;
    private final OnUpdateFilterFactory onUpdateFilterFactory;
    private final RequestBuilderFactory requestBuilderFactory;
    private final K8sApiProvider k8sApiProvider;
    private final ReconciliationService reconciliationService;

    public ProjectControllerFactory(
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
                .withName("project-controller")
                .withWorkerCount(1)
                .withReadyFunc(this.sharedInformerFactory.getExistingSharedIndexInformer(V1alpha1Project.class)::hasSynced)
                .withReadyFunc(this.sharedInformerFactory.getExistingSharedIndexInformer(V1alpha1AipubUser.class)::hasSynced)
                .withReadyFunc(this.sharedInformerFactory.getExistingSharedIndexInformer(V1ResourceQuota.class)::hasSynced)
                .withReadyFunc(this.sharedInformerFactory.getExistingSharedIndexInformer(V1alpha1NodeGroup.class)::hasSynced)
                .withReadyFunc(this.sharedInformerFactory.getExistingSharedIndexInformer(V1Node.class)::hasSynced)
                .withReadyFunc(this.sharedInformerFactory.getExistingSharedIndexInformer(V1alpha1ImageNamespace.class)::hasSynced)
                .watch(this::createProjectWatch)
                .watch(this::createAipubUserWatch)
                .watch(this::createResourceQuotaWatch)
                .watch(this::createNodeGroupWatch)
                .watch(this::createNodeWatch)
                .watch(this::createImageNamespaceWatch)
                .withReconciler(new ProjectReconciler(this.sharedInformerFactory, this.k8sApiProvider, this.reconciliationService))
                .build();
    }

    private ControllerWatch<V1alpha1Project> createProjectWatch(WorkQueue<Request> workQueue) {
        DefaultControllerWatch<V1alpha1Project> watch = new DefaultControllerWatch<>(workQueue, V1alpha1Project.class);
        watch.setOnUpdateFilter(this.onUpdateFilterFactory.projectSpecBindingFieldFilter());
        return watch;
    }

    private ControllerWatch<V1alpha1AipubUser> createAipubUserWatch(WorkQueue<Request> workQueue) {
        DefaultControllerWatch<V1alpha1AipubUser> watch = new DefaultControllerWatch<>(workQueue, V1alpha1AipubUser.class);
        watch.setOnUpdateFilter(this.onUpdateFilterFactory.alwaysFalseFilter());
        watch.setRequestBuilder(this.requestBuilderFactory.aipubUserToBoundProjects());
        return watch;
    }

    private ControllerWatch<V1ResourceQuota> createResourceQuotaWatch(WorkQueue<Request> workQueue) {
        DefaultControllerWatch<V1ResourceQuota> watch = new DefaultControllerWatch<>(workQueue, V1ResourceQuota.class);
        watch.setOnUpdateFilter(this.onUpdateFilterFactory.resourceQuotaStatusFieldFilter());
        watch.setRequestBuilder(this.requestBuilderFactory.quotaToBoundProjects());
        return watch;
    }

    private ControllerWatch<V1alpha1NodeGroup> createNodeGroupWatch(WorkQueue<Request> workQueue) {
        DefaultControllerWatch<V1alpha1NodeGroup> watch = new DefaultControllerWatch<>(workQueue, V1alpha1NodeGroup.class);
        watch.setOnUpdateFilter(this.onUpdateFilterFactory.nodeGroupSpecFieldFilter());
        watch.setRequestBuilder(this.requestBuilderFactory.nodeGroupToBoundProjects());
        return watch;
    }

    private ControllerWatch<V1Node> createNodeWatch(WorkQueue<Request> workQueue) {
        DefaultControllerWatch<V1Node> watch = new DefaultControllerWatch<>(workQueue, V1Node.class);
        watch.setOnUpdateFilter(this.onUpdateFilterFactory.nodeFilter());
        watch.setRequestBuilder(this.requestBuilderFactory.nodeToProjects());
        return watch;
    }

    private ControllerWatch<V1alpha1ImageNamespace> createImageNamespaceWatch(WorkQueue<Request> workQueue) {
        DefaultControllerWatch<V1alpha1ImageNamespace> watch = new DefaultControllerWatch<>(workQueue, V1alpha1ImageNamespace.class);
        watch.setOnUpdateFilter(this.onUpdateFilterFactory.alwaysFalseFilter());
        watch.setRequestBuilder(this.requestBuilderFactory.imageNamespaceToBoundProjects());
        return watch;
    }

}
