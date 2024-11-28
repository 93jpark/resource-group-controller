package io.ten1010.aipub.projectcontroller.controller.workload.daemonset;

import io.kubernetes.client.extended.controller.ControllerWatch;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.workqueue.WorkQueue;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.models.V1DaemonSet;
import io.ten1010.aipub.projectcontroller.controller.EventHandlerUtil;
import io.ten1010.aipub.projectcontroller.core.IndexNames;
import io.ten1010.aipub.projectcontroller.core.KeyUtil;
import io.ten1010.aipub.projectcontroller.model.V1alpha1NodeGroupBinding;
import io.ten1010.aipub.projectcontroller.model.V1alpha1Project;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class NodeGroupBindingWatch implements ControllerWatch<V1alpha1NodeGroupBinding> {

    public static final Duration RESYNC_PERIOD = Duration.ofSeconds(30);

    public static class EventHandler implements ResourceEventHandler<V1alpha1NodeGroupBinding> {

        private static Set<String> getAddedOrDeletedProjects(List<String> oldProjects, List<String> newProjects) {
            Set<String> deleted = new HashSet<>(oldProjects);
            newProjects.forEach(deleted::remove);
            Set<String> added = new HashSet<>(newProjects);
            oldProjects.forEach(added::remove);
            deleted.addAll(added);
            return deleted;
        }

        private WorkQueue<Request> queue;
        private Indexer<V1DaemonSet> daemonSetIndexer;
        private Indexer<V1alpha1Project> projectIndexer;

        public EventHandler(WorkQueue<Request> queue, Indexer<V1DaemonSet> daemonSetIndexer, Indexer<V1alpha1Project> projectIndexer) {
            this.queue = queue;
            this.daemonSetIndexer = daemonSetIndexer;
            this.projectIndexer = projectIndexer;
        }

        @Override
        public void onAdd(V1alpha1NodeGroupBinding obj) {
            obj.getProjects().stream()
                    .map(projectName -> this.projectIndexer.getByKey(KeyUtil.buildKey(projectName)))
                    .filter(Objects::nonNull)
                    .flatMap(project ->
                            Optional.of(resolveToDaemonSet(project.getNamespace()))
                                    .orElse(Collections.emptyList())
                                    .stream()
                                    .map(EventHandlerUtil::buildRequestFromNamespacedObject)
                    )
                    .forEach(this.queue::add);
        }

        @Override
        public void onUpdate(V1alpha1NodeGroupBinding oldObj, V1alpha1NodeGroupBinding newObj) {
            if (!oldObj.getProjects().equals(newObj.getProjects())) {
                getAddedOrDeletedProjects(oldObj.getProjects(), newObj.getProjects())
                        .stream().map(projectName -> this.projectIndexer.getByKey(KeyUtil.buildKey(projectName)))
                        .map(project -> resolveToDaemonSet(Objects.requireNonNull(project.getNamespace())))
                        .forEach(daemonSets -> {
                            daemonSets.stream()
                                    .map(EventHandlerUtil::buildRequestFromNamespacedObject)
                                    .forEach(this.queue::add);
                        });
            }
        }

        @Override
        public void onDelete(V1alpha1NodeGroupBinding obj, boolean deletedFinalStateUnknown) {
            obj.getProjects().stream()
                    .map(projectName -> this.projectIndexer.getByKey(KeyUtil.buildKey(projectName)))
                    .filter(Objects::nonNull)
                    .flatMap(project ->
                            Optional.of(resolveToDaemonSet(project.getNamespace()))
                                    .orElse(Collections.emptyList())
                                    .stream()
                                    .map(EventHandlerUtil::buildRequestFromNamespacedObject)
                    )
                    .forEach(this.queue::add);
        }

        private List<V1DaemonSet> resolveToDaemonSet(String namespaceName) {
            Objects.requireNonNull(namespaceName);
            return this.daemonSetIndexer.byIndex(IndexNames.BY_NAMESPACE_NAME_TO_DAEMON_SET_OBJECT, namespaceName);
        }

    }

    private WorkQueue<Request> queue;
    private Indexer<V1DaemonSet> daemonSetIndexer;
    private Indexer<V1alpha1Project> projectIndexer;

    public NodeGroupBindingWatch(WorkQueue<Request> queue, Indexer<V1DaemonSet> daemonSetIndexer, Indexer<V1alpha1Project> projectIndexer) {
        this.queue = queue;
        this.daemonSetIndexer = daemonSetIndexer;
        this.projectIndexer = projectIndexer;
    }

    @Override
    public Class<V1alpha1NodeGroupBinding> getResourceClass() {
        return V1alpha1NodeGroupBinding.class;
    }

    @Override
    public ResourceEventHandler<V1alpha1NodeGroupBinding> getResourceEventHandler() {
        return new EventHandler(this.queue, this.daemonSetIndexer, this.projectIndexer);
    }

    @Override
    public Duration getResyncPeriod() {
        return RESYNC_PERIOD;
    }

}
