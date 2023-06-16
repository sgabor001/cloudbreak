package com.sequenceiq.datalake.service.rotation;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.DATALAKE_EXTERNAL_DATABASE_ROOT_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.secret.RotationFlowExecutionType.FINALIZE;
import static com.sequenceiq.cloudbreak.rotation.secret.RotationFlowExecutionType.ROLLBACK;
import static com.sequenceiq.cloudbreak.rotation.secret.RotationFlowExecutionType.ROTATE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.context.PollerRotationContext;

@ExtendWith(MockitoExtension.class)
class CloudbreakPollerRotationExecutorTest {

    private static final String RESOURCE_CRN = "resourceCrn";

    @Mock
    private SdxRotationService sdxRotationService;

    @InjectMocks
    private CloudbreakPollerRotationExecutor underTest;

    @Test
    void rotateShouldThrowSecretRotationExceptionIfCloudbreakRotationFailed() {
        doThrow(new RuntimeException("error")).when(sdxRotationService).rotateCloudbreakSecret(anyString(),
                eq(DATALAKE_EXTERNAL_DATABASE_ROOT_PASSWORD), eq(ROTATE));
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.executeRotate(new PollerRotationContext(RESOURCE_CRN, DATALAKE_EXTERNAL_DATABASE_ROOT_PASSWORD)));
        Assertions.assertEquals("Rotation failed at CLOUDBREAK_ROTATE_POLLING step for resourceCrn", secretRotationException.getMessage());
    }

    @Test
    void rotateShouldSucceed() {
        underTest.executeRotate(new PollerRotationContext(RESOURCE_CRN, DATALAKE_EXTERNAL_DATABASE_ROOT_PASSWORD));
        verify(sdxRotationService, times(1)).rotateCloudbreakSecret(eq(RESOURCE_CRN),
                eq(DATALAKE_EXTERNAL_DATABASE_ROOT_PASSWORD), eq(ROTATE));
    }

    @Test
    void rollbackShouldThrowSecretRotationExceptionIfCloudbreakRollbackFailed() {
        doThrow(new RuntimeException("error")).when(sdxRotationService).rotateCloudbreakSecret(anyString(),
                eq(DATALAKE_EXTERNAL_DATABASE_ROOT_PASSWORD), eq(ROLLBACK));
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.executeRollback(new PollerRotationContext(RESOURCE_CRN, DATALAKE_EXTERNAL_DATABASE_ROOT_PASSWORD)));
        Assertions.assertEquals("Rollback of rotation failed at CLOUDBREAK_ROTATE_POLLING step for resourceCrn", secretRotationException.getMessage());
    }

    @Test
    void rollbackShouldSucceed() {
        underTest.executeRollback(new PollerRotationContext(RESOURCE_CRN, DATALAKE_EXTERNAL_DATABASE_ROOT_PASSWORD));
        verify(sdxRotationService, times(1)).rotateCloudbreakSecret(eq(RESOURCE_CRN),
                eq(DATALAKE_EXTERNAL_DATABASE_ROOT_PASSWORD), eq(ROLLBACK));
    }

    @Test
    void finalizeShouldThrowSecretRotationExceptionIfCloudbreakFinalizeFailed() {
        doThrow(new RuntimeException("error")).when(sdxRotationService).rotateCloudbreakSecret(anyString(),
                eq(DATALAKE_EXTERNAL_DATABASE_ROOT_PASSWORD), eq(FINALIZE));
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.executeFinalize(new PollerRotationContext(RESOURCE_CRN, DATALAKE_EXTERNAL_DATABASE_ROOT_PASSWORD)));
        Assertions.assertEquals("Finalize rotation failed at CLOUDBREAK_ROTATE_POLLING step for resourceCrn", secretRotationException.getMessage());
    }

    @Test
    void finalizeShouldSucceed() {
        underTest.executeFinalize(new PollerRotationContext(RESOURCE_CRN, DATALAKE_EXTERNAL_DATABASE_ROOT_PASSWORD));
        verify(sdxRotationService, times(1)).rotateCloudbreakSecret(eq(RESOURCE_CRN),
                eq(DATALAKE_EXTERNAL_DATABASE_ROOT_PASSWORD), eq(FINALIZE));
    }

}