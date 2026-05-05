package com.flagr.controller;

import com.flagr.dto.CreateFlagRequest;
import com.flagr.dto.FlagResponse;
import com.flagr.dto.RolloutRequest;
import com.flagr.dto.UpdateFlagRequest;
import com.flagr.service.FeatureFlagRolloutService;
import com.flagr.service.FeatureFlagService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/flags")
public class FeatureFlagController {

    private final FeatureFlagService featureFlagService;
    private final FeatureFlagRolloutService featureFlagRolloutService;

    public FeatureFlagController(FeatureFlagService featureFlagService, FeatureFlagRolloutService featureFlagRolloutService) {
        this.featureFlagService = featureFlagService;
        this.featureFlagRolloutService = featureFlagRolloutService;
    }

    @PostMapping
    public ResponseEntity<FlagResponse> createFlag(@Valid @RequestBody CreateFlagRequest request) {
        FlagResponse response = featureFlagService.createFlag(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FlagResponse> updateFlag(@PathVariable UUID id,
                                                    @Valid @RequestBody UpdateFlagRequest request) {
        FlagResponse response = featureFlagService.updateFlag(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFlag(@PathVariable UUID id) {
        featureFlagService.deleteFlag(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<FlagResponse> toggleFlag(@PathVariable UUID id) {
        FlagResponse response = featureFlagService.toggleFlag(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/rollout")
    public ResponseEntity<Void> setRollout(@PathVariable UUID id,
                                            @Valid @RequestBody RolloutRequest request) {
        featureFlagRolloutService.setRollout(id, request.getPercentage());
        return ResponseEntity.ok().build();
    }
}
