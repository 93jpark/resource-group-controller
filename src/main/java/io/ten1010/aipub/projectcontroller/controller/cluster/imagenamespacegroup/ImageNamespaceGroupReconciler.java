package io.ten1010.aipub.projectcontroller.controller.cluster.imagenamespacegroup;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.extended.controller.reconciler.Reconciler;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1TypedObjectReference;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import io.kubernetes.client.util.generic.KubernetesApiResponse;
import io.ten1010.aipub.projectcontroller.controller.KubernetesApiReconcileExceptionHandlingTemplate;
import io.ten1010.aipub.projectcontroller.controller.cluster.RegistryRobotResolver;
import io.ten1010.aipub.projectcontroller.core.K8sObjectUtil;
import io.ten1010.aipub.projectcontroller.core.KeyUtil;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroup;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroupBinding;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroupList;
import io.ten1010.aipub.projectcontroller.model.V1alpha1Project;
import io.ten1010.aipub.projectcontroller.service.RegistryRobot;
import io.ten1010.aipub.projectcontroller.service.RegistryRobotService;
import io.ten1010.aipub.projectcontroller.service.RobotPermission;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
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
    private RegistryRobotResolver robotResolver;
    private String registrySecretNamespace;

    public ImageNamespaceGroupReconciler(
            Indexer<V1alpha1ImageNamespaceGroup> imageNamespaceGroupIndexer,
            Indexer<V1Secret> secretIndexer,
            GenericKubernetesApi<V1alpha1ImageNamespaceGroup, V1alpha1ImageNamespaceGroupList> imageNamespaceGroupApi,
            CoreV1Api coreV1Api,
            RegistryRobotService registryRobotService,
            String registrySecretNamespace) {
        this.template = new KubernetesApiReconcileExceptionHandlingTemplate(API_CONFLICT_REQUEUE_DURATION, API_FAIL_REQUEUE_DURATION);
        this.imageNamespaceGroupIndexer = imageNamespaceGroupIndexer;
        this.secretIndexer = secretIndexer;
        this.imageNamespaceGroupApi = imageNamespaceGroupApi;
        this.coreV1Api = coreV1Api;
        this.registryRobotService = registryRobotService;
        this.robotResolver = new RegistryRobotResolver();
        this.registrySecretNamespace = registrySecretNamespace;
    }

    @Override
    public Result reconcile(Request request) {
        return this.template.execute(
                () -> {
                    log.info("Reconciling ImageNamespaceGroup [{}]", request.getName());
                    String imageNamespaceGroupKey = KeyUtil.buildKey(request.getName());
                    Optional<V1alpha1ImageNamespaceGroup> imageNamespaceGroupOpt = Optional.ofNullable(this.imageNamespaceGroupIndexer.getByKey(imageNamespaceGroupKey));
                    String robotUsername = this.robotResolver.resolveRobotUsername(request.getName());
                    this.robotResolver.resolveRobotUsername(request.getName());
                    Optional<RegistryRobot> robotOpt = this.registryRobotService.findByUsername(robotUsername);
                    if (imageNamespaceGroupOpt.isEmpty() && robotOpt.isPresent()) {
                        Objects.requireNonNull(robotOpt.get().getId(), "robot id must not be null");
                        this.registryRobotService.deleteRobot(robotOpt.get().getId());
                        log.info("Deleted Robot [{}] because ImageNamespaceGroup [{}] not found", robotUsername, imageNamespaceGroupKey);
                        return new Result(false);
                    }
                    if (imageNamespaceGroupOpt.isPresent()) {
                        Objects.requireNonNull(imageNamespaceGroupOpt.get().getMetadata(), "metadata must not be null");
                        Objects.requireNonNull(imageNamespaceGroupOpt.get().getMetadata().getName(), "name must not be null");
                        V1alpha1ImageNamespaceGroup imageNamespaceGroup = imageNamespaceGroupOpt.get();
                        if (robotOpt.isEmpty()) {
                            log.info("RegistryRobot [{}] not founded while reconciling", robotUsername);
                            return new Result(true, Duration.ofSeconds(3));
                        } else {
                            Objects.requireNonNull(robotOpt.get().getId(), "robot id must not be null");
                            RegistryRobot robot = robotOpt.get();
                            List<String> robotImageNamespaces = robot.getPermissions().stream().map(RobotPermission::getNamespace).toList();
                            if (!imageNamespaceGroup.getAipubImageNamespaces().equals(robotImageNamespaces)) {
                                this.registryRobotService.updateRobot(robot.getId(), robot);
                                log.info("Updated Robot [{}] because registry image namespace", robotUsername);
                            }
                        }
                        String secretKey = KeyUtil.buildKey(this.registrySecretNamespace, K8sObjectUtil.getName(imageNamespaceGroup));
                        Optional<V1Secret> secretOpt = Optional.ofNullable(this.secretIndexer.getByKey(secretKey));
                        if (secretOpt.isPresent()) {
                            V1Secret secret = secretOpt.get();
                            if (imageNamespaceGroup.getSecret() == null) {
                                updateImageNamespaceGroupSecretRef(imageNamespaceGroup, secret);
                                log.info("Updated ImageNamespaceGroup [{}] with secret [{}]", imageNamespaceGroupKey, secretKey);
                                return new Result(false);
                            }
                        }
                        return new Result(false);
                    }
                    log.info("ImageNamespaceGroup [{}] not founded while reconciling", imageNamespaceGroupKey);
                    return new Result(false);
                }, request);
    }

    private void updateImageNamespaceGroupSecretRef(V1alpha1ImageNamespaceGroup imageNamespaceGroup, V1Secret secret) throws ApiException {
        JsonObject patchBody = new JsonObject();
        JsonObject spec = new JsonObject();

        JsonObject secretRef = new JsonObject();
        secretRef.addProperty("kind", secret.getKind());
        secretRef.addProperty("name", K8sObjectUtil.getName(secret));
        secretRef.addProperty("namespace", K8sObjectUtil.getNamespace(secret));

        spec.add("secret", secretRef);
        patchBody.add("spec", spec);

        V1Patch patch = new V1Patch(patchBody.toString());

        log.info("Patch content: {}", patchBody.toString());

        KubernetesApiResponse<V1alpha1ImageNamespaceGroup> result = this.imageNamespaceGroupApi.patch(
                K8sObjectUtil.getName(imageNamespaceGroup),
                V1Patch.PATCH_FORMAT_STRATEGIC_MERGE_PATCH,
                patch
        );

        log.info("update image ns group result: {}", result.isSuccess());
    }

}
