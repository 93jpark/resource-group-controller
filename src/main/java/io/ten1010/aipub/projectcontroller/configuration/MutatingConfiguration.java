package io.ten1010.aipub.projectcontroller.configuration;

import io.ten1010.aipub.projectcontroller.mutating.AdmissionReviewService;
import io.ten1010.aipub.projectcontroller.controller.Reconciliation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MutatingConfiguration {

    @Bean
    public AdmissionReviewService admissionReviewService(Reconciliation reconciliation) {
        return new AdmissionReviewService(reconciliation);
    }

}
