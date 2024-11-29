package io.ten1010.aipub.projectcontroller.configuration;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.project")
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class ProjectProperties {

    private String registrySecretNamespace;

}
