package com.flagr.repository;

import com.flagr.model.FeatureFlag;
import com.flagr.model.FeatureFlagRolloutKey;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface FeatureFlagRolloutKeyRepository extends JpaRepository<FeatureFlagRolloutKey, UUID> {
    Optional<FeatureFlagRolloutKey> findByFeatureFlag(FeatureFlag featureFlag);
}
