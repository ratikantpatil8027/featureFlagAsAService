package com.flagr.service;

import com.flagr.dto.EvaluateRequest;
import com.flagr.dto.EvaluateResponse;
import com.flagr.exception.FlagNotFoundException;
import com.flagr.model.FeatureFlag;
import com.flagr.model.FeatureFlagRolloutKey;
import com.flagr.repository.FeatureFlagRepository;
import com.flagr.repository.FeatureFlagRolloutKeyRepository;
import com.flagr.util.FormulaEvaluatorUtil;
import com.flagr.util.RolloutKeyUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FlagEvaluationService {

    private final FeatureFlagRepository featureFlagRepository;
    private final FeatureFlagRolloutKeyRepository featureFlagRolloutKeyRepository;
    private final FormulaEvaluatorUtil formulaEvaluatorUtil;

    public FlagEvaluationService(FeatureFlagRepository featureFlagRepository,
                                 FeatureFlagRolloutKeyRepository featureFlagRolloutKeyRepository,
                                 FormulaEvaluatorUtil formulaEvaluatorUtil) {
        this.featureFlagRepository = featureFlagRepository;
        this.featureFlagRolloutKeyRepository = featureFlagRolloutKeyRepository;
        this.formulaEvaluatorUtil = formulaEvaluatorUtil;
    }

    @Transactional
    public EvaluateResponse evaluate(EvaluateRequest req) {
        FeatureFlag flag = featureFlagRepository.findByClientAndName(req.getClient(), req.getName())
                .orElseThrow(() -> new FlagNotFoundException(
                        "Flag not found for client: " + req.getClient() + ", name: " + req.getName()));

        if (!flag.isEnabled()) {
            return new EvaluateResponse(false);
        }

        if (flag.getRollout() > 0) {
            int hashVal = RolloutKeyUtil.computeRolloutHash(req.getAttributes());
            String key = RolloutKeyUtil.generateRolloutKey(req.getAttributes());

            FeatureFlagRolloutKey rolloutKey = featureFlagRolloutKeyRepository.findByFeatureFlag(flag).orElse(null);

            if (rolloutKey != null && hashVal >= rolloutKey.getThreshold()) {
                return new EvaluateResponse(false);
            }

            if (rolloutKey == null) {
                FeatureFlagRolloutKey newRolloutKey = FeatureFlagRolloutKey.builder()
                        .featureFlag(flag)
                        .rolloutKey(key)
                        .threshold(flag.getRollout())
                        .build();
                featureFlagRolloutKeyRepository.save(newRolloutKey);
            }
        }

        if (flag.getFormulaString() != null && !flag.getFormulaString().isBlank()) {
            boolean formulaResult = formulaEvaluatorUtil.evaluate(flag.getFormulaString(), req.getAttributes());
            if (!formulaResult) {
                return new EvaluateResponse(false);
            }
        }

        return new EvaluateResponse(true);
    }
}
