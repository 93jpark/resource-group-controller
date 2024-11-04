package io.ten1010.aipub.projectcontroller.configuration;

import io.ten1010.aipub.projectcontroller.domain.aipubbackend.AipubSubjectResolver;
import io.ten1010.aipub.projectcontroller.domain.aipubbackend.ArtifactService;
import io.ten1010.aipub.projectcontroller.domain.aipubbackend.OpenidProviderInfoService;
import io.ten1010.aipub.projectcontroller.domain.aipubbackend.RepositoryService;
import io.ten1010.aipub.projectcontroller.domain.aipubbackend.impl.ArtifactServiceImpl;
import io.ten1010.aipub.projectcontroller.domain.aipubbackend.impl.OpenidProviderInfoServiceImpl;
import io.ten1010.aipub.projectcontroller.domain.aipubbackend.impl.RepositoryServiceImpl;
import io.ten1010.aipub.projectcontroller.domain.k8s.DefaultSubjectResolver;
import io.ten1010.aipub.projectcontroller.domain.k8s.SubjectResolver;
import io.ten1010.common.apiclient.ApiClient;
import io.ten1010.common.apiclient.Authentication;
import io.ten1010.common.apiclient.HttpBasicAuthentication;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Objects;

@Configuration
public class AipubConfiguration {

    private final boolean aipubEnabled;
    @Nullable
    private final ApiClient aipubBackendClient;

    public AipubConfiguration(AipubProperties aipubProperties) {
        Objects.requireNonNull(aipubProperties.getEnabled());
        this.aipubEnabled = aipubProperties.getEnabled();

        if (this.aipubEnabled) {
            Objects.requireNonNull(aipubProperties.getServerUrl());
            Objects.requireNonNull(aipubProperties.getVerifyingSsl());
            Objects.requireNonNull(aipubProperties.getUsername());
            Objects.requireNonNull(aipubProperties.getPassword());

            ApiClient client = new ApiClient();
            client.setBasePath(aipubProperties.getServerUrl() + "/api/v1alpha1");
            client.setVerifyingSsl(aipubProperties.getVerifyingSsl());
            Authentication authentication = new HttpBasicAuthentication(aipubProperties.getUsername(), aipubProperties.getPassword());
            client.setAuthentication(authentication);

            this.aipubBackendClient = client;
        } else {
            this.aipubBackendClient = null;
        }
    }

    @Bean
    public SubjectResolver subjectResolver() {
        if (this.aipubEnabled) {
            OpenidProviderInfoService service = new OpenidProviderInfoServiceImpl(this.aipubBackendClient);
            return new AipubSubjectResolver(service);
        }
        return new DefaultSubjectResolver();
    }

    @Bean
    public RepositoryService repositoryService() {
        if (this.aipubEnabled) {
            return new RepositoryServiceImpl(this.aipubBackendClient);
        }
        return (namespacedId, options) -> List.of();
    }

    @Bean
    public ArtifactService artifactService() {
        if (this.aipubEnabled) {
            return new ArtifactServiceImpl(this.aipubBackendClient);
        }
        return (namespacedId, repositoryName, options) -> List.of();
    }

}
