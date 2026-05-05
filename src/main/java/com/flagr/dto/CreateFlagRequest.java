package com.flagr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public class CreateFlagRequest {

    @NotBlank
    private String client;

    @NotBlank
    private String name;

    @NotNull
    private Map<String, String> attributes;

    private String formulaString;

    private int rollout = 0;

    public CreateFlagRequest() {}

    public CreateFlagRequest(String client, String name, Map<String, String> attributes, String formulaString, int rollout) {
        this.client = client;
        this.name = name;
        this.attributes = attributes;
        this.formulaString = formulaString;
        this.rollout = rollout;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String client;
        private String name;
        private Map<String, String> attributes;
        private String formulaString;
        private int rollout = 0;

        public Builder client(String client) { this.client = client; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder attributes(Map<String, String> attributes) { this.attributes = attributes; return this; }
        public Builder formulaString(String formulaString) { this.formulaString = formulaString; return this; }
        public Builder rollout(int rollout) { this.rollout = rollout; return this; }

        public CreateFlagRequest build() {
            return new CreateFlagRequest(client, name, attributes, formulaString, rollout);
        }
    }

    public String getClient() { return client; }
    public void setClient(String client) { this.client = client; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Map<String, String> getAttributes() { return attributes; }
    public void setAttributes(Map<String, String> attributes) { this.attributes = attributes; }
    public String getFormulaString() { return formulaString; }
    public void setFormulaString(String formulaString) { this.formulaString = formulaString; }
    public int getRollout() { return rollout; }
    public void setRollout(int rollout) { this.rollout = rollout; }
}
