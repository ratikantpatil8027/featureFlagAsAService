package com.flagr.service;

import com.flagr.exception.FlagNotFoundException;
import com.flagr.exception.InvalidFlagOperationException;
import com.flagr.model.FeatureFlag;
import com.flagr.model.FeatureFlagRolloutKey;
import com.flagr.repository.FeatureFlagRepository;
import com.flagr.repository.FeatureFlagRolloutKeyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class FeatureFlagRolloutService {

    private final FeatureFlagRepository featureFlagRepository;
    private final FeatureFlagRolloutKeyRepository featureFlagRolloutKeyRepository;

    public FeatureFlagRolloutService(FeatureFlagRepository featureFlagRepository,
                                     FeatureFlagRolloutKeyRepository featureFlagRolloutKeyRepository) {
        this.featureFlagRepository = featureFlagRepository;
        this.featureFlagRolloutKeyRepository = featureFlagRolloutKeyRepository;
    }

    @Transactional
    public void setRollout(UUID featureFlagId, int percentage) {
        if (percentage < 0 || percentage > 100) {
            throw new InvalidFlagOperationException("Rollout percentage must be between 0 and 100");
        }

        FeatureFlag flag = featureFlagRepository.findById(featureFlagId)
                .orElseThrow(() -> new FlagNotFoundException("Flag not found with id: " + featureFlagId));

        flag.setRollout(percentage);
        featureFlagRepository.save(flag);

        FeatureFlagRolloutKey rolloutKey = featureFlagRolloutKeyRepository.findByFeatureFlag(flag)
                .orElseThrow(() -> new FlagNotFoundException("Rollout key not found for flag: " + featureFlagId));

        rolloutKey.setThreshold(percentage);
        featureFlagRolloutKeyRepository.save(rolloutKey);
    }
}
