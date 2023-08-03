package com.sequenceiq.cloudbreak.rotation.service.multicluster;

import static com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResourceType.INITIATED_PARENT;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.rotation.MultiSecretType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.entity.multicluster.MultiClusterRotationResource;

@Service
public class MultiClusterRotationValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiClusterRotationValidationService.class);

    @Inject
    private Map<SecretType, RotationContextProvider> rotationContextProviderMap;

    @Inject
    private MultiClusterRotationService multiClusterRotationService;

    @Inject
    private Optional<InterServiceMultiClusterRotationService> interServiceMultiClusterRotationTrackingService;

    public void validateMultiRotationRequest(String resourceCrn, MultiSecretType inputMultiSecretType) {
        CrnResourceDescriptor crnResourceDescriptor = CrnResourceDescriptor.getByCrnString(resourceCrn);
        Set<MultiClusterRotationResource> multiClusterRotationResources = multiClusterRotationService.getMultiRotationEntriesForResource(resourceCrn);
        validateMultiRotationInput(inputMultiSecretType, multiClusterRotationResources);
        if (crnResourceDescriptor.equals(inputMultiSecretType.parentCrnDescriptor())) {
            validateParentRotation(resourceCrn, multiClusterRotationResources, inputMultiSecretType);
        } else if (inputMultiSecretType.childSecretTypesByDescriptor().containsKey(crnResourceDescriptor)) {
            validateChildRotation(resourceCrn, multiClusterRotationResources);
        } else {
            throw new CloudbreakServiceException("Type of resource is not matching with the given secret type.");
        }
    }

    private void validateMultiRotationInput(MultiSecretType multiSecretType, Set<MultiClusterRotationResource> multiClusterRotationResources) {
        if (multiClusterRotationResources.stream()
                .anyMatch(multiRotationResource -> !multiRotationResource.getSecretType().equals(multiSecretType))) {
            throw new BadRequestException("There is another ongoing multi cluster rotation for the resource, thus rotation is not possible.");
        }
    }

    private void validateParentRotation(String resourceCrn, Set<MultiClusterRotationResource> multiClusterRotationResources, MultiSecretType multiSecretType) {
        if (noOngoingMultiRotation(multiClusterRotationResources)) {
            LOGGER.info("Rotation for parent resource {} is ready to be started.", resourceCrn);
        } else if (parentRotationInitiatedAndChildrenStillPending(resourceCrn, multiClusterRotationResources, multiSecretType)) {
            throw new BadRequestException(
                    String.format("Rotation for parent resource %s cannot be finalized, since children rotation already needed.", resourceCrn));
        }
    }

    private boolean parentRotationInitiatedAndChildrenStillPending(String resourceCrn, Set<MultiClusterRotationResource> multiClusterRotationResources,
            MultiSecretType multiSecretType) {
        return multiClusterRotationResources.stream().anyMatch(multiRotationResource -> INITIATED_PARENT.equals(multiRotationResource.getType()))
                && interServiceMultiClusterRotationTrackingService.isPresent()
                && interServiceMultiClusterRotationTrackingService.get().checkOngoingChildrenMultiSecretRotations(resourceCrn, multiSecretType);
    }

    private void validateChildRotation(String resourceCrn, Set<MultiClusterRotationResource> multiClusterRotationResources) {
        if (noOngoingMultiRotation(multiClusterRotationResources)) {
            throw new BadRequestException(String.format("Resource %s is not marked as rotatable cluster, thus rotation not needed.", resourceCrn));
        }
    }

    private static boolean noOngoingMultiRotation(Set<MultiClusterRotationResource> multiClusterRotationResources) {
        return multiClusterRotationResources.isEmpty();
    }
}