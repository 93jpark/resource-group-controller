package io.ten1010.aipub.projectcontroller.controller.cluster.project;

import com.google.gson.JsonObject;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.extended.controller.reconciler.Reconciler;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.extended.event.EventType;
import io.kubernetes.client.extended.event.legacy.EventRecorder;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.proto.Meta;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import io.kubernetes.client.util.generic.KubernetesApiResponse;
import io.kubernetes.client.util.generic.options.UpdateOptions;
import io.ten1010.aipub.projectcontroller.controller.KubernetesApiReconcileExceptionHandlingTemplate;
import io.ten1010.aipub.projectcontroller.core.Events;
import io.ten1010.aipub.projectcontroller.core.K8sObjectUtil;
import io.ten1010.aipub.projectcontroller.core.KeyUtil;
import io.ten1010.aipub.projectcontroller.model.V1alpha1Project;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ProjectList;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class ProjectReconciler implements Reconciler {

    public static final Duration INVALID_STATE_REQUEUE_DURATION = Duration.ofSeconds(30);
    public static final Duration API_CONFLICT_REQUEUE_DURATION = Duration.ofSeconds(5);
    public static final Duration API_FAIL_REQUEUE_DURATION = Duration.ofSeconds(60);

    private static final String MSG_PROJECT_NAME_CONFLICT_WITH_NAMESPACE = "Project [%s] namespace already exists";

    private KubernetesApiReconcileExceptionHandlingTemplate template;
    private Indexer<V1alpha1Project> projectIndexer;
    private Indexer<V1Namespace> namespaceIndexer;
    private GenericKubernetesApi<V1alpha1Project, V1alpha1ProjectList> projectApi;
    private EventRecorder eventRecorder;

    public ProjectReconciler(
            Indexer<V1alpha1Project> projectIndexer,
            Indexer<V1Namespace> namespaceIndexer,
            GenericKubernetesApi<V1alpha1Project, V1alpha1ProjectList> projectApi,
            EventRecorder eventRecorder) {
        this.template = new KubernetesApiReconcileExceptionHandlingTemplate(API_CONFLICT_REQUEUE_DURATION, API_FAIL_REQUEUE_DURATION);
        this.projectIndexer = projectIndexer;
        this.namespaceIndexer = namespaceIndexer;
        this.projectApi = projectApi;
        this.eventRecorder = eventRecorder;
    }

    @Override
    public Result reconcile(Request request) {
        return this.template.execute(
                () -> {
                    log.info("Reconciling Project [{}]", request.getName());
                    String projectKey = KeyUtil.buildKey(request.getName());
                    Optional<V1alpha1Project> projectOpt = Optional.ofNullable(this.projectIndexer.getByKey(projectKey));
                    if (projectOpt.isEmpty()) {
                        log.info("Project [{}] not founded while reconciling", projectKey);
                        return new Result(false);
                    }
                    V1alpha1Project project = projectOpt.get();
                    Objects.requireNonNull(K8sObjectUtil.getName(project));
                    log.info("Project [{}] founded while reconciling\n{}", projectKey, project);

                    String namespaceKey = KeyUtil.buildKey(K8sObjectUtil.getName(project));
                    Optional<V1Namespace> namespaceOpt = Optional.ofNullable(this.namespaceIndexer.getByKey(namespaceKey));
                    if (project.getNamespace() == null) {
                        // todo project의 namespace 유무가 project에 대한 ns 생성 여부를 표방함
                        if (namespaceOpt.isPresent()) {
                            this.eventRecorder.event(
                                    project,
                                    EventType.Warning,
                                    Events.REASON_NAMESPACE_CONFLICT,
                                    String.format(MSG_PROJECT_NAME_CONFLICT_WITH_NAMESPACE, K8sObjectUtil.getName(project))
                            );
                            deleteProject(K8sObjectUtil.getName(project));
                            log.debug("Deleted Project [{}] due to namespace conflict", K8sObjectUtil.getName(project));
                            return new Result(true, INVALID_STATE_REQUEUE_DURATION);
                        }
                        updateProject(project, K8sObjectUtil.getName(project));
                        log.info("Updated Project [{}] namespace", K8sObjectUtil.getName(project));
                    }

                    return new Result(false);
                }, request);
    }

    private void deleteProject(String name) {
        this.projectApi.delete(name);
    }

    private void updateProject(V1alpha1Project project, String namespace) {
        JsonObject patchBody = new JsonObject();
        patchBody.add("spec", new JsonObject());
        patchBody.getAsJsonObject("spec").addProperty("namespace", namespace);

        V1Patch patch = new V1Patch(patchBody.toString());

        this.projectApi.patch(
                K8sObjectUtil.getName(project),
                V1Patch.PATCH_FORMAT_JSON_MERGE_PATCH,
                patch
        );
    }

}
