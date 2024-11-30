package io.ten1010.aipub.projectcontroller.controller.cluster.imagenamespacegroupbinding;

import com.google.gson.JsonObject;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.extended.controller.reconciler.Reconciler;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import io.ten1010.aipub.projectcontroller.controller.KubernetesApiReconcileExceptionHandlingTemplate;
import io.ten1010.aipub.projectcontroller.core.K8sObjectUtil;
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

    public ImageNamespaceGroupBindingReconciler(
            Indexer<V1alpha1ImageNamespaceGroup> imageNamespaceGroupIndexer,
            Indexer<V1alpha1ImageNamespaceGroupBinding> imageNamespaceGroupBindingIndexer,
            Indexer<V1alpha1Project> projectIndexer,
            GenericKubernetesApi<V1alpha1ImageNamespaceGroupBinding, V1alpha1ImageNamespaceGroupBindingList> imageNamespaceGroupBindingApi) {
        this.template = new KubernetesApiReconcileExceptionHandlingTemplate(API_CONFLICT_REQUEUE_DURATION, API_FAIL_REQUEUE_DURATION);
        this.imageNamespaceGroupIndexer = imageNamespaceGroupIndexer;
        this.imageNamespaceGroupBindingIndexer = imageNamespaceGroupBindingIndexer;
        this.projectIndexer = projectIndexer;
        this.imageNamespaceGroupBindingApi = imageNamespaceGroupBindingApi;
    }

    @Override
    public Result reconcile(Request request) {
        return this.template.execute(
                () -> {
                    String imageNamespaceGroupBindingKey = KeyUtil.buildKey(request.getName());
                    Optional<V1alpha1ImageNamespaceGroupBinding> imageNamespaceGroupBindingOpt = Optional.ofNullable(this.imageNamespaceGroupBindingIndexer.getByKey(imageNamespaceGroupBindingKey));
                    if (imageNamespaceGroupBindingOpt.isEmpty()) {
                        log.info("ImageNamespaceGroupBinding [{}] not founded while reconciling", imageNamespaceGroupBindingKey);
                        return new Result(false);
                    }
                    V1alpha1ImageNamespaceGroupBinding imageNamespaceGroupBinding = imageNamespaceGroupBindingOpt.get();
                    log.info("ImageNamespaceGroupBinding [{}] founded while reconciling {}", imageNamespaceGroupBindingKey, imageNamespaceGroupBinding);

                    String imageNamespaceGroupKey = KeyUtil.buildKey(K8sObjectUtil.getName(imageNamespaceGroupBinding));
                    Optional<V1alpha1ImageNamespaceGroup> imageNamespaceGroupOpt = Optional.ofNullable(this.imageNamespaceGroupIndexer.getByKey(imageNamespaceGroupKey));
                    if (imageNamespaceGroupOpt.isEmpty()) {
                        log.info("ImageNamespaceGroup [{}] not founded while reconciling", imageNamespaceGroupKey);
                        return new Result(false);
                    }
                    V1alpha1ImageNamespaceGroup imageNamespaceGroup = imageNamespaceGroupOpt.get();

                    if (imageNamespaceGroupBinding.getImageNamespaceGroupRef() == null ||
                        !K8sObjectUtil.getName(imageNamespaceGroup).equals(imageNamespaceGroupBinding.getImageNamespaceGroupRef())) {
                        updateImageNamespaceGroupBinding(imageNamespaceGroupBinding, K8sObjectUtil.getName(imageNamespaceGroup));
                        log.info("Updated ImageNamespaceGroupBinding [{}] ref to [{}]", imageNamespaceGroupBindingKey, K8sObjectUtil.getName(imageNamespaceGroup));
                        return new Result(false);
                    }
                    return new Result(false);
                }, request);
    }

    private void updateImageNamespaceGroupBinding(V1alpha1ImageNamespaceGroupBinding imageNamespaceGroupBinding, String imageNamespaceGroupRef) {
        JsonObject patchBody = new JsonObject();
        patchBody.add("spec", new JsonObject());
        patchBody.getAsJsonObject("spec").addProperty("imageNamespaceGroupRef", imageNamespaceGroupRef);

        V1Patch patch = new V1Patch(patchBody.toString());

        this.imageNamespaceGroupBindingApi.patch(
                K8sObjectUtil.getName(imageNamespaceGroupBinding),
                V1Patch.PATCH_FORMAT_JSON_MERGE_PATCH,
                patch
        );
    }

}
