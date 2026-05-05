package com.flagr.controller;

import com.flagr.dto.EvaluateRequest;
import com.flagr.dto.EvaluateResponse;
import com.flagr.service.FlagEvaluationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/flags")
public class FlagEvaluationController {

    private final FlagEvaluationService flagEvaluationService;

    public FlagEvaluationController(FlagEvaluationService flagEvaluationService) {
        this.flagEvaluationService = flagEvaluationService;
    }

    @PostMapping("/evaluate")
    public ResponseEntity<EvaluateResponse> evaluate(@Valid @RequestBody EvaluateRequest request) {
        EvaluateResponse response = flagEvaluationService.evaluate(request);
        return ResponseEntity.ok(response);
    }
}
