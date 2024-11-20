package io.ten1010.aipub.projectcontroller.controller.cluster.secret;

import io.kubernetes.client.extended.controller.ControllerWatch;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.workqueue.WorkQueue;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1TypedObjectReference;
import io.ten1010.aipub.projectcontroller.controller.EventHandlerUtil;
import io.ten1010.aipub.projectcontroller.core.K8sObjectUtil;
import io.ten1010.aipub.projectcontroller.core.KeyUtil;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroup;

import java.time.Duration;
import java.util.Optional;

public class ImageNamespaceGroupWatch implements ControllerWatch<V1alpha1ImageNamespaceGroup> {

    public static final Duration RESYNC_PERIOD = Duration.ofSeconds(30);

    public static class EventHandler implements ResourceEventHandler<V1alpha1ImageNamespaceGroup> {

        private WorkQueue<Request> queue;
        private Indexer<V1Secret> secretIndexer;
        private String projectSecretNamespace;

        public EventHandler(WorkQueue<Request> queue, Indexer<V1Secret> secretIndexer, String projectSecretNamespace) {
            this.queue = queue;
            this.secretIndexer = secretIndexer;
            this.projectSecretNamespace = projectSecretNamespace;
        }

        @Override
        public void onAdd(V1alpha1ImageNamespaceGroup imageNamespaceGroup) {
            this.queue.add(EventHandlerUtil.buildRequestFromNamespacedObject(
                    resolveToSecret(K8sObjectUtil.getName(imageNamespaceGroup)))
            );
        }

        @Override
        public void onUpdate(V1alpha1ImageNamespaceGroup oldObj, V1alpha1ImageNamespaceGroup newObj) {
            Optional<V1TypedObjectReference> oldSecretOpt = Optional.ofNullable(oldObj.getSecret());
            Optional<V1TypedObjectReference> newSecretOpt = Optional.ofNullable(newObj.getSecret());
            if (!oldSecretOpt.equals(newSecretOpt)) {
                this.queue.add(EventHandlerUtil.buildRequestFromNamespacedObject(
                        resolveToSecret(K8sObjectUtil.getName(newObj)))
                );
            }
        }

        @Override
        public void onDelete(V1alpha1ImageNamespaceGroup v1alpha1ImageNamespaceGroup, boolean deletedFinalStateUnknown) {
        }

        private V1Secret resolveToSecret(String imageNamespaceGroupName) {
            return this.secretIndexer.getByKey(KeyUtil.buildKey(this.projectSecretNamespace, imageNamespaceGroupName));
        }

    }

    private WorkQueue<Request> queue;
    private Indexer<V1Secret> secretIndexer;
    private String projectSecretNamespace;

    public ImageNamespaceGroupWatch(WorkQueue<Request> queue, Indexer<V1Secret> secretIndexer, String projectSecretNamespace) {
        this.queue = queue;
        this.secretIndexer = secretIndexer;
        this.projectSecretNamespace = projectSecretNamespace;
    }

    @Override
    public Class<V1alpha1ImageNamespaceGroup> getResourceClass() {
        return V1alpha1ImageNamespaceGroup.class;
    }

    @Override
    public ResourceEventHandler<V1alpha1ImageNamespaceGroup> getResourceEventHandler() {
        return new EventHandler(this.queue, this.secretIndexer, this.projectSecretNamespace);
    }

    @Override
    public Duration getResyncPeriod() {
        return RESYNC_PERIOD;
    }

}
