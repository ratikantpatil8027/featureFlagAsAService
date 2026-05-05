package com.flagr.service;

import com.flagr.dto.EvaluateRequest;
import com.flagr.dto.EvaluateResponse;
import com.flagr.exception.FlagNotFoundException;
import com.flagr.model.FeatureFlag;
import com.flagr.model.FeatureFlagRolloutKey;
import com.flagr.repository.FeatureFlagRepository;
import com.flagr.repository.FeatureFlagRolloutKeyRepository;
import com.flagr.util.FormulaEvaluatorUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlagEvaluationServiceTest {

    @Mock
    private FeatureFlagRepository featureFlagRepository;

    @Mock
    private FeatureFlagRolloutKeyRepository featureFlagRolloutKeyRepository;

    @Mock
    private FormulaEvaluatorUtil formulaEvaluatorUtil;

    @InjectMocks
    private FlagEvaluationService flagEvaluationService;

    @Test
    void returnsFalseImmediatelyWhenFlagDisabled() {
        FeatureFlag flag = FeatureFlag.builder()
                .featureFlagId(UUID.randomUUID())
                .client("test")
                .name("flag")
                .enabled(false)
                .build();

        when(featureFlagRepository.findByClientAndName("test", "flag")).thenReturn(Optional.of(flag));

        EvaluateRequest request = EvaluateRequest.builder()
                .client("test")
                .name("flag")
                .attributes(Map.of("user_id", 1))
                .build();

        EvaluateResponse response = flagEvaluationService.evaluate(request);

        assertFalse(response.isEnabled());
        verify(featureFlagRolloutKeyRepository, never()).findByFeatureFlag(any());
    }

    @Test
    void returnsFalseWhenRolloutIs50AndHashIs60() {
        UUID flagId = UUID.randomUUID();
        FeatureFlag flag = FeatureFlag.builder()
                .featureFlagId(flagId)
                .client("test")
                .name("flag")
                .enabled(true)
                .rollout(50)
                .build();

        FeatureFlagRolloutKey rolloutKey = FeatureFlagRolloutKey.builder()
                .id(UUID.randomUUID())
                .featureFlag(flag)
                .threshold(50)
                .rolloutKey("somekey")
                .build();

        when(featureFlagRepository.findByClientAndName("test", "flag")).thenReturn(Optional.of(flag));
        when(featureFlagRolloutKeyRepository.findByFeatureFlag(flag)).thenReturn(Optional.of(rolloutKey));

        EvaluateRequest request = EvaluateRequest.builder()
                .client("test")
                .name("flag")
                .attributes(Map.of("user_id", 999))
                .build();

        EvaluateResponse response = flagEvaluationService.evaluate(request);

        assertFalse(response.isEnabled());
    }

    @Test
    void returnsTrueWhenRolloutIs50AndHashIs30() {
        UUID flagId = UUID.randomUUID();
        FeatureFlag flag = FeatureFlag.builder()
                .featureFlagId(flagId)
                .client("test")
                .name("flag")
                .enabled(true)
                .rollout(50)
                .build();

        FeatureFlagRolloutKey rolloutKey = FeatureFlagRolloutKey.builder()
                .id(UUID.randomUUID())
                .featureFlag(flag)
                .threshold(50)
                .rolloutKey("somekey")
                .build();

        when(featureFlagRepository.findByClientAndName("test", "flag")).thenReturn(Optional.of(flag));
        when(featureFlagRolloutKeyRepository.findByFeatureFlag(flag)).thenReturn(Optional.of(rolloutKey));

        EvaluateRequest request = EvaluateRequest.builder()
                .client("test")
                .name("flag")
                .attributes(Map.of("user_id", 1))
                .build();

        EvaluateResponse response = flagEvaluationService.evaluate(request);

        assertTrue(response.isEnabled());
    }

    @Test
    void createsNewRolloutKeyWhenNoneExists() {
        UUID flagId = UUID.randomUUID();
        FeatureFlag flag = FeatureFlag.builder()
                .featureFlagId(flagId)
                .client("test")
                .name("flag")
                .enabled(true)
                .rollout(50)
                .build();

        when(featureFlagRepository.findByClientAndName("test", "flag")).thenReturn(Optional.of(flag));
        when(featureFlagRolloutKeyRepository.findByFeatureFlag(flag)).thenReturn(Optional.empty());

        EvaluateRequest request = EvaluateRequest.builder()
                .client("test")
                .name("flag")
                .attributes(Map.of("user_id", 1))
                .build();

        flagEvaluationService.evaluate(request);

        verify(featureFlagRolloutKeyRepository).save(argThat(key ->
                key.getThreshold() == 50 && key.getRolloutKey() != null
        ));
    }

    @Test
    void returnsFalseWhenFormulaEvaluatesToFalse() {
        UUID flagId = UUID.randomUUID();
        FeatureFlag flag = FeatureFlag.builder()
                .featureFlagId(flagId)
                .client("test")
                .name("flag")
                .enabled(true)
                .rollout(0)
                .formulaString("region != 'Canada'")
                .build();

        when(featureFlagRepository.findByClientAndName("test", "flag")).thenReturn(Optional.of(flag));

        EvaluateRequest request = EvaluateRequest.builder()
                .client("test")
                .name("flag")
                .attributes(Map.of("region", "Canada"))
                .build();

        when(formulaEvaluatorUtil.evaluate("region != 'Canada'", request.getAttributes())).thenReturn(false);

        EvaluateResponse response = flagEvaluationService.evaluate(request);

        assertFalse(response.isEnabled());
    }

    @Test
    void returnsTrueWhenEnabledAndNoFormula() {
        UUID flagId = UUID.randomUUID();
        FeatureFlag flag = FeatureFlag.builder()
                .featureFlagId(flagId)
                .client("test")
                .name("flag")
                .enabled(true)
                .rollout(0)
                .build();

        when(featureFlagRepository.findByClientAndName("test", "flag")).thenReturn(Optional.of(flag));

        EvaluateRequest request = EvaluateRequest.builder()
                .client("test")
                .name("flag")
                .attributes(Map.of("user_id", 1))
                .build();

        EvaluateResponse response = flagEvaluationService.evaluate(request);

        assertTrue(response.isEnabled());
    }

    @Test
    void returnsTrueWhenEnabledAndFormulaEvaluatesTrue() {
        UUID flagId = UUID.randomUUID();
        FeatureFlag flag = FeatureFlag.builder()
                .featureFlagId(flagId)
                .client("test")
                .name("flag")
                .enabled(true)
                .rollout(0)
                .formulaString("region != 'Canada'")
                .build();

        when(featureFlagRepository.findByClientAndName("test", "flag")).thenReturn(Optional.of(flag));

        EvaluateRequest request = EvaluateRequest.builder()
                .client("test")
                .name("flag")
                .attributes(Map.of("region", "US"))
                .build();

        when(formulaEvaluatorUtil.evaluate("region != 'Canada'", request.getAttributes())).thenReturn(true);

        EvaluateResponse response = flagEvaluationService.evaluate(request);

        assertTrue(response.isEnabled());
    }

    @Test
    void stickyBehaviorIdenticalAttributesReturnSameResult() {
        UUID flagId = UUID.randomUUID();
        FeatureFlag flag = FeatureFlag.builder()
                .featureFlagId(flagId)
                .client("test")
                .name("flag")
                .enabled(true)
                .rollout(50)
                .build();

        FeatureFlagRolloutKey rolloutKey = FeatureFlagRolloutKey.builder()
                .id(UUID.randomUUID())
                .featureFlag(flag)
                .threshold(50)
                .rolloutKey("somekey")
                .build();

        when(featureFlagRepository.findByClientAndName("test", "flag")).thenReturn(Optional.of(flag));
        when(featureFlagRolloutKeyRepository.findByFeatureFlag(flag)).thenReturn(Optional.of(rolloutKey));

        EvaluateRequest request = EvaluateRequest.builder()
                .client("test")
                .name("flag")
                .attributes(Map.of("user_id", 42))
                .build();

        EvaluateResponse response1 = flagEvaluationService.evaluate(request);
        EvaluateResponse response2 = flagEvaluationService.evaluate(request);

        assertEquals(response1.isEnabled(), response2.isEnabled());
    }

    @Test
    void throwsFlagNotFoundExceptionWhenFlagAbsent() {
        when(featureFlagRepository.findByClientAndName("test", "flag")).thenReturn(Optional.empty());

        EvaluateRequest request = EvaluateRequest.builder()
                .client("test")
                .name("flag")
                .attributes(Map.of("user_id", 1))
                .build();

        assertThrows(FlagNotFoundException.class, () -> flagEvaluationService.evaluate(request));
    }
}
