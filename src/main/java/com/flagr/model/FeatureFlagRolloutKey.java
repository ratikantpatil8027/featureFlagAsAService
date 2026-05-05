package com.flagr.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "feature_flag_rollout_key")
public class FeatureFlagRolloutKey {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_flag_id", nullable = false)
    private FeatureFlag featureFlag;

    @Column(name = "rollout_key", nullable = false)
    private String rolloutKey;

    @Column
    private int threshold = 0;

    public FeatureFlagRolloutKey() {}

    public FeatureFlagRolloutKey(UUID id, FeatureFlag featureFlag, String rolloutKey, int threshold) {
        this.id = id;
        this.featureFlag = featureFlag;
        this.rolloutKey = rolloutKey;
        this.threshold = threshold;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private FeatureFlag featureFlag;
        private String rolloutKey;
        private int threshold = 0;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder featureFlag(FeatureFlag featureFlag) { this.featureFlag = featureFlag; return this; }
        public Builder rolloutKey(String rolloutKey) { this.rolloutKey = rolloutKey; return this; }
        public Builder threshold(int threshold) { this.threshold = threshold; return this; }

        public FeatureFlagRolloutKey build() {
            return new FeatureFlagRolloutKey(id, featureFlag, rolloutKey, threshold);
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public FeatureFlag getFeatureFlag() { return featureFlag; }
    public void setFeatureFlag(FeatureFlag featureFlag) { this.featureFlag = featureFlag; }
    public String getRolloutKey() { return rolloutKey; }
    public void setRolloutKey(String rolloutKey) { this.rolloutKey = rolloutKey; }
    public int getThreshold() { return threshold; }
    public void setThreshold(int threshold) { this.threshold = threshold; }
}
