package io.ten1010.aipub.projectcontroller.mutating.service;

import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.ten1010.aipub.projectcontroller.domain.k8s.ConfigMapConstants;
import io.ten1010.aipub.projectcontroller.domain.k8s.K8sGroupConstants;
import io.ten1010.aipub.projectcontroller.domain.k8s.K8sObjectTypeConstants;
import io.ten1010.aipub.projectcontroller.domain.k8s.KeyResolver;
import io.ten1010.aipub.projectcontroller.domain.k8s.NamespaceConstants;
import io.ten1010.aipub.projectcontroller.domain.k8s.SubjectResolver;
import io.ten1010.aipub.projectcontroller.domain.k8s.util.ConfigMapUtils;
import io.ten1010.aipub.projectcontroller.domain.k8s.util.K8sObjectUtils;
import io.ten1010.aipub.projectcontroller.mutating.V1AdmissionReviewUtils;
import io.ten1010.aipub.projectcontroller.mutating.dto.V1AdmissionReview;
import io.ten1010.aipub.projectcontroller.mutating.dto.V1UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class NamespaceReviewHandler extends AbstractReviewHandler<V1Namespace> {

    private final KeyResolver keyResolver;
    private final SubjectResolver subjectResolver;
    private final Indexer<V1ConfigMap> configMapIndexer;
    private final Indexer<V1Namespace> namespaceIndexer;

    public NamespaceReviewHandler(SubjectResolver subjectResolver, SharedInformerFactory globalSharedInformerFactory, SharedInformerFactory aipubSharedInformerFactory) {
        super(K8sObjectTypeConstants.NAMESPACE_V1);
        this.keyResolver = new KeyResolver();
        this.subjectResolver = subjectResolver;
        this.configMapIndexer = aipubSharedInformerFactory
                .getExistingSharedIndexInformer(V1ConfigMap.class)
                .getIndexer();
        this.namespaceIndexer = globalSharedInformerFactory
                .getExistingSharedIndexInformer(V1Namespace.class)
                .getIndexer();
    }

    @Override
    public void handle(V1AdmissionReview review) {
        Objects.requireNonNull(review.getRequest());
        Objects.requireNonNull(review.getRequest().getUserInfo());

        V1Namespace namespace = this.namespaceIndexer.getByKey(this.keyResolver.resolveKey(getNamespaceName(review)));
        String nsName = K8sObjectUtils.getName(namespace);
        if (isReservedName(nsName)) {
            log.info("4. Namespace {} is reserved", nsName);
            V1UserInfo userInfo = review.getRequest().getUserInfo();
            if (userInfo.getGroups() != null &&
                    (userInfo.getGroups().contains(K8sGroupConstants.SYSTEM_MASTERS_GROUP_NAME) && !userInfo.getGroups().contains(K8sGroupConstants.AIPUB_ADMIN_GROUP_NAME))) {
                log.info("5. allowed namespace delete because requester is system admin");
                V1AdmissionReviewUtils.allow(review);
                return;
            }
            log.info("6. allowed namespace delete because requester is system admin");
            V1AdmissionReviewUtils.reject(review, HttpStatus.FORBIDDEN.value(), String.format("%s is reserved name", nsName));
            return;
        }

        V1AdmissionReviewUtils.allow(review);
    }

    private String getNamespaceName(V1AdmissionReview review) {
        Objects.requireNonNull(review.getRequest());
        Objects.requireNonNull(review.getRequest().getName());

        return review.getRequest().getName();
    }

    private boolean isReservedName(String name) {
        Optional<V1ConfigMap> configMapOpt = Optional.ofNullable(this.configMapIndexer.getByKey(this.keyResolver.resolveKey(NamespaceConstants.AIPUB_NAMESPACE, ConfigMapConstants.AIPUB_CONFIGMAP_NAME)));
        if (configMapOpt.isEmpty()) {
            log.info("1. ConfigMap not found");
            return false;
        }
        V1ConfigMap configMap = configMapOpt.get();
        HashMap<String, String> data = ConfigMapUtils.getData(configMap, ConfigMapConstants.RESERVED_NAMESPACES_DATA_KEY);
        if (data.isEmpty()) {
            log.info("2. ConfigMap Data not found");
            return false;
        }
        Set<String> reservedNamespaces = StringUtils.commaDelimitedListToSet(data.get(ConfigMapConstants.RESERVED_NAMESPACES_DATA_KEY));
        if (reservedNamespaces.isEmpty()) {
            log.info("3. ConfigMap data is empty");
            return false;
        }
        return reservedNamespaces
                .stream()
                .anyMatch(line -> line.equals(name));
    }

}
