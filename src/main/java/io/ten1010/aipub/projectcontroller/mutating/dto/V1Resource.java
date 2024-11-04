package io.ten1010.aipub.projectcontroller.mutating.dto;

import lombok.Data;
import org.jspecify.annotations.Nullable;

@Data
public class V1Resource {

    @Nullable
    private String group;
    @Nullable
    private String version;
    @Nullable
    private String resource;

}
