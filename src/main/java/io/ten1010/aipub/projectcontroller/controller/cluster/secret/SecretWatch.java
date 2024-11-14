package io.ten1010.aipub.projectcontroller.controller.cluster.secret;

import io.kubernetes.client.extended.controller.ControllerWatch;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.workqueue.WorkQueue;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.models.V1Secret;
import io.ten1010.aipub.projectcontroller.controller.EventHandlerUtil;
import io.ten1010.aipub.projectcontroller.core.K8sObjectUtil;
import io.ten1010.aipub.projectcontroller.core.KeyUtil;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroup;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

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
            Optional<V1alpha1ImageNamespaceGroup> imageNamespaceGroupOpt = resolveToImageNamespaceGroup(obj);
            if (imageNamespaceGroupOpt.isPresent()) {
                this.queue.add(EventHandlerUtil.resolveNamespacedObjectToRequest(imageNamespaceGroupOpt.get()));
                return;
            }
        }

        @Override
        public void onUpdate(V1Secret oldObj, V1Secret newObj) {
            Optional<V1alpha1ImageNamespaceGroup> imageNamespaceGroupOpt = resolveToImageNamespaceGroup(newObj);
            if (imageNamespaceGroupOpt.isPresent()) {
                this.queue.add(EventHandlerUtil.resolveNamespacedObjectToRequest(imageNamespaceGroupOpt.get()));
                return;
            }
            Optional<Map<String, byte[]>> oldData = Optional.ofNullable(oldObj.getData());
            Optional<Map<String, byte[]>> newData = Optional.ofNullable(newObj.getData());
            if (!oldData.equals(newData)) {
                this.queue.add(EventHandlerUtil.resolveNamespacedObjectToRequest(newObj));
                return;
            }
        }

        @Override
        public void onDelete(V1Secret obj, boolean deletedFinalStateUnknown) {
        }

        private Optional<V1alpha1ImageNamespaceGroup> resolveToImageNamespaceGroup(V1Secret secret) {
            String imageNamespaceGroupKey = KeyUtil.buildKey(K8sObjectUtil.getNamespace(secret), K8sObjectUtil.getName(secret));
            return Optional.ofNullable(this.imageNamespaceGroupIndexer.getByKey(imageNamespaceGroupKey));
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
