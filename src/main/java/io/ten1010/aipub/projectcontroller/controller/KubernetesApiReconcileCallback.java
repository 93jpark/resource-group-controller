package io.ten1010.aipub.projectcontroller.controller;

import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.openapi.ApiException;

public interface KubernetesApiReconcileCallback {

    Result doWithApiExceptionHandling() throws ApiException;

}
