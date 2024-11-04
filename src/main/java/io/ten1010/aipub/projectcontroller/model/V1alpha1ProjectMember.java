package io.ten1010.aipub.projectcontroller.model;

import io.kubernetes.client.openapi.models.RbacV1Subject;
import lombok.*;
import org.springframework.lang.Nullable;

@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class V1alpha1ProjectMember {

    @Nullable
    private String aipubUser;
    @Nullable
    private RbacV1Subject subject;
    @Nullable
    private String role;

}
