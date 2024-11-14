package io.ten1010.aipub.projectcontroller.model;

import io.kubernetes.client.common.KubernetesListObject;
import io.kubernetes.client.openapi.models.V1ListMeta;
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
public class V1alpha1ImageNamespaceGroupList implements KubernetesListObject {

    @Nullable
    private String apiVersion;
    @Nullable
    private String kind;
    @Nullable
    private V1ListMeta metadata;
    private List<V1alpha1ImageNamespaceGroup> items;

    public V1alpha1ImageNamespaceGroupList() {
        this.items = new ArrayList<>();
    }

}
