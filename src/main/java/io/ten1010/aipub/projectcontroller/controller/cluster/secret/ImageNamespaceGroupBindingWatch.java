package io.ten1010.aipub.projectcontroller.controller.cluster.secret;

import io.kubernetes.client.extended.controller.ControllerWatch;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.workqueue.WorkQueue;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.cache.Indexer;
import io.ten1010.aipub.projectcontroller.core.KeyUtil;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroupBinding;
import io.ten1010.aipub.projectcontroller.model.V1alpha1Project;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class ImageNamespaceGroupBindingWatch implements ControllerWatch<V1alpha1ImageNamespaceGroupBinding> {

    public static final Duration RESYNC_PERIOD = Duration.ofSeconds(30);

    public static class EventHandler implements ResourceEventHandler<V1alpha1ImageNamespaceGroupBinding> {

        private WorkQueue<Request> queue;
        private Indexer<V1alpha1Project> projectIndexer;

        public EventHandler(WorkQueue<Request> queue, Indexer<V1alpha1Project> projectIndexer) {
            this.queue = queue;
            this.projectIndexer = projectIndexer;
        }

        @Override
        public void onAdd(V1alpha1ImageNamespaceGroupBinding obj) {
            obj.getProjects().stream()
                    .map(projectName -> Optional.ofNullable(projectIndexer.getByKey(KeyUtil.buildKey(projectName))))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(project -> buildRequest(obj.getImageNamespaceGroupRef(), project.getNamespace()))
                    .forEach(queue::add);
        }

        @Override
        public void onUpdate(V1alpha1ImageNamespaceGroupBinding oldObj, V1alpha1ImageNamespaceGroupBinding newObj) {
            Objects.requireNonNull(oldObj.getImageNamespaceGroupRef(), "ImageNamespaceGroupRef must not be null");
            Objects.requireNonNull(newObj.getImageNamespaceGroupRef(), "ImageNamespaceGroupRef must not be null");
            if (!oldObj.getImageNamespaceGroupRef().equals(newObj.getImageNamespaceGroupRef())) {
                processGroupRefChange(oldObj).forEach(this.queue::add);
                processGroupRefChange(newObj).forEach(this.queue::add);
            }
            getAddedOrDeletedProjects(oldObj.getProjects(), newObj.getProjects())
                    .forEach(projectName ->
                            this.queue.add(buildRequest(oldObj.getImageNamespaceGroupRef(), projectName))
                    );
        }

        @Override
        public void onDelete(V1alpha1ImageNamespaceGroupBinding obj, boolean deletedFinalStateUnknown) {
            obj.getProjects().stream()
                    .map(projectName -> Optional.ofNullable(projectIndexer.getByKey(KeyUtil.buildKey(projectName))))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(project -> buildRequest(obj.getImageNamespaceGroupRef(), project.getNamespace()))
                    .forEach(queue::add);
        }

        private Request buildRequest(String imageNamespaceGroupRef, String projectNamespace) {
            Objects.requireNonNull(imageNamespaceGroupRef);
            Objects.requireNonNull(projectNamespace);
            return new Request(projectNamespace, imageNamespaceGroupRef);
        }

        private static Set<String> getAddedOrDeletedProjects(List<String> oldProjects, List<String> newProjects) {
            Set<String> deleted = new HashSet<>(oldProjects);
            newProjects.forEach(deleted::remove);
            Set<String> added = new HashSet<>(newProjects);
            oldProjects.forEach(added::remove);
            deleted.addAll(added);
            return deleted;
        }

        private Stream<Request> processGroupRefChange(V1alpha1ImageNamespaceGroupBinding obj) {
            Objects.requireNonNull(obj.getImageNamespaceGroupRef(), "ImageNamespaceGroupRef must not be null");
            return obj.getProjects().stream()
                    .map(this::getProject)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(project -> buildRequest(
                            obj.getImageNamespaceGroupRef(),
                            Objects.requireNonNull(project.getNamespace(), "Project namespace must not be null")
                    ));
        }

        private Optional<V1alpha1Project> getProject(String projectName) {
            return Optional.ofNullable(projectIndexer.getByKey(KeyUtil.buildKey(projectName)));
        }

    }

    private WorkQueue<Request> queue;
    private Indexer<V1alpha1Project> projectIndexer;

    public ImageNamespaceGroupBindingWatch(WorkQueue<Request> queue, Indexer<V1alpha1Project> projectIndexer) {
        this.queue = queue;
        this.projectIndexer = projectIndexer;
    }

    @Override
    public Class<V1alpha1ImageNamespaceGroupBinding> getResourceClass() {
        return V1alpha1ImageNamespaceGroupBinding.class;
    }

    @Override
    public ResourceEventHandler<V1alpha1ImageNamespaceGroupBinding> getResourceEventHandler() {
        return new EventHandler(this.queue, this.projectIndexer);
    }

    @Override
    public Duration getResyncPeriod() {
        return RESYNC_PERIOD;
    }

}
