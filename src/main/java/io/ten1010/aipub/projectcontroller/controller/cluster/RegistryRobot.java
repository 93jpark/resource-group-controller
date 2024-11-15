package io.ten1010.aipub.projectcontroller.controller.cluster;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegistryRobot {

    private Long id;
    private String name;
    private String description;
    private String secret;
    private Instant creationTime;
    private Long expiresAt;
    private Integer duration;
    private String level;
    private Boolean disable;
    private List<Permission> permissions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Permission {
        private List<Access> access;
        private String kind;
        private String namespace;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Access {
        private String action;
        private String resource;
    }

    public List<String> getNamespaces() {
        return permissions.stream()
                .map(Permission::getNamespace)
                .toList();
    }

}
