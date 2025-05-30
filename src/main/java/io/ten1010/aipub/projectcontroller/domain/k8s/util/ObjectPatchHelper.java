package io.ten1010.aipub.projectcontroller.domain.k8s.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.util.PatchUtils;
import io.ten1010.aipub.projectcontroller.domain.k8s.K8sObjectType;
import io.ten1010.aipub.projectcontroller.domain.k8s.K8sObjectTypeKey;
import io.ten1010.aipub.projectcontroller.domain.k8s.ObjectMapperFactory;
import io.ten1010.common.jsonpatch.JsonPatchBuilder;
import io.ten1010.common.jsonpatch.JsonPatchOperationBuilder;
import io.ten1010.common.jsonpatch.dto.JsonPatch;
import io.ten1010.common.jsonpatch.dto.JsonPatchOperation;
import okhttp3.Call;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class ObjectPatchHelper<T extends KubernetesObject> {

    private final ApiClient apiClient;
    private final CustomObjectsApi customObjectsApi;
    private final K8sObjectType<T> resourceType;
    private final String resourcePlural;
    private final ObjectMapper mapper;

    public ObjectPatchHelper(ApiClient apiClient, K8sObjectType<T> resourceType, String resourcePlural) {
        this.apiClient = apiClient;
        this.customObjectsApi = new CustomObjectsApi(apiClient);
        this.resourceType = resourceType;
        this.resourcePlural = resourcePlural;
        this.mapper = new ObjectMapperFactory().createObjectMapper();
    }

    public KubernetesObject patchStatus(@Nullable String namespace, String name, Object status) throws ApiException {
        return PatchUtils.patch(
                this.resourceType.getObjClass(),
                () -> buildStatusCall(namespace, name, status),
                V1Patch.PATCH_FORMAT_JSON_PATCH,
                this.apiClient);
    }

    public KubernetesObject patchSpec(@Nullable String namespace, String name, Object spec) throws ApiException {
        return PatchUtils.patch(
                this.resourceType.getObjClass(),
                () -> buildSpecCall(namespace, name, spec),
                V1Patch.PATCH_FORMAT_JSON_PATCH,
                this.apiClient);
    }

    public KubernetesObject patchSpecAndStatus(@Nullable String namespace, String name, Object spec, Object status) throws ApiException {
        return PatchUtils.patch(
                this.resourceType.getObjClass(),
                () -> buildSpecAndStatusCall(namespace, name, spec, status),
                V1Patch.PATCH_FORMAT_JSON_PATCH,
                this.apiClient);
    }

    public KubernetesObject patchObject(@Nullable String namespace, String name, Object object) throws ApiException {
        return PatchUtils.patch(
                this.resourceType.getObjClass(),
                () -> buildObjectCall(namespace, name, object),
                V1Patch.PATCH_FORMAT_JSON_PATCH,
                this.apiClient);
    }

    private Call buildStatusCall(@Nullable String namespace, String name, Object status) throws ApiException {
        K8sObjectTypeKey typeKey = this.resourceType.getTypeKey();
        V1Patch patch = buildStatusPatch(status);
        if (namespace == null) {
            return this.customObjectsApi
                    .patchClusterCustomObjectStatus(typeKey.getGroup(), typeKey.getVersion(), this.resourcePlural, name, patch)
                    .buildCall(null);
        }
        return this.customObjectsApi
                .patchNamespacedCustomObjectStatus(typeKey.getGroup(), typeKey.getVersion(), namespace, this.resourcePlural, name, patch)
                .buildCall(null);
    }

    private Call buildSpecCall(@Nullable String namespace, String name, Object spec) throws ApiException {
        K8sObjectTypeKey typeKey = this.resourceType.getTypeKey();
        V1Patch patch = buildSpecPatch(spec);
        if (namespace == null) {
            return this.customObjectsApi
                    .patchClusterCustomObject(typeKey.getGroup(), typeKey.getVersion(), this.resourcePlural, name, patch)
                    .buildCall(null);
        }
        return this.customObjectsApi
                .patchNamespacedCustomObjectStatus(typeKey.getGroup(), typeKey.getVersion(), namespace, this.resourcePlural, name, patch)
                .buildCall(null);
    }

    private Call buildSpecAndStatusCall(@Nullable String namespace, String name, Object spec, Object status) throws ApiException {
        K8sObjectTypeKey typeKey = this.resourceType.getTypeKey();
        V1Patch patch = buildSpecAndStatusPatch(spec, status);
        if (namespace == null) {
            return this.customObjectsApi
                    .patchClusterCustomObject(typeKey.getGroup(), typeKey.getVersion(), this.resourcePlural, name, patch)
                    .buildCall(null);
        }
        return this.customObjectsApi
                .patchNamespacedCustomObject(typeKey.getGroup(), typeKey.getVersion(), namespace, this.resourcePlural, name, patch)
                .buildCall(null);
    }

    private Call buildObjectCall(@Nullable String namespace, String name, Object object) throws ApiException {
        K8sObjectTypeKey typeKey = this.resourceType.getTypeKey();
        V1Patch patch = buildObjectPatch(object);
        if (namespace == null) {
            return this.customObjectsApi
                    .patchClusterCustomObject(typeKey.getGroup(), typeKey.getVersion(), this.resourcePlural, name, patch)
                    .buildCall(null);
        }
        return this.customObjectsApi
                .patchNamespacedCustomObject(typeKey.getGroup(), typeKey.getVersion(), namespace, this.resourcePlural, name, patch)
                .buildCall(null);
    }

    private V1Patch buildStatusPatch(Object status) {
        JsonNode statusNode = this.mapper.valueToTree(status);
        JsonPatchOperation op = new JsonPatchOperationBuilder()
                .replace()
                .setPath("/status")
                .setValue(statusNode)
                .build();
        JsonPatch patch = new JsonPatchBuilder()
                .addToOperations(op)
                .build();

        try {
            String value = this.mapper.writeValueAsString(patch);
            return new V1Patch(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private V1Patch buildSpecPatch(Object spec) {
        JsonNode specNode = this.mapper.valueToTree(spec);
        JsonPatchOperation op = new JsonPatchOperationBuilder()
                .replace()
                .setPath("/spec")
                .setValue(specNode)
                .build();
        JsonPatch patch = new JsonPatchBuilder()
                .addToOperations(op)
                .build();

        try {
            String value = this.mapper.writeValueAsString(patch);
            return new V1Patch(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private V1Patch buildSpecAndStatusPatch(Object spec, Object status) {
        JsonNode specNode = this.mapper.valueToTree(spec);
        JsonPatchOperation specOp = new JsonPatchOperationBuilder()
                .replace()
                .setPath("/spec")
                .setValue(specNode)
                .build();
        JsonNode statusNode = this.mapper.valueToTree(status);
        JsonPatchOperation statusOp = new JsonPatchOperationBuilder()
                .replace()
                .setPath("/status")
                .setValue(statusNode)
                .build();
        JsonPatch patch = new JsonPatchBuilder()
                .withOperations(List.of(specOp, statusOp))
//                .addToOperations(specOp)
//                .addToOperations(statusOp)
                .build();

        try {
            String value = this.mapper.writeValueAsString(patch);
            return new V1Patch(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private V1Patch buildObjectPatch(Object object) {
        JsonNode objectNode = this.mapper.valueToTree(object);
        JsonPatchOperation objectOp = new JsonPatchOperationBuilder()
                .replace()
                .setPath("/")
                .setValue(objectNode)
                .build();
        JsonPatch patch = new JsonPatchBuilder()
                .addToOperations(objectOp)
                .build();

        try {
            String value = this.mapper.writeValueAsString(patch);
            return new V1Patch(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
