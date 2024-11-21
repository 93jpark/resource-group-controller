package io.ten1010.aipub.projectcontroller.controller.cluster.node;

import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.event.legacy.EventRecorder;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeSpec;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Taint;
import io.ten1010.aipub.projectcontroller.core.IndexNames;
import io.ten1010.aipub.projectcontroller.core.Labels;
import io.ten1010.aipub.projectcontroller.core.Taints;
import io.ten1010.aipub.projectcontroller.model.V1alpha1NodeGroup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;
import java.util.stream.Collectors;

class NodeReconcilerTest {

    Indexer<V1Node> nodeIndexer;
    Indexer<V1alpha1NodeGroup> groupIndexer;
    CoreV1Api coreV1Api;
    EventRecorder eventRecorder;

    @BeforeEach
    void setUp() {
        this.nodeIndexer = Mockito.mock(Indexer.class);
        this.groupIndexer = Mockito.mock(Indexer.class);
        this.coreV1Api = Mockito.mock(CoreV1Api.class);
        this.eventRecorder = Mockito.mock(EventRecorder.class);
    }

    @Test
    void should_patch_labels_and_taints_of_the_node() {
        V1alpha1NodeGroup group1 = new V1alpha1NodeGroup();
        V1ObjectMeta meta1 = new V1ObjectMeta();
        meta1.setName("group1");
        group1.setMetadata(meta1);
        group1.setNodes(List.of("node1"));

        V1Node node1 = new V1Node();
        V1ObjectMeta nodeMeta1 = new V1ObjectMeta();
        nodeMeta1.setName("node1");
        nodeMeta1.setLabels(new HashMap<>());
        node1.setMetadata(nodeMeta1);
        V1NodeSpec nodeSpec1 = new V1NodeSpec();
        nodeSpec1.setTaints(new ArrayList<>());
        node1.setSpec(nodeSpec1);

        Mockito.doReturn(List.of(group1)).when(this.groupIndexer).byIndex(IndexNames.BY_NODE_NAME_TO_GROUP_OBJECT, "node1");
        Mockito.doReturn(node1).when(this.nodeIndexer).getByKey("node1");
        NodeReconciler nodeReconciler = new NodeReconciler(this.nodeIndexer, this.groupIndexer, this.coreV1Api, this.eventRecorder);
        nodeReconciler.reconcile(new Request("node1"));
        try {
            Mockito.verify(this.coreV1Api).replaceNode(
                    Mockito.eq("node1"),
                    Mockito.argThat(node -> {
                        String labelValue = node.getMetadata().getLabels().get(Labels.KEY_NODE_GROUP);
                        if (!labelValue.equals("group1")) {
                            return false;
                        }
                        List<String> taints = node.getSpec().getTaints().stream()
                                .filter(e -> {
                                    if (e.getKey() == null) {
                                        return false;
                                    }
                                    if (!e.getKey().equals(Taints.KEY_NODE_GROUP)) {
                                        return false;
                                    }
                                    return e.getValue().equals("group1");
                                })
                                .map(V1Taint::getEffect)
                                .collect(Collectors.toList());
                        if (taints.size() != 1) {
                            return false;
                        }
                        Set<String> taintSet = new HashSet<>(taints);
                        return taintSet.equals(Set.of(Taints.EFFECT_NO_SCHEDULE));
                    })).execute();
        } catch (ApiException e) {
            Assertions.fail();
        }
    }

    @Test
    void should_do_nothing_the_given_labels_and_taints_of_node_are_equal_with_node_groups() {
        V1alpha1NodeGroup group1 = new V1alpha1NodeGroup();

        V1ObjectMeta meta1 = new V1ObjectMeta();
        meta1.setName("group1");
        group1.setMetadata(meta1);

        group1.setNodes(List.of("node1"));

        V1Node node1 = new V1Node();

        V1ObjectMeta nodeMeta1 = new V1ObjectMeta();
        nodeMeta1.setName("node1");
        Map<String, String> labels = new HashMap<>();
        labels.put(Labels.KEY_NODE_GROUP, "group1");
        nodeMeta1.setLabels(labels);
        node1.setMetadata(nodeMeta1);

        V1NodeSpec nodeSpec1 = new V1NodeSpec();
        V1Taint noSchd = new V1Taint();
        noSchd.setKey(Taints.KEY_NODE_GROUP);
        noSchd.setValue("group1");
        noSchd.setEffect(Taints.EFFECT_NO_SCHEDULE);
        nodeSpec1.setTaints(List.of(noSchd));

        node1.setSpec(nodeSpec1);

        Mockito.doReturn(List.of(group1)).when(this.groupIndexer).byIndex(IndexNames.BY_NODE_NAME_TO_GROUP_OBJECT, "node1");
        Mockito.doReturn(node1).when(this.nodeIndexer).getByKey("node1");

        NodeReconciler nodeReconciler = new NodeReconciler(this.nodeIndexer, this.groupIndexer, this.coreV1Api, this.eventRecorder);
        nodeReconciler.reconcile(new Request("node1"));

        try {
            Mockito.verifyNoInteractions(this.coreV1Api);
        } catch (Exception e) {
            Assertions.fail();
        }
    }

}