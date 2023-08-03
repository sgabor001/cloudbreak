package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor.DATALAKE;
import static com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor.ENVIRONMENT;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.projection.StackIdView;
import com.sequenceiq.cloudbreak.rotation.MultiSecretType;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.SecretTypeConverter;
import com.sequenceiq.cloudbreak.rotation.service.multicluster.MultiClusterRotationService;
import com.sequenceiq.cloudbreak.rotation.service.multicluster.MultiClusterRotationValidationService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class StackRotationService {

    @Inject
    private ReactorFlowManager flowManager;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private StackService stackService;

    @Inject
    private MultiClusterRotationValidationService multiClusterRotationValidationService;

    @Inject
    private MultiClusterRotationService multiClusterRotationService;

    public FlowIdentifier rotateSecrets(String crn, List<String> secrets, RotationFlowExecutionType executionType) {
        if (entitlementService.isSecretRotationEnabled(Crn.fromString(crn).getAccountId())) {
            StackView viewByCrn = stackDtoService.getStackViewByCrn(crn);
            Long stackId = viewByCrn.getId();
            List<SecretType> secretTypes = SecretTypeConverter.mapSecretTypes(secrets);
            return flowManager.triggerSecretRotation(stackId, crn, secretTypes, executionType);
        } else {
            throw new CloudbreakServiceException("Account is not entitled to execute any secret rotation!");
        }
    }

    public FlowIdentifier rotateMultiSecrets(String crn, String secret) {
        if (entitlementService.isSecretRotationEnabled(Crn.safeFromString(crn).getAccountId())) {
            MultiSecretType multiSecretType = SecretTypeConverter.mapMultiSecretType(secret);
            StackView viewByCrn = stackDtoService.getStackViewByCrn(crn);
            Long stackId = viewByCrn.getId();
            multiClusterRotationValidationService.validateMultiRotationRequest(crn, multiSecretType);
            return flowManager.triggerSecretRotation(stackId, crn, List.of(multiSecretType.getSecretTypeByResourceCrn(crn)), null);
        } else {
            throw new CloudbreakServiceException("Account is not entitled to execute any secret rotation!");
        }
    }

    public boolean checkOngoingChildrenMultiSecretRotations(String parentCrn, String secret) {
        Set<String> crns = getCrnsByParentCrn(parentCrn);
        MultiSecretType multiSecretType = SecretTypeConverter.mapMultiSecretType(secret);
        return CollectionUtils.isNotEmpty(multiClusterRotationService.getMultiRotationEntriesForSecretAndResources(multiSecretType, crns));
    }

    public void markMultiClusterChildrenResources(String parentCrn, String secret) {
        Set<String> crns = getCrnsByParentCrn(parentCrn);
        multiClusterRotationService.markChildrenMultiRotationEntriesLocally(crns, secret);
    }

    private Set<String> getCrnsByParentCrn(String parentCrn) {
        Set<String> crns = Set.of();
        if (CrnResourceDescriptor.getByCrnString(parentCrn).equals(ENVIRONMENT)) {
            crns = stackService.getByEnvironmentCrnAndStackType(parentCrn, StackType.WORKLOAD).stream().map(StackIdView::getCrn).collect(Collectors.toSet());
        } else if (CrnResourceDescriptor.getByCrnString(parentCrn).equals(DATALAKE)) {
            crns = stackService.findByDatalakeCrn(parentCrn).stream().map(StackIdView::getCrn).collect(Collectors.toSet());
        }
        return crns;
    }
}