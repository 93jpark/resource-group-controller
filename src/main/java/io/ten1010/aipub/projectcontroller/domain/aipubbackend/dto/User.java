package io.ten1010.aipub.projectcontroller.domain.aipubbackend.dto;

import lombok.Data;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Map;

@Data
public class User {

    private String id;
    private String username;
    private Long createdTimestamp;
    private String lastName;
    private String firstName;
    private String email;
    private Boolean enabled;
    private Boolean emailVerified;
    private Integer notBefore;
    @NonNull
    private List<String> requiredActions;
    @NonNull
    private Map<String, List<String>> attributes;

}
