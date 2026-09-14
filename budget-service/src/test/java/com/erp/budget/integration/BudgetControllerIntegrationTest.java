package com.erp.budget.integration;

import com.erp.budget.support.PostgresTestContainer;
import com.erp.budget.support.TestJwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class BudgetControllerIntegrationTest extends PostgresTestContainer {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String createBudgetJson(String name, String total, String spent, int fiscalYear, String status) {
        return String.format(
                "{\"budgetName\":\"%s\",\"totalAmount\":%s,\"spentAmount\":%s,\"fiscalYear\":%d,\"status\":\"%s\"}",
                name, total, spent, fiscalYear, status);
    }

    @Test
    void createUpdateAndFetchBudget_flipsToExceeded_whenSpentReachesTotal() throws Exception {
        String userAuth = "Bearer " + TestJwtUtil.userToken();

        MvcResult createResult = mockMvc.perform(post("/api/budgets")
                        .header(HttpHeaders.AUTHORIZATION, userAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBudgetJson("Ops Budget", "500.00", "0.00", 2026, "ACTIVE")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();

        Long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/budgets/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, userAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBudgetJson("Ops Budget", "500.00", "500.00", 2026, "ACTIVE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXCEEDED"));

        mockMvc.perform(get("/api/budgets/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, userAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXCEEDED"));
    }

    @Test
    void updateBudget_staysActive_whenSpentBelowTotal() throws Exception {
        String userAuth = "Bearer " + TestJwtUtil.userToken();

        MvcResult createResult = mockMvc.perform(post("/api/budgets")
                        .header(HttpHeaders.AUTHORIZATION, userAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBudgetJson("Travel Budget", "1000.00", "0.00", 2026, "ACTIVE")))
                .andExpect(status().isCreated())
                .andReturn();

        Long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/budgets/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, userAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBudgetJson("Travel Budget", "1000.00", "200.00", 2026, "ACTIVE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getBudgetById_returns404_whenMissing() throws Exception {
        String userAuth = "Bearer " + TestJwtUtil.userToken();

        mockMvc.perform(get("/api/budgets/{id}", 999999L)
                        .header(HttpHeaders.AUTHORIZATION, userAuth))
                .andExpect(status().isNotFound());
    }

    @Test
    void anyRequest_withoutAuth_isUnauthorized() throws Exception {
        mockMvc.perform(get("/api/budgets"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteBudget_requiresAdminRole() throws Exception {
        String userAuth = "Bearer " + TestJwtUtil.userToken();
        String adminAuth = "Bearer " + TestJwtUtil.adminToken();

        MvcResult createResult = mockMvc.perform(post("/api/budgets")
                        .header(HttpHeaders.AUTHORIZATION, userAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBudgetJson("Delete Me", "300.00", "0.00", 2026, "ACTIVE")))
                .andExpect(status().isCreated())
                .andReturn();
        Long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        // Non-admin user is forbidden.
        mockMvc.perform(delete("/api/budgets/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, userAuth))
                .andExpect(status().isForbidden());

        // Admin succeeds.
        mockMvc.perform(delete("/api/budgets/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, adminAuth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/budgets/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, adminAuth))
                .andExpect(status().isNotFound());
    }
}
