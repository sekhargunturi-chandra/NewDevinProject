package com.capstone.fraudcase;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CaseApiIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper om;

    private static String caseId;

    private String intakeJson() throws Exception {
        return om.writeValueAsString(new java.util.LinkedHashMap<String, Object>() {{
            put("maskedCustomerRef", "CUST-NEWDEV-4821");
            put("homeRegion", "IN-MH");
            put("transactionRef", "TXN-2026-784102");
            put("amount", 48750.00);
            put("currency", "INR");
            put("occurredAt", "2026-11-06T08:42:00+05:30");
            put("status", "POSTED");
            put("channel", "CARD_NOT_PRESENT");
            put("maskedMerchant", "NORTHSTAR DIGITAL ••••73");
            put("issueType", "MERCHANT_NOT_RECOGNISED");
            put("customerStatement", "I do not recognise this purchase and still have my card.");
            put("cardPossession", true);
            put("merchantRecognised", false);
            put("syntheticDataConfirmed", true);
            put("reviewRequired", true);
        }});
    }

    @Test
    @Order(1)
    void createCase() throws Exception {
        MvcResult result = mvc.perform(post("/api/cases")
                        .with(user("agent").roles("AGENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(intakeJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.caseId").exists())
                .andReturn();
        caseId = om.readTree(result.getResponse().getContentAsString()).get("caseId").asText();
        assertThat(caseId).startsWith("CASE-");
    }

    @Test
    @Order(2)
    void rejectsInvalidIntake() throws Exception {
        mvc.perform(post("/api/cases")
                        .with(user("agent").roles("AGENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maskedCustomerRef\":\"X\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"));
    }

    @Test
    @Order(3)
    void investigateProducesRecommendation() throws Exception {
        mvc.perform(post("/api/cases/{id}/investigations", caseId)
                        .with(user("investigator").roles("INVESTIGATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classification").exists())
                .andExpect(jsonPath("$.humanReviewRequired").value(true));

        mvc.perform(get("/api/cases/{id}/evidence", caseId)
                        .with(user("investigator").roles("INVESTIGATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(5)));

        mvc.perform(get("/api/cases/{id}/tool-traces", caseId)
                        .with(user("investigator").roles("INVESTIGATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(6)));

        mvc.perform(get("/api/cases/{id}/recommendation", caseId)
                        .with(user("reviewer").roles("REVIEWER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caseId").value(caseId));
    }

    @Test
    @Order(4)
    void reviewWithoutReasonIsRejected() throws Exception {
        mvc.perform(post("/api/cases/{id}/reviews", caseId)
                        .with(user("reviewer").roles("REVIEWER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disposition\":\"ACCEPT\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(5)
    void reviewWithReasonIsRecorded() throws Exception {
        mvc.perform(post("/api/cases/{id}/reviews", caseId)
                        .with(user("reviewer").roles("REVIEWER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disposition\":\"ACCEPT\",\"reason\":\"Evidence supports the recommended route.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.disposition").value("ACCEPT"));

        mvc.perform(get("/api/cases/{id}", caseId)
                        .with(user("reviewer").roles("REVIEWER")))
                .andExpect(jsonPath("$.status").value("ROUTED"));

        mvc.perform(get("/api/cases/{id}/audit", caseId)
                        .with(user("reviewer").roles("REVIEWER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(4)));
    }

    @Test
    @Order(6)
    void followUpIsCaseScoped() throws Exception {
        mvc.perform(post("/api/cases/{id}/messages", caseId)
                        .with(user("investigator").roles("INVESTIGATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"What evidence is missing?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").exists());
    }

    @Test
    @Order(7)
    void agentCannotReview() throws Exception {
        mvc.perform(post("/api/cases/{id}/reviews", caseId)
                        .with(user("agent").roles("AGENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disposition\":\"ACCEPT\",\"reason\":\"x\"}"))
                .andExpect(status().isForbidden());
    }
}
