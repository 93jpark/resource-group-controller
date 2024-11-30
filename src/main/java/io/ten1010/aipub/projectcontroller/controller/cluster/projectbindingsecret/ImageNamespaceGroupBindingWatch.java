package io.ten1010.aipub.projectcontroller.controller.cluster.projectbindingsecret;

import io.kubernetes.client.extended.controller.ControllerWatch;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.workqueue.WorkQueue;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.cache.Indexer;
import io.ten1010.aipub.projectcontroller.core.K8sObjectUtil;
import io.ten1010.aipub.projectcontroller.core.KeyUtil;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroupBinding;
import io.ten1010.aipub.projectcontroller.model.V1alpha1Project;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Slf4j
public class ImageNamespaceGroupBindingWatch implements ControllerWatch<V1alpha1ImageNamespaceGroupBinding> {

    public static final Duration RESYNC_PERIOD = Duration.ofSeconds(30);

    public static class EventHandler implements ResourceEventHandler<V1alpha1ImageNamespaceGroupBinding> {

        private static Set<String> getAddedOrDeletedProjects(List<String> oldProjects, List<String> newProjects) {
            Set<String> deleted = new HashSet<>(oldProjects);
            newProjects.forEach(deleted::remove);
            Set<String> added = new HashSet<>(newProjects);
            oldProjects.forEach(added::remove);
            deleted.addAll(added);
            return deleted;
        }

        private WorkQueue<Request> queue;
        private Indexer<V1alpha1Project> projectIndexer;

        public EventHandler(WorkQueue<Request> queue, Indexer<V1alpha1Project> projectIndexer) {
            this.queue = queue;
            this.projectIndexer = projectIndexer;
        }

        @Override
        public void onAdd(V1alpha1ImageNamespaceGroupBinding obj) {
            log.info("from project binding secret controller.EventHandler.onAdd");
            List<String> projectNames = obj.getProjects();
            for (String projectName : projectNames) {
                log.info("for project name: {}", projectName);
                Optional<V1alpha1Project> projectOpt = Optional.ofNullable(projectIndexer.getByKey(KeyUtil.buildKey(projectName)));
                if (projectOpt.isPresent()) {
                    V1alpha1Project project = projectOpt.get();
                    log.info("project [{}] namespace is [{}]", K8sObjectUtil.getName(project), project.getNamespace());
                    if (project.getNamespace() != null && obj.getImageNamespaceGroupRef() != null) {
                        log.info("adding request to queue");
                        Request request = new Request(project.getNamespace(), obj.getImageNamespaceGroupRef());
                        this.queue.add(request);
                    }
                    return;
                } else {
                    log.info("project not found");
                }
            }
        }

        @Override
        public void onUpdate(V1alpha1ImageNamespaceGroupBinding oldObj, V1alpha1ImageNamespaceGroupBinding newObj) {
            log.info("from project binding secret controller.EventHandler.onUpdate");
            List<String> projectNames = newObj.getProjects();
            for (String projectName : projectNames) {
                log.info("for project name: {}", projectName);
                Optional<V1alpha1Project> projectOpt = Optional.ofNullable(projectIndexer.getByKey(KeyUtil.buildKey(projectName)));
                if (projectOpt.isPresent()) {
                    V1alpha1Project project = projectOpt.get();
                    log.info("project [{}] namespace is [{}]", K8sObjectUtil.getName(project), project.getNamespace());
                    if (project.getNamespace() != null && newObj.getImageNamespaceGroupRef() != null) {
                        log.info("adding request to queue");
                        Request request = new Request(project.getNamespace(), newObj.getImageNamespaceGroupRef());
                        this.queue.add(request);
                    }
                    return;
                } else {
                    log.info("project not found");
                }
            }

            Set<String> updatedProjects = getAddedOrDeletedProjects(oldObj.getProjects(), newObj.getProjects());
            for (String projectName : updatedProjects) {
                Optional<V1alpha1Project> projectOpt = Optional.ofNullable(projectIndexer.getByKey(KeyUtil.buildKey(projectName)));
                if (projectOpt.isPresent()) {
                    V1alpha1Project project = projectOpt.get();
                    if (project.getNamespace() != null) {
                        this.queue.add(new Request(project.getNamespace(), newObj.getImageNamespaceGroupRef()));
                        this.queue.add(new Request(project.getNamespace(), oldObj.getImageNamespaceGroupRef()));
                    }
                }
            }
        }

        @Override
        public void onDelete(V1alpha1ImageNamespaceGroupBinding obj, boolean deletedFinalStateUnknown) {
            log.info("from project binding secret controller.EventHandler.onDelete");
            List<String> projectNames = obj.getProjects();
            for (String projectName : projectNames) {
                Optional<V1alpha1Project> projectOpt = Optional.ofNullable(projectIndexer.getByKey(KeyUtil.buildKey(projectName)));
                if (projectOpt.isPresent()) {
                    V1alpha1Project project = projectOpt.get();
                    if (project.getNamespace() != null && obj.getImageNamespaceGroupRef() != null) {
                        Request request = new Request(project.getNamespace(), obj.getImageNamespaceGroupRef());
                        this.queue.add(request);
                    }
                }
            }
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
