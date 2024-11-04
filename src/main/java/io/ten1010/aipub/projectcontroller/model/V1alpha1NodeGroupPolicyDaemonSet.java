package io.ten1010.aipub.projectcontroller.model;

import io.kubernetes.client.openapi.models.V1TypedObjectReference;
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
public class V1alpha1NodeGroupPolicyDaemonSet {

    @Nullable
    private Boolean allowAllDaemonSets;
    private List<String> allowedNamespaces;
    private List<V1TypedObjectReference> allowedDaemonSets;

    public V1alpha1NodeGroupPolicyDaemonSet() {
        this.allowedNamespaces = new ArrayList<>();
        this.allowedDaemonSets = new ArrayList<>();
    }

}
