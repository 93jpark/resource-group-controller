package io.ten1010.aipub.projectcontroller.controller.cluster;

import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1TypedObjectReference;
import io.ten1010.aipub.projectcontroller.core.KeyUtil;
import io.ten1010.aipub.projectcontroller.model.V1alpha1ImageNamespaceGroup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Objects;

@Getter
@Setter
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ProjectImageNamespaceGroup {

    private String name;
    private String namespace;
    private String registryRobotName;
    private List<String> imageNamespaces;
    private String secretValue;
    private V1TypedObjectReference secretRef;

    public static ProjectImageNamespaceGroup from(V1alpha1ImageNamespaceGroup imageNamespaceGroup) {
        Objects.requireNonNull(imageNamespaceGroup.getMetadata());
        Objects.requireNonNull(imageNamespaceGroup.getMetadata().getName());
        Objects.requireNonNull(imageNamespaceGroup.getMetadata().getNamespace());
        return ProjectImageNamespaceGroup.builder()
                .name(imageNamespaceGroup.getMetadata().getName())
                .namespace(imageNamespaceGroup.getMetadata().getNamespace())
                .imageNamespaces(imageNamespaceGroup.getAipubImageNamespaces())
                .registryRobotName(KeyUtil.buildKey(imageNamespaceGroup.getMetadata().getNamespace(), imageNamespaceGroup.getMetadata().getName()))
                .build();
    }

    public String getRegistryRobotName() {
        return KeyUtil.buildKey(this.namespace, this.name);
    }

    public ProjectImageNamespaceGroup setSecretRef(V1Secret secret) {
        V1TypedObjectReference secretRef = new V1TypedObjectReference();
        secretRef.setKind(secret.getKind());
        secretRef.setName(secret.getMetadata().getName());
        secretRef.setNamespace(secret.getMetadata().getNamespace());
        this.secretRef = secretRef;
        return this;
    }

}
