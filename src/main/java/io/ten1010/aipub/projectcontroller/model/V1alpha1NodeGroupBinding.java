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
public class V1alpha1NodeGroupBinding implements KubernetesObject {

    @Nullable
    private String apiVersion;
    @Nullable
    private String kind;
    @Nullable
    private V1ObjectMeta metadata;
    @Nullable
    private String nodeGroupRef;
    private List<String> projects;

    public V1alpha1NodeGroupBinding() {
        this.projects = new ArrayList<>();
    }

}
