package io.ten1010.aipub.projectcontroller.controller.cluster.namespace;

import io.kubernetes.client.extended.controller.ControllerWatch;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.workqueue.WorkQueue;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.ten1010.aipub.projectcontroller.core.K8sObjectUtil;
import io.ten1010.aipub.projectcontroller.model.V1alpha1Project;

import java.time.Duration;

public class ProjectWatch implements ControllerWatch<V1alpha1Project> {

    public static final Duration RESYNC_PERIOD = Duration.ofSeconds(30);

    public static class EventHandler implements ResourceEventHandler<V1alpha1Project> {

        private WorkQueue<Request> queue;

        public EventHandler(WorkQueue<Request> queue) {
            this.queue = queue;
        }

        @Override
        public void onAdd(V1alpha1Project obj) {
            this.queue.add(buildRequestFromProject(obj));
        }

        @Override
        public void onUpdate(V1alpha1Project oldObj, V1alpha1Project newObj) {
            String newProjectName = K8sObjectUtil.getName(newObj);
            String oldProjectName = K8sObjectUtil.getName(oldObj);
            if (!newProjectName.equals(oldProjectName)) {
                this.queue.add(buildRequestFromProject(newObj));
                this.queue.add(buildRequestFromProject(oldObj));
            }
        }

        @Override
        public void onDelete(V1alpha1Project obj, boolean deletedFinalStateUnknown) {
            this.queue.add(buildRequestFromProject(obj));
        }

        private Request buildRequestFromProject(V1alpha1Project project) {
            return new Request(K8sObjectUtil.getName(project));
        }

    }

    private WorkQueue<Request> queue;

    public ProjectWatch(WorkQueue<Request> queue) {
        this.queue = queue;
    }

    @Override
    public Class<V1alpha1Project> getResourceClass() {
        return V1alpha1Project.class;
    }

    @Override
    public ResourceEventHandler<V1alpha1Project> getResourceEventHandler() {
        return new EventHandler(this.queue);
    }

    @Override
    public Duration getResyncPeriod() {
        return RESYNC_PERIOD;
    }

}
