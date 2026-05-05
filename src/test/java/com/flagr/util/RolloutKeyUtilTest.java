package com.flagr.util;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RolloutKeyUtilTest {

    @Test
    void sameAttributesReturnSameHashAcrossRepeatedCalls() {
        Map<String, Object> attributes = Map.of("user_id", 123, "region", "US");
        int hash1 = RolloutKeyUtil.computeRolloutHash(attributes);
        int hash2 = RolloutKeyUtil.computeRolloutHash(attributes);
        int hash3 = RolloutKeyUtil.computeRolloutHash(attributes);
        assertEquals(hash1, hash2);
        assertEquals(hash2, hash3);
    }

    @Test
    void distinctAttributeMapsReturnDistinctValuesInRange() {
        Map<String, Object> map1 = Map.of("user_id", 1);
        Map<String, Object> map2 = Map.of("user_id", 2);
        Map<String, Object> map3 = Map.of("user_id", 3, "region", "US");
        Map<String, Object> map4 = Map.of("user_id", 4, "region", "CA");
        Map<String, Object> map5 = Map.of("region", "EU");

        int h1 = RolloutKeyUtil.computeRolloutHash(map1);
        int h2 = RolloutKeyUtil.computeRolloutHash(map2);
        int h3 = RolloutKeyUtil.computeRolloutHash(map3);
        int h4 = RolloutKeyUtil.computeRolloutHash(map4);
        int h5 = RolloutKeyUtil.computeRolloutHash(map5);

        assertAll(
                () -> assertTrue(h1 >= 0 && h1 < 100),
                () -> assertTrue(h2 >= 0 && h2 < 100),
                () -> assertTrue(h3 >= 0 && h3 < 100),
                () -> assertTrue(h4 >= 0 && h4 < 100),
                () -> assertTrue(h5 >= 0 && h5 < 100)
        );

        java.util.Set<Integer> values = new java.util.HashSet<>();
        values.add(h1);
        values.add(h2);
        values.add(h3);
        values.add(h4);
        values.add(h5);
        assertEquals(5, values.size());
    }

    @Test
    void differentInsertionOrderReturnsSameHash() {
        Map<String, Object> map1 = new LinkedHashMap<>();
        map1.put("user_id", 123);
        map1.put("region", "US");

        Map<String, Object> map2 = new LinkedHashMap<>();
        map2.put("region", "US");
        map2.put("user_id", 123);

        assertEquals(RolloutKeyUtil.computeRolloutHash(map1), RolloutKeyUtil.computeRolloutHash(map2));
        assertEquals(RolloutKeyUtil.generateRolloutKey(map1), RolloutKeyUtil.generateRolloutKey(map2));
    }

    @Test
    void emptyMapReturnsValidInt() {
        int hash = RolloutKeyUtil.computeRolloutHash(Map.of());
        assertTrue(hash >= 0 && hash < 100);
    }

    @Test
    void generateRolloutKeyReturnsHexString() {
        String key = RolloutKeyUtil.generateRolloutKey(Map.of("user_id", 123));
        assertNotNull(key);
        assertEquals(64, key.length());
        assertTrue(key.matches("[0-9a-f]+"));
    }
}
