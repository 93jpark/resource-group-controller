package io.ten1010.aipub.projectcontroller.controller.cluster.role;

import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.extended.controller.ControllerWatch;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.workqueue.WorkQueue;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.ten1010.aipub.projectcontroller.model.V1alpha1NodeGroup;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class ResourceGroupWatch implements ControllerWatch<V1alpha1NodeGroup> {

    public static final Duration RESYNC_PERIOD = Duration.ofSeconds(30);

    public static class EventHandler implements ResourceEventHandler<V1alpha1NodeGroup> {

        private static List<String> getNamespaces(V1alpha1NodeGroup obj) {
            if (obj.getPolicy() == null ||
                    obj.getPolicy().getDaemonSet() == null ||
                    obj.getPolicy().getDaemonSet().getAllowedNamespaces() == null) {
                return new ArrayList<>();
            }
            return obj.getPolicy().getDaemonSet().getAllowedNamespaces(); // todo change
        }

        private static Set<String> getAddedOrDeletedNamespaces(List<String> oldNamespaces, List<String> newNamespaces) {
            Set<String> deleted = new HashSet<>(oldNamespaces);
            deleted.removeAll(newNamespaces);
            Set<String> added = new HashSet<>(newNamespaces);
            added.removeAll(oldNamespaces);
            deleted.addAll(added);

            return deleted;
        }

        private static String getName(KubernetesObject obj) {
            Objects.requireNonNull(obj.getMetadata());
            Objects.requireNonNull(obj.getMetadata().getName());

            return obj.getMetadata().getName();
        }

        private WorkQueue<Request> queue;

        public EventHandler(WorkQueue<Request> queue) {
            this.queue = queue;
        }

        @Override
        public void onAdd(V1alpha1NodeGroup obj) {
            String groupName = getName(obj);
            Set<Request> requests = getNamespaces(obj).stream()
                    .map(e -> buildRequest(groupName, e))
                    .collect(Collectors.toSet());
            requests.forEach(this.queue::add);
        }

        @Override
        public void onUpdate(V1alpha1NodeGroup oldObj, V1alpha1NodeGroup newObj) {
            String groupName = getName(newObj);
            Set<Request> requests = getAddedOrDeletedNamespaces(getNamespaces(oldObj), getNamespaces(newObj)).stream()
                    .map(e -> buildRequest(groupName, e))
                    .collect(Collectors.toSet());
            requests.forEach(this.queue::add);
        }

        @Override
        public void onDelete(V1alpha1NodeGroup obj, boolean deletedFinalStateUnknown) {
        }

        private Request buildRequest(String groupName, String roleNamespace) {
            String roleName = new ResourceGroupRoleName(groupName).getName();
            return new Request(roleNamespace, roleName);
        }

    }

    private WorkQueue<Request> queue;

    public ResourceGroupWatch(WorkQueue<Request> queue) {
        this.queue = queue;
    }

    @Override
    public Class<V1alpha1NodeGroup> getResourceClass() {
        return V1alpha1NodeGroup.class;
    }

    @Override
    public ResourceEventHandler<V1alpha1NodeGroup> getResourceEventHandler() {
        return new EventHandler(this.queue);
    }

    @Override
    public Duration getResyncPeriod() {
        return RESYNC_PERIOD;
    }

}
