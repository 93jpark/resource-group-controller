package io.ten1010.aipub.projectcontroller.configuration;

import io.kubernetes.client.informer.SharedInformerFactory;
import io.ten1010.aipub.projectcontroller.domain.k8s.K8sApiProvider;
import io.ten1010.aipub.projectcontroller.informer.AipubSharedInformerFactoryProvider;
import io.ten1010.aipub.projectcontroller.informer.GlobalSharedInformerFactoryProvider;
import io.ten1010.aipub.projectcontroller.informer.InformerRegistrar;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class InformerConfiguration {

    @Bean(name = "globalSharedInformerFactory")
    public SharedInformerFactory globalSharedInformerFactory(K8sApiProvider k8sApiProvider, List<InformerRegistrar> registrars) {
        return new GlobalSharedInformerFactoryProvider(k8sApiProvider, registrars).createSharedInformerFactory();
    }

    @Bean(name = "aipubSharedInformerFactory")
    public SharedInformerFactory aipubSharedInformerFactory(K8sApiProvider k8sApiProvider, List<InformerRegistrar> registrars) {
        return new AipubSharedInformerFactoryProvider(k8sApiProvider, registrars).createSharedInformerFactory();
    }

}
