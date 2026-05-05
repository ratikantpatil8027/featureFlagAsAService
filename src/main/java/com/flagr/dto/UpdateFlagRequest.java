package com.flagr.dto;

import java.util.Map;

public class UpdateFlagRequest {

    private String client;
    private String name;
    private Map<String, String> attributes;
    private String formulaString;
    private Integer rollout;

    public UpdateFlagRequest() {}

    public UpdateFlagRequest(String client, String name, Map<String, String> attributes, String formulaString, Integer rollout) {
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
        private Integer rollout;

        public Builder client(String client) { this.client = client; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder attributes(Map<String, String> attributes) { this.attributes = attributes; return this; }
        public Builder formulaString(String formulaString) { this.formulaString = formulaString; return this; }
        public Builder rollout(Integer rollout) { this.rollout = rollout; return this; }

        public UpdateFlagRequest build() {
            return new UpdateFlagRequest(client, name, attributes, formulaString, rollout);
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
    public Integer getRollout() { return rollout; }
    public void setRollout(Integer rollout) { this.rollout = rollout; }
}
