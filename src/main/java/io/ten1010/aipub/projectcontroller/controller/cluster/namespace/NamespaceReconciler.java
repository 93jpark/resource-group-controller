package io.ten1010.aipub.projectcontroller.controller.cluster.namespace;

import io.kubernetes.client.extended.controller.reconciler.Reconciler;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.extended.event.legacy.EventRecorder;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.ten1010.aipub.projectcontroller.configuration.ProjectProperties;
import io.ten1010.aipub.projectcontroller.controller.KubernetesApiReconcileExceptionHandlingTemplate;
import io.ten1010.aipub.projectcontroller.core.K8sObjectUtil;
import io.ten1010.aipub.projectcontroller.core.KeyUtil;
import io.ten1010.aipub.projectcontroller.model.V1alpha1Project;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Optional;

@Slf4j
public class NamespaceReconciler implements Reconciler {

    public static final Duration API_CONFLICT_REQUEUE_DURATION = Duration.ofSeconds(5);
    public static final Duration API_FAIL_REQUEUE_DURATION = Duration.ofSeconds(60);

    private static final String MSG_NAMESPACE_BELONGS_TO_MULTIPLE_PROJECTS = "Namespace [%s] belongs to multiple projects";

    private KubernetesApiReconcileExceptionHandlingTemplate template;
    private Indexer<V1Namespace> namespaceIndexer;
    private Indexer<V1alpha1Project> projectIndexer;
    private CoreV1Api coreV1Api;
    private EventRecorder eventRecorder;
    private ProjectProperties projectProperties;

    public NamespaceReconciler(
            Indexer<V1Namespace> namespaceIndexer,
            Indexer<V1alpha1Project> projectIndexer,
            CoreV1Api coreV1Api,
            EventRecorder eventRecorder,
            ProjectProperties projectProperties) {
        this.template = new KubernetesApiReconcileExceptionHandlingTemplate(API_CONFLICT_REQUEUE_DURATION, API_FAIL_REQUEUE_DURATION);
        this.namespaceIndexer = namespaceIndexer;
        this.projectIndexer = projectIndexer;
        this.coreV1Api = coreV1Api;
        this.eventRecorder = eventRecorder;
        this.projectProperties = projectProperties;
    }

    @Override
    public Result reconcile(Request request) {
        return this.template.execute(
                () -> {
                    log.info("Reconciling Namespace [{}]", request.getName());
                    String projectKey = KeyUtil.buildKey(request.getName());
                    Optional<V1alpha1Project> projectOpt = Optional.ofNullable(this.projectIndexer.getByKey(projectKey));
                    if (projectOpt.isPresent()) {
                        log.info("Project [{}] found while reconciling", projectKey);
                        // 프로젝트가 존재할때
                        V1alpha1Project project = projectOpt.get();
                        if (project.getNamespace() != null) {
                            // 프로젝트 reconciler 검증이 된 경우
                            log.info("Project [{}] has namespace value", projectKey);
                            String namespaceKey = request.getName();
                            Optional<V1Namespace> namespaceOpt = Optional.ofNullable(this.namespaceIndexer.getByKey(namespaceKey));
                            if (namespaceOpt.isPresent()) {
                                // 이미 해당하는 네임스페이스가 있다는건 뭔가 잘못됬다는것
                                throw new RuntimeException(String.format(MSG_NAMESPACE_BELONGS_TO_MULTIPLE_PROJECTS, namespaceKey));
                            }
                        }
                        createNamespace(request.getName());
                        log.info("Namespace [{}] created for Project [{}]", request.getName(), K8sObjectUtil.getName(project));
                        return new Result(false);
                    }
                    log.info("Project [{}] not found while reconciling", projectKey);
                    // 프로젝트가 삭제되면
                    String namespaceKey = request.getName();
                    Optional<V1Namespace> namespaceOpt = Optional.ofNullable(this.namespaceIndexer.getByKey(namespaceKey));
                    if (namespaceOpt.isPresent()) {
                        V1Namespace namespace = namespaceOpt.get();
                        deleteNamespace(K8sObjectUtil.getName(namespace));
                        log.info("Namespace [{}] deleted because related project not found", namespaceKey);
                        return new Result(false);
                    }
                    return new Result(false);
                }, request);
    }

    private void createNamespace(String namespaceName) throws ApiException {
        V1Namespace newNamespace = new V1Namespace()
                .metadata(new V1ObjectMeta().name(namespaceName));
        coreV1Api.createNamespace(newNamespace)
                .execute();
    }

    private void deleteNamespace(String namespaceName) throws ApiException {
        coreV1Api.deleteNamespace(namespaceName)
                .execute();
    }

}
