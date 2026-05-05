package com.flagr.dto;

public class EvaluateResponse {

    private boolean enabled;

    public EvaluateResponse() {}

    public EvaluateResponse(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
