package com.interview.fraud;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:flow;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.kafka.listener.auto-startup=false"
})
@AutoConfigureMockMvc
class InvestigationFlowTest {
    @Autowired MockMvc mvc;

    @Test
    void completeInvestigationFlow() throws Exception {
        String body = "{\"transactionId\":\"E2E-1\",\"customerId\":\"C1\",\"merchantId\":\"CRYPTO-9\",\"deviceId\":\"NEW\",\"amount\":7200,\"currency\":\"USD\",\"country\":\"SG\",\"occurredAt\":\"2026-08-12T02:00:00Z\"}";
        String created = mvc.perform(post("/api/transactions")
                        .with(httpBasic("analyst", "analyst-demo"))
                        .header("Idempotency-Key", "e2e-1")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.riskLevel").value("HIGH"))
                .andReturn().getResponse().getContentAsString();
        String id = new ObjectMapper().readTree(created).get("id").asText();

        mvc.perform(post("/api/cases/" + id + "/investigate").with(httpBasic("analyst", "analyst-demo")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.policyCitations[0].citation").exists())
                .andExpect(jsonPath("$.humanApprovalRequired").value(true));
        String approval = "{\"action\":\"FREEZE_ACCOUNT\",\"rationale\":\"Evidence verified\",\"version\":1}";
        mvc.perform(post("/api/cases/" + id + "/approve").with(httpBasic("analyst", "analyst-demo"))
                        .contentType(MediaType.APPLICATION_JSON).content(approval)).andExpect(status().isForbidden());
        mvc.perform(post("/api/cases/" + id + "/approve").with(httpBasic("senior", "senior-demo"))
                        .contentType(MediaType.APPLICATION_JSON).content(approval))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
    }
}
