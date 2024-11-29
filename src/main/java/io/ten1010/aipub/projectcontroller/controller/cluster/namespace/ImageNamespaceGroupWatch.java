package io.ten1010.aipub.projectcontroller.controller.cluster.namespace;

import io.kubernetes.client.extended.controller.ControllerWatch;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.workqueue.WorkQueue;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroup;

import java.time.Duration;

public class ImageNamespaceGroupWatch implements ControllerWatch<V1alpha1ImageNamespaceGroup> {

    public static final Duration RESYNC_PERIOD = Duration.ofSeconds(30);

    public static class EventHandler implements ResourceEventHandler<V1alpha1ImageNamespaceGroup> {

        private WorkQueue<Request> queue;
        private String registrySecretNamespace;

        public EventHandler(WorkQueue<Request> queue, String registrySecretNamespace) {
            this.queue = queue;
            this.registrySecretNamespace = registrySecretNamespace;
        }

        @Override
        public void onAdd(V1alpha1ImageNamespaceGroup obj) {
            this.queue.add(new Request(registrySecretNamespace));
        }

        @Override
        public void onUpdate(V1alpha1ImageNamespaceGroup oldObj, V1alpha1ImageNamespaceGroup newObj) {

        }

        @Override
        public void onDelete(V1alpha1ImageNamespaceGroup obj, boolean deletedFinalStateUnknown) {
        }

    }

    private WorkQueue<Request> queue;
    private String registrySecretNamespace;

    public ImageNamespaceGroupWatch(WorkQueue<Request> queue, String registrySecretNamespace) {
        this.queue = queue;
        this.registrySecretNamespace = registrySecretNamespace;
    }

    @Override
    public Class<V1alpha1ImageNamespaceGroup> getResourceClass() {
        return V1alpha1ImageNamespaceGroup.class;
    }

    @Override
    public ResourceEventHandler<V1alpha1ImageNamespaceGroup> getResourceEventHandler() {
        return new EventHandler(this.queue, registrySecretNamespace);
    }

    @Override
    public Duration getResyncPeriod() {
        return RESYNC_PERIOD;
    }

}
