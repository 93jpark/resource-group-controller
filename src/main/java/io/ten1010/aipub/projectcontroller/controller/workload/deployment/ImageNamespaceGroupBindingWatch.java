package io.ten1010.aipub.projectcontroller.controller.workload.deployment;

import io.kubernetes.client.extended.controller.ControllerWatch;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.workqueue.WorkQueue;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.ten1010.aipub.projectcontroller.controller.EventHandlerUtil;
import io.ten1010.aipub.projectcontroller.core.IndexNames;
import io.ten1010.aipub.projectcontroller.core.KeyUtil;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroupBinding;
import io.ten1010.aipub.projectcontroller.model.V1alpha1Project;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class ImageNamespaceGroupBindingWatch implements ControllerWatch<V1alpha1ImageNamespaceGroupBinding> {

    public static final Duration RESYNC_PERIOD = Duration.ofSeconds(30);

    public static class EventHandler implements ResourceEventHandler<V1alpha1ImageNamespaceGroupBinding> {

        private WorkQueue<Request> queue;
        private Indexer<V1Deployment> deploymentIndexer;
        private Indexer<V1alpha1Project> projectIndexer;

        public EventHandler(WorkQueue<Request> queue, Indexer<V1Deployment> deploymentIndexer, Indexer<V1alpha1Project> projectIndexer) {
            this.queue = queue;
            this.deploymentIndexer = deploymentIndexer;
            this.projectIndexer = projectIndexer;
        }

        @Override
        public void onAdd(V1alpha1ImageNamespaceGroupBinding obj) {
            obj.getProjects().stream()
                    .map(projectName -> projectIndexer.getByKey(KeyUtil.buildKey(projectName)))
                    .filter(Objects::nonNull)
                    .flatMap(project ->
                            Optional.ofNullable(resolveToDeployment(project.getNamespace()))
                                    .orElse(Collections.emptyList())
                                    .stream()
                                    .map(EventHandlerUtil::buildRequestFromNamespacedObject)
                    )
                    .forEach(queue::add);
        }

        @Override
        public void onUpdate(V1alpha1ImageNamespaceGroupBinding oldObj, V1alpha1ImageNamespaceGroupBinding newObj) {
            if (!oldObj.getProjects().equals(newObj.getProjects())) {
                getAddedOrDeletedProjects(oldObj.getProjects(), newObj.getProjects())
                        .stream().map(projectName -> this.projectIndexer.getByKey(KeyUtil.buildKey(projectName)))
                        .map(project -> this.resolveToDeployment(project.getNamespace()))
                        .forEach(deployments -> {
                            deployments.stream()
                                    .map(EventHandlerUtil::buildRequestFromNamespacedObject)
                                    .forEach(this.queue::add);
                        });
            }
        }

        @Override
        public void onDelete(V1alpha1ImageNamespaceGroupBinding obj, boolean deletedFinalStateUnknown) {

        }

        private static Set<String> getAddedOrDeletedProjects(List<String> oldProjects, List<String> newProjects) {
            Set<String> deleted = new HashSet<>(oldProjects);
            newProjects.forEach(deleted::remove);
            Set<String> added = new HashSet<>(newProjects);
            oldProjects.forEach(added::remove);
            deleted.addAll(added);
            return deleted;
        }

        private List<V1Deployment> resolveToDeployment(String namespaceName) {
            return this.deploymentIndexer.byIndex(IndexNames.BY_NAMESPACE_NAME_TO_DEPLOYMENT_OBJECT, namespaceName);
        }

    }

    private WorkQueue<Request> queue;
    private Indexer<V1Deployment> deploymentIndexer;
    private Indexer<V1alpha1Project> projectIndexer;

    public ImageNamespaceGroupBindingWatch(WorkQueue<Request> queue, Indexer<V1Deployment> deploymentIndexer, Indexer<V1alpha1Project> projectIndexer) {
        this.queue = queue;
        this.deploymentIndexer = deploymentIndexer;
        this.projectIndexer = projectIndexer;
    }

    @Override
    public Class<V1alpha1ImageNamespaceGroupBinding> getResourceClass() {
        return V1alpha1ImageNamespaceGroupBinding.class;
    }

    @Override
    public ResourceEventHandler<V1alpha1ImageNamespaceGroupBinding> getResourceEventHandler() {
        return new EventHandler(this.queue, this.deploymentIndexer, this.projectIndexer);
    }

    @Override
    public Duration getResyncPeriod() {
        return RESYNC_PERIOD;
    }

}
