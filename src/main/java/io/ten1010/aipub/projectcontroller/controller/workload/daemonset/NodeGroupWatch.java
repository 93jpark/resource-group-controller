package io.ten1010.aipub.projectcontroller.controller.workload.daemonset;

import io.kubernetes.client.extended.controller.ControllerWatch;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.workqueue.WorkQueue;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.models.V1DaemonSet;
import io.kubernetes.client.openapi.models.V1TypedObjectReference;
import io.ten1010.aipub.projectcontroller.controller.EventHandlerUtil;
import io.ten1010.aipub.projectcontroller.core.IndexNames;
import io.ten1010.aipub.projectcontroller.core.NodeGroupUtil;
import io.ten1010.aipub.projectcontroller.model.V1alpha1NodeGroup;

import java.time.Duration;
import java.util.stream.Stream;

public class NodeGroupWatch implements ControllerWatch<V1alpha1NodeGroup> {

    public static final Duration RESYNC_PERIOD = Duration.ofSeconds(30);

    public static class EventHandler implements ResourceEventHandler<V1alpha1NodeGroup> {

        private WorkQueue<Request> queue;
        private Indexer<V1DaemonSet> daemonSetIndexer;

        public EventHandler(WorkQueue<Request> queue, Indexer<V1DaemonSet> daemonSetIndexer) {
            this.queue = queue;
            this.daemonSetIndexer = daemonSetIndexer;
        }

        @Override
        public void onAdd(V1alpha1NodeGroup obj) {
            if (NodeGroupUtil.isAllowAllDaemonSets(obj)) {
                this.daemonSetIndexer.list().forEach(daemonSet -> {
                    this.queue.add(buildRequestFromDaemonSet(daemonSet));
                });
                return;
            }
            Stream<Request> daemonSets = NodeGroupUtil.getDaemonSets(obj).stream()
                    .map(V1TypedObjectReference::getName)
                    .flatMap(daemonSetName ->
                            this.daemonSetIndexer.byIndex(IndexNames.BY_DAEMON_SET_NAME_TO_DAEMON_SET_OBJECT, daemonSetName)
                                    .stream())
                    .map(this::buildRequestFromDaemonSet);
            Stream<Request> namespaces = NodeGroupUtil.getNamespaces(obj).stream()
                    .flatMap(namespace ->
                            this.daemonSetIndexer.byIndex(IndexNames.BY_NAMESPACE_NAME_TO_DAEMON_SET_OBJECT, namespace).stream())
                    .map(EventHandlerUtil::buildRequestFromNamespacedObject);
            Stream.concat(daemonSets, namespaces)
                    .distinct()
                    .forEach(this.queue::add);
        }

        @Override
        public void onUpdate(V1alpha1NodeGroup oldObj, V1alpha1NodeGroup newObj) {
            if (NodeGroupUtil.isAllowAllDaemonSets(oldObj) != NodeGroupUtil.isAllowAllDaemonSets(newObj)) {
                this.daemonSetIndexer.list().forEach(daemonSet -> {
                    this.queue.add(buildRequestFromDaemonSet(daemonSet));
                });
                return;
            }
            if (!NodeGroupUtil.getDaemonSets(oldObj).equals(NodeGroupUtil.getDaemonSets(newObj)) ||
                    !NodeGroupUtil.getNamespaces(oldObj).equals(NodeGroupUtil.getNamespaces(newObj))) {
                Stream.concat(getDaemonSetRequests(newObj), getNamespaceRequests(newObj))
                        .distinct()
                        .forEach(this.queue::add);
            }
        }

        @Override
        public void onDelete(V1alpha1NodeGroup obj, boolean deletedFinalStateUnknown) {
            if (NodeGroupUtil.isAllowAllDaemonSets(obj)) {
                this.daemonSetIndexer.list().forEach(daemonSet ->
                        this.queue.add(buildRequestFromDaemonSet(daemonSet)));
                return;
            }
            Stream.concat(getDaemonSetRequests(obj), getNamespaceRequests(obj))
                    .distinct()
                    .forEach(this.queue::add);
        }

        private Request buildRequestFromDaemonSet(V1DaemonSet daemonSet) {
            return EventHandlerUtil.buildRequestFromNamespacedObject(daemonSet);
        }

        private Stream<Request> getDaemonSetRequests(V1alpha1NodeGroup obj) {
            return NodeGroupUtil.getDaemonSets(obj).stream()
                    .map(V1TypedObjectReference::getName)
                    .flatMap(daemonSetName ->
                            this.daemonSetIndexer.byIndex(IndexNames.BY_DAEMON_SET_NAME_TO_DAEMON_SET_OBJECT, daemonSetName)
                                    .stream())
                    .map(this::buildRequestFromDaemonSet);
        }

        private Stream<Request> getNamespaceRequests(V1alpha1NodeGroup obj) {
            return NodeGroupUtil.getNamespaces(obj).stream()
                    .flatMap(namespace ->
                            this.daemonSetIndexer.byIndex(IndexNames.BY_NAMESPACE_NAME_TO_DAEMON_SET_OBJECT, namespace)
                                    .stream())
                    .map(EventHandlerUtil::buildRequestFromNamespacedObject);
        }

    }

    private WorkQueue<Request> queue;
    private Indexer<V1DaemonSet> daemonSetIndexer;

    public NodeGroupWatch(WorkQueue<Request> queue, Indexer<V1DaemonSet> daemonSetIndexer) {
        this.queue = queue;
        this.daemonSetIndexer = daemonSetIndexer;
    }

    @Override
    public Class<V1alpha1NodeGroup> getResourceClass() {
        return V1alpha1NodeGroup.class;
    }

    @Override
    public ResourceEventHandler<V1alpha1NodeGroup> getResourceEventHandler() {
        return new EventHandler(this.queue, this.daemonSetIndexer);
    }

    @Override
    public Duration getResyncPeriod() {
        return RESYNC_PERIOD;
    }

}
