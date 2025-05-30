package io.ten1010.aipub.projectcontroller.domain.k8s.dto;

import lombok.Data;
import org.jspecify.annotations.Nullable;

@Data
public class V1alpha1ProjectImageHub {

    @Nullable
    private String name;
    @Nullable
    private String id;

}
