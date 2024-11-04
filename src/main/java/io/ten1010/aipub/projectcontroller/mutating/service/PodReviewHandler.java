package io.ten1010.aipub.projectcontroller.mutating.service;

import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1Toleration;
import io.ten1010.aipub.projectcontroller.controller.workload.PodNodesResolver;
import io.ten1010.aipub.projectcontroller.controller.workload.UnsupportedControllerException;
import io.ten1010.aipub.projectcontroller.domain.k8s.K8sObjectTypeConstants;
import io.ten1010.aipub.projectcontroller.domain.k8s.ReconciliationService;
import io.ten1010.aipub.projectcontroller.mutating.V1AdmissionReviewUtils;
import io.ten1010.aipub.projectcontroller.mutating.dto.V1AdmissionReview;
import io.ten1010.common.jsonpatch.JsonPatchBuilder;
import io.ten1010.common.jsonpatch.JsonPatchOperationBuilder;
import io.ten1010.common.jsonpatch.dto.JsonPatch;
import io.ten1010.common.jsonpatch.dto.JsonPatchOperation;

import java.util.List;
import java.util.Objects;

public class PodReviewHandler extends ReviewHandlerSupport<V1Pod> {

    private final PodNodesResolver podNodesResolver;
    private final ReconciliationService reconciliationService;

    public PodReviewHandler(PodNodesResolver podNodesResolver, ReconciliationService reconciliationService) {
        super(K8sObjectTypeConstants.POD_V1);
        this.podNodesResolver = podNodesResolver;
        this.reconciliationService = reconciliationService;
    }

    @Override
    public void handle(V1AdmissionReview review) {
        Objects.requireNonNull(review.getRequest());

        V1Pod pod = getObject(review);

        List<V1Node> allowedProjectNodeObjects;
        try {
            allowedProjectNodeObjects = this.podNodesResolver.getNodes(pod);
        } catch (UnsupportedControllerException e) {
            allowedProjectNodeObjects = List.of();
        }
        List<V1Toleration> reconciledTolerations = this.reconciliationService.reconcileTolerations(pod, allowedProjectNodeObjects);
        JsonPatchOperation patchOperation = new JsonPatchOperationBuilder()
                .replace()
                .setPath("/spec/tolerations")
                .setValue(createJsonNode(reconciledTolerations))
                .build();
        JsonPatch jsonPatch = new JsonPatchBuilder()
                .addToOperations(patchOperation)
                .build();

        V1AdmissionReviewUtils.allow(review, jsonPatch);
    }

}
