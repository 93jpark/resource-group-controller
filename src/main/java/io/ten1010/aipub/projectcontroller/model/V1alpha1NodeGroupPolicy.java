package io.ten1010.aipub.projectcontroller.model;

import lombok.*;
import org.springframework.lang.Nullable;

@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class V1alpha1NodeGroupPolicy {

    @Nullable
    private V1alpha1NodeGroupPolicyDaemonSet daemonSet;

}
