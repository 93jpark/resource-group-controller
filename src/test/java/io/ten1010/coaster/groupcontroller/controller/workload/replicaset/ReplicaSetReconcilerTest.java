package io.ten1010.coaster.groupcontroller.controller.workload.replicaset;

import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
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

public class ReplicaSetReconcilerTest {

    private Indexer<V1ReplicaSet> replicaSetIndexer;
    private Indexer<V1Beta1ResourceGroup> groupIndexer;
    private Reconciliation reconciliation;
    private AppsV1Api appsV1Api;

    @BeforeEach
    void setUp() {
        this.replicaSetIndexer = Mockito.mock(Indexer.class);
        this.groupIndexer = Mockito.mock(Indexer.class);
        this.reconciliation = new Reconciliation(this.groupIndexer);
        this.appsV1Api = Mockito.mock(AppsV1Api.class);
    }

    @Test
    void given_replicaset_does_not_have_toleration_and_affinity_when_it_belongs_to_resource_group_then_should_update_replicaset() {
        V1Beta1ResourceGroup group1 = new V1Beta1ResourceGroup();
        V1ObjectMeta meta1 = new V1ObjectMeta();
        meta1.setName("group1");
        group1.setMetadata(meta1);
        V1Beta1ResourceGroupSpec spec1 = new V1Beta1ResourceGroupSpec();
        spec1.setNamespaces(List.of("ns1"));
        group1.setSpec(spec1);

        V1ReplicaSet replicaSet1 = new V1ReplicaSet();
        V1ObjectMeta rsMeta1 = new V1ObjectMeta();
        rsMeta1.setName("rs1");
        rsMeta1.setNamespace("ns1");
        replicaSet1.setMetadata(rsMeta1);
        V1ReplicaSetSpec rsSpec1 = new V1ReplicaSetSpec();
        V1PodTemplateSpec podTemplateSpec1 = new V1PodTemplateSpec();
        V1PodSpec podSpec1 = new V1PodSpec();
        podTemplateSpec1.setSpec(podSpec1);
        V1ObjectMeta podMeta1 = new V1ObjectMeta();
        podMeta1.setNamespace("ns1");
        podMeta1.setName("pod1");
        podTemplateSpec1.setMetadata(podMeta1);
        rsSpec1.setTemplate(podTemplateSpec1);
        replicaSet1.setSpec(rsSpec1);
        Mockito.doReturn(List.of(group1))
                .when(this.groupIndexer)
                .byIndex(IndexNames.BY_NAMESPACE_NAME_TO_GROUP_OBJECT, "ns1");
        Mockito.doReturn(replicaSet1).when(this.replicaSetIndexer).getByKey(KeyUtil.buildKey("ns1", "rs1"));
        ReplicaSetReconciler replicaSetReconciler = new ReplicaSetReconciler(this.replicaSetIndexer, this.reconciliation, this.appsV1Api);
        replicaSetReconciler.reconcile(new Request("ns1", "rs1"));
        try {
            Mockito.verify(this.appsV1Api).replaceNamespacedReplicaSet(
                    Mockito.eq("rs1"),
                    Mockito.eq("ns1"),
                    Mockito.argThat(replicaSet -> {
                        List<String> tolerationValues = replicaSet.getSpec().getTemplate().getSpec().getTolerations().stream()
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
                        Set<String> affinityValues = replicaSet.getSpec().getTemplate().getSpec().getAffinity()
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
            Mockito.verifyNoMoreInteractions(this.appsV1Api);
        } catch (ApiException e) {
            Assertions.fail();
        }
    }

    @Test
    void given_replicaset_does_not_have_toleration_when_it_belongs_to_the_resource_group_then_should_update_replicaset() {
        V1Beta1ResourceGroup group1 = new V1Beta1ResourceGroup();
        V1ObjectMeta meta1 = new V1ObjectMeta();
        meta1.setName("group1");
        group1.setMetadata(meta1);
        V1Beta1ResourceGroupSpec spec1 = new V1Beta1ResourceGroupSpec();
        spec1.setNamespaces(List.of("ns1"));
        group1.setSpec(spec1);

        V1ReplicaSet replicaSet1 = new V1ReplicaSet();
        V1ObjectMeta rsMeta1 = new V1ObjectMeta();
        rsMeta1.setName("rs1");
        rsMeta1.setNamespace("ns1");
        replicaSet1.setMetadata(rsMeta1);
        V1ReplicaSetSpec rsSpec1 = new V1ReplicaSetSpec();
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
        podTemplateSpec1.setSpec(podSpec1);
        V1ObjectMeta podMeta1 = new V1ObjectMeta();
        podMeta1.setNamespace("ns1");
        podMeta1.setName("pod1");
        podTemplateSpec1.setMetadata(podMeta1);
        rsSpec1.setTemplate(podTemplateSpec1);
        replicaSet1.setSpec(rsSpec1);
        Mockito.doReturn(List.of(group1))
                .when(this.groupIndexer)
                .byIndex(IndexNames.BY_NAMESPACE_NAME_TO_GROUP_OBJECT, "ns1");
        Mockito.doReturn(replicaSet1).when(this.replicaSetIndexer).getByKey(KeyUtil.buildKey("ns1", "rs1"));
        ReplicaSetReconciler replicaSetReconciler = new ReplicaSetReconciler(this.replicaSetIndexer, this.reconciliation, this.appsV1Api);
        replicaSetReconciler.reconcile(new Request("ns1", "rs1"));
        try {
            Mockito.verify(this.appsV1Api).replaceNamespacedReplicaSet(
                    Mockito.eq("rs1"),
                    Mockito.eq("ns1"),
                    Mockito.argThat(replicaSet -> {
                        List<String> tolerationValues = replicaSet.getSpec().getTemplate().getSpec().getTolerations().stream()
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
                        Set<String> affinityValues = replicaSet.getSpec().getTemplate().getSpec().getAffinity()
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
            Mockito.verifyNoMoreInteractions(this.appsV1Api);
        } catch (ApiException e) {
            Assertions.fail();
        }
    }

    @Test
    void given_replicaset_does_not_have_affinity_when_it_belongs_to_resource_group_then_should_update_replicaset() {
        V1Beta1ResourceGroup group1 = new V1Beta1ResourceGroup();
        V1ObjectMeta meta1 = new V1ObjectMeta();
        meta1.setName("group1");
        group1.setMetadata(meta1);
        V1Beta1ResourceGroupSpec spec1 = new V1Beta1ResourceGroupSpec();
        spec1.setNamespaces(List.of("ns1"));
        group1.setSpec(spec1);

        V1ReplicaSet replicaSet1 = new V1ReplicaSet();
        V1ObjectMeta rsMeta1 = new V1ObjectMeta();
        rsMeta1.setName("rs1");
        rsMeta1.setNamespace("ns1");
        replicaSet1.setMetadata(rsMeta1);
        V1ReplicaSetSpec rsSpec1 = new V1ReplicaSetSpec();
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
        rsSpec1.setTemplate(podTemplateSpec1);
        replicaSet1.setSpec(rsSpec1);
        Mockito.doReturn(List.of(group1))
                .when(this.groupIndexer)
                .byIndex(IndexNames.BY_NAMESPACE_NAME_TO_GROUP_OBJECT, "ns1");
        Mockito.doReturn(replicaSet1).when(this.replicaSetIndexer).getByKey(KeyUtil.buildKey("ns1", "rs1"));
        ReplicaSetReconciler replicaSetReconciler = new ReplicaSetReconciler(this.replicaSetIndexer, this.reconciliation, this.appsV1Api);
        replicaSetReconciler.reconcile(new Request("ns1", "rs1"));
        try {
            Mockito.verify(this.appsV1Api).replaceNamespacedReplicaSet(
                    Mockito.eq("rs1"),
                    Mockito.eq("ns1"),
                    Mockito.argThat(replicaSet -> {
                        List<String> tolerationValues = replicaSet.getSpec().getTemplate().getSpec().getTolerations().stream()
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
                        Set<String> affinityValues = replicaSet.getSpec().getTemplate().getSpec().getAffinity()
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
            Mockito.verifyNoMoreInteractions(this.appsV1Api);
        } catch (ApiException e) {
            Assertions.fail();
        }
    }

    @Test
    void given_replicaset_has_unsupported_controller_then_should_delete_replicaset() {
        V1Beta1ResourceGroup group1 = new V1Beta1ResourceGroup();
        V1ObjectMeta meta1 = new V1ObjectMeta();
        meta1.setName("group1");
        group1.setMetadata(meta1);
        V1Beta1ResourceGroupSpec spec1 = new V1Beta1ResourceGroupSpec();
        spec1.setNamespaces(List.of("ns1"));
        group1.setSpec(spec1);

        V1ReplicaSet replicaSet1 = new V1ReplicaSet();
        V1ObjectMeta rsMeta1 = new V1ObjectMeta();
        rsMeta1.setName("rs1");
        rsMeta1.setNamespace("ns1");
        replicaSet1.setMetadata(rsMeta1);
        V1ReplicaSetSpec rsSpec1 = new V1ReplicaSetSpec();
        V1PodTemplateSpec podTemplateSpec1 = new V1PodTemplateSpec();
        V1PodSpec podSpec1 = new V1PodSpec();
        V1TolerationBuilder tolerationBuilder = new V1TolerationBuilder()
                .withKey(Taints.KEY_RESOURCE_GROUP_EXCLUSIVE)
                .withValue("group1")
                .withOperator("Equal");
        podSpec1.setTolerations(List.of(tolerationBuilder.withEffect("NoSchedule").build()));
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
        podTemplateSpec1.setSpec(podSpec1);
        V1ObjectMeta podMeta1 = new V1ObjectMeta();
        podMeta1.setNamespace("ns1");
        podMeta1.setName("pod1");
        podTemplateSpec1.setMetadata(podMeta1);
        rsSpec1.setTemplate(podTemplateSpec1);
        replicaSet1.setSpec(rsSpec1);
        V1OwnerReference ownerReference1 = new V1OwnerReference();
        ownerReference1.setApiVersion("v1");
        ownerReference1.setKind("Deployment");
        ownerReference1.setController(true);
        replicaSet1.getMetadata().setOwnerReferences(List.of(ownerReference1));
        Mockito.doReturn(List.of(group1))
                .when(this.groupIndexer)
                .byIndex(IndexNames.BY_NAMESPACE_NAME_TO_GROUP_OBJECT, "ns1");
        Mockito.doReturn(replicaSet1).when(this.replicaSetIndexer).getByKey(KeyUtil.buildKey("ns1", "rs1"));
        ReplicaSetReconciler replicaSetReconciler = new ReplicaSetReconciler(this.replicaSetIndexer, this.reconciliation, this.appsV1Api);
        replicaSetReconciler.reconcile(new Request("ns1", "rs1"));
        try {
            Mockito.verify(this.appsV1Api).deleteNamespacedReplicaSet(
                    Mockito.eq("rs1"),
                    Mockito.eq("ns1"),
                    Mockito.eq(null),
                    Mockito.eq(null),
                    Mockito.eq(null),
                    Mockito.eq(null),
                    Mockito.eq(null),
                    Mockito.eq(null));
            Mockito.verifyNoMoreInteractions(this.appsV1Api);
        } catch (ApiException e) {
            Assertions.fail();
        }
    }

}
