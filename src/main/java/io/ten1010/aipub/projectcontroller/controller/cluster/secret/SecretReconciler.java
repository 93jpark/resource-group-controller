package io.ten1010.aipub.projectcontroller.controller.cluster.secret;

import io.kubernetes.client.extended.controller.reconciler.Reconciler;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1ObjectMetaBuilder;
import io.kubernetes.client.openapi.models.V1OwnerReference;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1SecretBuilder;
import io.ten1010.aipub.projectcontroller.controller.KubernetesApiReconcileExceptionHandlingTemplate;
import io.ten1010.aipub.projectcontroller.controller.cluster.ProjectImageNamespaceGroup;
import io.ten1010.aipub.projectcontroller.controller.cluster.RegistryRobot;
import io.ten1010.aipub.projectcontroller.controller.cluster.RegistryRobotService;
import io.ten1010.aipub.projectcontroller.core.ImagePullSecretUtil;
import io.ten1010.aipub.projectcontroller.core.K8sObjectUtil;
import io.ten1010.aipub.projectcontroller.core.KeyUtil;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.Optional;

@Slf4j
public class SecretReconciler implements Reconciler {

    public static final Duration API_CONFLICT_REQUEUE_DURATION = Duration.ofSeconds(5);
    public static final Duration API_FAIL_REQUEUE_DURATION = Duration.ofSeconds(60);

    private KubernetesApiReconcileExceptionHandlingTemplate template;
    private Indexer<V1alpha1ImageNamespaceGroup> imageNamespaceGroupIndexer;
    private Indexer<V1Secret> secretIndexer;
    private CoreV1Api coreV1Api;
    private RegistryRobotService registryRobotService;

    public SecretReconciler(
            Indexer<V1alpha1ImageNamespaceGroup> imageNamespaceGroupIndexer,
            Indexer<V1Secret> secretIndexer,
            CoreV1Api coreV1Api,
            RegistryRobotService registryRobotService) {
        this.template = new KubernetesApiReconcileExceptionHandlingTemplate(API_CONFLICT_REQUEUE_DURATION, API_FAIL_REQUEUE_DURATION);
        this.imageNamespaceGroupIndexer = imageNamespaceGroupIndexer;
        this.secretIndexer = secretIndexer;
        this.coreV1Api = coreV1Api;
        this.registryRobotService = registryRobotService;
    }

    @Override
    public Result reconcile(Request request) {
        return this.template.execute(
                () -> {
                    String imageNamespaceGroupKey = KeyUtil.buildKey(request.getNamespace(), request.getName());
                    Optional<V1alpha1ImageNamespaceGroup> imageNamespaceGroupOpt = Optional.ofNullable(imageNamespaceGroupIndexer.getByKey(imageNamespaceGroupKey));
                    if (imageNamespaceGroupOpt.isEmpty()) {
                        return new Result(false);
                    }
                    V1alpha1ImageNamespaceGroup imageNamespaceGroup = imageNamespaceGroupOpt.get();
                    Assert.notNull(imageNamespaceGroup.getMetadata(), "metadata must not be null");
                    Assert.notNull(imageNamespaceGroup.getMetadata().getName(), "name must not be null");
                    ProjectImageNamespaceGroup projectImageNamespaceGroup = ProjectImageNamespaceGroup.from(imageNamespaceGroup);
                    Optional<RegistryRobot> registryRobotOpt = registryRobotService.getRegistryRobot(projectImageNamespaceGroup.getRegistryRobotName());
                    if (registryRobotOpt.isEmpty()) {
                        log.debug("RegistryRobot [{}] not founded while reconciling", projectImageNamespaceGroup.getRegistryRobotName());
                        return new Result(false);
                    }
                    RegistryRobot registryRobot = registryRobotOpt.get();
                    projectImageNamespaceGroup.setSecretValue(registryRobot.getSecret());
                    String secretKey = KeyUtil.buildKey(request.getNamespace(), request.getName());
                    Optional<V1Secret> secretOpt = Optional.ofNullable(secretIndexer.getByKey(secretKey));
                    if (secretOpt.isEmpty()) {
                        this.createNamespacedSecret(projectImageNamespaceGroup);
                        log.debug("Secret [{}] created for ImageNamespaceGroup[{}] while reconciling", secretKey, imageNamespaceGroupKey);
                        return new Result(false);
                    }
                    V1Secret secret = secretOpt.get();
                    if (!ImagePullSecretUtil.hasPullSecretData(secret, projectImageNamespaceGroup.getSecretValue())) {
                        this.updateNamespacedSecretValueAndOwnerRef(secret, ImagePullSecretUtil.castToBytes(projectImageNamespaceGroup.getSecretValue()), imageNamespaceGroup);
                        log.debug("Secret [{}] updated while reconciling", secretKey);
                        return new Result(false);
                    }
                    return new Result(false);
                },
                request);
    }

    private void createNamespacedSecret(ProjectImageNamespaceGroup projectImageNamespaceGroup) {
        V1Secret secret = new V1Secret();
        V1ObjectMeta objectMeta = new V1ObjectMeta();
        objectMeta.setName(projectImageNamespaceGroup.getName());
        objectMeta.setNamespace(projectImageNamespaceGroup.getNamespace());
        secret.setMetadata(objectMeta);
        ImagePullSecretUtil.applyNewSecretValue(secret, projectImageNamespaceGroup.getSecretValue().getBytes());
        this.coreV1Api.createNamespacedSecret(projectImageNamespaceGroup.getNamespace(), secret);
    }

    private void updateNamespacedSecretValueAndOwnerRef(V1Secret target, byte[] secretValue, V1alpha1ImageNamespaceGroup imageNamespaceGroup) throws ApiException {
        Assert.notNull(imageNamespaceGroup.getMetadata(), "metadata must not be null");
        Assert.notNull(imageNamespaceGroup.getMetadata().getName(), "name must not be null");
        V1OwnerReference ownerReference = new V1OwnerReference();
        ownerReference.setApiVersion("v1alpha1");
        ownerReference.setKind("ImageNamespaceGroup");
        ownerReference.setName(imageNamespaceGroup.getMetadata().getName());
        ownerReference.setUid(imageNamespaceGroup.getMetadata().getUid());
        ownerReference.setController(false);
        ownerReference.setBlockOwnerDeletion(true);
        V1Secret updated = new V1SecretBuilder(target)
                .withData(ImagePullSecretUtil.applyNewSecretValue(target, secretValue))
                .withMetadata(new V1ObjectMetaBuilder(target.getMetadata())
                        .withOwnerReferences(ownerReference)
                        .build())
                .build();
        coreV1Api.replaceNamespacedSecret(
                K8sObjectUtil.getName(target),
                K8sObjectUtil.getNamespace(target),
                updated
        );
    }

}
