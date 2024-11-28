package io.ten1010.aipub.projectcontroller.controller.cluster.namespace;

import io.kubernetes.client.extended.controller.reconciler.Reconciler;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1NamespaceBuilder;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.ten1010.aipub.projectcontroller.controller.KubernetesApiReconcileExceptionHandlingTemplate;
import io.ten1010.aipub.projectcontroller.model.V1alpha1Project;

import java.time.Duration;

public class NamespaceReconciler implements Reconciler {

    public static final Duration API_CONFLICT_REQUEUE_DURATION = Duration.ofSeconds(5);
    public static final Duration API_FAIL_REQUEUE_DURATION = Duration.ofSeconds(60);

    private KubernetesApiReconcileExceptionHandlingTemplate template;
    private Indexer<V1Namespace> namespaceIndexer;
    private Indexer<V1alpha1Project> projectIndexer;
    private CoreV1Api coreV1Api;

    public NamespaceReconciler(
            Indexer<V1Namespace> namespaceIndexer,
            Indexer<V1alpha1Project> projectIndexer,
            CoreV1Api coreV1Api) {
        this.template = new KubernetesApiReconcileExceptionHandlingTemplate(API_CONFLICT_REQUEUE_DURATION, API_FAIL_REQUEUE_DURATION);
        this.namespaceIndexer = namespaceIndexer;
        this.projectIndexer = projectIndexer;
        this.coreV1Api = coreV1Api;
    }

    @Override
    public Result reconcile(Request request) {
        return this.template.execute(
                () -> {
                    String namespaceKey = request.getName();
                    V1Namespace namespace = this.namespaceIndexer.getByKey(namespaceKey);
                    if (namespace == null) {
                        createNamespace(namespaceKey);
                        return new Result(false);
                    }
                    return new Result(true);
                }, request);
    }

    private void createNamespace(String namespaceName) throws ApiException {
        V1Namespace newNamespace = new V1Namespace()
                .metadata(new V1ObjectMeta().name(namespaceName));
        coreV1Api.createNamespace(newNamespace)
                .execute();
    }


}
