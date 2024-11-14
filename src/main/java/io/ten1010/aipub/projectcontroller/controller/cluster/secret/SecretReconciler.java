package io.ten1010.aipub.projectcontroller.controller.cluster.secret;

import io.kubernetes.client.extended.controller.reconciler.Reconciler;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ObjectMetaBuilder;
import io.kubernetes.client.openapi.models.V1OwnerReference;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1SecretBuilder;
import io.ten1010.aipub.projectcontroller.controller.KubernetesApiReconcileExceptionHandlingTemplate;
import io.ten1010.aipub.projectcontroller.controller.cluster.ProjectImageNamespaceGroup;
import io.ten1010.aipub.projectcontroller.controller.cluster.RegistryRobotAccount;
import io.ten1010.aipub.projectcontroller.controller.cluster.RobotAccountService;
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
    private RobotAccountService robotAccountService;

    public SecretReconciler(
            Indexer<V1alpha1ImageNamespaceGroup> imageNamespaceGroupIndexer,
            Indexer<V1Secret> secretIndexer,
            CoreV1Api coreV1Api,
            RobotAccountService robotAccountService) {
        this.template = new KubernetesApiReconcileExceptionHandlingTemplate(API_CONFLICT_REQUEUE_DURATION, API_FAIL_REQUEUE_DURATION);
        this.imageNamespaceGroupIndexer = imageNamespaceGroupIndexer;
        this.secretIndexer = secretIndexer;
        this.coreV1Api = coreV1Api;
        this.robotAccountService = robotAccountService;
    }

    @Override
    public Result reconcile(Request request) {
        return this.template.execute(
                () -> {
                    String secretKey = KeyUtil.buildKey(request.getNamespace(), request.getName());
                    Optional<V1Secret> secretOpt = Optional.ofNullable(secretIndexer.getByKey(secretKey));
                    if (secretOpt.isEmpty()) {
                        log.debug("Secret [{}] not founded while reconciling", secretKey);
                        return new Result(false);
                    }
                    V1Secret secret = secretOpt.get();
                    String imageNamespaceGroupKey = KeyUtil.buildKey(request.getNamespace(), request.getName());
                    Optional<V1alpha1ImageNamespaceGroup> imageNamespaceGroupOpt = Optional.ofNullable(this.imageNamespaceGroupIndexer.getByKey(imageNamespaceGroupKey));
                    if (imageNamespaceGroupOpt.isEmpty()) {
                        return new Result(false);
                    }
                    V1alpha1ImageNamespaceGroup imageNamespaceGroup = imageNamespaceGroupOpt.get();
                    Assert.notNull(imageNamespaceGroup.getMetadata(), "metadata must not be null");
                    Assert.notNull(imageNamespaceGroup.getMetadata().getName(), "name must not be null");
                    ProjectImageNamespaceGroup projectImageNamespaceGroup = ProjectImageNamespaceGroup.from(imageNamespaceGroup);
                    Optional<RegistryRobotAccount> robotAccountOpt = robotAccountService.getRobotAccount(imageNamespaceGroup.getMetadata().getName());
                    if (robotAccountOpt.isEmpty()) {
                        log.debug("RobotAccount [{}] not founded while reconciling", imageNamespaceGroup.getMetadata().getName());
                        return new Result(false);
                    }
                    RegistryRobotAccount robotAccount = robotAccountOpt.get();
                    projectImageNamespaceGroup.setSecretValue(robotAccount.getSecret());

                    if (!ImagePullSecretUtil.hasPullSecretData(secret, projectImageNamespaceGroup.getSecretValue())) {
                        this.updateNamespacedSecretValueAndOwnerRef(secret, ImagePullSecretUtil.castToBytes(projectImageNamespaceGroup.getSecretValue()), imageNamespaceGroup);
                        log.debug("Secret [{}] updated while reconciling", secretKey);
                        return new Result(false);
                    }

                    return new Result(false);
                },
                request);
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
