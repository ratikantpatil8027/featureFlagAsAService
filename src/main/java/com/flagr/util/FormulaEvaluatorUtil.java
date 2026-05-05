package com.flagr.util;

import com.flagr.exception.InvalidFlagOperationException;
import org.mvel2.MVEL;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility for evaluating feature flag formulas using MVEL2.
 *
 * Supported operators: IN, NOT IN, ==, !=, &&, ||, >, <, >=, <=
 */
@Component
public class FormulaEvaluatorUtil {

    /**
     * Validates a formula string against an attribute schema by performing a dry-run evaluation.
     *
     * @param formulaString the formula to validate
     * @param attributeSchema map of attribute names to types ("num" or "string")
     * @throws InvalidFlagOperationException if the formula is invalid
     */
    public void validate(String formulaString, Map<String, String> attributeSchema) {
        Map<String, Object> context = new HashMap<>();
        for (Map.Entry<String, String> entry : attributeSchema.entrySet()) {
            if ("num".equals(entry.getValue())) {
                context.put(entry.getKey(), 0);
            } else {
                context.put(entry.getKey(), "");
            }
        }

        try {
            String processed = preprocessFormula(formulaString);
            MVEL.eval(processed, context);
        } catch (Exception e) {
            throw new InvalidFlagOperationException("Invalid formula: " + e.getMessage());
        }
    }

    /**
     * Evaluates a formula string against actual attribute values.
     *
     * @param formulaString the formula to evaluate
     * @param attributeValues map of attribute names to their actual values
     * @return boolean result of formula evaluation
     * @throws InvalidFlagOperationException if evaluation fails
     */
    public boolean evaluate(String formulaString, Map<String, Object> attributeValues) {
        try {
            String processed = preprocessFormula(formulaString);
            Object result = MVEL.eval(processed, new HashMap<>(attributeValues));
            return (Boolean) result;
        } catch (Exception e) {
            throw new InvalidFlagOperationException("Formula evaluation failed: " + e.getMessage());
        }
    }

    private String preprocessFormula(String formula) {
        String result = formula;
        result = result.replaceAll("(\\w+)\\s+not\\s+in\\s+\\{([^}]+)\\}", "!java.util.Arrays.asList($2).contains($1)");
        result = result.replaceAll("(\\w+)\\s+not\\s+in\\s+\\[([^\\]]+)\\]", "!java.util.Arrays.asList($2).contains($1)");
        result = result.replaceAll("(\\w+)\\s+in\\s+\\{([^}]+)\\}", "java.util.Arrays.asList($2).contains($1)");
        result = result.replaceAll("(\\w+)\\s+in\\s+\\[([^\\]]+)\\]", "java.util.Arrays.asList($2).contains($1)");
        return result;
    }
}
