package com.flagr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public class EvaluateRequest {

    @NotBlank
    private String client;

    @NotBlank
    private String name;

    @NotNull
    private Map<String, Object> attributes;

    public EvaluateRequest() {}

    public EvaluateRequest(String client, String name, Map<String, Object> attributes) {
        this.client = client;
        this.name = name;
        this.attributes = attributes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String client;
        private String name;
        private Map<String, Object> attributes;

        public Builder client(String client) { this.client = client; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder attributes(Map<String, Object> attributes) { this.attributes = attributes; return this; }

        public EvaluateRequest build() {
            return new EvaluateRequest(client, name, attributes);
        }
    }

    public String getClient() { return client; }
    public void setClient(String client) { this.client = client; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Map<String, Object> getAttributes() { return attributes; }
    public void setAttributes(Map<String, Object> attributes) { this.attributes = attributes; }
}
