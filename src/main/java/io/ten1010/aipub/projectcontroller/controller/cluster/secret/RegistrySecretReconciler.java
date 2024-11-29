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
import io.ten1010.aipub.projectcontroller.controller.cluster.RegistryRobotFactory;
import io.ten1010.aipub.projectcontroller.controller.cluster.RegistryRobotResolver;
import io.ten1010.aipub.projectcontroller.core.ImagePullSecretUtil;
import io.ten1010.aipub.projectcontroller.core.K8sObjectUtil;
import io.ten1010.aipub.projectcontroller.core.KeyUtil;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroup;
import io.ten1010.aipub.projectcontroller.service.RegistryRobot;
import io.ten1010.aipub.projectcontroller.service.RegistryRobotService;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class RegistrySecretReconciler implements Reconciler {

    public static final Duration API_CONFLICT_REQUEUE_DURATION = Duration.ofSeconds(5);
    public static final Duration API_FAIL_REQUEUE_DURATION = Duration.ofSeconds(60);

    private KubernetesApiReconcileExceptionHandlingTemplate template;
    private Indexer<V1alpha1ImageNamespaceGroup> imageNamespaceGroupIndexer;
    private Indexer<V1Secret> secretIndexer;
    private CoreV1Api coreV1Api;
    private RegistryRobotService registryRobotService;
    private RegistryRobotResolver robotResolver;
    private String registrySecretNamespace;

    public RegistrySecretReconciler(
            Indexer<V1alpha1ImageNamespaceGroup> imageNamespaceGroupIndexer,
            Indexer<V1Secret> secretIndexer,
            CoreV1Api coreV1Api,
            RegistryRobotService registryRobotService,
            String registrySecretNamespace) {
        this.template = new KubernetesApiReconcileExceptionHandlingTemplate(API_CONFLICT_REQUEUE_DURATION, API_FAIL_REQUEUE_DURATION);
        this.imageNamespaceGroupIndexer = imageNamespaceGroupIndexer;
        this.secretIndexer = secretIndexer;
        this.coreV1Api = coreV1Api;
        this.registryRobotService = registryRobotService;
        this.robotResolver = new RegistryRobotResolver();
        this.registrySecretNamespace = registrySecretNamespace;
    }

    @Override
    public Result reconcile(Request request) {
        return this.template.execute(
                () -> {
                    String imageNamespaceGroupKey = KeyUtil.buildKey(request.getName());
                    Optional<V1alpha1ImageNamespaceGroup> imageNamespaceGroupOpt = Optional.ofNullable(this.imageNamespaceGroupIndexer.getByKey(imageNamespaceGroupKey));
                    if (imageNamespaceGroupOpt.isEmpty()) {
                        return new Result(false);
                    }
                    V1alpha1ImageNamespaceGroup imageNamespaceGroup = imageNamespaceGroupOpt.get();
                    Objects.requireNonNull(imageNamespaceGroup.getMetadata(), "metadata must not be null");
                    Objects.requireNonNull(imageNamespaceGroup.getMetadata().getName(), "name must not be null");
                    String robotUsername = this.robotResolver.resolveRobotUsername(K8sObjectUtil.getName(imageNamespaceGroup));
                    Optional<RegistryRobot> robotOpt = registryRobotService.findByUsername(robotUsername);
                    if (robotOpt.isEmpty()) {
                        log.debug("RegistryRobot [{}] not founded while reconciling", K8sObjectUtil.getName(imageNamespaceGroup));
                        String registrySecretKey = KeyUtil.buildKey(this.registrySecretNamespace, request.getName());
                        Optional<V1Secret> registrySecretOpt = Optional.ofNullable(secretIndexer.getByKey(registrySecretKey));
                        if (registrySecretOpt.isPresent()) {
                            V1Secret registrySecret = registrySecretOpt.get();
                            deleteSecret(K8sObjectUtil.getName(registrySecret), K8sObjectUtil.getNamespace(registrySecret));
                            log.debug("Deleted secreted [{}] which exists before creating RegistryRobot while reconciling", registrySecretKey);
                        }
                        RegistryRobot robot = RegistryRobotFactory.create(K8sObjectUtil.getName(imageNamespaceGroup), imageNamespaceGroup.getAipubImageNamespaces());
                        String robotSecret = registryRobotService.createRobot(robot);
                        log.debug("Created Robot [{}] while reconciling", robotUsername);
                        createSecret(K8sObjectUtil.getName(imageNamespaceGroup), this.registrySecretNamespace, robotSecret);
                        log.debug("Created Secret [{}] for ImageNamespaceGroup[{}] while reconciling", registrySecretKey, imageNamespaceGroupKey);
                        return new Result(false);
                    }
                    String registrySecretKey = KeyUtil.buildKey(this.registrySecretNamespace, request.getName());
                    Optional<V1Secret> registrySecretOpt = Optional.ofNullable(secretIndexer.getByKey(registrySecretKey));
                    if (registrySecretOpt.isPresent()) {
                        V1Secret registrySecret = registrySecretOpt.get();
                        if (!ImagePullSecretUtil.hasPullSecretData(registrySecret)) {
                            deleteSecret(K8sObjectUtil.getName(registrySecret), K8sObjectUtil.getNamespace(registrySecret));
                            log.debug("Deleted Secret [{}] which doesn't have imagePullSecret while reconciling", registrySecretKey);
                            String newRobotSecret = recreateRegistryRobot(robotOpt.get(), imageNamespaceGroup);
                            createSecret(K8sObjectUtil.getName(imageNamespaceGroup), this.registrySecretNamespace, newRobotSecret);
                            log.debug("Created Secret [{}] for ImageNamespaceGroup[{}] while reconciling", registrySecretKey, imageNamespaceGroupKey);
                            return new Result(false);
                        }
                    }
                    String newRobotSecret = recreateRegistryRobot(robotOpt.get(), imageNamespaceGroup);
                    createSecret(K8sObjectUtil.getName(imageNamespaceGroup), this.registrySecretNamespace, newRobotSecret);
                    log.debug("Created Secret [{}] for ImageNamespaceGroup[{}] while reconciling", registrySecretKey, imageNamespaceGroupKey);
                    return new Result(false);
                },
                request);
    }

    private String recreateRegistryRobot(RegistryRobot existingRobot, V1alpha1ImageNamespaceGroup imageNamespaceGroup) {
        Objects.requireNonNull(existingRobot.getId(), "robot id must not be null");
        registryRobotService.deleteRobot(existingRobot.getId());
        RegistryRobot newRobot = RegistryRobotFactory.create(K8sObjectUtil.getName(imageNamespaceGroup), imageNamespaceGroup.getAipubImageNamespaces());
        return registryRobotService.createRobot(newRobot);
    }

    private void createSecret(String name, String namespace, String secretValue) throws ApiException {
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
