package io.ten1010.aipub.projectcontroller.service;

import lombok.*;
import org.springframework.lang.Nullable;

@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class RegistryAccess {

    @Nullable
    private String action;
    @Nullable
    private String resource;

}
