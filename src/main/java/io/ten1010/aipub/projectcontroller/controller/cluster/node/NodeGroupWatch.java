package io.ten1010.aipub.projectcontroller.controller.cluster.node;

import io.kubernetes.client.extended.controller.ControllerWatch;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.workqueue.WorkQueue;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.ten1010.aipub.projectcontroller.model.V1alpha1NodeGroup;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class NodeGroupWatch implements ControllerWatch<V1alpha1NodeGroup> {

    public static final Duration RESYNC_PERIOD = Duration.ofSeconds(30);

    public static class EventHandler implements ResourceEventHandler<V1alpha1NodeGroup> {

        private static List<String> getNodes(V1alpha1NodeGroup obj) {
            return obj.getNodes();
        }

        private static Set<String> getAddedOrDeletedNodes(List<String> oldNodes, List<String> newNodes) {
            Set<String> deleted = new HashSet<>(oldNodes);
            deleted.removeAll(newNodes);
            Set<String> added = new HashSet<>(newNodes);
            added.removeAll(oldNodes);
            deleted.addAll(added);

            return deleted;
        }

        private WorkQueue<Request> queue;

        public EventHandler(WorkQueue<Request> queue) {
            this.queue = queue;
        }

        @Override
        public void onAdd(V1alpha1NodeGroup obj) {
            Set<Request> requests = getNodes(obj).stream()
                    .map(Request::new)
                    .collect(Collectors.toSet());
            requests.forEach(this.queue::add);
        }

        @Override
        public void onUpdate(V1alpha1NodeGroup oldObj, V1alpha1NodeGroup newObj) {
            Set<Request> requests = getAddedOrDeletedNodes(getNodes(oldObj), getNodes(newObj)).stream()
                    .map(Request::new)
                    .collect(Collectors.toSet());
            requests.forEach(this.queue::add);
        }

        @Override
        public void onDelete(V1alpha1NodeGroup obj, boolean deletedFinalStateUnknown) {
            Set<Request> requests = getNodes(obj).stream()
                    .map(Request::new)
                    .collect(Collectors.toSet());
            requests.forEach(this.queue::add);
        }

    }

    private WorkQueue<Request> queue;

    public NodeGroupWatch(WorkQueue<Request> queue) {
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
