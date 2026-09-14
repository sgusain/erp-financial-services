package com.erp.notification.integration;

import com.erp.notification.support.PostgresTestContainer;
import com.erp.notification.support.TestJwtUtil;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class NotificationControllerIntegrationTest extends PostgresTestContainer {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createAndFetchNotification() throws Exception {
        String userAuth = "Bearer " + TestJwtUtil.userToken();

        MvcResult createResult = mockMvc.perform(post("/api/notifications")
                        .header(HttpHeaders.AUTHORIZATION, userAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"message\":\"Budget exceeded\",\"type\":\"EMAIL\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Budget exceeded"))
                .andReturn();

        Long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/notifications/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, userAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("EMAIL"));
    }

    @Test
    void anyRequest_withoutAuth_isForbidden() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteNotification_requiresAdminRole() throws Exception {
        String userAuth = "Bearer " + TestJwtUtil.userToken();
        String adminAuth = "Bearer " + TestJwtUtil.adminToken();

        MvcResult createResult = mockMvc.perform(post("/api/notifications")
                        .header(HttpHeaders.AUTHORIZATION, userAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":2,\"message\":\"Delete me\",\"type\":\"SMS\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        Long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/notifications/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, userAuth))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/notifications/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, adminAuth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/notifications/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, adminAuth))
                .andExpect(status().isNotFound());
    }
}
