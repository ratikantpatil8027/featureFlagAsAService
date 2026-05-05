package com.flagr.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "feature_flag", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"client", "name"})
})
public class FeatureFlag {

    @Id
    @GeneratedValue
    @Column(name = "feature_flag_id")
    private UUID featureFlagId;

    @Column(nullable = false)
    private String client;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "jsonb")
    private String attributes;

    @Column(name = "formula_string")
    private String formulaString;

    @Column
    private int rollout = 0;

    @Column
    private boolean enabled = false;

    public FeatureFlag() {}

    public FeatureFlag(UUID featureFlagId, String client, String name, String attributes,
                       String formulaString, int rollout, boolean enabled) {
        this.featureFlagId = featureFlagId;
        this.client = client;
        this.name = name;
        this.attributes = attributes;
        this.formulaString = formulaString;
        this.rollout = rollout;
        this.enabled = enabled;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID featureFlagId;
        private String client;
        private String name;
        private String attributes;
        private String formulaString;
        private int rollout = 0;
        private boolean enabled = false;

        public Builder featureFlagId(UUID featureFlagId) { this.featureFlagId = featureFlagId; return this; }
        public Builder client(String client) { this.client = client; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder attributes(String attributes) { this.attributes = attributes; return this; }
        public Builder formulaString(String formulaString) { this.formulaString = formulaString; return this; }
        public Builder rollout(int rollout) { this.rollout = rollout; return this; }
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }

        public FeatureFlag build() {
            return new FeatureFlag(featureFlagId, client, name, attributes, formulaString, rollout, enabled);
        }
    }

    public UUID getFeatureFlagId() { return featureFlagId; }
    public void setFeatureFlagId(UUID featureFlagId) { this.featureFlagId = featureFlagId; }
    public String getClient() { return client; }
    public void setClient(String client) { this.client = client; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAttributes() { return attributes; }
    public void setAttributes(String attributes) { this.attributes = attributes; }
    public String getFormulaString() { return formulaString; }
    public void setFormulaString(String formulaString) { this.formulaString = formulaString; }
    public int getRollout() { return rollout; }
    public void setRollout(int rollout) { this.rollout = rollout; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
