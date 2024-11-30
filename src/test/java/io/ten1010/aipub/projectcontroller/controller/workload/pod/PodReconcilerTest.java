package io.ten1010.aipub.projectcontroller.controller.workload.pod;

import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.ten1010.aipub.projectcontroller.core.IndexNames;
import io.ten1010.aipub.projectcontroller.core.KeyUtil;
import io.ten1010.aipub.projectcontroller.core.Labels;
import io.ten1010.aipub.projectcontroller.core.Taints;
import io.ten1010.aipub.projectcontroller.controller.Reconciliation;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroup;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroupBinding;
import io.ten1010.aipub.projectcontroller.model.V1alpha1NodeGroup;
import io.ten1010.aipub.projectcontroller.model.V1alpha1NodeGroupBinding;
import io.ten1010.aipub.projectcontroller.model.V1alpha1Project;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

class PodReconcilerTest {

    Indexer<V1alpha1Project> projectIndexer;
    Indexer<V1alpha1NodeGroup> nodeGroupIndexer;
    Indexer<V1alpha1NodeGroupBinding> nodeGroupBindingIndexer;
    Indexer<V1alpha1ImageNamespaceGroup> imageNamespacenodeGroupIndexer;
    Indexer<V1alpha1ImageNamespaceGroupBinding> imageNamespaceGroupBindingIndexer;
    Indexer<V1Secret> secretIndexer;
    Reconciliation reconciliation;
    Indexer<V1Pod> podIndexer;
    CoreV1Api coreV1Api;

    @BeforeEach
    void setUp() {
        this.projectIndexer = Mockito.mock(Indexer.class);
        this.nodeGroupIndexer = Mockito.mock(Indexer.class);
        this.nodeGroupBindingIndexer = Mockito.mock(Indexer.class);
        this.imageNamespacenodeGroupIndexer = Mockito.mock(Indexer.class);
        this.imageNamespaceGroupBindingIndexer = Mockito.mock(Indexer.class);
        this.secretIndexer = Mockito.mock(Indexer.class);
        this.reconciliation = new Reconciliation(this.projectIndexer, this.nodeGroupIndexer, this.nodeGroupBindingIndexer, this.imageNamespacenodeGroupIndexer, this.imageNamespaceGroupBindingIndexer, this.secretIndexer);
        this.podIndexer = Mockito.mock(Indexer.class);
        this.coreV1Api = Mockito.mock(CoreV1Api.class);
    }

    @Test
    void given_pod_that_has_invalid_tolerations_when_reconcile_the_pod_then_should_delete_the_pod() {
        V1alpha1NodeGroup group1 = new V1alpha1NodeGroup();
        V1ObjectMeta meta1 = new V1ObjectMeta();
        meta1.setName("group1");
        group1.setMetadata(meta1);
//        V1alpha1NodeGroupSpec spec1 = new V1alpha1NodeGroupSpec();
//        spec1.setNamespaces(List.of("ns1"));
//        group1.setSpec(spec1);

        V1alpha1Project project1 = new V1alpha1Project();
        V1ObjectMeta projectMeta1 = new V1ObjectMeta();
        projectMeta1.setName("project1");

        V1Pod pod1 = new V1Pod();
        V1ObjectMeta podMeta1 = new V1ObjectMeta();
        podMeta1.setNamespace("ns1");
        podMeta1.setName("pod1");
        pod1.setMetadata(podMeta1);
        V1PodSpec podSpec1 = new V1PodSpec();
        podSpec1.setTolerations(new ArrayList<>());
        V1Affinity affinity1 = new V1AffinityBuilder().withNodeAffinity(
                new V1NodeAffinityBuilder().withRequiredDuringSchedulingIgnoredDuringExecution(
                        new V1NodeSelectorBuilder().withNodeSelectorTerms(
                                new V1NodeSelectorTermBuilder().withMatchExpressions(
                                        new V1NodeSelectorRequirementBuilder()
                                                .withKey(Labels.KEY_NODE_GROUP)
                                                .withOperator("In")
                                                .withValues("group1").build()
                                ).build()
                        ).build()
                ).build()
        ).build();
        podSpec1.setAffinity(affinity1);
        pod1.setSpec(podSpec1);

        Mockito.doReturn(List.of(group1))
                .when(this.nodeGroupIndexer)
                .byIndex(IndexNames.BY_NAMESPACE_NAME_TO_NODE_GROUP_OBJECT, "ns1");
        Mockito.doReturn(pod1).when(this.podIndexer).getByKey(KeyUtil.buildKey("ns1", "pod1"));
        PodReconciler podReconciler = new PodReconciler(this.podIndexer, this.reconciliation, this.coreV1Api);
        podReconciler.reconcile(new Request("ns1", "pod1"));
        try {
            Mockito.verify(this.coreV1Api).deleteNamespacedPod(
                    Mockito.eq("pod1"),
                    Mockito.eq("ns1")).execute();
        } catch (ApiException e) {
            Assertions.fail();
        }
        Mockito.verifyNoMoreInteractions(this.coreV1Api);
    }

    @Test
    void given_pod_has_proper_tolerations_when_reconcile_the_pod_then_should_do_nothing() {
        V1alpha1NodeGroup group1 = new V1alpha1NodeGroup();
        V1ObjectMeta meta1 = new V1ObjectMeta();
        meta1.setName("group1");
        group1.setMetadata(meta1);

//        V1alpha1NodeGroupSpec spec1 = new V1alpha1NodeGroupSpec();
//        spec1.setNamespaces(List.of("ns1"));
//        group1.setSpec(spec1);

        V1Pod pod1 = new V1Pod();
        V1ObjectMeta podMeta1 = new V1ObjectMeta();
        podMeta1.setNamespace("ns1");
        podMeta1.setName("pod1");
        pod1.setMetadata(podMeta1);
        V1PodSpec podSpec1 = new V1PodSpec();
        V1TolerationBuilder tolerationBuilder = new V1TolerationBuilder()
                .withKey(Taints.KEY_NODE_GROUP)
                .withValue("group1")
                .withOperator("Equal");
        podSpec1.setTolerations(List.of(tolerationBuilder.withEffect("NoSchedule").build()));
        V1Affinity affinity1 = new V1AffinityBuilder().withNodeAffinity(
                new V1NodeAffinityBuilder().withRequiredDuringSchedulingIgnoredDuringExecution(
                        new V1NodeSelectorBuilder().withNodeSelectorTerms(
                                new V1NodeSelectorTermBuilder().withMatchExpressions(
                                        new V1NodeSelectorRequirementBuilder()
                                                .withKey(Labels.KEY_NODE_GROUP)
                                                .withOperator("In")
                                                .withValues("group1").build()
                                ).build()
                        ).build()
                ).build()
        ).build();
        podSpec1.setAffinity(affinity1);
        pod1.setSpec(podSpec1);

        Mockito.doReturn(List.of(group1))
                .when(this.nodeGroupIndexer)
                .byIndex(IndexNames.BY_NAMESPACE_NAME_TO_NODE_GROUP_OBJECT, "ns1");
        Mockito.doReturn(pod1).when(this.podIndexer).getByKey(KeyUtil.buildKey("ns1", "pod1"));
        PodReconciler podReconciler = new PodReconciler(this.podIndexer, this.reconciliation, this.coreV1Api);
        podReconciler.reconcile(new Request("ns1", "pod1"));
        try {
            Mockito.verifyNoInteractions(this.coreV1Api);
        } catch (Exception e) {
            Assertions.fail();
        }
    }

    @Test
    void given_pod_has_tolerations_for_not_existing_group_then_should_delete_pod() {
        V1alpha1NodeGroup group1 = new V1alpha1NodeGroup();
        V1ObjectMeta meta1 = new V1ObjectMeta();
        meta1.setName("group1");
        group1.setMetadata(meta1);
//        V1alpha1NodeGroupSpec spec1 = new V1alpha1NodeGroupSpec();
//        spec1.setNamespaces(List.of("ns1"));
//        group1.setSpec(spec1);
        V1Pod pod1 = new V1Pod();
        V1ObjectMeta podMeta1 = new V1ObjectMeta();
        podMeta1.setNamespace("ns1");
        podMeta1.setName("pod1");
        pod1.setMetadata(podMeta1);
        V1PodSpec podSpec1 = new V1PodSpec();
        V1TolerationBuilder tolerationBuilder = new V1TolerationBuilder()
                .withKey(Taints.KEY_NODE_GROUP)
                .withValue("group2")
                .withOperator("Equal");
        podSpec1.setTolerations(List.of(
                tolerationBuilder.withEffect("NoSchedule").build(),
                tolerationBuilder.withEffect("NoExecute").build()
        ));
        V1Affinity affinity1 = new V1AffinityBuilder().withNodeAffinity(
                new V1NodeAffinityBuilder().withRequiredDuringSchedulingIgnoredDuringExecution(
                        new V1NodeSelectorBuilder().withNodeSelectorTerms(
                                new V1NodeSelectorTermBuilder().withMatchExpressions(
                                        new V1NodeSelectorRequirementBuilder()
                                                .withKey(Labels.KEY_NODE_GROUP)
                                                .withOperator("In")
                                                .withValues("group1").build()
                                ).build()
                        ).build()
                ).build()
        ).build();
        podSpec1.setAffinity(affinity1);
        pod1.setSpec(podSpec1);

        Mockito.doReturn(pod1).when(this.podIndexer).getByKey(KeyUtil.buildKey("ns1", "pod1"));
        PodReconciler podReconciler = new PodReconciler(this.podIndexer, this.reconciliation, this.coreV1Api);
        podReconciler.reconcile(new Request("ns1", "pod1"));
        try {
            Mockito.verify(this.coreV1Api).deleteNamespacedPod(
                    Mockito.eq("pod1"),
                    Mockito.eq("ns1")).execute();
        } catch (ApiException e) {
            Assertions.fail();
        }
        Mockito.verifyNoMoreInteractions(this.coreV1Api);
    }

    @Test
    void given_pod_does_not_have_any_affinities_then_should_delete_pod() {
        V1alpha1NodeGroup group1 = new V1alpha1NodeGroup();
        V1ObjectMeta meta1 = new V1ObjectMeta();
        meta1.setName("group1");
        group1.setMetadata(meta1);
//        V1alpha1NodeGroupSpec spec1 = new V1alpha1NodeGroupSpec();
//        spec1.setNamespaces(List.of("ns1"));
//        group1.setSpec(spec1);
        V1Pod pod1 = new V1Pod();
        V1ObjectMeta podMeta1 = new V1ObjectMeta();
        podMeta1.setNamespace("ns1");
        podMeta1.setName("pod1");
        pod1.setMetadata(podMeta1);
        V1Affinity affinity1 = new V1Affinity();
        V1PodSpec podSpec1 = new V1PodSpec();
        podSpec1.setAffinity(affinity1);
        V1TolerationBuilder tolerationBuilder = new V1TolerationBuilder()
                .withKey(Taints.KEY_NODE_GROUP)
                .withValue("group1")
                .withOperator("Equal");
        podSpec1.setTolerations(List.of(
                tolerationBuilder.withEffect("NoSchedule").build(),
                tolerationBuilder.withEffect("NoExecute").build()
        ));
        pod1.setSpec(podSpec1);

        Mockito.doReturn(pod1).when(this.podIndexer).getByKey(KeyUtil.buildKey("ns1", "pod1"));
        PodReconciler podReconciler = new PodReconciler(this.podIndexer, this.reconciliation, this.coreV1Api);
        podReconciler.reconcile(new Request("ns1", "pod1"));
        try {
            Mockito.verify(this.coreV1Api).deleteNamespacedPod(
                    Mockito.eq("pod1"),
                    Mockito.eq("ns1")).execute();
        } catch (ApiException e) {
            Assertions.fail();
        }
        Mockito.verifyNoMoreInteractions(this.coreV1Api);
    }

    @Test
    void given_pod_has_affinity_not_for_group_then_should_delete_pod() {
        V1alpha1NodeGroup group1 = new V1alpha1NodeGroup();
        V1ObjectMeta meta1 = new V1ObjectMeta();
        meta1.setName("group1");
        group1.setMetadata(meta1);
//        V1alpha1NodeGroupSpec spec1 = new V1alpha1NodeGroupSpec();
//        spec1.setNamespaces(List.of("ns1"));
//        group1.setSpec(spec1);
        V1Pod pod1 = new V1Pod();
        V1ObjectMeta podMeta1 = new V1ObjectMeta();
        podMeta1.setNamespace("ns1");
        podMeta1.setName("pod1");
        pod1.setMetadata(podMeta1);
        V1Affinity affinity1 = new V1AffinityBuilder().withNodeAffinity(
                new V1NodeAffinityBuilder().withRequiredDuringSchedulingIgnoredDuringExecution(
                        new V1NodeSelectorBuilder().withNodeSelectorTerms(
                                new V1NodeSelectorTermBuilder().withMatchExpressions(
                                        new V1NodeSelectorRequirementBuilder()
                                                .withKey("kubernetes.io/hostname")
                                                .withOperator("In")
                                                .withValues("cpu1")
                                                .build()
                                ).build()
                        ).build()
                ).build()
        ).build();
        V1PodSpec podSpec1 = new V1PodSpec();
        podSpec1.setAffinity(affinity1);
        V1TolerationBuilder tolerationBuilder = new V1TolerationBuilder()
                .withKey(Taints.KEY_NODE_GROUP)
                .withValue("group1")
                .withOperator("Equal");
        podSpec1.setTolerations(List.of(
                tolerationBuilder.withEffect("NoSchedule").build(),
                tolerationBuilder.withEffect("NoExecute").build()
        ));
        pod1.setSpec(podSpec1);

        Mockito.doReturn(pod1).when(this.podIndexer).getByKey(KeyUtil.buildKey("ns1", "pod1"));
        PodReconciler podReconciler = new PodReconciler(this.podIndexer, this.reconciliation, this.coreV1Api);
        podReconciler.reconcile(new Request("ns1", "pod1"));
        try {
            Mockito.verify(this.coreV1Api).deleteNamespacedPod(
                    Mockito.eq("pod1"),
                    Mockito.eq("ns1")).execute();
        } catch (ApiException e) {
            Assertions.fail();
        }
        Mockito.verifyNoMoreInteractions(this.coreV1Api);
    }

    @Test
    void given_pod_has_an_affinity_which_includes_an_resource_group_exclusive_match_expression_then_do_nothing() {
        V1alpha1NodeGroup group1 = new V1alpha1NodeGroup();
        V1ObjectMeta meta1 = new V1ObjectMeta();
        meta1.setName("group1");
        group1.setMetadata(meta1);
//        V1alpha1NodeGroupSpec spec1 = new V1alpha1NodeGroupSpec();
//        spec1.setNamespaces(List.of("ns1"));
//        group1.setSpec(spec1);
        V1Pod pod1 = new V1Pod();
        V1ObjectMeta podMeta1 = new V1ObjectMeta();
        podMeta1.setNamespace("ns1");
        podMeta1.setName("pod1");
        pod1.setMetadata(podMeta1);
        V1Affinity affinity1 = new V1AffinityBuilder().withNodeAffinity(
                new V1NodeAffinityBuilder().withRequiredDuringSchedulingIgnoredDuringExecution(
                        new V1NodeSelectorBuilder().withNodeSelectorTerms(
                                new V1NodeSelectorTermBuilder().withMatchExpressions(
                                        new V1NodeSelectorRequirementBuilder()
                                                .withKey("kubernetes.io/hostname")
                                                .withOperator("In")
                                                .withValues("cpu1")
                                                .build(),
                                        new V1NodeSelectorRequirementBuilder()
                                                .withKey(Labels.KEY_NODE_GROUP)
                                                .withOperator("In")
                                                .withValues("group1")
                                                .build()
                                ).build()
                        ).build()
                ).build()
        ).build();
        V1PodSpec podSpec1 = new V1PodSpec();

        podSpec1.setAffinity(affinity1);
        V1TolerationBuilder tolerationBuilder = new V1TolerationBuilder()
                .withKey(Taints.KEY_NODE_GROUP)
                .withValue("group1")
                .withOperator("Equal");
        podSpec1.setTolerations(List.of(tolerationBuilder.withEffect("NoSchedule").build()));
        pod1.setSpec(podSpec1);
        Mockito.doReturn(List.of(group1))
                .when(this.nodeGroupIndexer)
                .byIndex(IndexNames.BY_NAMESPACE_NAME_TO_NODE_GROUP_OBJECT, "ns1");
        Mockito.doReturn(pod1).when(this.podIndexer).getByKey(KeyUtil.buildKey("ns1", "pod1"));
        PodReconciler podReconciler = new PodReconciler(this.podIndexer, this.reconciliation, this.coreV1Api);
        podReconciler.reconcile(new Request("ns1", "pod1"));
        try {
            Mockito.verifyNoInteractions(this.coreV1Api);
        } catch (Exception e) {
            Assertions.fail();
        }
    }

    @Test
    void given_pod_has_affinities_and_each_affinity_has_group_exclusive_match_expressions_then_do_nothing() {
        V1alpha1NodeGroup group1 = new V1alpha1NodeGroup();
        V1ObjectMeta meta1 = new V1ObjectMeta();
        meta1.setName("group1");
        group1.setMetadata(meta1);
//        V1alpha1NodeGroupSpec spec1 = new V1alpha1NodeGroupSpec();
//        spec1.setNamespaces(List.of("ns1"));
//        group1.setSpec(spec1);
        V1Pod pod1 = new V1Pod();
        V1ObjectMeta podMeta1 = new V1ObjectMeta();
        podMeta1.setNamespace("ns1");
        podMeta1.setName("pod1");
        pod1.setMetadata(podMeta1);
        V1Affinity affinity1 = new V1AffinityBuilder().withNodeAffinity(
                new V1NodeAffinityBuilder().withRequiredDuringSchedulingIgnoredDuringExecution(
                        new V1NodeSelectorBuilder().withNodeSelectorTerms(
                                new V1NodeSelectorTermBuilder().withMatchExpressions(
                                        new V1NodeSelectorRequirementBuilder()
                                                .withKey("kubernetes.io/hostname")
                                                .withOperator("In")
                                                .withValues("cpu1")
                                                .build(),
                                        new V1NodeSelectorRequirementBuilder()
                                                .withKey(Labels.KEY_NODE_GROUP)
                                                .withOperator("In")
                                                .withValues("group1")
                                                .build()
                                ).build(),
                                new V1NodeSelectorTermBuilder().withMatchExpressions(
                                        new V1NodeSelectorRequirementBuilder()
                                                .withKey("kubernetes.io/hostname")
                                                .withOperator("In")
                                                .withValues("cpu2")
                                                .build(),
                                        new V1NodeSelectorRequirementBuilder()
                                                .withKey(Labels.KEY_NODE_GROUP)
                                                .withOperator("In")
                                                .withValues("group1")
                                                .build()
                                ).build()
                        ).build()
                ).build()
        ).build();
        V1PodSpec podSpec1 = new V1PodSpec();

        podSpec1.setAffinity(affinity1);
        V1TolerationBuilder tolerationBuilder = new V1TolerationBuilder()
                .withKey(Taints.KEY_NODE_GROUP)
                .withValue("group1")
                .withOperator("Equal");
        podSpec1.setTolerations(List.of(tolerationBuilder.withEffect("NoSchedule").build()));
        pod1.setSpec(podSpec1);
        Mockito.doReturn(List.of(group1))
                .when(this.nodeGroupIndexer)
                .byIndex(IndexNames.BY_NAMESPACE_NAME_TO_NODE_GROUP_OBJECT, "ns1");
        Mockito.doReturn(pod1).when(this.podIndexer).getByKey(KeyUtil.buildKey("ns1", "pod1"));
        PodReconciler podReconciler = new PodReconciler(this.podIndexer, this.reconciliation, this.coreV1Api);
        podReconciler.reconcile(new Request("ns1", "pod1"));
        try {
            Mockito.verifyNoInteractions(this.coreV1Api);
        } catch (Exception e) {
            Assertions.fail();
        }
    }

    @Test
    void given_pod_has_affinities_including_a_match_expression_for_not_existing_group_then_should_delete_pod() {
        V1alpha1NodeGroup group1 = new V1alpha1NodeGroup();
        V1ObjectMeta meta1 = new V1ObjectMeta();
        meta1.setName("group1");
        group1.setMetadata(meta1);
//        V1alpha1NodeGroupSpec spec1 = new V1alpha1NodeGroupSpec();
//        spec1.setNamespaces(List.of("ns1"));
//        group1.setSpec(spec1);
        V1Pod pod1 = new V1Pod();
        V1ObjectMeta podMeta1 = new V1ObjectMeta();
        podMeta1.setNamespace("ns1");
        podMeta1.setName("pod1");
        pod1.setMetadata(podMeta1);
        V1Affinity affinity1 = new V1AffinityBuilder().withNodeAffinity(
                new V1NodeAffinityBuilder().withRequiredDuringSchedulingIgnoredDuringExecution(
                        new V1NodeSelectorBuilder().withNodeSelectorTerms(
                                new V1NodeSelectorTermBuilder().withMatchExpressions(
                                        new V1NodeSelectorRequirementBuilder()
                                                .withKey("kubernetes.io/hostname")
                                                .withOperator("In")
                                                .withValues("cpu1")
                                                .build(),
                                        new V1NodeSelectorRequirementBuilder()
                                                .withKey(Labels.KEY_NODE_GROUP)
                                                .withOperator("In")
                                                .withValues("group2")
                                                .build()
                                ).build()
                        ).build()
                ).build()
        ).build();
        V1PodSpec podSpec1 = new V1PodSpec();

        podSpec1.setAffinity(affinity1);
        V1TolerationBuilder tolerationBuilder = new V1TolerationBuilder()
                .withKey(Taints.KEY_NODE_GROUP)
                .withValue("group1")
                .withOperator("Equal");
        podSpec1.setTolerations(List.of(tolerationBuilder.withEffect("NoSchedule").build()));
        pod1.setSpec(podSpec1);
        Mockito.doReturn(List.of(group1))
                .when(this.nodeGroupIndexer)
                .byIndex(IndexNames.BY_NAMESPACE_NAME_TO_NODE_GROUP_OBJECT, "ns1");
        Mockito.doReturn(pod1).when(this.podIndexer).getByKey(KeyUtil.buildKey("ns1", "pod1"));
        PodReconciler podReconciler = new PodReconciler(this.podIndexer, this.reconciliation, this.coreV1Api);
        podReconciler.reconcile(new Request("ns1", "pod1"));
        try {
            Mockito.verify(this.coreV1Api).deleteNamespacedPod(
                    Mockito.eq("pod1"),
                    Mockito.eq("ns1")).execute();
        } catch (Exception e) {
            Assertions.fail();
        }
    }

    @Test
    void given_pod_has_exclusive_node_affinity_only_then_do_nothing() {
        V1alpha1NodeGroup group1 = new V1alpha1NodeGroup();
        V1ObjectMeta meta1 = new V1ObjectMeta();
        meta1.setName("group1");
        group1.setMetadata(meta1);
//        V1alpha1NodeGroupSpec spec1 = new V1alpha1NodeGroupSpec();
//        spec1.setNamespaces(List.of("ns1"));
//        group1.setSpec(spec1);

        V1Pod pod1 = new V1Pod();
        V1ObjectMeta podMeta1 = new V1ObjectMeta();
        podMeta1.setNamespace("ns1");
        podMeta1.setName("pod1");
        pod1.setMetadata(podMeta1);
        V1Affinity affinity1 = new V1AffinityBuilder().withNodeAffinity(
                new V1NodeAffinityBuilder().withRequiredDuringSchedulingIgnoredDuringExecution(
                        new V1NodeSelectorBuilder().withNodeSelectorTerms(
                                new V1NodeSelectorTermBuilder().withMatchExpressions(
                                        new V1NodeSelectorRequirementBuilder()
                                                .withKey(Labels.KEY_NODE_GROUP)
                                                .withOperator("In")
                                                .withValues("group1")
                                                .build()
                                ).build()
                        ).build()
                ).build()
        ).build();
        V1PodSpec podSpec1 = new V1PodSpec();
        podSpec1.setAffinity(affinity1);
        V1TolerationBuilder tolerationBuilder = new V1TolerationBuilder()
                .withKey(Taints.KEY_NODE_GROUP)
                .withValue("group1")
                .withOperator("Equal");
        podSpec1.setTolerations(List.of(tolerationBuilder.withEffect("NoSchedule").build()));
        pod1.setSpec(podSpec1);

        Mockito.doReturn(List.of(group1))
                .when(this.nodeGroupIndexer)
                .byIndex(IndexNames.BY_NAMESPACE_NAME_TO_NODE_GROUP_OBJECT, "ns1");
        Mockito.doReturn(pod1).when(this.podIndexer).getByKey(KeyUtil.buildKey("ns1", "pod1"));
        PodReconciler podReconciler = new PodReconciler(this.podIndexer, this.reconciliation, this.coreV1Api);
        podReconciler.reconcile(new Request("ns1", "pod1"));
        try {
            Mockito.verifyNoInteractions(this.coreV1Api);
        } catch (Exception e) {
            Assertions.fail();
        }
    }

    @Test
    void given_pod_has_no_tolerations_and_affinity_and_groups_not_exist_then_do_nothing() {
        V1Pod pod1 = new V1Pod();
        V1ObjectMeta podMeta1 = new V1ObjectMeta();
        podMeta1.setNamespace("ns1");
        podMeta1.setName("pod1");
        pod1.setMetadata(podMeta1);
        V1PodSpec podSpec1 = new V1PodSpec();
        podSpec1.setTolerations(new ArrayList<>());
        podSpec1.setTolerations(new ArrayList<>());
        pod1.setSpec(podSpec1);

        Mockito.doReturn(new ArrayList<>())
                .when(this.nodeGroupIndexer)
                .byIndex(IndexNames.BY_NAMESPACE_NAME_TO_NODE_GROUP_OBJECT, "ns1");
        Mockito.doReturn(pod1).when(this.podIndexer).getByKey(KeyUtil.buildKey("ns1", "pod1"));
        PodReconciler podReconciler = new PodReconciler(this.podIndexer, this.reconciliation, this.coreV1Api);
        podReconciler.reconcile(new Request("ns1", "pod1"));
        try {
            Mockito.verifyNoInteractions(this.coreV1Api);
        } catch (Exception e) {
            Assertions.fail();
        }
    }

}

