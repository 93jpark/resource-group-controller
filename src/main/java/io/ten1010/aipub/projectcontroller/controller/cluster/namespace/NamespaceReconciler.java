package io.ten1010.aipub.projectcontroller.controller.cluster.namespace;

import io.kubernetes.client.extended.controller.reconciler.Reconciler;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.extended.event.EventType;
import io.kubernetes.client.extended.event.legacy.EventRecorder;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.ten1010.aipub.projectcontroller.configuration.ProjectProperties;
import io.ten1010.aipub.projectcontroller.controller.KubernetesApiReconcileExceptionHandlingTemplate;
import io.ten1010.aipub.projectcontroller.core.Events;
import io.ten1010.aipub.projectcontroller.core.IndexNames;
import io.ten1010.aipub.projectcontroller.core.K8sObjectUtil;
import io.ten1010.aipub.projectcontroller.model.V1alpha1Project;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
public class NamespaceReconciler implements Reconciler {

    public static final Duration INVALID_STATE_REQUEUE_DURATION = Duration.ofSeconds(30);
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
                    String namespaceKey = request.getName();
                    Optional<V1Namespace> namespaceOpt = Optional.ofNullable(this.namespaceIndexer.getByKey(namespaceKey));
                    if (isRegistryNamespace(request.getName())) {
                        if (namespaceOpt.isEmpty()) {
                            createNamespace(request.getName());
                            log.debug("Created Registry Secret Namespace [{}]", request.getName());
                            return new Result(false);
                        }
                    }
                    if (namespaceOpt.isPresent()) {
                        V1Namespace namespace = namespaceOpt.get();
                        List<V1alpha1Project> projects = this.projectIndexer.byIndex(IndexNames.BY_NAMESPACE_NAME_TO_PROJECT_OBJECT, K8sObjectUtil.getName(namespace));
                        if (projects.size() > 1) {
                            for (V1alpha1Project p : projects) {
                                this.eventRecorder.event(
                                        p,
                                        EventType.Warning,
                                        Events.REASON_NAMESPACE_CONFLICT, MSG_NAMESPACE_BELONGS_TO_MULTIPLE_PROJECTS,
                                        K8sObjectUtil.getName(p));
                            }
                            log.debug("Namespace [{}] belongs to multiple projects", K8sObjectUtil.getName(namespace));
                            return new Result(true, INVALID_STATE_REQUEUE_DURATION);
                        }
                        if (projects.isEmpty()) {
                            deleteNamespace(K8sObjectUtil.getName(namespace));
                            log.debug("Deleted Namespace [{}] because related Project not found", K8sObjectUtil.getName(namespace));
                            return new Result(false);
                        }
                    }
                    createNamespace(request.getName());
                    log.debug("Created Namespace [{}]", request.getName());
                    return new Result(true);
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

    private boolean isRegistryNamespace(String namespaceName) {
        return namespaceName.equals(projectProperties.getRegistrySecretNamespace());
    }

}
