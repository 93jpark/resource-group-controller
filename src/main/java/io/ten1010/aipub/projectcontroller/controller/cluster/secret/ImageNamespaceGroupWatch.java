package io.ten1010.aipub.projectcontroller.controller.cluster.secret;

import io.kubernetes.client.extended.controller.ControllerWatch;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.workqueue.WorkQueue;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1TypedObjectReference;
import io.ten1010.aipub.projectcontroller.controller.EventHandlerUtil;
import io.ten1010.aipub.projectcontroller.core.IndexNames;
import io.ten1010.aipub.projectcontroller.core.K8sObjectUtil;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroup;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class ImageNamespaceGroupWatch implements ControllerWatch<V1alpha1ImageNamespaceGroup> {

    public static final Duration RESYNC_PERIOD = Duration.ofSeconds(30);

    public static class EventHandler implements ResourceEventHandler<V1alpha1ImageNamespaceGroup> {

        private WorkQueue<Request> queue;
        private Indexer<V1Secret> secretIndexer;

        public EventHandler(WorkQueue<Request> queue, Indexer<V1Secret> secretIndexer) {
            this.queue = queue;
            this.secretIndexer = secretIndexer;
        }

        @Override
        public void onAdd(V1alpha1ImageNamespaceGroup v1alpha1ImageNamespaceGroup) {
            this.resolveToSecrets(K8sObjectUtil.getNamespace(v1alpha1ImageNamespaceGroup))
                    .map(EventHandlerUtil::resolveNamespacedObjectToRequest)
                    .forEach(this.queue::add);
        }

        @Override
        public void onUpdate(V1alpha1ImageNamespaceGroup oldObj, V1alpha1ImageNamespaceGroup newObj) {
            if (!oldObj.getAipubImageNamespaces().equals(newObj.getAipubImageNamespaces())) {
                this.resolveToSecrets(K8sObjectUtil.getNamespace(oldObj))
                        .map(EventHandlerUtil::resolveNamespacedObjectToRequest)
                        .forEach(this.queue::add);
                return;
            }
            Optional<V1TypedObjectReference> oldSecretOpt = Optional.ofNullable(oldObj.getSecret());
            Optional<V1TypedObjectReference> newSecretOpt = Optional.ofNullable(newObj.getSecret());
            if (!oldSecretOpt.equals(newSecretOpt)) {
                this.resolveToSecrets(K8sObjectUtil.getNamespace(oldObj))
                        .map(EventHandlerUtil::resolveNamespacedObjectToRequest)
                        .forEach(this.queue::add);
                return;
            }
        }

        @Override
        public void onDelete(V1alpha1ImageNamespaceGroup v1alpha1ImageNamespaceGroup, boolean deletedFinalStateUnknown) {
            this.resolveToSecrets(K8sObjectUtil.getNamespace(v1alpha1ImageNamespaceGroup))
                    .map(EventHandlerUtil::resolveNamespacedObjectToRequest)
                    .forEach(this.queue::add);
        }

        private Stream<V1Secret> resolveToSecrets(String namespace) {
            List<V1Secret> secrets = this.secretIndexer.byIndex(IndexNames.BY_NAMESPACE_NAME_TO_SECRET_OBJECT, namespace);
            return secrets.stream();
        }

    }

    private WorkQueue<Request> queue;
    private Indexer<V1Secret> secretIndexer;

    public ImageNamespaceGroupWatch(WorkQueue<Request> queue, Indexer<V1Secret> secretIndexer) {
        this.queue = queue;
        this.secretIndexer = secretIndexer;
    }

    @Override
    public Class<V1alpha1ImageNamespaceGroup> getResourceClass() {
        return V1alpha1ImageNamespaceGroup.class;
    }

    @Override
    public ResourceEventHandler<V1alpha1ImageNamespaceGroup> getResourceEventHandler() {
        return new EventHandler(this.queue, this.secretIndexer);
    }

    @Override
    public Duration getResyncPeriod() {
        return RESYNC_PERIOD;
    }

}
