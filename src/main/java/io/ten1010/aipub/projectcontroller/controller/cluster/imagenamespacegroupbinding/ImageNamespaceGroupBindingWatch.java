package io.ten1010.aipub.projectcontroller.controller.cluster.imagenamespacegroupbinding;

import io.kubernetes.client.extended.controller.ControllerWatch;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.workqueue.WorkQueue;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.ten1010.aipub.projectcontroller.controller.EventHandlerUtil;
import io.ten1010.aipub.projectcontroller.core.K8sObjectUtil;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroupBinding;

import java.time.Duration;

public class ImageNamespaceGroupBindingWatch implements ControllerWatch<V1alpha1ImageNamespaceGroupBinding> {

    public static final Duration RESYNC_PERIOD = Duration.ofSeconds(30);

    public static class EventHandler implements ResourceEventHandler<V1alpha1ImageNamespaceGroupBinding> {

        private WorkQueue<Request> queue;

        public EventHandler(WorkQueue<Request> queue) {
            this.queue = queue;
        }

        @Override
        public void onAdd(V1alpha1ImageNamespaceGroupBinding obj) {
            this.queue.add(EventHandlerUtil.buildRequestFromClusterScopedObject(obj));
        }

        @Override
        public void onUpdate(V1alpha1ImageNamespaceGroupBinding oldObj, V1alpha1ImageNamespaceGroupBinding newObj) {
            if (!K8sObjectUtil.getName(oldObj).equals(K8sObjectUtil.getName(newObj))) {
                this.queue.add(EventHandlerUtil.buildRequestFromClusterScopedObject(oldObj));
                this.queue.add(EventHandlerUtil.buildRequestFromClusterScopedObject(newObj));
                return;
            }
        }

        @Override
        public void onDelete(V1alpha1ImageNamespaceGroupBinding obj, boolean deletedFinalStateUnknown) {
        }

    }

    private WorkQueue<Request> queue;

    public ImageNamespaceGroupBindingWatch(WorkQueue<Request> queue) {
        this.queue = queue;
    }

    @Override
    public Class<V1alpha1ImageNamespaceGroupBinding> getResourceClass() {
        return V1alpha1ImageNamespaceGroupBinding.class;
    }

    @Override
    public ResourceEventHandler<V1alpha1ImageNamespaceGroupBinding> getResourceEventHandler() {
        return new EventHandler(this.queue);
    }

    @Override
    public Duration getResyncPeriod() {
        return RESYNC_PERIOD;
    }

}
