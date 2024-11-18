package io.ten1010.aipub.projectcontroller.service;

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
public class RegistryRobot {

    @Nullable
    private String id;
    @Nullable
    private Long createdTimestamp;
    @Nullable
    private String username;
    @Nullable
    private String secret;
    private List<RobotPermission> permissions;

    public RegistryRobot() {
        this.permissions = new ArrayList<>();
    }

}
