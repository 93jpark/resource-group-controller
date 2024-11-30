package io.ten1010.aipub.projectcontroller.controller.cluster.nodegroupbinding;

import io.kubernetes.client.extended.controller.ControllerWatch;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.workqueue.WorkQueue;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.ten1010.aipub.projectcontroller.controller.EventHandlerUtil;
import io.ten1010.aipub.projectcontroller.model.V1alpha1NodeGroupBinding;

import java.time.Duration;

public class NodeGroupBindingWatch implements ControllerWatch<V1alpha1NodeGroupBinding> {

    public static final Duration RESYNC_PERIOD = Duration.ofSeconds(30);

    public static class EventHandler implements ResourceEventHandler<V1alpha1NodeGroupBinding> {

        private WorkQueue<Request> queue;

        public EventHandler(WorkQueue<Request> queue) {
            this.queue = queue;
        }

        @Override
        public void onAdd(V1alpha1NodeGroupBinding obj) {
            this.queue.add(EventHandlerUtil.buildRequestFromClusterScopedObject(obj));
        }

        @Override
        public void onUpdate(V1alpha1NodeGroupBinding oldObj, V1alpha1NodeGroupBinding newObj) {
            this.queue.add(EventHandlerUtil.buildRequestFromClusterScopedObject(newObj));
        }

        @Override
        public void onDelete(V1alpha1NodeGroupBinding obj, boolean deletedFinalStateUnknown) {
            this.queue.add(EventHandlerUtil.buildRequestFromClusterScopedObject(obj));
        }
    }


    private WorkQueue<Request> queue;

    public NodeGroupBindingWatch(WorkQueue<Request> queue) {
        this.queue = queue;
    }

    @Override
    public Class<V1alpha1NodeGroupBinding> getResourceClass() {
        return V1alpha1NodeGroupBinding.class;
    }

    @Override
    public ResourceEventHandler<V1alpha1NodeGroupBinding> getResourceEventHandler() {
        return new EventHandler(this.queue);
    }

    @Override
    public Duration getResyncPeriod() {
        return RESYNC_PERIOD;
    }
}
