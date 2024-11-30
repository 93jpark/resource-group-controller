package io.ten1010.aipub.projectcontroller.controller.cluster.projectbindingsecret;

import io.kubernetes.client.extended.controller.reconciler.Reconciler;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Secret;
import io.ten1010.aipub.projectcontroller.controller.KubernetesApiReconcileExceptionHandlingTemplate;
import io.ten1010.aipub.projectcontroller.core.ImagePullSecretUtil;
import io.ten1010.aipub.projectcontroller.core.K8sObjectUtil;
import io.ten1010.aipub.projectcontroller.core.KeyUtil;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroup;
import io.ten1010.aipub.projectcontroller.model.V1alpha1Project;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class ProjectBindingSecretReconciler implements Reconciler {

    public static final Duration API_CONFLICT_REQUEUE_DURATION = Duration.ofSeconds(5);
    public static final Duration API_FAIL_REQUEUE_DURATION = Duration.ofSeconds(60);

    private KubernetesApiReconcileExceptionHandlingTemplate template;
    private Indexer<V1alpha1ImageNamespaceGroup> imageNamespaceGroupIndexer;
    private Indexer<V1Secret> secretIndexer;
    private CoreV1Api coreV1Api;
    private String registrySecretNamespace;

    public ProjectBindingSecretReconciler(
            Indexer<V1alpha1ImageNamespaceGroup> imageNamespaceGroupIndexer,
            Indexer<V1Secret> secretIndexer,
            CoreV1Api coreV1Api,
            String registrySecretNamespace) {
        this.template = new KubernetesApiReconcileExceptionHandlingTemplate(API_CONFLICT_REQUEUE_DURATION, API_FAIL_REQUEUE_DURATION);
        this.imageNamespaceGroupIndexer = imageNamespaceGroupIndexer;
        this.secretIndexer = secretIndexer;
        this.coreV1Api = coreV1Api;
        this.registrySecretNamespace = registrySecretNamespace;
    }

    @Override
    public Result reconcile(Request request) {
        return this.template.execute(
                () -> {
                    log.info("Reconciling ProjectBindingSecret [{}]", request.getName());
                    String projectBindingSecretKey = KeyUtil.buildKey(request.getNamespace(), request.getName());
                    Optional<V1Secret> projectBindingSecretOpt = Optional.ofNullable(this.secretIndexer.getByKey(projectBindingSecretKey));

                    String imageNamespaceGroupKey = KeyUtil.buildKey(request.getName());
                    Optional<V1alpha1ImageNamespaceGroup> imageNamespaceGroupOpt = Optional.ofNullable(this.imageNamespaceGroupIndexer.getByKey(imageNamespaceGroupKey));
                    if (imageNamespaceGroupOpt.isEmpty()) {
                        log.info("ImageNamespaceGroup [{}] not founded while reconciling", imageNamespaceGroupKey);
                        if (projectBindingSecretOpt.isPresent()) {
                            deleteSecret(request.getNamespace(), request.getName());
                            log.info("Deleted Secret [{}] because ImageNamespaceGroup not found", projectBindingSecretKey);
                        }
                        return new Result(false);
                    }
                    V1alpha1ImageNamespaceGroup imageNamespaceGroup = imageNamespaceGroupOpt.get();

                    String registrySecretKey = KeyUtil.buildKey(this.registrySecretNamespace, request.getName());
                    Optional<V1Secret> registrySecretOpt = Optional.ofNullable(this.secretIndexer.getByKey(registrySecretKey));
                    if (registrySecretOpt.isEmpty()) {
                        log.info("Registry Secret [{}] not founded while reconciling", registrySecretKey);
                        if (projectBindingSecretOpt.isPresent()) {
                            deleteSecret(request.getNamespace(), request.getName());
                            log.info("Deleted Secret [{}] because Registry Secret not found", projectBindingSecretKey);
                        }
                        return new Result(false);
                    }
                    V1Secret registrySecret = registrySecretOpt.get();

                    if (!ImagePullSecretUtil.hasPullSecretData(registrySecret)) {
                        if (projectBindingSecretOpt.isPresent()) {
                            deleteSecret(request.getNamespace(), request.getName());
                            log.info("Deleted Secret [{}] which has no image pull secret from registry key", projectBindingSecretKey);
                        }
                        throw new RuntimeException("Registry Secret must have data");
                    }

                    if (projectBindingSecretOpt.isEmpty()) {
                        createSecretByRegistrySecret(request.getNamespace(), request.getName(), registrySecret);
                        log.info("Created Secret [{}] for Project Namespace[{}] while reconciling", projectBindingSecretKey, request.getNamespace());
                    }

                    V1Secret projectBindingSecret = projectBindingSecretOpt.get();
                    if (!ImagePullSecretUtil.hasPullSecretData(projectBindingSecret)) {
                        deleteSecret(K8sObjectUtil.getNamespace(projectBindingSecret), K8sObjectUtil.getName(projectBindingSecret));
                        log.info("Deleted Project binding Secret [{}] which doesn't have imagePullSecret data while reconciling", projectBindingSecretKey);
                        createSecretByRegistrySecret(request.getNamespace(), request.getName(), registrySecret);
                        log.info("Created Project binding Secret [{}] for Project Namespace[{}] while reconciling", projectBindingSecretKey, request.getNamespace());
                        return new Result(false);
                    }
                    return new Result(false);
                },
                request);
    }

    private void createSecretByRegistrySecret(String namespace, String name, V1Secret registrySecret) throws ApiException {
        V1Secret secret = new V1Secret();
        V1ObjectMeta objectMeta = new V1ObjectMeta();
        objectMeta.setName(name);
        objectMeta.setNamespace(namespace);
        secret.setMetadata(objectMeta);
        secret.setKind("kubernetes.io/dockerconfigjson");
        secret.setData(registrySecret.getData());

        this.coreV1Api.createNamespacedSecret(namespace, secret)
                .execute();
    }

    private void deleteSecret(String name, String namespace) throws ApiException {
        this.coreV1Api.deleteNamespacedSecret(name, namespace)
                .execute();
    }

}
