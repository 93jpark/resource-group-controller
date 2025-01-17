package io.ten1010.aipub.projectcontroller.controller.namespaced;

import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.ControllerWatch;
import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.workqueue.WorkQueue;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.models.V1ResourceQuota;
import io.ten1010.aipub.projectcontroller.controller.ControllerFactory;
import io.ten1010.aipub.projectcontroller.controller.watch.DefaultControllerWatch;
import io.ten1010.aipub.projectcontroller.controller.watch.OnUpdateFilterFactory;
import io.ten1010.aipub.projectcontroller.controller.watch.RequestBuilderFactory;
import io.ten1010.aipub.projectcontroller.domain.k8s.K8sApiProvider;
import io.ten1010.aipub.projectcontroller.domain.k8s.ReconciliationService;
import io.ten1010.aipub.projectcontroller.domain.k8s.ResourceQuotaNameResolver;
import io.ten1010.aipub.projectcontroller.domain.k8s.dto.V1alpha1Project;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ResourceQuotaControllerFactory implements ControllerFactory {

    private final SharedInformerFactory sharedInformerFactory;
    private final ResourceQuotaNameResolver quotaNameResolver;
    private final OnUpdateFilterFactory onUpdateFilterFactory;
    private final RequestBuilderFactory requestBuilderFactory;
    private final K8sApiProvider k8sApiProvider;
    private final ReconciliationService reconciliationService;

    public ResourceQuotaControllerFactory(
            SharedInformerFactory sharedInformerFactory,
            K8sApiProvider k8sApiProvider,
            ReconciliationService reconciliationService) {
        this.sharedInformerFactory = sharedInformerFactory;
        this.quotaNameResolver = new ResourceQuotaNameResolver();
        this.onUpdateFilterFactory = new OnUpdateFilterFactory();
        this.requestBuilderFactory = new RequestBuilderFactory(sharedInformerFactory);
        this.k8sApiProvider = k8sApiProvider;
        this.reconciliationService = reconciliationService;
    }

    @Override
    public Controller createController() {
        return ControllerBuilder.defaultBuilder(this.sharedInformerFactory)
                .withName("resource-quota-controller")
                .withWorkerCount(1)
                .withReadyFunc(this.sharedInformerFactory.getExistingSharedIndexInformer(V1ResourceQuota.class)::hasSynced)
                .withReadyFunc(this.sharedInformerFactory.getExistingSharedIndexInformer(V1alpha1Project.class)::hasSynced)
                .watch(this::createResourceQuotaWatch)
                .watch(this::createProjectWatch)
                .withReconciler(new ResourceQuotaReconciler(this.sharedInformerFactory, this.k8sApiProvider, this.reconciliationService))
                .build();
    }

    private ControllerWatch<V1ResourceQuota> createResourceQuotaWatch(WorkQueue<Request> workQueue) {
        DefaultControllerWatch<V1ResourceQuota> watch = new DefaultControllerWatch<>(workQueue, V1ResourceQuota.class);
        watch.setOnUpdateFilter(this.onUpdateFilterFactory.resourceQuotaFilter());
        return watch;
    }

    private ControllerWatch<V1alpha1Project> createProjectWatch(WorkQueue<Request> workQueue) {
        DefaultControllerWatch<V1alpha1Project> watch = new DefaultControllerWatch<>(workQueue, V1alpha1Project.class);
        watch.setOnUpdateFilter(this.onUpdateFilterFactory.projectSpecQuotaFieldFilter());
        watch.setRequestBuilder(this.requestBuilderFactory.projectToResourceQuotas());
        return watch;
    }

}
