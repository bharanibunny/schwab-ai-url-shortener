package com.schwab.urlshortener.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schwab.urlshortener.dto.CreateUrlRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class CustomAliasIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createShortUrl_validCustomAlias_returns201Created() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest("https://example.com/profile", null, "my-profile");

        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode", is("my-profile")))
                .andExpect(jsonPath("$.shortUrl", is("http://localhost:8080/my-profile")))
                .andExpect(jsonPath("$.originalUrl", is("https://example.com/profile")));
    }

    @Test
    void createShortUrl_mixedCaseAlias_normalizesToLowercase() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest("https://example.com/mixed-case", null, "My-Custom-Link");

        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode", is("my-custom-link")))
                .andExpect(jsonPath("$.shortUrl", is("http://localhost:8080/my-custom-link")));
    }

    @Test
    void createShortUrl_duplicateCustomAlias_returns409Conflict() throws Exception {
        CreateUrlRequest request1 = new CreateUrlRequest("https://example.com/first", null, "dup-alias");
        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        CreateUrlRequest request2 = new CreateUrlRequest("https://example.com/second", null, "dup-alias");
        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", containsString("dup-alias")));
    }

    @Test
    void createShortUrl_collisionWithExistingGeneratedCode_returns409Conflict() throws Exception {
        // Create an auto-generated link first
        CreateUrlRequest autoReq = new CreateUrlRequest("https://example.com/auto", null);
        MvcResult result = mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(autoReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String generatedCode = objectMapper.readTree(result.getResponse().getContentAsString()).get("shortCode").asText();

        // Attempt to claim the exact same generated code as a custom alias
        CreateUrlRequest collideReq = new CreateUrlRequest("https://example.com/claim-auto", null, generatedCode);
        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(collideReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)));
    }

    @Test
    void createShortUrl_reservedAlias_returns400BadRequest() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest("https://example.com/reserved", null, "swagger-ui");

        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("reserved system keyword")));
    }

    @Test
    void createShortUrl_aliasTooShort_returns400BadRequest() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest("https://example.com/short-alias", null, "abc");

        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("at least 4 characters")));
    }

    @Test
    void createShortUrl_aliasTooLong_returns400BadRequest() throws Exception {
        String longAlias = "a" + "b".repeat(30); // 31 chars
        CreateUrlRequest request = new CreateUrlRequest("https://example.com/long-alias", null, longAlias);

        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("must not exceed 30 characters")));
    }

    @Test
    void createShortUrl_aliasBeginningWithHyphenOrEndingWithUnderscore_returns400BadRequest() throws Exception {
        CreateUrlRequest req1 = new CreateUrlRequest("https://example.com/test1", null, "-start-hyphen");
        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isBadRequest());

        CreateUrlRequest req2 = new CreateUrlRequest("https://example.com/test2", null, "end-underscore_");
        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void customAlias_redirectAndAnalyticsWorkflow() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest("https://example.com/full-workflow", null, "full-flow");

        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Perform redirect
        mockMvc.perform(get("/full-flow"))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "https://example.com/full-workflow"));

        // Verify analytics click count = 1
        mockMvc.perform(get("/api/urls/full-flow/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode", is("full-flow")))
                .andExpect(jsonPath("$.clickCount", is(1)));
    }

    @Test
    void createShortUrl_omittedOrBlankCustomAlias_usesGeneratedCode() throws Exception {
        CreateUrlRequest reqOmitted = new CreateUrlRequest("https://example.com/omitted", null);
        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqOmitted)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode", matchesPattern("^[A-Za-z0-9]{7}$")));

        CreateUrlRequest reqBlank = new CreateUrlRequest("https://example.com/blank", null, "   ");
        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBlank)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode", matchesPattern("^[A-Za-z0-9]{7}$")));
    }

    @Test
    void customAlias_expiredBehavior_returns410OnRedirectAnd200OnAnalytics() throws Exception {
        // 1. Attempt creating with past expiration -> 400 Bad Request
        java.time.Instant pastExpiration = java.time.Instant.now().minus(1, java.time.temporal.ChronoUnit.HOURS);
        CreateUrlRequest pastReq = new CreateUrlRequest("https://example.com/past", pastExpiration, "past-alias");
        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pastReq)))
                .andExpect(status().isBadRequest());

        // 2. Create valid custom alias with future expiration
        java.time.Instant futureExpiration = java.time.Instant.now().plus(2, java.time.temporal.ChronoUnit.SECONDS);
        CreateUrlRequest validReq = new CreateUrlRequest("https://example.com/expiring-alias", futureExpiration, "expiring-alias");
        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isCreated());

        // 3. Immediate redirect succeeds (302 Found)
        mockMvc.perform(get("/expiring-alias"))
                .andExpect(status().isFound());

        // 4. Analytics accessible (200 OK, clickCount = 1)
        mockMvc.perform(get("/api/urls/expiring-alias/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clickCount", is(1)));
    }
}
