package io.ten1010.aipub.projectcontroller.controller.cluster.clusterrole;

import io.kubernetes.client.extended.controller.ControllerWatch;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.workqueue.WorkQueue;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.ten1010.aipub.projectcontroller.core.K8sObjectUtil;
import io.ten1010.aipub.projectcontroller.model.V1alpha1NodeGroup;

import java.time.Duration;

public class ResourceGroupWatch implements ControllerWatch<V1alpha1NodeGroup> {

    public static final Duration RESYNC_PERIOD = Duration.ofSeconds(30);

    public static class EventHandler implements ResourceEventHandler<V1alpha1NodeGroup> {

        private static Request buildRequest(String groupName) {
            return new Request(new ResourceGroupClusterRoleName(groupName).getName());
        }

        private WorkQueue<Request> queue;

        public EventHandler(WorkQueue<Request> queue) {
            this.queue = queue;
        }

        @Override
        public void onAdd(V1alpha1NodeGroup obj) {
            String groupName = K8sObjectUtil.getName(obj);
            this.queue.add(buildRequest(groupName));
        }

        @Override
        public void onUpdate(V1alpha1NodeGroup oldObj, V1alpha1NodeGroup newObj) {
            String groupName = K8sObjectUtil.getName(newObj);
            this.queue.add(buildRequest(groupName));
        }

        @Override
        public void onDelete(V1alpha1NodeGroup obj, boolean deletedFinalStateUnknown) {
        }

    }

    private WorkQueue<Request> queue;

    public ResourceGroupWatch(WorkQueue<Request> queue) {
        this.queue = queue;
    }

    @Override
    public Class<V1alpha1NodeGroup> getResourceClass() {
        return V1alpha1NodeGroup.class;
    }

    @Override
    public ResourceEventHandler<V1alpha1NodeGroup> getResourceEventHandler() {
        return new EventHandler(this.queue);
    }

    @Override
    public Duration getResyncPeriod() {
        return RESYNC_PERIOD;
    }

}
