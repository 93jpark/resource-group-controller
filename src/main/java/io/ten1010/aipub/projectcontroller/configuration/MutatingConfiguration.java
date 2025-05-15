package io.ten1010.aipub.projectcontroller.configuration;

import io.kubernetes.client.informer.SharedInformerFactory;
import io.ten1010.aipub.projectcontroller.controller.workload.PodNodesResolver;
import io.ten1010.aipub.projectcontroller.controller.workload.WorkloadControllerNodesResolver;
import io.ten1010.aipub.projectcontroller.domain.aipubbackend.ArtifactService;
import io.ten1010.aipub.projectcontroller.domain.aipubbackend.RepositoryService;
import io.ten1010.aipub.projectcontroller.domain.k8s.ReconciliationService;
import io.ten1010.aipub.projectcontroller.domain.k8s.SubjectResolver;
import io.ten1010.aipub.projectcontroller.mutating.AdmissionReviewController;
import io.ten1010.aipub.projectcontroller.mutating.RequestContentCachingFilter;
import io.ten1010.aipub.projectcontroller.mutating.service.*;
import org.springframework.beans.factory.annotation.Qualifier;
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
    public PodReviewHandler podReviewHandler(
            PodNodesResolver podNodesResolver, SharedInformerFactory globalSharedInformerFactory, ReconciliationService reconciliationService) {
        return new PodReviewHandler(podNodesResolver, globalSharedInformerFactory, reconciliationService);
    }

    @Bean
    public DeploymentReviewHandler deploymentReviewHandler(
            WorkloadControllerNodesResolver workloadControllerNodesResolver,
            SharedInformerFactory globalSharedInformerFactory,
            ReconciliationService reconciliationService) {
        return new DeploymentReviewHandler(workloadControllerNodesResolver, globalSharedInformerFactory, reconciliationService);
    }

    @Bean
    public NamespaceReviewHandler namespaceReviewHandler(
            SubjectResolver subjectResolver,
            @Qualifier("globalSharedInformerFactory") SharedInformerFactory globalSharedInformerFactory,
            @Qualifier("aipubSharedInformerFactory") SharedInformerFactory aipubSharedIndexerFactory) {
        return new NamespaceReviewHandler(subjectResolver, globalSharedInformerFactory, aipubSharedIndexerFactory);
    }

    @Bean
    public ProjectReviewHandler projectReviewHandler(
            SubjectResolver subjectResolver,
            @Qualifier("globalSharedInformerFactory") SharedInformerFactory globalSharedInformerFactory,
            @Qualifier("aipubSharedInformerFactory") SharedInformerFactory aipubSharedIndexerFactory) {
        return new ProjectReviewHandler(subjectResolver, globalSharedInformerFactory, aipubSharedIndexerFactory);
    }

    @Bean
    public ImageReviewReviewHandler imageReviewReviewHandler(
            RepositoryService repositoryService, ArtifactService artifactService, SharedInformerFactory globalSharedInformerFactory) {
        return new ImageReviewReviewHandler(repositoryService, artifactService, globalSharedInformerFactory);
    }

}
