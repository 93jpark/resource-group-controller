package io.ten1010.coaster.groupcontroller.controller.workload.replicationcontroller;

import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.ten1010.coaster.groupcontroller.controller.Reconciliation;
import io.ten1010.coaster.groupcontroller.core.IndexNames;
import io.ten1010.coaster.groupcontroller.core.KeyUtil;
import io.ten1010.coaster.groupcontroller.core.Labels;
import io.ten1010.coaster.groupcontroller.core.Taints;
import io.ten1010.coaster.groupcontroller.model.V1Beta1ResourceGroup;
import io.ten1010.coaster.groupcontroller.model.V1Beta1ResourceGroupSpec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ReplicationControllerReconcilerTest {

    Indexer<V1ReplicationController> replicationControllerIndexer;
    Indexer<V1Beta1ResourceGroup> groupIndexer;
    Reconciliation reconciliation;
    CoreV1Api coreV1Api;

    @BeforeEach
    void setUp() {
        this.replicationControllerIndexer = Mockito.mock(Indexer.class);
        this.groupIndexer = Mockito.mock(Indexer.class);
        this.reconciliation = new Reconciliation(this.groupIndexer);
        this.coreV1Api = Mockito.mock(CoreV1Api.class);
    }

    @Test
    void given_replication_controller_does_not_have_toleration_and_affinity_when_it_belongs_to_the_resource_group_then_should_update_replication_controller() {
        V1Beta1ResourceGroup group1 = new V1Beta1ResourceGroup();
        V1ObjectMeta meta1 = new V1ObjectMeta();
        meta1.setName("group1");
        group1.setMetadata(meta1);
        V1Beta1ResourceGroupSpec spec1 = new V1Beta1ResourceGroupSpec();
        spec1.setNamespaces(List.of("ns1"));
        group1.setSpec(spec1);

        V1ReplicationController rc1 = new V1ReplicationController();
        V1ReplicationControllerSpec rcSpec1 = new V1ReplicationControllerSpec();
        V1PodTemplateSpec podTemplateSpec1 = new V1PodTemplateSpec();
        podTemplateSpec1.setSpec(new V1PodSpec());
        V1ObjectMeta podMeta1 = new V1ObjectMeta();
        podMeta1.setNamespace("ns1");
        podMeta1.setName("pod1");
        podTemplateSpec1.setMetadata(podMeta1);
        V1ObjectMeta rcMeta1 = new V1ObjectMeta();
        rcMeta1.setNamespace("ns1");
        rcMeta1.setName("rc1");
        rc1.setMetadata(rcMeta1);
        rcSpec1.setTemplate(podTemplateSpec1);
        rc1.setSpec(rcSpec1);
        Mockito.doReturn(List.of(group1))
                .when(this.groupIndexer)
                .byIndex(IndexNames.BY_NAMESPACE_NAME_TO_GROUP_OBJECT, "ns1");
        Mockito.doReturn(rc1).when(this.replicationControllerIndexer).getByKey(KeyUtil.buildKey("ns1", "rc1"));
        ReplicationControllerReconciler rcReconciler = new ReplicationControllerReconciler(this.replicationControllerIndexer, this.reconciliation, this.coreV1Api);
        rcReconciler.reconcile(new Request("ns1", "rc1"));
        try {
            Mockito.verify(this.coreV1Api).replaceNamespacedReplicationController(
                    Mockito.eq("rc1"),
                    Mockito.eq("ns1"),
                    Mockito.argThat(rc -> {
                        List<String> tolerationValues = rc.getSpec().getTemplate().getSpec().getTolerations().stream()
                                .filter(e -> {
                                    if (e.getKey() == null) {
                                        return false;
                                    }
                                    return e.getKey().equals(Taints.KEY_RESOURCE_GROUP_EXCLUSIVE);
                                })
                                .map(V1Toleration::getValue)
                                .collect(Collectors.toList());
                        Set<String> valueSet = new HashSet<>(tolerationValues);
                        if (!valueSet.equals(Set.of("group1"))) {
                            return false;
                        }
                        Set<String> affinityValues = rc.getSpec().getTemplate().getSpec().getAffinity()
                                .getNodeAffinity().getRequiredDuringSchedulingIgnoredDuringExecution().getNodeSelectorTerms().stream()
                                .flatMap(nst -> nst.getMatchExpressions().stream())
                                .filter(nsr -> nsr.getKey().equals(Labels.KEY_RESOURCE_GROUP_EXCLUSIVE))
                                .flatMap(nsr -> nsr.getValues().stream())
                                .collect(Collectors.toSet());
                        if (!affinityValues.equals(Set.of("group1"))) {
                            return false;
                        }
                        return true;
                    }),
                    Mockito.eq(null),
                    Mockito.eq(null),
                    Mockito.eq(null),
                    Mockito.eq(null));
            Mockito.verifyNoMoreInteractions(this.coreV1Api);
        } catch (ApiException e) {
            Assertions.fail();
        }
    }

    @Test
    void given_replication_controller_does_not_have_toleration_when_it_belongs_to_the_resource_group_then_should_update_replication_controller() {
        V1Beta1ResourceGroup group1 = new V1Beta1ResourceGroup();
        V1ObjectMeta meta1 = new V1ObjectMeta();
        meta1.setName("group1");
        group1.setMetadata(meta1);
        V1Beta1ResourceGroupSpec spec1 = new V1Beta1ResourceGroupSpec();
        spec1.setNamespaces(List.of("ns1"));
        group1.setSpec(spec1);

        V1ReplicationController rc1 = new V1ReplicationController();
        V1ReplicationControllerSpec rcSpec1 = new V1ReplicationControllerSpec();
        V1PodTemplateSpec podTemplateSpec1 = new V1PodTemplateSpec();
        V1PodSpec podSpec1 = new V1PodSpec();
        V1Affinity affinity1 = new V1AffinityBuilder().withNodeAffinity(
                new V1NodeAffinityBuilder().withRequiredDuringSchedulingIgnoredDuringExecution(
                        new V1NodeSelectorBuilder().withNodeSelectorTerms(
                                new V1NodeSelectorTermBuilder().withMatchExpressions(
                                        new V1NodeSelectorRequirementBuilder()
                                                .withKey(Labels.KEY_RESOURCE_GROUP_EXCLUSIVE)
                                                .withOperator("In")
                                                .withValues("group1")
                                                .build()
                                ).build()
                        ).build()
                ).build()
        ).build();
        podSpec1.setAffinity(affinity1);
        podTemplateSpec1.setSpec(new V1PodSpec());
        V1ObjectMeta podMeta1 = new V1ObjectMeta();
        podMeta1.setNamespace("ns1");
        podMeta1.setName("pod1");
        podTemplateSpec1.setMetadata(podMeta1);
        V1ObjectMeta rcMeta1 = new V1ObjectMeta();
        rcMeta1.setNamespace("ns1");
        rcMeta1.setName("rc1");
        rc1.setMetadata(rcMeta1);
        rcSpec1.setTemplate(podTemplateSpec1);
        rc1.setSpec(rcSpec1);
        Mockito.doReturn(List.of(group1))
                .when(this.groupIndexer)
                .byIndex(IndexNames.BY_NAMESPACE_NAME_TO_GROUP_OBJECT, "ns1");
        Mockito.doReturn(rc1).when(this.replicationControllerIndexer).getByKey(KeyUtil.buildKey("ns1", "rc1"));
        ReplicationControllerReconciler rcReconciler = new ReplicationControllerReconciler(this.replicationControllerIndexer, this.reconciliation, this.coreV1Api);
        rcReconciler.reconcile(new Request("ns1", "rc1"));
        try {
            Mockito.verify(this.coreV1Api).replaceNamespacedReplicationController(
                    Mockito.eq("rc1"),
                    Mockito.eq("ns1"),
                    Mockito.argThat(rc -> {
                        List<String> tolerationValues = rc.getSpec().getTemplate().getSpec().getTolerations().stream()
                                .filter(e -> {
                                    if (e.getKey() == null) {
                                        return false;
                                    }
                                    return e.getKey().equals(Taints.KEY_RESOURCE_GROUP_EXCLUSIVE);
                                })
                                .map(V1Toleration::getValue)
                                .collect(Collectors.toList());
                        Set<String> valueSet = new HashSet<>(tolerationValues);
                        if (!valueSet.equals(Set.of("group1"))) {
                            return false;
                        }
                        Set<String> affinityValues = rc.getSpec().getTemplate().getSpec().getAffinity()
                                .getNodeAffinity().getRequiredDuringSchedulingIgnoredDuringExecution().getNodeSelectorTerms().stream()
                                .flatMap(nst -> nst.getMatchExpressions().stream())
                                .filter(nsr -> nsr.getKey().equals(Labels.KEY_RESOURCE_GROUP_EXCLUSIVE))
                                .flatMap(nsr -> nsr.getValues().stream())
                                .collect(Collectors.toSet());
                        if (!affinityValues.equals(Set.of("group1"))) {
                            return false;
                        }
                        return true;
                    }),
                    Mockito.eq(null),
                    Mockito.eq(null),
                    Mockito.eq(null),
                    Mockito.eq(null));
            Mockito.verifyNoMoreInteractions(this.coreV1Api);
        } catch (ApiException e) {
            Assertions.fail();
        }
    }

    @Test
    void given_replication_controller_does_not_have_affinity_when_it_belongs_to_the_resource_group_then_should_update_replication_controller() {
        V1Beta1ResourceGroup group1 = new V1Beta1ResourceGroup();
        V1ObjectMeta meta1 = new V1ObjectMeta();
        meta1.setName("group1");
        group1.setMetadata(meta1);
        V1Beta1ResourceGroupSpec spec1 = new V1Beta1ResourceGroupSpec();
        spec1.setNamespaces(List.of("ns1"));
        group1.setSpec(spec1);

        V1ReplicationController rc1 = new V1ReplicationController();
        V1ReplicationControllerSpec rcSpec1 = new V1ReplicationControllerSpec();
        V1PodTemplateSpec podTemplateSpec1 = new V1PodTemplateSpec();
        V1PodSpec podSpec1 = new V1PodSpec();
        V1TolerationBuilder tolerationBuilder = new V1TolerationBuilder()
                .withKey(Taints.KEY_RESOURCE_GROUP_EXCLUSIVE)
                .withValue("group1")
                .withOperator("Equal");
        podSpec1.setTolerations(List.of(tolerationBuilder.withEffect("NoSchedule").build()));
        podTemplateSpec1.setSpec(podSpec1);
        V1ObjectMeta podMeta1 = new V1ObjectMeta();
        podMeta1.setNamespace("ns1");
        podMeta1.setName("pod1");
        podTemplateSpec1.setMetadata(podMeta1);
        V1ObjectMeta rcMeta1 = new V1ObjectMeta();
        rcMeta1.setNamespace("ns1");
        rcMeta1.setName("rc1");
        rc1.setMetadata(rcMeta1);
        rcSpec1.setTemplate(podTemplateSpec1);
        rc1.setSpec(rcSpec1);
        Mockito.doReturn(List.of(group1))
                .when(this.groupIndexer)
                .byIndex(IndexNames.BY_NAMESPACE_NAME_TO_GROUP_OBJECT, "ns1");
        Mockito.doReturn(rc1).when(this.replicationControllerIndexer).getByKey(KeyUtil.buildKey("ns1", "rc1"));
        ReplicationControllerReconciler rcReconciler = new ReplicationControllerReconciler(this.replicationControllerIndexer, this.reconciliation, this.coreV1Api);
        rcReconciler.reconcile(new Request("ns1", "rc1"));
        try {
            Mockito.verify(this.coreV1Api).replaceNamespacedReplicationController(
                    Mockito.eq("rc1"),
                    Mockito.eq("ns1"),
                    Mockito.argThat(rc -> {
                        List<String> tolerationValues = rc.getSpec().getTemplate().getSpec().getTolerations().stream()
                                .filter(e -> {
                                    if (e.getKey() == null) {
                                        return false;
                                    }
                                    return e.getKey().equals(Taints.KEY_RESOURCE_GROUP_EXCLUSIVE);
                                })
                                .map(V1Toleration::getValue)
                                .collect(Collectors.toList());
                        Set<String> valueSet = new HashSet<>(tolerationValues);
                        if (!valueSet.equals(Set.of("group1"))) {
                            return false;
                        }
                        Set<String> affinityValues = rc.getSpec().getTemplate().getSpec().getAffinity()
                                .getNodeAffinity().getRequiredDuringSchedulingIgnoredDuringExecution().getNodeSelectorTerms().stream()
                                .flatMap(nst -> nst.getMatchExpressions().stream())
                                .filter(nsr -> nsr.getKey().equals(Labels.KEY_RESOURCE_GROUP_EXCLUSIVE))
                                .flatMap(nsr -> nsr.getValues().stream())
                                .collect(Collectors.toSet());
                        if (!affinityValues.equals(Set.of("group1"))) {
                            return false;
                        }
                        return true;
                    }),
                    Mockito.eq(null),
                    Mockito.eq(null),
                    Mockito.eq(null),
                    Mockito.eq(null));
            Mockito.verifyNoMoreInteractions(this.coreV1Api);
        } catch (ApiException e) {
            Assertions.fail();
        }
    }

    @Test
    void given_replication_controller_has_unsupported_controller_then_should_delete_replication_controller() {
        V1Beta1ResourceGroup group1 = new V1Beta1ResourceGroup();
        V1ObjectMeta meta1 = new V1ObjectMeta();
        meta1.setName("group1");
        group1.setMetadata(meta1);
        V1Beta1ResourceGroupSpec spec1 = new V1Beta1ResourceGroupSpec();
        spec1.setNamespaces(List.of("ns1"));
        group1.setSpec(spec1);

        V1ReplicationController rc1 = new V1ReplicationController();
        V1ReplicationControllerSpec rcSpec1 = new V1ReplicationControllerSpec();
        V1PodTemplateSpec podTemplateSpec1 = new V1PodTemplateSpec();
        V1PodSpec podSpec1 = new V1PodSpec();
        V1Affinity affinity1 = new V1AffinityBuilder().withNodeAffinity(
                new V1NodeAffinityBuilder().withRequiredDuringSchedulingIgnoredDuringExecution(
                        new V1NodeSelectorBuilder().withNodeSelectorTerms(
                                new V1NodeSelectorTermBuilder().withMatchExpressions(
                                        new V1NodeSelectorRequirementBuilder()
                                                .withKey(Labels.KEY_RESOURCE_GROUP_EXCLUSIVE)
                                                .withOperator("In")
                                                .withValues("group1")
                                                .build()
                                ).build()
                        ).build()
                ).build()
        ).build();
        podSpec1.setAffinity(affinity1);
        V1TolerationBuilder tolerationBuilder = new V1TolerationBuilder()
                .withKey(Taints.KEY_RESOURCE_GROUP_EXCLUSIVE)
                .withValue("group1")
                .withOperator("Equal");
        podSpec1.setTolerations(List.of(tolerationBuilder.withEffect("NoSchedule").build()));
        podTemplateSpec1.setSpec(podSpec1);
        V1ObjectMeta podMeta1 = new V1ObjectMeta();
        podMeta1.setNamespace("ns1");
        podMeta1.setName("pod1");
        podTemplateSpec1.setMetadata(podMeta1);
        V1ObjectMeta rcMeta1 = new V1ObjectMeta();
        rcMeta1.setNamespace("ns1");
        rcMeta1.setName("rc1");
        rc1.setMetadata(rcMeta1);
        rcSpec1.setTemplate(podTemplateSpec1);
        rc1.setSpec(rcSpec1);
        V1OwnerReference ownerReference1 = new V1OwnerReference();
        ownerReference1.setApiVersion("v1");
        ownerReference1.setKind("dummy");
        ownerReference1.setController(true);
        rc1.getMetadata().setOwnerReferences(List.of(ownerReference1));
        Mockito.doReturn(List.of(group1))
                .when(this.groupIndexer)
                .byIndex(IndexNames.BY_NAMESPACE_NAME_TO_GROUP_OBJECT, "ns1");
        Mockito.doReturn(rc1).when(this.replicationControllerIndexer).getByKey(KeyUtil.buildKey("ns1", "rc1"));
        ReplicationControllerReconciler rcReconciler = new ReplicationControllerReconciler(this.replicationControllerIndexer, this.reconciliation, this.coreV1Api);
        rcReconciler.reconcile(new Request("ns1", "rc1"));
        try {
            Mockito.verify(this.coreV1Api).deleteNamespacedReplicationController(
                    Mockito.eq("rc1"),
                    Mockito.eq("ns1"),
                    Mockito.eq(null),
                    Mockito.eq(null),
                    Mockito.eq(null),
                    Mockito.eq(null),
                    Mockito.eq(null),
                    Mockito.eq(null));
            Mockito.verifyNoMoreInteractions(this.coreV1Api);
        } catch (ApiException e) {
            Assertions.fail();
        }
    }

}
