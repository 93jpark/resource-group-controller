package io.ten1010.aipub.projectcontroller.service;

import lombok.*;
import org.springframework.lang.Nullable;

import java.util.List;

@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class RobotPermission {

    @Nullable
    private String namespace;
    @Nullable
    private List<RegistryAccess> accesses;

}
