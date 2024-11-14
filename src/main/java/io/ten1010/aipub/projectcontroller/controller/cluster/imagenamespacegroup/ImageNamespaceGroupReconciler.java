package io.ten1010.aipub.projectcontroller.controller.cluster.imagenamespacegroup;

import io.kubernetes.client.extended.controller.reconciler.Reconciler;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1TypedObjectReference;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import io.ten1010.aipub.projectcontroller.controller.KubernetesApiReconcileExceptionHandlingTemplate;
import io.ten1010.aipub.projectcontroller.controller.cluster.ProjectImageNamespaceGroup;
import io.ten1010.aipub.projectcontroller.controller.cluster.RegistryRobot;
import io.ten1010.aipub.projectcontroller.controller.cluster.RegistryRobotService;
import io.ten1010.aipub.projectcontroller.core.KeyUtil;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroup;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroupList;
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
    private RegistryRobotService registryRobotService;

    public ImageNamespaceGroupReconciler(
            Indexer<V1alpha1ImageNamespaceGroup> imageNamespaceGroupIndexer,
            Indexer<V1Secret> secretIndexer,
            GenericKubernetesApi<V1alpha1ImageNamespaceGroup, V1alpha1ImageNamespaceGroupList> imageNamespaceGroupApi,
            RegistryRobotService registryRobotService) {
        this.template = new KubernetesApiReconcileExceptionHandlingTemplate(API_CONFLICT_REQUEUE_DURATION, API_FAIL_REQUEUE_DURATION);
        this.imageNamespaceGroupIndexer = imageNamespaceGroupIndexer;
        this.secretIndexer = secretIndexer;
        this.imageNamespaceGroupApi = imageNamespaceGroupApi;
        this.registryRobotService = registryRobotService;
    }

    @Override
    public Result reconcile(Request request) {
        return this.template.execute(
                () -> {
                    String imageNamespaceGroupKey = KeyUtil.buildKey(request.getNamespace(), request.getName());
                    Optional<V1alpha1ImageNamespaceGroup> imageNamespaceGroupOpt = Optional.ofNullable(this.imageNamespaceGroupIndexer.getByKey(imageNamespaceGroupKey));
                    Optional<RegistryRobot> registryRobotOpt = registryRobotService.getRegistryRobot(request.getName());
                    if (imageNamespaceGroupOpt.isEmpty() && registryRobotOpt.isPresent()) {
                        registryRobotService.deleteRegistryRobot(registryRobotOpt.get().getId());
                        log.debug("Deleted RegistryRobot [{}] because ImageNamespaceGroup [{}] not found", registryRobotOpt.get().getName(), imageNamespaceGroupKey);
                        return new Result(false);
                    }
                    if (imageNamespaceGroupOpt.isPresent()) {
                        Assert.notNull(imageNamespaceGroupOpt.get().getMetadata(), "metadata must not be null");
                        Assert.notNull(imageNamespaceGroupOpt.get().getMetadata().getName(), "name must not be null");
                        V1alpha1ImageNamespaceGroup imageNamespaceGroup = imageNamespaceGroupOpt.get();
                        log.debug("Reconciling ImageNamespaceGroup [{}]", imageNamespaceGroupKey);
                        ProjectImageNamespaceGroup projectImageNamespaceGroup = ProjectImageNamespaceGroup.from(imageNamespaceGroup);
                        RegistryRobot registryRobot = registryRobotOpt.orElse(
                                registryRobotService.createRegistryRobot(projectImageNamespaceGroup.getRegistryRobotName(), projectImageNamespaceGroup.getImageNamespaces()));
                        if (!projectImageNamespaceGroup.getImageNamespaces().equals(registryRobot.getNamespaces())) {
                            registryRobotService.updateRegistryRobot(registryRobot.getId(), projectImageNamespaceGroup.getImageNamespaces());
                            log.debug("Updating RegistryRobot [{}] with new namespaces", registryRobot.getName());
                        }
                        String secretKey = KeyUtil.buildKey(projectImageNamespaceGroup.getNamespace(), projectImageNamespaceGroup.getName());
                        Optional<V1Secret> secretOpt = Optional.ofNullable(secretIndexer.getByKey(secretKey));
                        if (secretOpt.isPresent()) {
                            V1Secret secret = secretOpt.get();
                            projectImageNamespaceGroup.setSecretRef(secret);
                            if (imageNamespaceGroup.getSecret() == null) {
                                updateNamespacedImageNamespaceGroup(imageNamespaceGroup, projectImageNamespaceGroup.getSecretRef());
                                log.debug("Updated ImageNamespaceGroup [{}] with secret [{}]", imageNamespaceGroupKey, secretKey);
                            }
                            if (!imageNamespaceGroup.getSecret().equals(projectImageNamespaceGroup.getSecretRef())) {
                                updateNamespacedImageNamespaceGroup(imageNamespaceGroup, projectImageNamespaceGroup.getSecretRef());
                                log.debug("Updated ImageNamespaceGroup [{}] with secret [{}]", imageNamespaceGroupKey, secretKey);
                            }
                        }
                        return new Result(false);
                    }
                    return new Result(false);
                }, request);
    }

    private void updateNamespacedImageNamespaceGroup(V1alpha1ImageNamespaceGroup imageNamespaceGroup, V1TypedObjectReference secretRef) throws ApiException {
        imageNamespaceGroup.setSecret(secretRef);
        this.imageNamespaceGroupApi.update(imageNamespaceGroup);
    }

}
