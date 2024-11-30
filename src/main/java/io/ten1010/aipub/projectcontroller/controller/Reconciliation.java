package io.ten1010.aipub.projectcontroller.controller;

import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.models.V1Affinity;
import io.kubernetes.client.openapi.models.V1AffinityBuilder;
import io.kubernetes.client.openapi.models.V1CronJob;
import io.kubernetes.client.openapi.models.V1DaemonSet;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1LocalObjectReference;
import io.kubernetes.client.openapi.models.V1NodeSelectorRequirement;
import io.kubernetes.client.openapi.models.V1NodeSelectorRequirementBuilder;
import io.kubernetes.client.openapi.models.V1NodeSelectorTerm;
import io.kubernetes.client.openapi.models.V1NodeSelectorTermBuilder;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1ReplicaSet;
import io.kubernetes.client.openapi.models.V1ReplicationController;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.kubernetes.client.openapi.models.V1Toleration;
import io.kubernetes.client.openapi.models.V1TolerationBuilder;
import io.ten1010.aipub.projectcontroller.core.CronJobUtil;
import io.ten1010.aipub.projectcontroller.core.DaemonSetUtil;
import io.ten1010.aipub.projectcontroller.core.DeploymentUtil;
import io.ten1010.aipub.projectcontroller.core.IndexNames;
import io.ten1010.aipub.projectcontroller.core.JobUtil;
import io.ten1010.aipub.projectcontroller.core.K8sObjectUtil;
import io.ten1010.aipub.projectcontroller.core.KeyUtil;
import io.ten1010.aipub.projectcontroller.core.Labels;
import io.ten1010.aipub.projectcontroller.core.PodUtil;
import io.ten1010.aipub.projectcontroller.core.ReplicaSetUtil;
import io.ten1010.aipub.projectcontroller.core.ReplicationControllerUtil;
import io.ten1010.aipub.projectcontroller.core.StatefulSetUtil;
import io.ten1010.aipub.projectcontroller.core.Taints;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroup;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroupBinding;
import io.ten1010.aipub.projectcontroller.model.V1alpha1NodeGroup;
import io.ten1010.aipub.projectcontroller.model.V1alpha1NodeGroupBinding;
import io.ten1010.aipub.projectcontroller.model.V1alpha1Project;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Reconciliation {

    private static Optional<V1Affinity> reconcileAffinity(@Nullable V1Affinity existingAffinity, List<V1alpha1NodeGroup> nodeGroups) {
        if (existingAffinity == null) {
            List<V1NodeSelectorRequirement> reconciledExpressions = reconcileMatchExpressions(new ArrayList<>(), nodeGroups);
            if (reconciledExpressions == null) {
                return Optional.empty();
            }
            return Optional.of(new V1AffinityBuilder()
                    .withNewNodeAffinity()
                    .withNewRequiredDuringSchedulingIgnoredDuringExecution()
                    .withNodeSelectorTerms(new V1NodeSelectorTermBuilder()
                            .withMatchExpressions(reconciledExpressions)
                            .build())
                    .endRequiredDuringSchedulingIgnoredDuringExecution()
                    .endNodeAffinity()
                    .build());
        }
        V1AffinityBuilder builder = new V1AffinityBuilder(existingAffinity);
        if (existingAffinity.getNodeAffinity() == null) {
            List<V1NodeSelectorRequirement> reconciledExpressions = reconcileMatchExpressions(new ArrayList<>(), nodeGroups);
            if (reconciledExpressions == null) {
                return Optional.of(builder.build());
            }
            return Optional.of(builder
                    .withNewNodeAffinity()
                    .withNewRequiredDuringSchedulingIgnoredDuringExecution()
                    .withNodeSelectorTerms(new V1NodeSelectorTermBuilder()
                            .withMatchExpressions(reconciledExpressions)
                            .build())
                    .endRequiredDuringSchedulingIgnoredDuringExecution()
                    .endNodeAffinity()
                    .build());
        }
        if (existingAffinity.getNodeAffinity().getRequiredDuringSchedulingIgnoredDuringExecution() == null) {
            List<V1NodeSelectorRequirement> reconciledExpressions = reconcileMatchExpressions(new ArrayList<>(), nodeGroups);
            if (reconciledExpressions == null) {
                return Optional.of(builder.build());
            }
            return Optional.of(new V1AffinityBuilder(existingAffinity)
                    .editNodeAffinity()
                    .withNewRequiredDuringSchedulingIgnoredDuringExecution()
                    .withNodeSelectorTerms(new V1NodeSelectorTermBuilder()
                            .withMatchExpressions(reconciledExpressions)
                            .build())
                    .endRequiredDuringSchedulingIgnoredDuringExecution()
                    .endNodeAffinity()
                    .build());
        }
        List<V1NodeSelectorTerm> reconciledTerms = existingAffinity.getNodeAffinity().getRequiredDuringSchedulingIgnoredDuringExecution().getNodeSelectorTerms().stream()
                .map(term -> new V1NodeSelectorTermBuilder(term)
                        .withMatchExpressions(reconcileMatchExpressions(term.getMatchExpressions(), nodeGroups))
                        .build())
                .collect(Collectors.toList());
        return Optional.of(builder
                .editNodeAffinity()
                .editRequiredDuringSchedulingIgnoredDuringExecution()
                .withNodeSelectorTerms(reconciledTerms)
                .endRequiredDuringSchedulingIgnoredDuringExecution()
                .endNodeAffinity()
                .build());
    }

    private static List<V1Toleration> reconcileTolerations(List<V1Toleration> existingTolerations, List<V1alpha1NodeGroup> nodeGroups) {
        List<V1Toleration> tolerations = replaceAllKeyAllEffectTolerations(existingTolerations);
        tolerations = replaceAllKeyNoScheduleEffectTolerations(tolerations);
        tolerations = removeNodeGroupExclusiveTolerations(tolerations);
        tolerations.addAll(buildNodeGroupExclusiveTolerations(nodeGroups));
        return tolerations;
    }

    private static List<V1Toleration> replaceAllKeyAllEffectTolerations(List<V1Toleration> tolerations) {
        return tolerations.stream()
                .map(e -> {
                    if (isAllKeyAllEffectToleration(e)) {
                        return List.of(new V1TolerationBuilder()
                                        .withEffect(Taints.EFFECT_NO_EXECUTE)
                                        .withKey(null)
                                        .withOperator(e.getOperator())
                                        .withTolerationSeconds(e.getTolerationSeconds())
                                        .withValue(e.getValue())
                                        .build(),
                                new V1TolerationBuilder()
                                        .withEffect(Taints.EFFECT_NO_SCHEDULE)
                                        .withKey("node-role.kubernetes.io/control-plane")
                                        .withOperator("Exists")
                                        .withTolerationSeconds(null)
                                        .withValue(null)
                                        .build());
                    }
                    return List.of(e);
                })
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private static List<V1Toleration> replaceAllKeyNoScheduleEffectTolerations(List<V1Toleration> tolerations) {
        return tolerations.stream()
                .map(e -> {
                    if (isAllKeyNoScheduleEffectToleration(e)) {
                        return List.of(new V1TolerationBuilder()
                                        .withEffect(Taints.EFFECT_NO_SCHEDULE)
                                        .withKey("node-role.kubernetes.io/control-plane")
                                        .withOperator("Exists")
                                        .withTolerationSeconds(null)
                                        .withValue(null)
                                        .build(),
                                new V1TolerationBuilder()
                                        .withEffect(Taints.EFFECT_NO_SCHEDULE)
                                        .withKey("node.kubernetes.io/not-ready")
                                        .withOperator("Exists")
                                        .withTolerationSeconds(null)
                                        .withValue(null)
                                        .build());
                    }
                    return List.of(e);
                })
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private static boolean isAllKeyAllEffectToleration(V1Toleration toleration) {
        return (toleration.getKey() == null && toleration.getEffect() == null);
    }

    private static boolean isAllKeyNoScheduleEffectToleration(V1Toleration toleration) {
        if (toleration.getEffect() == null) {
            return false;
        }
        return (toleration.getKey() == null && toleration.getEffect().equals(Taints.EFFECT_NO_SCHEDULE));
    }

    @Nullable
    private static List<V1NodeSelectorRequirement> reconcileMatchExpressions(@Nullable List<V1NodeSelectorRequirement> existingExpressions, List<V1alpha1NodeGroup> nodeGroups) {
        if (existingExpressions == null) {
            List<V1NodeSelectorRequirement> expressions = buildNodeGroupExclusiveMatchExpressions(nodeGroups);
            if (expressions.isEmpty()) {
                return null;
            }
            return expressions;
        }
        List<V1NodeSelectorRequirement> expressions = extractNonNodeGroupExclusiveMatchExpressions(existingExpressions);
        expressions.addAll(buildNodeGroupExclusiveMatchExpressions(nodeGroups));
        if (expressions.isEmpty()) {
            return null;
        }
        return expressions;
    }

    private static List<V1NodeSelectorRequirement> extractNonNodeGroupExclusiveMatchExpressions(List<V1NodeSelectorRequirement> existingExpressions) {
        return existingExpressions
                .stream()
                .filter(exp -> !isNodeGroupExclusiveNodeSelectorRequirement(exp))
                .collect(Collectors.toList());
    }

    private static List<V1NodeSelectorRequirement> buildNodeGroupExclusiveMatchExpressions(List<V1alpha1NodeGroup> nodeGroups) {
        if (nodeGroups.isEmpty()) {
            return new ArrayList<>();
        }
        V1NodeSelectorRequirement expression = new V1NodeSelectorRequirementBuilder()
                .withKey(Labels.KEY_NODE_GROUP)
                .withOperator("In")
                .withValues(nodeGroups.stream()
                        .map(K8sObjectUtil::getName)
                        .distinct()
                        .collect(Collectors.toList()))
                .build();
        return List.of(expression);
    }

    private static boolean isNodeGroupExclusiveNodeSelectorRequirement(V1NodeSelectorRequirement requirement) {
        if (requirement.getKey() == null) {
            return false;
        }
        return requirement.getKey().equals(Labels.KEY_NODE_GROUP);
    }

    private static List<V1Toleration> removeNodeGroupExclusiveTolerations(List<V1Toleration> tolerations) {
        return tolerations.stream()
                .filter(e -> !isNodeGroupExclusiveToleration(e))
                .collect(Collectors.toList());
    }

    private static List<V1Toleration> buildNodeGroupExclusiveTolerations(List<V1alpha1NodeGroup> nodeGroups) {
        return nodeGroups.stream()
                .flatMap(e -> buildNodeGroupExclusiveTolerations(e).stream())
                .collect(Collectors.toList());
    }

    private static List<V1Toleration> buildNodeGroupExclusiveTolerations(V1alpha1NodeGroup nodeGroup) {
        V1TolerationBuilder baseBuilder = new V1TolerationBuilder()
                .withKey(Taints.KEY_NODE_GROUP)
                .withValue(K8sObjectUtil.getName(nodeGroup))
                .withOperator("Equal");
        V1Toleration noSchedule = baseBuilder
                .withEffect(Taints.EFFECT_NO_SCHEDULE)
                .build();

        return List.of(noSchedule);
    }

    private static boolean isNodeGroupExclusiveToleration(V1Toleration toleration) {
        if (toleration.getKey() == null) {
            return false;
        }
        return toleration.getKey().equals(Taints.KEY_NODE_GROUP);
    }

    /**
     * Reconcile image pull secrets of workload with project image pull secrets.
     *
     * @param existingImagePullSecrets
     * @param projectImagePullSecrets
     * @return
     */
    private static List<V1LocalObjectReference> reconcileImagePullSecrets(
            List<V1LocalObjectReference> existingImagePullSecrets, List<V1Secret> projectImagePullSecrets) {
        if (existingImagePullSecrets.isEmpty()) {

        }
        Set<String> existingSecretNames = existingImagePullSecrets.stream()
                .filter(Objects::nonNull)
                .map(V1LocalObjectReference::getName)
                .collect(Collectors.toSet());
        List<V1LocalObjectReference> requiredImagePullSecrets = projectImagePullSecrets.stream()
                .filter(Objects::nonNull)
                .map(secret -> {
                    V1LocalObjectReference reference = new V1LocalObjectReference();
                    reference.setName(K8sObjectUtil.getName(secret));
                    return reference;
                }).toList();
        List<V1LocalObjectReference> reconciledImagePullSecrets = new ArrayList<>(existingImagePullSecrets);
        requiredImagePullSecrets.stream()
                .filter(Objects::nonNull)
                .filter(imagePullSecret -> !existingSecretNames.contains(imagePullSecret.getName()))
                .forEach(reconciledImagePullSecrets::add);
        return reconciledImagePullSecrets;
    }

    private Indexer<V1alpha1Project> projectIndexer;
    private Indexer<V1alpha1NodeGroup> nodeGroupIndexer;
    private Indexer<V1alpha1NodeGroupBinding> nodeGroupBindingIndexer;
    private Indexer<V1alpha1ImageNamespaceGroup> imageNamespaceGroupIndexer;
    private Indexer<V1alpha1ImageNamespaceGroupBinding> imageNamespaceGroupBindingIndexer;
    private Indexer<V1Secret> secretIndexer;

    public Reconciliation(
            Indexer<V1alpha1Project> projectIndexer,
            Indexer<V1alpha1NodeGroup> nodeGroupIndexer,
            Indexer<V1alpha1NodeGroupBinding> nodeGroupBindingIndexer,
            Indexer<V1alpha1ImageNamespaceGroup> imageNamespaceGroupIndexer,
            Indexer<V1alpha1ImageNamespaceGroupBinding> imageNamespaceGroupBindingIndexer,
            Indexer<V1Secret> secretIndexer) {
        this.projectIndexer = projectIndexer;
        this.nodeGroupIndexer = nodeGroupIndexer;
        this.nodeGroupBindingIndexer = nodeGroupBindingIndexer;
        this.imageNamespaceGroupIndexer = imageNamespaceGroupIndexer;
        this.imageNamespaceGroupBindingIndexer = imageNamespaceGroupBindingIndexer;
        this.secretIndexer = secretIndexer;
    }

    public Optional<V1Affinity> reconcileUncontrolledCronJobAffinity(V1CronJob cronJob) {
        if (K8sObjectUtil.isControlled(cronJob)) {
            throw new IllegalArgumentException();
        }
        List<V1alpha1NodeGroup> nodeGroups = this.resolveNodeGroupByNamespace(K8sObjectUtil.getNamespace(cronJob));

        return reconcileAffinity(CronJobUtil.getAffinity(cronJob).orElse(null), nodeGroups);
    }

    public Optional<V1Affinity> reconcileUncontrolledDaemonSetAffinity(V1DaemonSet daemonSet) {
        if (K8sObjectUtil.isControlled(daemonSet)) {
            throw new IllegalArgumentException();
        }

        return DaemonSetUtil.getAffinity(daemonSet);
    }

    public Optional<V1Affinity> reconcileUncontrolledDeploymentAffinity(V1Deployment deployment) {
        if (K8sObjectUtil.isControlled(deployment)) {
            throw new IllegalArgumentException();
        }
        List<V1alpha1NodeGroup> nodeGroups = this.resolveNodeGroupByNamespace(K8sObjectUtil.getNamespace(deployment));

        return reconcileAffinity(DeploymentUtil.getAffinity(deployment).orElse(null), nodeGroups);
    }

    public Optional<V1Affinity> reconcileUncontrolledJobAffinity(V1Job job) {
        if (K8sObjectUtil.isControlled(job)) {
            throw new IllegalArgumentException();
        }
        List<V1alpha1NodeGroup> nodeGroups = this.resolveNodeGroupByNamespace(K8sObjectUtil.getNamespace(job));

        return reconcileAffinity(JobUtil.getAffinity(job).orElse(null), nodeGroups);
    }

    public Optional<V1Affinity> reconcileUncontrolledPodAffinity(V1Pod pod) {
        if (K8sObjectUtil.isControlled(pod)) {
            throw new IllegalArgumentException();
        }
        List<V1alpha1NodeGroup> nodeGroups = this.resolveNodeGroupByNamespace(K8sObjectUtil.getNamespace(pod));

        return reconcileAffinity(PodUtil.getAffinity(pod).orElse(null), nodeGroups);
    }

    public Optional<V1Affinity> reconcileUncontrolledReplicaSetAffinity(V1ReplicaSet replicaSet) {
        if (K8sObjectUtil.isControlled(replicaSet)) {
            throw new IllegalArgumentException();
        }
        List<V1alpha1NodeGroup> nodeGroups = this.resolveNodeGroupByNamespace(K8sObjectUtil.getNamespace(replicaSet));

        return reconcileAffinity(ReplicaSetUtil.getAffinity(replicaSet).orElse(null), nodeGroups);
    }

    public Optional<V1Affinity> reconcileUncontrolledReplicationControllerAffinity(V1ReplicationController replicationController) {
        if (K8sObjectUtil.isControlled(replicationController)) {
            throw new IllegalArgumentException();
        }
        List<V1alpha1NodeGroup> nodeGroups = this.resolveNodeGroupByNamespace(K8sObjectUtil.getNamespace(replicationController));

        return reconcileAffinity(ReplicationControllerUtil.getAffinity(replicationController).orElse(null), nodeGroups);
    }

    public Optional<V1Affinity> reconcileUncontrolledStatefulSetAffinity(V1StatefulSet statefulSet) {
        if (K8sObjectUtil.isControlled(statefulSet)) {
            throw new IllegalArgumentException();
        }
        List<V1alpha1NodeGroup> nodeGroups = this.resolveNodeGroupByNamespace(K8sObjectUtil.getNamespace(statefulSet));

        return reconcileAffinity(StatefulSetUtil.getAffinity(statefulSet).orElse(null), nodeGroups);
    }

    public List<V1Toleration> reconcileUncontrolledCronJobTolerations(V1CronJob cronJob) {
        if (K8sObjectUtil.isControlled(cronJob)) {
            throw new IllegalArgumentException();
        }
        List<V1alpha1NodeGroup> nodeGroups = this.resolveNodeGroupByNamespace(K8sObjectUtil.getNamespace(cronJob));

        return reconcileTolerations(CronJobUtil.getTolerations(cronJob), nodeGroups);
    }

    public List<V1Toleration> reconcileUncontrolledDaemonSetTolerations(V1DaemonSet daemonSet) {
        if (K8sObjectUtil.isControlled(daemonSet)) {
            throw new IllegalArgumentException();
        }
        List<V1alpha1NodeGroup> nodeGroupsContainingNamespace = resolveNodeGroupByNamespace(K8sObjectUtil.getNamespace(daemonSet));
        List<V1alpha1NodeGroup> groupsContainingDaemonSet = this.nodeGroupIndexer.byIndex(
                IndexNames.BY_DAEMON_SET_KEY_TO_NODE_GROUP_OBJECT,
                KeyUtil.buildKey(K8sObjectUtil.getNamespace(daemonSet), K8sObjectUtil.getName(daemonSet)));
        List<V1alpha1NodeGroup> groupsAllowAllDaemonSets = this.nodeGroupIndexer.byIndex(
                IndexNames.BY_ALLOW_ALL_DAEMON_SET_POLICY_TO_NODE_GROUP_OBJECT,
                String.valueOf(Boolean.TRUE));

        List<V1alpha1NodeGroup> nodeGroups = Stream.of(nodeGroupsContainingNamespace.stream(), groupsContainingDaemonSet.stream(), groupsAllowAllDaemonSets.stream())
                .flatMap(stream -> stream)
                .distinct()
                .collect(Collectors.toList());

        return reconcileTolerations(DaemonSetUtil.getTolerations(daemonSet), nodeGroups);
    }

    public List<V1Toleration> reconcileUncontrolledDeploymentTolerations(V1Deployment deployment) {
        if (K8sObjectUtil.isControlled(deployment)) {
            throw new IllegalArgumentException();
        }
        List<V1alpha1NodeGroup> nodeGroups = this.resolveNodeGroupByNamespace(K8sObjectUtil.getNamespace(deployment));

        return reconcileTolerations(DeploymentUtil.getTolerations(deployment), nodeGroups);
    }

    public List<V1Toleration> reconcileUncontrolledJobTolerations(V1Job job) {
        if (K8sObjectUtil.isControlled(job)) {
            throw new IllegalArgumentException();
        }
        List<V1alpha1NodeGroup> nodeGroups = this.resolveNodeGroupByNamespace(K8sObjectUtil.getNamespace(job));

        return reconcileTolerations(JobUtil.getTolerations(job), nodeGroups);
    }

    public List<V1Toleration> reconcileUncontrolledPodTolerations(V1Pod pod) {
        if (K8sObjectUtil.isControlled(pod)) {
            throw new IllegalArgumentException();
        }
        List<V1alpha1NodeGroup> nodeGroups = this.resolveNodeGroupByNamespace(K8sObjectUtil.getNamespace(pod));

        return reconcileTolerations(PodUtil.getTolerations(pod), nodeGroups);
    }

    public List<V1Toleration> reconcileUncontrolledReplicaSetTolerations(V1ReplicaSet replicaSet) {
        if (K8sObjectUtil.isControlled(replicaSet)) {
            throw new IllegalArgumentException();
        }
        List<V1alpha1NodeGroup> nodeGroups = this.resolveNodeGroupByNamespace(K8sObjectUtil.getNamespace(replicaSet));

        return reconcileTolerations(ReplicaSetUtil.getTolerations(replicaSet), nodeGroups);
    }

    public List<V1Toleration> reconcileUncontrolledReplicationControllerTolerations(V1ReplicationController replicationController) {
        if (K8sObjectUtil.isControlled(replicationController)) {
            throw new IllegalArgumentException();
        }
        List<V1alpha1NodeGroup> nodeGroups = this.resolveNodeGroupByNamespace(K8sObjectUtil.getNamespace(replicationController));

        return reconcileTolerations(ReplicationControllerUtil.getTolerations(replicationController), nodeGroups);
    }

    public List<V1Toleration> reconcileUncontrolledStatefulSetTolerations(V1StatefulSet statefulSet) {
        if (K8sObjectUtil.isControlled(statefulSet)) {
            throw new IllegalArgumentException();
        }
        List<V1alpha1NodeGroup> nodeGroups = this.resolveNodeGroupByNamespace(K8sObjectUtil.getNamespace(statefulSet));

        return reconcileTolerations(StatefulSetUtil.getTolerations(statefulSet), nodeGroups);
    }

    public List<V1LocalObjectReference> reconcileUncontrolledCronJobImagePullSecrets(V1CronJob cronJob) {
        if (K8sObjectUtil.isControlled(cronJob)) {
            throw new IllegalArgumentException();
        }
        List<V1Secret> imageNamespaceGroupSecrets = this.resolveProjectImagePullSecretsByNamespace(K8sObjectUtil.getNamespace(cronJob));

        return reconcileImagePullSecrets(CronJobUtil.getImagePullSecrets(cronJob), imageNamespaceGroupSecrets);
    }

    public List<V1LocalObjectReference> reconcileUncontrolledDaemonSetImagePullSecrets(V1DaemonSet daemonSet) {
        if (K8sObjectUtil.isControlled(daemonSet)) {
            throw new IllegalArgumentException();
        }
        List<V1Secret> imageNamespaceGroupSecrets = this.resolveProjectImagePullSecretsByNamespace(K8sObjectUtil.getNamespace(daemonSet));

        return reconcileImagePullSecrets(DaemonSetUtil.getImagePullSecrets(daemonSet), imageNamespaceGroupSecrets);
    }

    public List<V1LocalObjectReference> reconcileUncontrolledDeploymentImagePullSecrets(V1Deployment deployment) {
        if (K8sObjectUtil.isControlled(deployment)) {
            throw new IllegalArgumentException();
        }
        List<V1Secret> imageNamespaceGroupSecrets = this.resolveProjectImagePullSecretsByNamespace(K8sObjectUtil.getNamespace(deployment));

        return reconcileImagePullSecrets(DeploymentUtil.getImagePullSecrets(deployment), imageNamespaceGroupSecrets);
    }

    public List<V1LocalObjectReference> reconcileUncontrolledJobImagePullSecrets(V1Job job) {
        if (K8sObjectUtil.isControlled(job)) {
            throw new IllegalArgumentException();
        }
        List<V1Secret> imageNamespaceGroupSecrets = this.resolveProjectImagePullSecretsByNamespace(K8sObjectUtil.getNamespace(job));

        return reconcileImagePullSecrets(JobUtil.getImagePullSecrets(job), imageNamespaceGroupSecrets);
    }

    public List<V1LocalObjectReference> reconcileUncontrolledPodImagePullSecrets(V1Pod pod) {
        if (K8sObjectUtil.isControlled(pod)) {
            throw new IllegalArgumentException();
        }
        List<V1Secret> imageNamespaceGroupSecrets = this.resolveProjectImagePullSecretsByNamespace(K8sObjectUtil.getNamespace(pod));

        return reconcileImagePullSecrets(PodUtil.getImagePullSecrets(pod), imageNamespaceGroupSecrets);
    }

    public List<V1LocalObjectReference> reconcileUncontrolledReplicaSetImagePullSecrets(V1ReplicaSet replicaSet) {
        if (K8sObjectUtil.isControlled(replicaSet)) {
            throw new IllegalArgumentException();
        }
        List<V1Secret> imageNamespaceGroupSecrets = this.resolveProjectImagePullSecretsByNamespace(K8sObjectUtil.getNamespace(replicaSet));

        return reconcileImagePullSecrets(ReplicaSetUtil.getImagePullSecrets(replicaSet), imageNamespaceGroupSecrets);
    }

    public List<V1LocalObjectReference> reconcileUncontrolledReplicationControllerImagePullSecrets(V1ReplicationController replicationController) {
        if (K8sObjectUtil.isControlled(replicationController)) {
            throw new IllegalArgumentException();
        }
        List<V1Secret> imageNamespaceGroupSecrets = this.resolveProjectImagePullSecretsByNamespace(K8sObjectUtil.getNamespace(replicationController));

        return reconcileImagePullSecrets(ReplicationControllerUtil.getImagePullSecrets(replicationController), imageNamespaceGroupSecrets);
    }

    public List<V1LocalObjectReference> reconcileUncontrolledStatefulSetImagePullSecrets(V1StatefulSet statefulSet) {
        if (K8sObjectUtil.isControlled(statefulSet)) {
            throw new IllegalArgumentException();
        }
        List<V1Secret> projectImagePullSecrets = this.resolveProjectImagePullSecretsByNamespace(K8sObjectUtil.getNamespace(statefulSet));

        return reconcileImagePullSecrets(StatefulSetUtil.getImagePullSecrets(statefulSet), projectImagePullSecrets);
    }

    /**
     * 주어진 namespace에 속한 project에 바인딩되어있는 ImageNamespaceGroup Secret들을 반환함.
     *
     * @param namespace
     * @return
     */
    private List<V1Secret> resolveProjectImagePullSecretsByNamespace(String namespace) {
        Optional<V1alpha1Project> projectOpt = Optional.ofNullable(this.projectIndexer.getByKey(KeyUtil.buildKey(namespace)));
        if (projectOpt.isEmpty()) {
            return new ArrayList<>();
        }
        List<V1alpha1ImageNamespaceGroup> imageNamespaceGroups = resolveImageNamespaceGroupBindingsByProject(projectOpt.get());
        return resolveNamespacedImageNamespaceGroupSecrets(imageNamespaceGroups, namespace);
    }

    /**
     * 주어진 project들에 바인딩되어있는 ImageNamespaceGroup들을 반환함.
     *
     * @param project
     * @return
     */
    private List<V1alpha1ImageNamespaceGroup> resolveImageNamespaceGroupBindingsByProject(V1alpha1Project project) {
        return imageNamespaceGroupBindingIndexer.byIndex(IndexNames.BY_PROJECT_NAME_TO_IMAGE_NAMESPACE_GROUP_BINDING_OBJECT, K8sObjectUtil.getName(project))
                .stream().map(V1alpha1ImageNamespaceGroupBinding::getImageNamespaceGroupRef)
                .filter(Objects::nonNull)
                .map(KeyUtil::buildKey)
                .map(imageNamespaceGroupIndexer::getByKey)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /**
     * 주어진 namespace에 속한 ImageNamespaceGroup들에 바인딩되어있는 Secret들을 반환함.
     *
     * @param imageNamespaceGroups
     * @param namespace
     * @return
     */
    private List<V1Secret> resolveNamespacedImageNamespaceGroupSecrets(List<V1alpha1ImageNamespaceGroup> imageNamespaceGroups, String namespace) {
        return imageNamespaceGroups.stream()
                .map(imageNamespaceGroup -> {
                    Objects.requireNonNull(imageNamespaceGroup.getSecret());
                    Objects.requireNonNull(imageNamespaceGroup.getSecret().getName());
                    return this.secretIndexer.getByKey(KeyUtil.buildKey(namespace, imageNamespaceGroup.getSecret().getName()));
                })
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /**
     * 주어진 namespace에 속한 NodeGroup들을 반환함.
     *
     * @param namespace
     * @return
     */
    private List<V1alpha1NodeGroup> resolveNodeGroupByNamespace(String namespace) {
        Optional<V1alpha1Project> projectOpt = Optional.ofNullable(this.projectIndexer.getByKey(KeyUtil.buildKey(namespace)));
        if (projectOpt.isEmpty()) {
            return new ArrayList<>();
        }
        List<V1alpha1NodeGroupBinding> nodeGroupBindings = resolveNodeGroupBindingsByProjects(projectOpt.get());
        return resolveNodeGroupsByNodeGroupBindings(nodeGroupBindings);
    }

    private List<V1alpha1NodeGroupBinding> resolveNodeGroupBindingsByProjects(V1alpha1Project project) {
        return this.nodeGroupBindingIndexer.byIndex(IndexNames.BY_PROJECT_NAME_TO_NODE_GROUP_BINDING_OBJECT, K8sObjectUtil.getName(project));
    }

    private List<V1alpha1NodeGroup> resolveNodeGroupsByNodeGroupBindings(List<V1alpha1NodeGroupBinding> nodeGroupBindings) {
        return nodeGroupBindings.stream()
                .map(V1alpha1NodeGroupBinding::getNodeGroupRef)
                .filter(Objects::nonNull)
                .map(KeyUtil::buildKey)
                .map(this.nodeGroupIndexer::getByKey)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

}
