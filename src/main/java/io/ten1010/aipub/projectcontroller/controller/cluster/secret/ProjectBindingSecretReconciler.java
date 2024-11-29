package io.ten1010.aipub.projectcontroller.controller.cluster.secret;

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
                    String imageNamespaceGroupKey = KeyUtil.buildKey(request.getName());
                    Optional<V1alpha1ImageNamespaceGroup> imageNamespaceGroupOpt = Optional.ofNullable(this.imageNamespaceGroupIndexer.getByKey(imageNamespaceGroupKey));
                    if (imageNamespaceGroupOpt.isEmpty()) {
                        log.debug("ImageNamespaceGroup [{}] not founded while reconciling", imageNamespaceGroupKey);
                        return new Result(false);
                    }
                    V1alpha1ImageNamespaceGroup imageNamespaceGroup = imageNamespaceGroupOpt.get();
                    Objects.requireNonNull(imageNamespaceGroup.getSecret(), "ImageNamespaceGroup's secret must not be null");
                    String registrySecretKey = KeyUtil.buildKey(this.registrySecretNamespace, imageNamespaceGroup.getSecret().getName());
                    Optional<V1Secret> registrySecretOpt = Optional.ofNullable(secretIndexer.getByKey(registrySecretKey));
                    if (registrySecretOpt.isEmpty()) {
                        log.debug("Registry Secret [{}] not founded while reconciling", registrySecretKey);
                        return new Result(true, Duration.ofSeconds(3));
                    }
                    V1Secret registrySecret = registrySecretOpt.get();
                    if (!ImagePullSecretUtil.hasPullSecretData(registrySecret)) {
                        log.debug("Registry Secret [{}] doesn't have imagePullSecret while reconciling", registrySecretKey);
                        return new Result(true, Duration.ofSeconds(3));
                    }
                    String projectBindingSecretKey = KeyUtil.buildKey(request.getNamespace(), request.getName());
                    Optional<V1Secret> projectBindingSecretOpt = Optional.ofNullable(this.secretIndexer.getByKey(projectBindingSecretKey));
                    if (projectBindingSecretOpt.isEmpty()) {
                        createSecretByRegistrySecret(request.getName(), request.getNamespace(), ImagePullSecretUtil.getPullSecretValue(registrySecret));
                        log.debug("Created Project binding Secret [{}] for Project Namespace[{}] while reconciling", projectBindingSecretKey, request.getNamespace());
                        return new Result(false);
                    }
                    V1Secret projectBindingSecret = projectBindingSecretOpt.get();
                    if (!ImagePullSecretUtil.hasPullSecretData(projectBindingSecret)) {
                        deleteSecret(K8sObjectUtil.getName(projectBindingSecret), K8sObjectUtil.getNamespace(projectBindingSecret));
                        log.debug("Deleted Project binding Secret [{}] which doesn't have imagePullSecret while reconciling", projectBindingSecretKey);
                        createSecretByRegistrySecret(request.getName(), request.getNamespace(), ImagePullSecretUtil.getPullSecretValue(registrySecret));
                        log.debug("Created Project binding Secret [{}] for Project Namespace[{}] while reconciling", projectBindingSecretKey, request.getNamespace());
                        return new Result(false);
                    }
                    return new Result(false);
                },
                request);
    }

    private void createSecretByRegistrySecret(String name, String namespace, String secretValue) throws ApiException {
        V1Secret secret = new V1Secret();
        V1ObjectMeta objectMeta = new V1ObjectMeta();
        objectMeta.setName(name);
        objectMeta.setNamespace(namespace);
        secret.setMetadata(objectMeta);
        secret.setKind("kubernetes.io/dockerconfigjson");
        ImagePullSecretUtil.applyNewSecretValue(secret, secretValue);
        this.coreV1Api.createNamespacedSecret(namespace, secret)
                .execute();
    }

    private void deleteSecret(String name, String namespace) throws ApiException {
        this.coreV1Api.deleteNamespacedSecret(name, namespace)
                .execute();
    }

}
