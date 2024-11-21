package io.ten1010.aipub.projectcontroller.controller.cluster.imagenamespacegroup;

import io.kubernetes.client.extended.controller.ControllerWatch;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.workqueue.WorkQueue;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.openapi.models.V1TypedObjectReference;
import io.ten1010.aipub.projectcontroller.controller.EventHandlerUtil;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroup;

import java.time.Duration;
import java.util.Optional;

public class ImageNamespaceGroupWatch implements ControllerWatch<V1alpha1ImageNamespaceGroup> {

    public static final Duration RESYNC_PERIOD = Duration.ofSeconds(30);

    public static class EventHandler implements ResourceEventHandler<V1alpha1ImageNamespaceGroup> {

        private WorkQueue<Request> queue;

        public EventHandler(WorkQueue<Request> queue) {
            this.queue = queue;
        }

        @Override
        public void onAdd(V1alpha1ImageNamespaceGroup obj) {
            this.queue.add(EventHandlerUtil.buildRequestFromClusterScopedObject(obj));
        }

        @Override
        public void onUpdate(V1alpha1ImageNamespaceGroup oldObj, V1alpha1ImageNamespaceGroup newObj) {
            if (!oldObj.getAipubImageNamespaces().equals(newObj.getAipubImageNamespaces())) {
                this.queue.add(EventHandlerUtil.buildRequestFromClusterScopedObject(newObj));
                return;
            }
            Optional<V1TypedObjectReference> oldPullSecretOpt = Optional.ofNullable(oldObj.getSecret());
            Optional<V1TypedObjectReference> newPullSecretOpt = Optional.ofNullable(newObj.getSecret());
            if (oldPullSecretOpt.equals(newPullSecretOpt)) {
                this.queue.add(EventHandlerUtil.buildRequestFromClusterScopedObject(newObj));
                return;
            }
        }

        @Override
        public void onDelete(V1alpha1ImageNamespaceGroup obj, boolean deletedFinalStateUnknown) {
            this.queue.add(EventHandlerUtil.buildRequestFromClusterScopedObject(obj));
        }
    }

    private WorkQueue<Request> queue;

    public ImageNamespaceGroupWatch(WorkQueue<Request> queue) {
        this.queue = queue;
    }

    @Override
    public Class<V1alpha1ImageNamespaceGroup> getResourceClass() {
        return V1alpha1ImageNamespaceGroup.class;
    }

    @Override
    public ResourceEventHandler<V1alpha1ImageNamespaceGroup> getResourceEventHandler() {
        return new EventHandler(this.queue);
    }

    @Override
    public Duration getResyncPeriod() {
        return RESYNC_PERIOD;
    }

}
