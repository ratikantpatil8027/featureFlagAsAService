package com.flagr.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FlagIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createFlagReturns201WithFeatureFlagId() throws Exception {
        String request = """
                {
                    "client": "test-client",
                    "name": "test-flag",
                    "attributes": {"user_id": "num", "region": "string"}
                }
                """;

        mockMvc.perform(post("/flags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.featureFlagId").isNotEmpty())
                .andExpect(jsonPath("$.client").value("test-client"))
                .andExpect(jsonPath("$.name").value("test-flag"))
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.rollout").value(0));
    }

    @Test
    void updateFlagReturns200WithUpdatedValue() throws Exception {
        String createRequest = """
                {
                    "client": "test-client",
                    "name": "update-test",
                    "attributes": {"region": "string"}
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/flags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String flagId = created.get("featureFlagId").asText();

        String updateRequest = """
                {
                    "formulaString": "region != 'Canada'"
                }
                """;

        mockMvc.perform(put("/flags/{id}", flagId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formulaString").value("region != 'Canada'"));
    }

    @Test
    void deleteFlagReturns204AndSubsequentEvaluateReturns404() throws Exception {
        String createRequest = """
                {
                    "client": "delete-test",
                    "name": "to-delete",
                    "attributes": {"user_id": "num"}
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/flags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String flagId = created.get("featureFlagId").asText();

        mockMvc.perform(delete("/flags/{id}", flagId))
                .andExpect(status().isNoContent());

        String evaluateRequest = """
                {
                    "client": "delete-test",
                    "name": "to-delete",
                    "attributes": {"user_id": 1}
                }
                """;

        mockMvc.perform(post("/flags/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(evaluateRequest))
                .andExpect(status().isNotFound());
    }

    @Test
    void toggleSimpleFlagReturns200WithFlippedEnabled() throws Exception {
        String createRequest = """
                {
                    "client": "toggle-test",
                    "name": "simple-toggle",
                    "attributes": {"user_id": "num"}
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/flags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String flagId = created.get("featureFlagId").asText();
        assertFalse(created.get("enabled").asBoolean());

        mockMvc.perform(patch("/flags/{id}/toggle", flagId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void toggleConditionalFlagReturns400() throws Exception {
        String createRequest = """
                {
                    "client": "toggle-test",
                    "name": "conditional-toggle",
                    "attributes": {"region": "string"},
                    "formulaString": "region != 'Canada'"
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/flags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String flagId = created.get("featureFlagId").asText();

        mockMvc.perform(patch("/flags/{id}/toggle", flagId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rolloutDistributionIsApproximatelyCorrect() throws Exception {
        String createRequest = """
                {
                    "client": "rollout-test",
                    "name": "rollout-flag",
                    "attributes": {"user_id": "num"}
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/flags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String flagId = created.get("featureFlagId").asText();

        String rolloutRequest = """
                {"percentage": 50}
                """;

        mockMvc.perform(put("/flags/{id}/rollout", flagId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rolloutRequest))
                .andExpect(status().isOk());

        String toggleRequest = """
                {"client": "rollout-test", "name": "rollout-flag"}
                """;
        mockMvc.perform(patch("/flags/{id}/toggle", flagId))
                .andExpect(status().isOk());

        int trueCount = 0;
        Random random = new Random();

        for (int i = 0; i < 100; i++) {
            int userId = random.nextInt(100000);
            String evaluateRequest = String.format("""
                    {
                        "client": "rollout-test",
                        "name": "rollout-flag",
                        "attributes": {"user_id": %d}
                    }
                    """, userId);

            MvcResult result = mockMvc.perform(post("/flags/evaluate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(evaluateRequest))
                    .andReturn();

            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            if (response.get("enabled").asBoolean()) {
                trueCount++;
            }
        }

        assertTrue(trueCount >= 35 && trueCount <= 65,
                "Expected true count between 35 and 65, got " + trueCount);
    }

    @Test
    void stickyBehaviorIdenticalAttributesReturnSameResult() throws Exception {
        String createRequest = """
                {
                    "client": "sticky-test",
                    "name": "sticky-flag",
                    "attributes": {"user_id": "num"}
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/flags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String flagId = created.get("featureFlagId").asText();

        String rolloutRequest = """
                {"percentage": 50}
                """;

        mockMvc.perform(put("/flags/{id}/rollout", flagId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rolloutRequest))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/flags/{id}/toggle", flagId))
                .andExpect(status().isOk());

        String evaluateRequest = """
                {
                    "client": "sticky-test",
                    "name": "sticky-flag",
                    "attributes": {"user_id": 42}
                }
                """;

        MvcResult result1 = mockMvc.perform(post("/flags/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(evaluateRequest))
                .andReturn();

        MvcResult result2 = mockMvc.perform(post("/flags/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(evaluateRequest))
                .andReturn();

        JsonNode response1 = objectMapper.readTree(result1.getResponse().getContentAsString());
        JsonNode response2 = objectMapper.readTree(result2.getResponse().getContentAsString());

        assertEquals(response1.get("enabled").asBoolean(), response2.get("enabled").asBoolean());
    }

    @Test
    void stickyOnRolloutIncrease() throws Exception {
        String createRequest = """
                {
                    "client": "sticky-increase",
                    "name": "increase-flag",
                    "attributes": {"user_id": "num"}
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/flags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String flagId = created.get("featureFlagId").asText();

        String rollout20 = """
                {"percentage": 20}
                """;

        mockMvc.perform(put("/flags/{id}/rollout", flagId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rollout20))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/flags/{id}/toggle", flagId))
                .andExpect(status().isOk());

        List<Map<String, Object>> trueMaps = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < 200; i++) {
            int userId = random.nextInt(100000);
            Map<String, Object> attrs = Map.of("user_id", userId);
            String evaluateRequest = String.format("""
                    {
                        "client": "sticky-increase",
                        "name": "increase-flag",
                        "attributes": {"user_id": %d}
                    }
                    """, userId);

            MvcResult result = mockMvc.perform(post("/flags/evaluate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(evaluateRequest))
                    .andReturn();

            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            if (response.get("enabled").asBoolean()) {
                trueMaps.add(attrs);
            }
        }

        String rollout60 = """
                {"percentage": 60}
                """;

        mockMvc.perform(put("/flags/{id}/rollout", flagId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rollout60))
                .andExpect(status().isOk());

        for (Map<String, Object> attrs : trueMaps) {
            int userId = (int) attrs.get("user_id");
            String evaluateRequest = String.format("""
                    {
                        "client": "sticky-increase",
                        "name": "increase-flag",
                        "attributes": {"user_id": %d}
                    }
                    """, userId);

            MvcResult result = mockMvc.perform(post("/flags/evaluate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(evaluateRequest))
                    .andReturn();

            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            assertTrue(response.get("enabled").asBoolean(),
                    "Attribute map that was true at 20%% should still be true at 60%%: " + attrs);
        }
    }

    @Test
    void conditionalFlagRegionNotCanada() throws Exception {
        String createRequest = """
                {
                    "client": "conditional-test",
                    "name": "region-flag",
                    "attributes": {"region": "string"},
                    "formulaString": "region != 'Canada'"
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/flags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String flagId = created.get("featureFlagId").asText();

        String evaluateUS = """
                {
                    "client": "conditional-test",
                    "name": "region-flag",
                    "attributes": {"region": "US"}
                }
                """;

        mockMvc.perform(post("/flags/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(evaluateUS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        String evaluateCanada = """
                {
                    "client": "conditional-test",
                    "name": "region-flag",
                    "attributes": {"region": "Canada"}
                }
                """;

        mockMvc.perform(post("/flags/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(evaluateCanada))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void conditionalFlagUserIdInSet() throws Exception {
        String createRequest = """
                {
                    "client": "conditional-test",
                    "name": "userid-flag",
                    "attributes": {"user_id": "num"},
                    "formulaString": "user_id in {1,2,3}"
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/flags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String flagId = created.get("featureFlagId").asText();

        String evaluateIn = """
                {
                    "client": "conditional-test",
                    "name": "userid-flag",
                    "attributes": {"user_id": 2}
                }
                """;

        mockMvc.perform(post("/flags/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(evaluateIn))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        String evaluateNotIn = """
                {
                    "client": "conditional-test",
                    "name": "userid-flag",
                    "attributes": {"user_id": 5}
                }
                """;

        mockMvc.perform(post("/flags/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(evaluateNotIn))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }
}
