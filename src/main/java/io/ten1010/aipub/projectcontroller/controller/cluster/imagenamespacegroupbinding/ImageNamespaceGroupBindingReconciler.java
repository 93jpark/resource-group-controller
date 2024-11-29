package io.ten1010.aipub.projectcontroller.controller.cluster.imagenamespacegroupbinding;

import io.kubernetes.client.extended.controller.reconciler.Reconciler;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import io.ten1010.aipub.projectcontroller.controller.KubernetesApiReconcileExceptionHandlingTemplate;
import io.ten1010.aipub.projectcontroller.core.KeyUtil;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroup;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroupBinding;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroupBindingList;
import io.ten1010.aipub.projectcontroller.model.V1alpha1Project;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Optional;

@Slf4j
public class ImageNamespaceGroupBindingReconciler implements Reconciler {

    public static final Duration API_CONFLICT_REQUEUE_DURATION = Duration.ofSeconds(5);
    public static final Duration API_FAIL_REQUEUE_DURATION = Duration.ofSeconds(60);

    private KubernetesApiReconcileExceptionHandlingTemplate template;
    private Indexer<V1alpha1ImageNamespaceGroupBinding> imageNamespaceGroupBindingIndexer;
    private Indexer<V1alpha1Project> projectIndexer;
    private Indexer<V1alpha1ImageNamespaceGroup> imageNamespaceGroupIndexer;
    private GenericKubernetesApi<V1alpha1ImageNamespaceGroupBinding, V1alpha1ImageNamespaceGroupBindingList> imageNamespaceGroupBindingApi;
    private CoreV1Api coreV1Api;

    public ImageNamespaceGroupBindingReconciler(
            Indexer<V1alpha1ImageNamespaceGroup> imageNamespaceGroupIndexer,
            Indexer<V1alpha1ImageNamespaceGroupBinding> imageNamespaceGroupBindingIndexer,
            Indexer<V1alpha1Project> projectIndexer,
            GenericKubernetesApi<V1alpha1ImageNamespaceGroupBinding, V1alpha1ImageNamespaceGroupBindingList> imageNamespaceGroupBindingApi,
            CoreV1Api coreV1Api) {
        this.template = new KubernetesApiReconcileExceptionHandlingTemplate(API_CONFLICT_REQUEUE_DURATION, API_FAIL_REQUEUE_DURATION);
        this.imageNamespaceGroupIndexer = imageNamespaceGroupIndexer;
        this.imageNamespaceGroupBindingIndexer = imageNamespaceGroupBindingIndexer;
        this.projectIndexer = projectIndexer;
        this.imageNamespaceGroupBindingApi = imageNamespaceGroupBindingApi;
        this.coreV1Api = coreV1Api;
    }

    @Override
    public Result reconcile(Request request) {
        return this.template.execute(
                () -> {
                    String imageNamespaceGroupBindingKey = KeyUtil.buildKey(request.getName());
                    Optional<V1alpha1ImageNamespaceGroupBinding> imageNamespaceGroupBindingOpt = Optional.ofNullable(this.imageNamespaceGroupBindingIndexer.getByKey(imageNamespaceGroupBindingKey));
                    if (imageNamespaceGroupBindingOpt.isEmpty()) {
                        log.debug("ImageNamespaceGroupBinding [{}] not founded while reconciling", imageNamespaceGroupBindingKey);
                        return new Result(false);
                    }
                    V1alpha1ImageNamespaceGroupBinding imageNamespaceGroupBinding = imageNamespaceGroupBindingOpt.get();
                    log.debug("ImageNamespaceGroupBinding [{}] founded while reconciling", imageNamespaceGroupBindingKey);

                    if (imageNamespaceGroupBinding.getImageNamespaceGroupRef() == null) {
                        log.debug("ImageNamespaceGroupBinding [{}] has no ImageNamespaceGroupRef", imageNamespaceGroupBindingKey);
                        return new Result(false);
                    }

                    String imageNamespaceGroupKey = KeyUtil.buildKey(imageNamespaceGroupBinding.getImageNamespaceGroupRef());
                    Optional<V1alpha1ImageNamespaceGroup> imageNamespaceGroupOpt = Optional.ofNullable(this.imageNamespaceGroupIndexer.getByKey(imageNamespaceGroupKey));
                    if (imageNamespaceGroupOpt.isEmpty()) {
                        log.debug("ImageNamespaceGroup [{}] not founded while reconciling", imageNamespaceGroupKey);
                        return new Result(false);
                    }

                    imageNamespaceGroupBinding.getProjects()
                            .stream().map(KeyUtil::buildKey)
                            .forEach(projectKey -> {
                                Optional<V1alpha1Project> projectOpt = Optional.ofNullable(this.projectIndexer.getByKey(projectKey));
                                if (projectOpt.isEmpty()) {
                                    log.debug("Project [{}] not founded while reconciling", projectKey);
                                }
                            });
                    return new Result(false);
                }, request);
    }

}
