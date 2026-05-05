package com.flagr.service;

import com.flagr.dto.CreateFlagRequest;
import com.flagr.dto.FlagResponse;
import com.flagr.dto.UpdateFlagRequest;
import com.flagr.exception.FlagNotFoundException;
import com.flagr.exception.InvalidFlagOperationException;
import com.flagr.model.FeatureFlag;
import com.flagr.model.FeatureFlagRolloutKey;
import com.flagr.repository.FeatureFlagRepository;
import com.flagr.repository.FeatureFlagRolloutKeyRepository;
import com.flagr.util.FormulaEvaluatorUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeatureFlagServiceTest {

    @Mock
    private FeatureFlagRepository featureFlagRepository;

    @Mock
    private FeatureFlagRolloutKeyRepository featureFlagRolloutKeyRepository;

    private FeatureFlagService featureFlagService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        FormulaEvaluatorUtil formulaEvaluatorUtil = new FormulaEvaluatorUtil();
        featureFlagService = new FeatureFlagService(featureFlagRepository, featureFlagRolloutKeyRepository,
                formulaEvaluatorUtil, objectMapper);
    }

    @Test
    void createFlagPersistsFlagAndRolloutKeyAndReturnsNonNullId() {
        CreateFlagRequest request = CreateFlagRequest.builder()
                .client("test-client")
                .name("test-flag")
                .attributes(Map.of("user_id", "num"))
                .build();

        when(featureFlagRepository.save(any(FeatureFlag.class))).thenAnswer(invocation -> {
            FeatureFlag flag = invocation.getArgument(0);
            return FeatureFlag.builder()
                    .featureFlagId(UUID.randomUUID())
                    .client(flag.getClient())
                    .name(flag.getName())
                    .attributes(flag.getAttributes())
                    .formulaString(flag.getFormulaString())
                    .rollout(flag.getRollout())
                    .enabled(flag.isEnabled())
                    .build();
        });

        when(featureFlagRolloutKeyRepository.save(any(FeatureFlagRolloutKey.class))).thenAnswer(invocation -> {
            FeatureFlagRolloutKey key = invocation.getArgument(0);
            return FeatureFlagRolloutKey.builder()
                    .id(UUID.randomUUID())
                    .featureFlag(key.getFeatureFlag())
                    .rolloutKey(key.getRolloutKey())
                    .threshold(key.getThreshold())
                    .build();
        });

        FlagResponse response = featureFlagService.createFlag(request);

        assertNotNull(response.getFeatureFlagId());
        assertEquals("test-client", response.getClient());
        assertEquals("test-flag", response.getName());
        assertFalse(response.isEnabled());
        assertEquals(0, response.getRollout());
        verify(featureFlagRepository).save(any(FeatureFlag.class));
        verify(featureFlagRolloutKeyRepository).save(any(FeatureFlagRolloutKey.class));
    }

    @Test
    void createFlagWithInvalidFormulaThrowsBeforeSave() {
        CreateFlagRequest request = CreateFlagRequest.builder()
                .client("test-client")
                .name("test-flag")
                .attributes(Map.of("region", "string"))
                .formulaString("region @#$% invalid")
                .build();

        assertThrows(Exception.class, () -> featureFlagService.createFlag(request));

        verify(featureFlagRepository, never()).save(any());
        verify(featureFlagRolloutKeyRepository, never()).save(any());
    }

    @Test
    void toggleFlagFlipsEnabledFromFalseToTrueOnSimpleFlag() {
        UUID flagId = UUID.randomUUID();
        FeatureFlag flag = FeatureFlag.builder()
                .featureFlagId(flagId)
                .client("test-client")
                .name("simple-flag")
                .attributes("{\"user_id\":\"num\"}")
                .formulaString(null)
                .rollout(0)
                .enabled(false)
                .build();

        when(featureFlagRepository.findById(flagId)).thenReturn(Optional.of(flag));
        when(featureFlagRepository.save(any(FeatureFlag.class))).thenReturn(flag);

        FlagResponse response = featureFlagService.toggleFlag(flagId);

        assertTrue(response.isEnabled());
        verify(featureFlagRepository).save(argThat(f -> f.isEnabled()));
    }

    @Test
    void toggleFlagThrowsWhenFormulaStringPresent() {
        UUID flagId = UUID.randomUUID();
        FeatureFlag flag = FeatureFlag.builder()
                .featureFlagId(flagId)
                .client("test-client")
                .name("conditional-flag")
                .attributes("{\"region\":\"string\"}")
                .formulaString("region != 'Canada'")
                .rollout(0)
                .enabled(false)
                .build();

        when(featureFlagRepository.findById(flagId)).thenReturn(Optional.of(flag));

        assertThrows(InvalidFlagOperationException.class, () -> featureFlagService.toggleFlag(flagId));
        verify(featureFlagRepository, never()).save(any());
    }

    @Test
    void deleteFlagThrowsForUnknownUuid() {
        UUID unknownId = UUID.randomUUID();
        when(featureFlagRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(FlagNotFoundException.class, () -> featureFlagService.deleteFlag(unknownId));
        verify(featureFlagRepository, never()).delete(any());
    }

    @Test
    void updateFlagUpdatesFields() {
        UUID flagId = UUID.randomUUID();
        FeatureFlag flag = FeatureFlag.builder()
                .featureFlagId(flagId)
                .client("old-client")
                .name("old-name")
                .attributes("{\"region\":\"string\"}")
                .formulaString("region != 'Canada'")
                .rollout(0)
                .enabled(false)
                .build();

        when(featureFlagRepository.findById(flagId)).thenReturn(Optional.of(flag));
        when(featureFlagRepository.save(any(FeatureFlag.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateFlagRequest request = UpdateFlagRequest.builder()
                .name("new-name")
                .build();

        FlagResponse response = featureFlagService.updateFlag(flagId, request);

        assertEquals("new-name", response.getName());
        assertEquals("old-client", response.getClient());
        verify(featureFlagRepository).save(any(FeatureFlag.class));
    }
}
