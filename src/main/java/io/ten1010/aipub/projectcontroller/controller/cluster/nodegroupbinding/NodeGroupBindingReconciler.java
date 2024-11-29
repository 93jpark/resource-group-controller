package io.ten1010.aipub.projectcontroller.controller.cluster.nodegroupbinding;

import io.kubernetes.client.extended.controller.reconciler.Reconciler;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.ten1010.aipub.projectcontroller.controller.KubernetesApiReconcileExceptionHandlingTemplate;
import io.ten1010.aipub.projectcontroller.core.KeyUtil;
import io.ten1010.aipub.projectcontroller.model.V1alpha1NodeGroup;
import io.ten1010.aipub.projectcontroller.model.V1alpha1NodeGroupBinding;
import io.ten1010.aipub.projectcontroller.model.V1alpha1Project;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class NodeGroupBindingReconciler implements Reconciler {

    public static final Duration API_CONFLICT_REQUEUE_DURATION = Duration.ofSeconds(5);
    public static final Duration API_FAIL_REQUEUE_DURATION = Duration.ofSeconds(60);

    private KubernetesApiReconcileExceptionHandlingTemplate template;
    private Indexer<V1alpha1NodeGroup> nodeGroupIndexer;
    private Indexer<V1alpha1NodeGroupBinding> nodeGroupBindingIndexer;
    private Indexer<V1alpha1Project> projectIndexer;
    private CoreV1Api coreV1Api;

    public NodeGroupBindingReconciler(Indexer<V1alpha1NodeGroup> nodeGroupIndexer, Indexer<V1alpha1NodeGroupBinding> nodeGroupBindingIndexer, Indexer<V1alpha1Project> projectIndexer, CoreV1Api coreV1Api) {
        this.template = new KubernetesApiReconcileExceptionHandlingTemplate(API_CONFLICT_REQUEUE_DURATION, API_FAIL_REQUEUE_DURATION);
        this.nodeGroupIndexer = nodeGroupIndexer;
        this.nodeGroupBindingIndexer = nodeGroupBindingIndexer;
        this.projectIndexer = projectIndexer;
        this.coreV1Api = coreV1Api;
    }

    @Override
    public Result reconcile(Request request) {
        return this.template.execute(
                () -> {
                    String nodeGroupBindingKey = KeyUtil.buildKey(request.getName());
                    Optional<V1alpha1NodeGroupBinding> nodeGroupBindingOpt = Optional.ofNullable(this.nodeGroupBindingIndexer.getByKey(nodeGroupBindingKey));
                    if (nodeGroupBindingOpt.isEmpty()) {
                        log.debug("NodeGroupBinding [{}] not founded while reconciling", nodeGroupBindingKey);
                        return new Result(false);
                    }

                    V1alpha1NodeGroupBinding nodeGroupBinding = nodeGroupBindingOpt.get();
                    Objects.requireNonNull(nodeGroupBinding.getNodeGroupRef());
                    String nodeGroupKey = KeyUtil.buildKey(nodeGroupBinding.getNodeGroupRef());
                    Optional<V1alpha1NodeGroup> nodeGroupOpt = Optional.ofNullable(this.nodeGroupIndexer.getByKey(nodeGroupKey));
                    if (nodeGroupOpt.isEmpty()) {
                        log.debug("NodeGroup [{}] not founded while reconciling", nodeGroupKey);
                        return new Result(false);
                    }

                    nodeGroupBinding.getProjects()
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
