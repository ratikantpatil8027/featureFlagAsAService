package com.flagr.service;

import com.flagr.dto.CreateFlagRequest;
import com.flagr.dto.FlagResponse;
import com.flagr.dto.UpdateFlagRequest;
import com.flagr.exception.FlagNotFoundException;
import com.flagr.exception.InvalidFlagOperationException;
import com.flagr.model.FeatureFlag;
import com.flagr.model.FeatureFlagRolloutKey;
import com.flagr.repository.FeatureFlagRepository;
import com.flagr.repository.FeatureFlagRolloutKeyRepository;
import com.flagr.util.FormulaEvaluatorUtil;
import com.flagr.util.RolloutKeyUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Service
public class FeatureFlagService {

    private final FeatureFlagRepository featureFlagRepository;
    private final FeatureFlagRolloutKeyRepository featureFlagRolloutKeyRepository;
    private final FormulaEvaluatorUtil formulaEvaluatorUtil;
    private final ObjectMapper objectMapper;

    public FeatureFlagService(FeatureFlagRepository featureFlagRepository,
                              FeatureFlagRolloutKeyRepository featureFlagRolloutKeyRepository,
                              FormulaEvaluatorUtil formulaEvaluatorUtil,
                              ObjectMapper objectMapper) {
        this.featureFlagRepository = featureFlagRepository;
        this.featureFlagRolloutKeyRepository = featureFlagRolloutKeyRepository;
        this.formulaEvaluatorUtil = formulaEvaluatorUtil;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public FlagResponse createFlag(CreateFlagRequest req) {
        if (req.getFormulaString() != null && !req.getFormulaString().isBlank()) {
            formulaEvaluatorUtil.validate(req.getFormulaString(), req.getAttributes());
        }

        String attributesJson;
        try {
            attributesJson = objectMapper.writeValueAsString(req.getAttributes());
        } catch (JsonProcessingException e) {
            throw new InvalidFlagOperationException("Invalid attributes JSON: " + e.getMessage());
        }

        FeatureFlag flag = FeatureFlag.builder()
                .client(req.getClient())
                .name(req.getName())
                .attributes(attributesJson)
                .formulaString(req.getFormulaString())
                .rollout(0)
                .enabled(req.getFormulaString() != null && !req.getFormulaString().isBlank())
                .build();

        flag = featureFlagRepository.save(flag);

        FeatureFlagRolloutKey rolloutKey = FeatureFlagRolloutKey.builder()
                .featureFlag(flag)
                .rolloutKey(RolloutKeyUtil.generateRolloutKey(Collections.emptyMap()))
                .threshold(0)
                .build();

        featureFlagRolloutKeyRepository.save(rolloutKey);

        return toFlagResponse(flag);
    }

    @Transactional
    public FlagResponse updateFlag(UUID featureFlagId, UpdateFlagRequest req) {
        FeatureFlag flag = featureFlagRepository.findById(featureFlagId)
                .orElseThrow(() -> new FlagNotFoundException("Flag not found with id: " + featureFlagId));

        if (req.getFormulaString() != null && !req.getFormulaString().equals(flag.getFormulaString())) {
            Map<String, String> schema = parseAttributes(flag.getAttributes());
            formulaEvaluatorUtil.validate(req.getFormulaString(), schema);
        }

        if (req.getClient() != null) {
            flag.setClient(req.getClient());
        }
        if (req.getName() != null) {
            flag.setName(req.getName());
        }
        if (req.getAttributes() != null) {
            try {
                flag.setAttributes(objectMapper.writeValueAsString(req.getAttributes()));
            } catch (JsonProcessingException e) {
                throw new InvalidFlagOperationException("Invalid attributes JSON: " + e.getMessage());
            }
        }
        if (req.getFormulaString() != null) {
            flag.setFormulaString(req.getFormulaString());
        }
        if (req.getRollout() != null) {
            flag.setRollout(req.getRollout());
        }

        featureFlagRepository.save(flag);
        return toFlagResponse(flag);
    }

    @Transactional
    public void deleteFlag(UUID featureFlagId) {
        FeatureFlag flag = featureFlagRepository.findById(featureFlagId)
                .orElseThrow(() -> new FlagNotFoundException("Flag not found with id: " + featureFlagId));

        featureFlagRolloutKeyRepository.findByFeatureFlag(flag)
                .ifPresent(featureFlagRolloutKeyRepository::delete);

        featureFlagRepository.delete(flag);
    }

    @Transactional
    public FlagResponse toggleFlag(UUID featureFlagId) {
        FeatureFlag flag = featureFlagRepository.findById(featureFlagId)
                .orElseThrow(() -> new FlagNotFoundException("Flag not found with id: " + featureFlagId));

        if (flag.getFormulaString() != null && !flag.getFormulaString().isBlank()) {
            throw new InvalidFlagOperationException("Cannot toggle a conditional flag");
        }

        flag.setEnabled(!flag.isEnabled());
        featureFlagRepository.save(flag);
        return toFlagResponse(flag);
    }

    private FlagResponse toFlagResponse(FeatureFlag flag) {
        return FlagResponse.builder()
                .featureFlagId(flag.getFeatureFlagId())
                .client(flag.getClient())
                .name(flag.getName())
                .attributes(flag.getAttributes())
                .formulaString(flag.getFormulaString())
                .rollout(flag.getRollout())
                .enabled(flag.isEnabled())
                .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> parseAttributes(String attributesJson) {
        try {
            String cleaned = attributesJson;
            if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
                cleaned = cleaned.substring(1, cleaned.length() - 1);
                cleaned = cleaned.replace("\\\"", "\"");
            }
            return objectMapper.readValue(cleaned, Map.class);
        } catch (JsonProcessingException e) {
            throw new InvalidFlagOperationException("Failed to parse attributes: " + e.getMessage());
        }
    }
}
