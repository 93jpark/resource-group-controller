package io.ten1010.aipub.projectcontroller.controller.cluster.imagenamespacegroup;

import io.kubernetes.client.extended.controller.reconciler.Reconciler;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1TypedObjectReference;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import io.ten1010.aipub.projectcontroller.controller.KubernetesApiReconcileExceptionHandlingTemplate;
import io.ten1010.aipub.projectcontroller.controller.cluster.RegistryRobotConverter;
import io.ten1010.aipub.projectcontroller.controller.cluster.RegistryRobotFactory;
import io.ten1010.aipub.projectcontroller.core.K8sObjectUtil;
import io.ten1010.aipub.projectcontroller.core.KeyUtil;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroup;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroupList;
import io.ten1010.aipub.projectcontroller.service.RegistryRobot;
import io.ten1010.aipub.projectcontroller.service.RegistryRobotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.Optional;

@Slf4j
public class ImageNamespaceGroupReconciler implements Reconciler {

    public static final Duration API_CONFLICT_REQUEUE_DURATION = Duration.ofSeconds(5);
    public static final Duration API_FAIL_REQUEUE_DURATION = Duration.ofSeconds(60);

    private KubernetesApiReconcileExceptionHandlingTemplate template;
    private Indexer<V1alpha1ImageNamespaceGroup> imageNamespaceGroupIndexer;
    private Indexer<V1Secret> secretIndexer;
    private GenericKubernetesApi<V1alpha1ImageNamespaceGroup, V1alpha1ImageNamespaceGroupList> imageNamespaceGroupApi;
    private CoreV1Api coreV1Api;
    private RegistryRobotService registryRobotService;

    public ImageNamespaceGroupReconciler(
            Indexer<V1alpha1ImageNamespaceGroup> imageNamespaceGroupIndexer,
            Indexer<V1Secret> secretIndexer,
            GenericKubernetesApi<V1alpha1ImageNamespaceGroup, V1alpha1ImageNamespaceGroupList> imageNamespaceGroupApi,
            CoreV1Api coreV1Api,
            RegistryRobotService registryRobotService) {
        this.template = new KubernetesApiReconcileExceptionHandlingTemplate(API_CONFLICT_REQUEUE_DURATION, API_FAIL_REQUEUE_DURATION);
        this.imageNamespaceGroupIndexer = imageNamespaceGroupIndexer;
        this.secretIndexer = secretIndexer;
        this.imageNamespaceGroupApi = imageNamespaceGroupApi;
        this.coreV1Api = coreV1Api;
        this.registryRobotService = registryRobotService;
    }

    @Override
    public Result reconcile(Request request) {
        return this.template.execute(
                () -> {
                    String imageNamespaceGroupKey = KeyUtil.buildKey(request.getName());
                    Optional<V1alpha1ImageNamespaceGroup> imageNamespaceGroupOpt = Optional.ofNullable(this.imageNamespaceGroupIndexer.getByKey(imageNamespaceGroupKey));
                    String robotUsername = RegistryRobotConverter.toRegistryRobotUsername(request.getName());
                    Optional<RegistryRobot> robotOpt = registryRobotService.findByUsername(robotUsername);
                    if (imageNamespaceGroupOpt.isEmpty() && robotOpt.isPresent()) {
                        Assert.notNull(robotOpt.get().getId(), "robot id must not be null");
                        registryRobotService.deleteRobot(robotOpt.get().getId());
                        log.debug("Deleted Robot [{}] because ImageNamespaceGroup [{}] not found", robotUsername, imageNamespaceGroupKey);
                        return new Result(false);
                    }
                    if (imageNamespaceGroupOpt.isPresent()) {
                        Assert.notNull(imageNamespaceGroupOpt.get().getMetadata(), "metadata must not be null");
                        Assert.notNull(imageNamespaceGroupOpt.get().getMetadata().getName(), "name must not be null");
                        V1alpha1ImageNamespaceGroup imageNamespaceGroup = imageNamespaceGroupOpt.get();
                        log.debug("Reconciling ImageNamespaceGroup [{}]", imageNamespaceGroupKey);
                        RegistryRobot robot = RegistryRobotFactory.create(robotUsername, imageNamespaceGroup.getAipubImageNamespaces());
                        if (robotOpt.isEmpty()) {
                            this.registryRobotService.createRobot(robot);
                        } else {
                            if (!robot.getPermissions().equals(robotOpt.get().getPermissions())) {
                                registryRobotService.updateRobot(robotOpt.get().getId(), robot);
                            }
                        }
                        String secretKey = KeyUtil.buildKey(K8sObjectUtil.getName(imageNamespaceGroup));
                        Optional<V1Secret> secretOpt = Optional.ofNullable(secretIndexer.getByKey(secretKey));
                        if (secretOpt.isPresent()) {
                            V1Secret secret = secretOpt.get();
                            if (imageNamespaceGroup.getSecret() == null) {
                                updateImageNamespaceGroupSecretRef(imageNamespaceGroup, secret);
                                log.debug("Updated ImageNamespaceGroup [{}] with secret [{}]", imageNamespaceGroupKey, secretKey);
                            }
                        }
                        return new Result(false);
                    }
                    return new Result(false);
                }, request);
    }

    private void updateImageNamespaceGroupSecretRef(V1alpha1ImageNamespaceGroup imageNamespaceGroup, V1Secret secret) throws ApiException {
        V1TypedObjectReference secretRef = new V1TypedObjectReference();
        secretRef.setKind(secret.getKind());
        secretRef.setName(secret.getMetadata().getName());
        secretRef.setNamespace(secret.getMetadata().getNamespace());
        imageNamespaceGroup.setSecret(secretRef);
        this.imageNamespaceGroupApi.update(imageNamespaceGroup);
    }

}
