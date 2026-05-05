package com.flagr.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class RolloutRequest {

    @NotNull
    @Min(0)
    @Max(100)
    private int percentage;

    public RolloutRequest() {}

    public RolloutRequest(int percentage) {
        this.percentage = percentage;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int percentage;

        public Builder percentage(int percentage) { this.percentage = percentage; return this; }

        public RolloutRequest build() {
            return new RolloutRequest(percentage);
        }
    }

    public int getPercentage() { return percentage; }
    public void setPercentage(int percentage) { this.percentage = percentage; }
}
