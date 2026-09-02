package com.schwab.urlshortener.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schwab.urlshortener.dto.CreateUrlRequest;
import com.schwab.urlshortener.entity.ShortUrl;
import com.schwab.urlshortener.repository.ShortUrlRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class RedirectControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ShortUrlRepository shortUrlRepository;

    @Test
    void redirectToOriginalUrl_existingUnexpiredCode_returns302AndIncrementsClickCount() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest("https://example.com/target-redirect", null);

        MvcResult result = mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String shortCode = objectMapper.readTree(result.getResponse().getContentAsString()).get("shortCode").asText();

        // Verify redirect
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "https://example.com/target-redirect"));

        // Verify click count incremented to 1
        mockMvc.perform(get("/api/urls/" + shortCode + "/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clickCount", is(1)));
    }

    @Test
    void redirectToOriginalUrl_nonExistentCode_returns404NotFound() throws Exception {
        mockMvc.perform(get("/nonexist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")));
    }

    @Test
    void redirectToOriginalUrl_expiredCode_returns410Gone() throws Exception {
        // Manually insert an expired short url record into repo
        Instant pastExpiration = Instant.now().minus(5, ChronoUnit.MINUTES);
        ShortUrl expiredShortUrl = new ShortUrl("expCode", "https://example.com/expired-target", Instant.now().minus(1, ChronoUnit.HOURS), pastExpiration);
        shortUrlRepository.save(expiredShortUrl);

        mockMvc.perform(get("/expCode"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.status", is(410)))
                .andExpect(jsonPath("$.error", is("Gone")));
    }
}
