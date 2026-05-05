package com.flagr.repository;

import com.flagr.model.FeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, UUID> {
    Optional<FeatureFlag> findByClientAndName(String client, String name);
}
