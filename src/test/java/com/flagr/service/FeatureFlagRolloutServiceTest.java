package com.flagr.service;

import com.flagr.exception.FlagNotFoundException;
import com.flagr.exception.InvalidFlagOperationException;
import com.flagr.model.FeatureFlag;
import com.flagr.model.FeatureFlagRolloutKey;
import com.flagr.repository.FeatureFlagRepository;
import com.flagr.repository.FeatureFlagRolloutKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeatureFlagRolloutServiceTest {

    @Mock
    private FeatureFlagRepository featureFlagRepository;

    @Mock
    private FeatureFlagRolloutKeyRepository featureFlagRolloutKeyRepository;

    @InjectMocks
    private FeatureFlagRolloutService featureFlagRolloutService;

    @Test
    void setRolloutValidPercentage() {
        UUID flagId = UUID.randomUUID();
        FeatureFlag flag = FeatureFlag.builder()
                .featureFlagId(flagId)
                .client("test")
                .name("test-flag")
                .rollout(0)
                .build();

        FeatureFlagRolloutKey rolloutKey = FeatureFlagRolloutKey.builder()
                .id(UUID.randomUUID())
                .featureFlag(flag)
                .threshold(0)
                .rolloutKey("abc123")
                .build();

        when(featureFlagRepository.findById(flagId)).thenReturn(Optional.of(flag));
        when(featureFlagRolloutKeyRepository.findByFeatureFlag(flag)).thenReturn(Optional.of(rolloutKey));

        featureFlagRolloutService.setRollout(flagId, 50);

        assertEquals(50, flag.getRollout());
        assertEquals(50, rolloutKey.getThreshold());
        verify(featureFlagRepository).save(flag);
        verify(featureFlagRolloutKeyRepository).save(rolloutKey);
    }

    @Test
    void setRolloutThrowsForNegativePercentage() {
        assertThrows(InvalidFlagOperationException.class, () -> featureFlagRolloutService.setRollout(UUID.randomUUID(), -1));
        verify(featureFlagRepository, never()).findById(any());
    }

    @Test
    void setRolloutThrowsForPercentageOver100() {
        assertThrows(InvalidFlagOperationException.class, () -> featureFlagRolloutService.setRollout(UUID.randomUUID(), 101));
        verify(featureFlagRepository, never()).findById(any());
    }

    @Test
    void setRolloutThrowsForUnknownFlag() {
        UUID unknownId = UUID.randomUUID();
        when(featureFlagRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(FlagNotFoundException.class, () -> featureFlagRolloutService.setRollout(unknownId, 50));
    }
}
