package com.schwab.urlshortener.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schwab.urlshortener.dto.CreateUrlRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class UrlControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createShortUrl_validUrl_returns201Created() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest("https://example.com/some/long/url", Instant.now().plus(30, ChronoUnit.DAYS));

        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode", matchesPattern("^[A-Za-z0-9]{7}$")))
                .andExpect(jsonPath("$.originalUrl", is("https://example.com/some/long/url")))
                .andExpect(jsonPath("$.clickCount", is(0)));
    }

    @Test
    void createShortUrl_malformedUrl_returns400BadRequest() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest("htt&p://invalid-format", null);

        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")));
    }

    @Test
    void createShortUrl_unsupportedScheme_returns400BadRequest() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest("ftp://example.com/file.txt", null);

        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Unsupported URL scheme")));
    }

    @Test
    void createShortUrl_pastExpiration_returns400BadRequest() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest("https://example.com", Instant.now().minus(1, ChronoUnit.DAYS));

        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Expiration date cannot be in the past")));
    }

    @Test
    void createShortUrl_crlfInjection_returns400BadRequest() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest("https://example.com/test\r\nSet-Cookie:evil", null);

        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("control characters")));
    }

    @Test
    void createShortUrl_oversizedUrl2049Chars_returns400BadRequest() throws Exception {
        String url2049 = "https://example.com/" + "a".repeat(2029);
        CreateUrlRequest request = new CreateUrlRequest(url2049, null);

        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("exceeds maximum permitted length")));
    }

    @Test
    void getAnalytics_existingShortCode_returns200OK() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest("https://example.com/analytics-test", null);

        MvcResult result = mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        String shortCode = objectMapper.readTree(jsonResponse).get("shortCode").asText();

        mockMvc.perform(get("/api/urls/" + shortCode + "/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode", is(shortCode)))
                .andExpect(jsonPath("$.originalUrl", is("https://example.com/analytics-test")))
                .andExpect(jsonPath("$.clickCount", is(0)));
    }
}
