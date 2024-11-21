package io.ten1010.aipub.projectcontroller.controller.cluster.nodegroup;

import io.kubernetes.client.extended.controller.ControllerWatch;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.workqueue.WorkQueue;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.models.V1Node;
import io.ten1010.aipub.projectcontroller.controller.EventHandlerUtil;
import io.ten1010.aipub.projectcontroller.model.V1alpha1NodeGroup;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public class NodeGroupWatch implements ControllerWatch<V1alpha1NodeGroup> {

    public static final Duration RESYNC_PERIOD = Duration.ofSeconds(30);

    public static class EventHandler implements ResourceEventHandler<V1alpha1NodeGroup> {

        private WorkQueue<Request> queue;
        private Indexer<V1Node> nodeIndexer;

        private static List<String> getNodes(V1alpha1NodeGroup obj) {
            return obj.getNodes();
        }

        public EventHandler(WorkQueue<Request> queue, Indexer<V1Node> nodeIndexer) {
            this.queue = queue;
            this.nodeIndexer = nodeIndexer;
        }

        @Override
        public void onAdd(V1alpha1NodeGroup obj) {
            this.queue.add(EventHandlerUtil.buildRequestFromClusterScopedObject(obj));
        }

        @Override
        public void onUpdate(V1alpha1NodeGroup oldObj, V1alpha1NodeGroup newObj) {
            if (!oldObj.getNodes().equals(newObj.getNodes())) {
                queue.add(EventHandlerUtil.buildRequestFromClusterScopedObject(newObj));
                return;
            }
            if (!Objects.equals(oldObj.getPolicy(), newObj.getPolicy())) {
                queue.add(EventHandlerUtil.buildRequestFromClusterScopedObject(newObj));
                return;
            }
        }

        @Override
        public void onDelete(V1alpha1NodeGroup obj, boolean deletedFinalStateUnknown) {
        }

    }

    private WorkQueue<Request> queue;
    private Indexer<V1Node> nodeIndexer;

    public NodeGroupWatch(WorkQueue<Request> queue, Indexer<V1Node> nodeIndexer) {
        this.queue = queue;
        this.nodeIndexer = nodeIndexer;
    }

    @Override
    public Class<V1alpha1NodeGroup> getResourceClass() {
        return V1alpha1NodeGroup.class;
    }

    @Override
    public ResourceEventHandler<V1alpha1NodeGroup> getResourceEventHandler() {
        return new EventHandler(this.queue, this.nodeIndexer);
    }

    @Override
    public Duration getResyncPeriod() {
        return RESYNC_PERIOD;
    }

}
