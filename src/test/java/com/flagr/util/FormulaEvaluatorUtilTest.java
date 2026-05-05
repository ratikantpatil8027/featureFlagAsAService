package com.flagr.util;

import com.flagr.exception.InvalidFlagOperationException;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class FormulaEvaluatorUtilTest {

    private final FormulaEvaluatorUtil evaluator = new FormulaEvaluatorUtil();

    @Test
    void validatePassesForValidFormula() {
        Map<String, String> schema = Map.of("region", "string");
        assertDoesNotThrow(() -> evaluator.validate("region != 'Canada'", schema));
    }

    @Test
    void validateThrowsForMalformedFormula() {
        Map<String, String> schema = Map.of("region", "string");
        assertThrows(Exception.class,
                () -> evaluator.validate("region @#$% invalid", schema));
    }

    @Test
    void evaluateReturnsTrueForInOperator() {
        Map<String, Object> attributes = Map.of("user_id", 2);
        assertTrue(evaluator.evaluate("user_id in {1,2,3}", attributes));
    }

    @Test
    void evaluateReturnsFalseForInOperator() {
        Map<String, Object> attributes = Map.of("user_id", 5);
        assertFalse(evaluator.evaluate("user_id in {1,2,3}", attributes));
    }

    @Test
    void evaluateReturnsTrueForNotInOperator() {
        Map<String, Object> attributes = Map.of("user_id", 5);
        assertTrue(evaluator.evaluate("user_id not in {1,2,3}", attributes));
    }

    @Test
    void evaluateReturnsFalseForNotInOperator() {
        Map<String, Object> attributes = Map.of("user_id", 2);
        assertFalse(evaluator.evaluate("user_id not in {1,2,3}", attributes));
    }

    @Test
    void evaluateReturnsTrueForCombinedConditions() {
        Map<String, Object> attributes = Map.of("user_id", 15, "region", "US");
        assertTrue(evaluator.evaluate("user_id > 10 && region == 'US'", attributes));
    }

    @Test
    void evaluateReturnsFalseWhenRegionIsCanada() {
        Map<String, Object> attributes = Map.of("user_id", 15, "region", "Canada");
        assertFalse(evaluator.evaluate("user_id > 10 && region == 'US'", attributes));
    }
}
