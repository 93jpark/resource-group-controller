package io.ten1010.aipub.projectcontroller.model;

import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class V1alpha1ImageNamespaceGroupBinding implements KubernetesObject {

    @Nullable
    private String apiVersion;
    @Nullable
    private String kind;
    @Nullable
    private V1ObjectMeta metadata;
    @Nullable
    private String imageNamespaceGroupRef;
    private List<String> projects;

    public V1alpha1ImageNamespaceGroupBinding() {
        this.projects = new ArrayList<>();
    }

}
