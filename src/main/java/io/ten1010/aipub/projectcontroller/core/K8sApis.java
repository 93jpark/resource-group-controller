package io.ten1010.aipub.projectcontroller.core;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.RbacAuthorizationV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import io.ten1010.aipub.projectcontroller.model.*;
import lombok.Getter;

@Getter
public class K8sApis {

    private ApiClient apiClient;
    private CoreV1Api coreV1Api;
    private AppsV1Api appsV1Api;
    private BatchV1Api batchV1Api;
    private RbacAuthorizationV1Api rbacAuthorizationV1Api;

    private GenericKubernetesApi<V1alpha1Project, V1alpha1ProjectList> projectApi;
    private GenericKubernetesApi<V1alpha1NodeGroup, V1alpha1NodeGroupList> nodeGroupApi;
    private GenericKubernetesApi<V1alpha1NodeGroupBinding, V1alpha1NodeGroupBindingList> nodeGroupBindingApi;
    private GenericKubernetesApi<V1alpha1ImageNamespaceGroup, V1alpha1ImageNamespaceGroupList> imageNamespaceGroupApi;
    private GenericKubernetesApi<V1alpha1ImageNamespaceGroupBinding, V1alpha1ImageNamespaceGroupBindingList> imageNamespaceGroupBindingApi;

    private GenericKubernetesApi<V1CronJob, V1CronJobList> cronJobApi;
    private GenericKubernetesApi<V1DaemonSet, V1DaemonSetList> daemonSetApi;
    private GenericKubernetesApi<V1Deployment, V1DeploymentList> deploymentApi;
    private GenericKubernetesApi<V1Job, V1JobList> jobApi;
    private GenericKubernetesApi<V1Pod, V1PodList> podApi;
    private GenericKubernetesApi<V1ReplicaSet, V1ReplicaSetList> replicaSetApi;
    private GenericKubernetesApi<V1ReplicationController, V1ReplicationControllerList> replicationControllerApi;
    private GenericKubernetesApi<V1StatefulSet, V1StatefulSetList> statefulSetApi;

    private GenericKubernetesApi<V1Node, V1NodeList> nodeApi;
    private GenericKubernetesApi<V1Role, V1RoleList> roleApi;
    private GenericKubernetesApi<V1RoleBinding, V1RoleBindingList> roleBindingApi;
    private GenericKubernetesApi<V1ClusterRole, V1ClusterRoleList> clusterRoleApi;
    private GenericKubernetesApi<V1ClusterRoleBinding, V1ClusterRoleBindingList> clusterRoleBindingApi;
    private GenericKubernetesApi<V1Namespace, V1NamespaceList> namespaceApi;
    private GenericKubernetesApi<V1Secret, V1SecretList> secretApi;

    public K8sApis(ApiClient apiClient) {
        this.apiClient = apiClient;
        this.coreV1Api = new CoreV1Api(apiClient);
        this.appsV1Api = new AppsV1Api(apiClient);
        this.batchV1Api = new BatchV1Api(apiClient);
        this.rbacAuthorizationV1Api = new RbacAuthorizationV1Api(apiClient);
        this.projectApi = new GenericKubernetesApi<>(
                V1alpha1Project.class,
                V1alpha1ProjectList.class,
                "project.aipub.ten1010.io",
                "v1alpha1",
                "projects",
                apiClient);
        this.nodeGroupApi = new GenericKubernetesApi<>(
                V1alpha1NodeGroup.class,
                V1alpha1NodeGroupList.class,
                "project.aipub.ten1010.io",
                "v1alpha1",
                "nodegroups",
                apiClient);
        this.nodeGroupBindingApi = new GenericKubernetesApi<>(
                V1alpha1NodeGroupBinding.class,
                V1alpha1NodeGroupBindingList.class,
                "project.aipub.ten1010.io",
                "v1alpha1",
                "nodegroupbindings",
                apiClient);
        this.imageNamespaceGroupApi = new GenericKubernetesApi<>(
                V1alpha1ImageNamespaceGroup.class,
                V1alpha1ImageNamespaceGroupList.class,
                "project.aipub.ten1010.io",
                "v1alpha1",
                "imagenamespacegroups",
                apiClient);
        this.imageNamespaceGroupBindingApi = new GenericKubernetesApi<>(
                V1alpha1ImageNamespaceGroupBinding.class,
                V1alpha1ImageNamespaceGroupBindingList.class,
                "project.aipub.ten1010.io",
                "v1alpha1",
                "imagenamespacegroupbindings",
                apiClient);
        this.cronJobApi = new GenericKubernetesApi<>(
                V1CronJob.class,
                V1CronJobList.class,
                "batch",
                "v1",
                "cronjobs",
                apiClient);
        this.daemonSetApi = new GenericKubernetesApi<>(
                V1DaemonSet.class,
                V1DaemonSetList.class,
                "apps",
                "v1",
                "daemonsets",
                apiClient);
        this.deploymentApi = new GenericKubernetesApi<>(
                V1Deployment.class,
                V1DeploymentList.class,
                "apps",
                "v1",
                "deployments",
                apiClient);
        this.jobApi = new GenericKubernetesApi<>(
                V1Job.class,
                V1JobList.class,
                "batch",
                "v1",
                "jobs",
                apiClient);
        this.podApi = new GenericKubernetesApi<>(
                V1Pod.class,
                V1PodList.class,
                "",
                "v1",
                "pods",
                apiClient);
        this.replicaSetApi = new GenericKubernetesApi<>(
                V1ReplicaSet.class,
                V1ReplicaSetList.class,
                "apps",
                "v1",
                "replicasets",
                apiClient);
        this.replicationControllerApi = new GenericKubernetesApi<>(
                V1ReplicationController.class,
                V1ReplicationControllerList.class,
                "",
                "v1",
                "replicationcontrollers",
                apiClient);
        this.statefulSetApi = new GenericKubernetesApi<>(
                V1StatefulSet.class,
                V1StatefulSetList.class,
                "apps",
                "v1",
                "statefulsets",
                apiClient);
        this.nodeApi = new GenericKubernetesApi<>(
                V1Node.class,
                V1NodeList.class,
                "",
                "v1",
                "nodes",
                apiClient);
        this.roleApi = new GenericKubernetesApi<>(
                V1Role.class,
                V1RoleList.class,
                "rbac.authorization.k8s.io",
                "v1",
                "roles",
                apiClient);
        this.roleBindingApi = new GenericKubernetesApi<>(
                V1RoleBinding.class,
                V1RoleBindingList.class,
                "rbac.authorization.k8s.io",
                "v1",
                "rolebindings",
                apiClient);
        this.clusterRoleApi = new GenericKubernetesApi<>(
                V1ClusterRole.class,
                V1ClusterRoleList.class,
                "rbac.authorization.k8s.io",
                "v1",
                "clusterroles",
                apiClient);
        this.clusterRoleBindingApi = new GenericKubernetesApi<>(
                V1ClusterRoleBinding.class,
                V1ClusterRoleBindingList.class,
                "rbac.authorization.k8s.io",
                "v1",
                "clusterrolebindings",
                apiClient);
        this.namespaceApi = new GenericKubernetesApi<>(
                V1Namespace.class,
                V1NamespaceList.class,
                "",
                "v1",
                "namespaces",
                apiClient);
        this.secretApi = new GenericKubernetesApi<>(
                V1Secret.class,
                V1SecretList.class,
                "",
                "v1",
                "secrets");
    }

}
