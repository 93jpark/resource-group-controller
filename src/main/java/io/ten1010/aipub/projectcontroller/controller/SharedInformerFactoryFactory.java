package io.ten1010.aipub.projectcontroller.controller;

import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.models.*;
import io.ten1010.aipub.projectcontroller.core.*;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroup;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroupBinding;
import io.ten1010.aipub.projectcontroller.model.V1alpha1NodeGroup;
import io.ten1010.aipub.projectcontroller.model.V1alpha1NodeGroupBinding;
import io.ten1010.aipub.projectcontroller.model.V1alpha1Project;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SharedInformerFactoryFactory {

    public static long RESYNC_PERIOD_IN_MILLIS = 30000;

    private static Map<String, Function<V1alpha1NodeGroup, List<String>>> byNodeNameToNodeGroupObject() {
        return Map.of(IndexNames.BY_NODE_NAME_TO_NODE_GROUP_OBJECT, NodeGroupUtil::getNodes);
    }

    private static Map<String, Function<V1alpha1NodeGroup, List<String>>> byNamespaceNameToNodeGroupObject() {
        return Map.of(IndexNames.BY_NAMESPACE_NAME_TO_NODE_GROUP_OBJECT, NodeGroupUtil::getNamespaces);
    }

    private static Map<String, Function<V1alpha1NodeGroup, List<String>>> byDaemonSetKeyToNodeGroupObject() {
        return Map.of(IndexNames.BY_DAEMON_SET_KEY_TO_NODE_GROUP_OBJECT,
                object -> NodeGroupUtil.getDaemonSets(object).stream()
                        .map(KeyUtil::getKey)
                        .collect(Collectors.toList()));
    }

    private static Map<String, Function<V1alpha1NodeGroup, List<String>>> byAllowAllDaemonSetsPolicyToNodeGroupObject() {
        return Map.of(IndexNames.BY_ALLOW_ALL_DAEMON_SET_POLICY_TO_NODE_GROUP_OBJECT,
                object -> List.of(String.valueOf(NodeGroupUtil.isAllowAllDaemonSets(object))));
    }

    private static Map<String, Function<V1alpha1Project, List<String>>> byNamespaceNameToProjectObject() {
        return Map.of(IndexNames.BY_NAMESPACE_NAME_TO_PROJECT_OBJECT,
                object -> {
                    String namespace = object.getNamespace();
                    return namespace != null ? List.of(namespace) : List.of();
                });
    }

    private static Map<String, Function<V1alpha1NodeGroupBinding, List<String>>> byNodeGroupNameToNodeGroupBindingObject() {
        return Map.of(IndexNames.BY_NODE_GROUP_NAME_TO_NODE_GROUP_BINDING_OBJECT,
                object -> List.of(K8sObjectUtil.getName(object)));
    }

    private static Map<String, Function<V1alpha1ImageNamespaceGroupBinding, List<String>>> byProjectNameToImageNamespaceGroupBindingObject() {
        return Map.of(IndexNames.BY_PROJECT_NAME_TO_IMAGE_NAMESPACE_GROUP_BINDING_OBJECT,
                V1alpha1ImageNamespaceGroupBinding::getProjects);
    }

    private static Map<String, Function<V1CronJob, List<String>>> byNamespaceNameToCronJobObject() {
        return Map.of(IndexNames.BY_NAMESPACE_NAME_TO_CRON_JOB_OBJECT,
                object -> List.of(K8sObjectUtil.getNamespace(object)));
    }

    private static Map<String, Function<V1DaemonSet, List<String>>> byNamespaceNameToDaemonSetObject() {
        return Map.of(IndexNames.BY_NAMESPACE_NAME_TO_DAEMON_SET_OBJECT,
                object -> List.of(K8sObjectUtil.getNamespace(object)));
    }

    private static Map<String, Function<V1DaemonSet, List<String>>> byDaemonSetNameToDaemonSetObject() {
        return Map.of(IndexNames.BY_DAEMON_SET_NAME_TO_DAEMON_SET_OBJECT,
                object -> List.of(K8sObjectUtil.getName(object)));
    }

    private static Map<String, Function<V1Deployment, List<String>>> byNamespaceNameToDeploymentObject() {
        return Map.of(IndexNames.BY_NAMESPACE_NAME_TO_DEPLOYMENT_OBJECT,
                object -> List.of(K8sObjectUtil.getNamespace(object)));
    }

    private static Map<String, Function<V1Job, List<String>>> byNamespaceNameToJobObject() {
        return Map.of(IndexNames.BY_NAMESPACE_NAME_TO_JOB_OBJECT,
                object -> List.of(K8sObjectUtil.getNamespace(object)));
    }

    private static Map<String, Function<V1Pod, List<String>>> byNamespaceNameToPodObject() {
        return Map.of(IndexNames.BY_NAMESPACE_NAME_TO_POD_OBJECT,
                object -> List.of(K8sObjectUtil.getNamespace(object)));
    }

    private static Map<String, Function<V1ReplicaSet, List<String>>> byNamespaceNameToReplicaSetObject() {
        return Map.of(IndexNames.BY_NAMESPACE_NAME_TO_REPLICA_SET_OBJECT,
                object -> List.of(K8sObjectUtil.getNamespace(object)));
    }

    private static Map<String, Function<V1ReplicationController, List<String>>> byNamespaceNameToReplicationControllerObject() {
        return Map.of(IndexNames.BY_NAMESPACE_NAME_TO_REPLICATION_CONTROLLER_OBJECT,
                object -> List.of(K8sObjectUtil.getNamespace(object)));
    }

    private static Map<String, Function<V1StatefulSet, List<String>>> byNamespaceNameToStatefulSetObject() {
        return Map.of(IndexNames.BY_NAMESPACE_NAME_TO_STATEFUL_SET_OBJECT,
                object -> List.of(K8sObjectUtil.getNamespace(object)));
    }

    private K8sApis k8sApis;

    public SharedInformerFactoryFactory(K8sApis k8sApis) {
        this.k8sApis = k8sApis;
    }

    public SharedInformerFactory create() {
        SharedInformerFactory informerFactory = new SharedInformerFactory(this.k8sApis.getApiClient());
        SharedIndexInformer<V1alpha1Project> projectInformer = informerFactory.sharedIndexInformerFor(
                this.k8sApis.getProjectApi(),
                V1alpha1Project.class,
                RESYNC_PERIOD_IN_MILLIS);
        projectInformer.addIndexers(byNamespaceNameToProjectObject());
        SharedIndexInformer<V1alpha1NodeGroup> groupInformer = informerFactory.sharedIndexInformerFor(
                this.k8sApis.getNodeGroupApi(),
                V1alpha1NodeGroup.class,
                RESYNC_PERIOD_IN_MILLIS);
        groupInformer.addIndexers(byNodeNameToNodeGroupObject());
        groupInformer.addIndexers(byNamespaceNameToNodeGroupObject());
        groupInformer.addIndexers(byDaemonSetKeyToNodeGroupObject());
        groupInformer.addIndexers(byAllowAllDaemonSetsPolicyToNodeGroupObject());
        SharedIndexInformer<V1alpha1NodeGroupBinding> nodeGroupBindingInformer = informerFactory.sharedIndexInformerFor(
                this.k8sApis.getNodeGroupBindingApi(),
                V1alpha1NodeGroupBinding.class,
                RESYNC_PERIOD_IN_MILLIS);
        nodeGroupBindingInformer.addIndexers(byNodeGroupNameToNodeGroupBindingObject());
        SharedIndexInformer<V1alpha1ImageNamespaceGroup> imageNamespaceGroupInformer = informerFactory.sharedIndexInformerFor(
                this.k8sApis.getImageNamespaceGroupApi(),
                V1alpha1ImageNamespaceGroup.class,
                RESYNC_PERIOD_IN_MILLIS);
        SharedIndexInformer<V1alpha1ImageNamespaceGroupBinding> imageNamespaceGroupBindingInformer = informerFactory.sharedIndexInformerFor(
                this.k8sApis.getImageNamespaceGroupBindingApi(),
                V1alpha1ImageNamespaceGroupBinding.class,
                RESYNC_PERIOD_IN_MILLIS);
        imageNamespaceGroupBindingInformer.addIndexers(byProjectNameToImageNamespaceGroupBindingObject());

        SharedIndexInformer<V1CronJob> cronJobInformer = informerFactory.sharedIndexInformerFor(
                this.k8sApis.getCronJobApi(),
                V1CronJob.class,
                RESYNC_PERIOD_IN_MILLIS);
        cronJobInformer.addIndexers(byNamespaceNameToCronJobObject());
        SharedIndexInformer<V1DaemonSet> daemonSetInformer = informerFactory.sharedIndexInformerFor(
                this.k8sApis.getDaemonSetApi(),
                V1DaemonSet.class,
                RESYNC_PERIOD_IN_MILLIS);
        daemonSetInformer.addIndexers(byNamespaceNameToDaemonSetObject());
        daemonSetInformer.addIndexers(byDaemonSetNameToDaemonSetObject());
        SharedIndexInformer<V1Deployment> deploymentInformer = informerFactory.sharedIndexInformerFor(
                this.k8sApis.getDeploymentApi(),
                V1Deployment.class,
                RESYNC_PERIOD_IN_MILLIS);
        deploymentInformer.addIndexers(byNamespaceNameToDeploymentObject());
        SharedIndexInformer<V1Job> jobInformer = informerFactory.sharedIndexInformerFor(
                this.k8sApis.getJobApi(),
                V1Job.class,
                RESYNC_PERIOD_IN_MILLIS);
        jobInformer.addIndexers(byNamespaceNameToJobObject());
        SharedIndexInformer<V1Pod> podInformer = informerFactory.sharedIndexInformerFor(
                this.k8sApis.getPodApi(),
                V1Pod.class,
                RESYNC_PERIOD_IN_MILLIS);
        podInformer.addIndexers(byNamespaceNameToPodObject());
        SharedIndexInformer<V1ReplicaSet> replicaSetInformer = informerFactory.sharedIndexInformerFor(
                this.k8sApis.getReplicaSetApi(),
                V1ReplicaSet.class,
                RESYNC_PERIOD_IN_MILLIS);
        replicaSetInformer.addIndexers(byNamespaceNameToReplicaSetObject());
        SharedIndexInformer<V1ReplicationController> replicationControllerInformer = informerFactory.sharedIndexInformerFor(
                this.k8sApis.getReplicationControllerApi(),
                V1ReplicationController.class,
                RESYNC_PERIOD_IN_MILLIS);
        replicationControllerInformer.addIndexers(byNamespaceNameToReplicationControllerObject());
        SharedIndexInformer<V1StatefulSet> statefulSetInformer = informerFactory.sharedIndexInformerFor(
                this.k8sApis.getStatefulSetApi(),
                V1StatefulSet.class,
                RESYNC_PERIOD_IN_MILLIS);
        statefulSetInformer.addIndexers(byNamespaceNameToStatefulSetObject());

        SharedIndexInformer<V1Node> nodeInformer = informerFactory.sharedIndexInformerFor(
                this.k8sApis.getNodeApi(),
                V1Node.class,
                RESYNC_PERIOD_IN_MILLIS);
        SharedIndexInformer<V1Role> roleInformer = informerFactory.sharedIndexInformerFor(
                this.k8sApis.getRoleApi(),
                V1Role.class,
                RESYNC_PERIOD_IN_MILLIS);
        SharedIndexInformer<V1RoleBinding> roleBindingInformer = informerFactory.sharedIndexInformerFor(
                this.k8sApis.getRoleBindingApi(),
                V1RoleBinding.class,
                RESYNC_PERIOD_IN_MILLIS);
        SharedIndexInformer<V1ClusterRole> clusterRoleInformer = informerFactory.sharedIndexInformerFor(
                this.k8sApis.getClusterRoleApi(),
                V1ClusterRole.class,
                RESYNC_PERIOD_IN_MILLIS);
        SharedIndexInformer<V1ClusterRoleBinding> clusterRoleBindingInformer = informerFactory.sharedIndexInformerFor(
                this.k8sApis.getClusterRoleBindingApi(),
                V1ClusterRoleBinding.class,
                RESYNC_PERIOD_IN_MILLIS);
        SharedIndexInformer<V1Secret> secretInformer = informerFactory.sharedIndexInformerFor(
                this.k8sApis.getSecretApi(),
                V1Secret.class,
                RESYNC_PERIOD_IN_MILLIS);
        SharedIndexInformer<V1Namespace> namespaceInformer = informerFactory.sharedIndexInformerFor(
                this.k8sApis.getNamespaceApi(),
                V1Namespace.class,
                RESYNC_PERIOD_IN_MILLIS);
        return informerFactory;
    }

}
