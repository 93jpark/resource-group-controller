package io.ten1010.aipub.projectcontroller.controller.cluster.project;

import io.kubernetes.client.extended.controller.ControllerWatch;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.workqueue.WorkQueue;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.ten1010.aipub.projectcontroller.controller.EventHandlerUtil;
import io.ten1010.aipub.projectcontroller.core.K8sObjectUtil;
import io.ten1010.aipub.projectcontroller.model.V1alpha1Project;

import java.time.Duration;

public class ProjectWatch implements ControllerWatch<V1alpha1Project> {

    public static final Duration RESYNC_PERIOD = Duration.ofSeconds(30);

    public static class EventHandler implements ResourceEventHandler<V1alpha1Project> {

        private WorkQueue<Request> queue;
        private Indexer<V1Namespace> namespaceIndexer;

        public EventHandler(WorkQueue<Request> queue, Indexer<V1Namespace> namespaceIndexer) {
            this.queue = queue;
            this.namespaceIndexer = namespaceIndexer;
        }

        @Override
        public void onAdd(V1alpha1Project obj) {
            this.queue.add(EventHandlerUtil.buildRequestFromClusterScopedObject(obj));
        }

        @Override
        public void onUpdate(V1alpha1Project oldObj, V1alpha1Project newObj) {
            if (!K8sObjectUtil.getName(oldObj).equals(K8sObjectUtil.getName(newObj))) {
                this.queue.add(EventHandlerUtil.buildRequestFromClusterScopedObject(newObj));
                return;
            }
        }

        @Override
        public void onDelete(V1alpha1Project obj, boolean deletedFinalStateUnknown) {
            this.queue.add(EventHandlerUtil.buildRequestFromClusterScopedObject(obj));
        }

    }

    private WorkQueue<Request> queue;
    private Indexer<V1Namespace> namespaceIndexer;

    public ProjectWatch(WorkQueue<Request> queue, Indexer<V1Namespace> namespaceIndexer) {
        this.queue = queue;
        this.namespaceIndexer = namespaceIndexer;
    }

    @Override
    public Class<V1alpha1Project> getResourceClass() {
        return V1alpha1Project.class;
    }

    @Override
    public ResourceEventHandler<V1alpha1Project> getResourceEventHandler() {
        return new EventHandler(this.queue, this.namespaceIndexer);
    }

    @Override
    public Duration getResyncPeriod() {
        return RESYNC_PERIOD;
    }

}
