package io.ten1010.aipub.projectcontroller.controller.cluster.imagenamespacegroup;

import io.kubernetes.client.extended.controller.ControllerWatch;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.workqueue.WorkQueue;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.models.V1Secret;
import io.ten1010.aipub.projectcontroller.core.K8sObjectUtil;
import io.ten1010.aipub.projectcontroller.core.KeyUtil;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroup;

import java.time.Duration;

public class SecretWatch implements ControllerWatch<V1Secret> {

    public static final Duration RESYNC_PERIOD = Duration.ofSeconds(30);

    public static class EventHandler implements ResourceEventHandler<V1Secret> {

        private WorkQueue<Request> queue;
        private Indexer<V1alpha1ImageNamespaceGroup> imageNamespaceGroupIndexer;

        public EventHandler(WorkQueue<Request> queue, Indexer<V1alpha1ImageNamespaceGroup> imageNamespaceGroupIndexer) {
            this.queue = queue;
            this.imageNamespaceGroupIndexer = imageNamespaceGroupIndexer;
        }

        @Override
        public void onAdd(V1Secret obj) {
            V1alpha1ImageNamespaceGroup imageNamespaceGroup = this.resolveToImageNamespaceGroup(KeyUtil.getKey(obj));
            if (imageNamespaceGroup != null && imageNamespaceGroup.getSecret() == null) {
                this.queue.add(new Request(K8sObjectUtil.getNamespace(obj), K8sObjectUtil.getName(obj)));
            }
        }

        @Override
        public void onUpdate(V1Secret oldObj, V1Secret newObj) {
            V1alpha1ImageNamespaceGroup imageNamespaceGroup = this.resolveToImageNamespaceGroup(KeyUtil.getKey(newObj));
            if (imageNamespaceGroup != null && imageNamespaceGroup.getSecret() == null) {
                this.queue.add(new Request(K8sObjectUtil.getNamespace(newObj), K8sObjectUtil.getName(newObj)));
            }
        }

        @Override
        public void onDelete(V1Secret obj, boolean deletedFinalStateUnknown) {

        }

        private V1alpha1ImageNamespaceGroup resolveToImageNamespaceGroup(String key) {
            return this.imageNamespaceGroupIndexer.getByKey(key);
        }

    }

    private WorkQueue<Request> queue;
    private Indexer<V1alpha1ImageNamespaceGroup> imageNamespaceGroupIndexer;

    public SecretWatch(WorkQueue<Request> queue, Indexer<V1alpha1ImageNamespaceGroup> imageNamespaceGroupIndexer) {
        this.queue = queue;
        this.imageNamespaceGroupIndexer = imageNamespaceGroupIndexer;
    }

    @Override
    public Class<V1Secret> getResourceClass() {
        return V1Secret.class;
    }

    @Override
    public ResourceEventHandler<V1Secret> getResourceEventHandler() {
        return new EventHandler(this.queue, this.imageNamespaceGroupIndexer);
    }

    @Override
    public Duration getResyncPeriod() {
        return RESYNC_PERIOD;
    }
}
