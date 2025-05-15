package io.ten1010.aipub.projectcontroller.informer;

import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ConfigMapList;
import io.kubernetes.client.util.CallGeneratorParams;
import io.ten1010.aipub.projectcontroller.domain.k8s.ConfigMapConstants;
import io.ten1010.aipub.projectcontroller.domain.k8s.K8sApiProvider;
import io.ten1010.aipub.projectcontroller.domain.k8s.KeyResolver;
import io.ten1010.aipub.projectcontroller.domain.k8s.NamespaceConstants;

import java.util.List;

public class AipubSharedInformerFactoryProvider {

    private static final long DEFAULT_RESYNC_PERIOD = 0;

    private final KeyResolver keyResolver;
    private final K8sApiProvider k8sApiProvider;

    public AipubSharedInformerFactoryProvider(K8sApiProvider k8sApiProvider, List<InformerRegistrar> registrars) {
        this.keyResolver = new KeyResolver();
        this.k8sApiProvider = k8sApiProvider;
    }

    public SharedInformerFactory createSharedInformerFactory() {
        SharedInformerFactory informerFactory = new SharedInformerFactory(this.k8sApiProvider.getApiClient());
        registerConfigMapInformer(informerFactory);
        informerFactory.startAllRegisteredInformers();
        return informerFactory;
    }

    private void registerConfigMapInformer(SharedInformerFactory informerFactory) {
        ApiClient apiClient = this.k8sApiProvider.getApiClient();
        informerFactory.sharedIndexInformerFor(
                (CallGeneratorParams params) -> new CoreV1Api(apiClient).listNamespacedConfigMap(NamespaceConstants.AIPUB_NAMESPACE)
                        .fieldSelector("metadata.name=" + ConfigMapConstants.AIPUB_CONFIGMAP_NAME)
                        .resourceVersion(params.resourceVersion)
                        .watch(params.watch)
                        .timeoutSeconds(params.timeoutSeconds)
                        .buildCall(null),
                V1ConfigMap.class,
                V1ConfigMapList.class);
    }

}
