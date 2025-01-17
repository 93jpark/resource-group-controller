package io.ten1010.aipub.projectcontroller.configuration;

import io.kubernetes.client.informer.SharedInformerFactory;
import io.ten1010.aipub.projectcontroller.controller.workload.PodNodesResolver;
import io.ten1010.aipub.projectcontroller.domain.aipubbackend.ArtifactService;
import io.ten1010.aipub.projectcontroller.domain.aipubbackend.RepositoryService;
import io.ten1010.aipub.projectcontroller.domain.k8s.ReconciliationService;
import io.ten1010.aipub.projectcontroller.domain.k8s.SubjectResolver;
import io.ten1010.aipub.projectcontroller.mutating.AdmissionReviewController;
import io.ten1010.aipub.projectcontroller.mutating.RequestContentCachingFilter;
import io.ten1010.aipub.projectcontroller.mutating.service.*;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class MutatingConfiguration {

    @Bean
    public FilterRegistrationBean<RequestContentCachingFilter> requestContentCachingFilter() {
        FilterRegistrationBean<RequestContentCachingFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new RequestContentCachingFilter());
        registrationBean.addUrlPatterns(AdmissionReviewController.PATH);

        return registrationBean;
    }

    @Bean
    public AdmissionReviewService admissionReviewService(List<ReviewHandler> reviewHandlers) {
        return new AdmissionReviewService(new CompositeReviewHandler(reviewHandlers));
    }

    @Bean
    public PodReviewHandler podReviewHandler(PodNodesResolver podNodesResolver, ReconciliationService reconciliationService) {
        return new PodReviewHandler(podNodesResolver, reconciliationService);
    }

    @Bean
    public ProjectReviewHandler projectReviewHandler(SubjectResolver subjectResolver, SharedInformerFactory sharedInformerFactory) {
        return new ProjectReviewHandler(subjectResolver, sharedInformerFactory);
    }

    @Bean
    public ImageReviewReviewHandler imageReviewReviewHandler(
            RepositoryService repositoryService, ArtifactService artifactService, SharedInformerFactory sharedInformerFactory) {
        return new ImageReviewReviewHandler(repositoryService, artifactService, sharedInformerFactory);
    }

}
