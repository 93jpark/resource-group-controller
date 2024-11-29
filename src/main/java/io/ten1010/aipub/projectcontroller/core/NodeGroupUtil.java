package io.ten1010.aipub.projectcontroller.core;

import io.kubernetes.client.openapi.models.V1TypedObjectReference;
import io.ten1010.aipub.projectcontroller.model.V1alpha1NodeGroup;

import java.util.ArrayList;
import java.util.List;

public final class NodeGroupUtil {

    public static List<String> getNodes(V1alpha1NodeGroup obj) {
        if (obj.getNodes() == null) {
            return new ArrayList<>();
        }
        return obj.getNodes();
    }

    public static List<String> getNamespaces(V1alpha1NodeGroup obj) {
        if (obj.getPolicy() == null ||
                obj.getPolicy().getDaemonSet() == null ||
                obj.getPolicy().getDaemonSet().getAllowedNamespaces() == null) {
            return new ArrayList<>();
        }
        return obj.getPolicy().getDaemonSet().getAllowedNamespaces();
    }

    public static List<V1TypedObjectReference> getDaemonSets(V1alpha1NodeGroup obj) {
        if (obj.getPolicy() == null ||
                obj.getPolicy().getDaemonSet() == null ||
                obj.getPolicy().getDaemonSet().getAllowedDaemonSets() == null) {
            return new ArrayList<>();
        }
        return obj.getPolicy().getDaemonSet().getAllowedDaemonSets();
    }

    public static boolean isAllowAllDaemonSets(V1alpha1NodeGroup obj) {
        if (obj == null ||
                obj.getPolicy() == null ||
                obj.getPolicy().getDaemonSet() == null ||
                obj.getPolicy().getDaemonSet().getAllowAllDaemonSets() == null) {
            return false;
        }
        return obj.getPolicy().getDaemonSet().getAllowAllDaemonSets();
    }

    private NodeGroupUtil() {
        throw new UnsupportedOperationException();
    }

}
