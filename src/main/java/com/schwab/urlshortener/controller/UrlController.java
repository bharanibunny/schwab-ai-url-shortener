package com.schwab.urlshortener.controller;

import com.schwab.urlshortener.dto.AnalyticsResponse;
import com.schwab.urlshortener.dto.CreateUrlRequest;
import com.schwab.urlshortener.dto.ShortUrlResponse;
import com.schwab.urlshortener.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/urls")
@Tag(name = "URL Management", description = "Endpoints for creating short URLs and retrieving analytics")
public class UrlController {

    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    @Operation(summary = "Create a shortened URL", description = "Validates the long URL, generates a 7-character Base62 short code, and saves the mapping.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Short URL created successfully",
            content = @Content(schema = @Schema(implementation = ShortUrlResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid URL or expiration date in past")
    })
    @PostMapping
    public ResponseEntity<ShortUrlResponse> createShortUrl(@Valid @RequestBody CreateUrlRequest request) {
        ShortUrlResponse response = urlService.createShortUrl(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get URL Analytics", description = "Retrieves creation metadata and total click count for a short code.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Analytics retrieved successfully",
            content = @Content(schema = @Schema(implementation = AnalyticsResponse.class))),
        @ApiResponse(responseCode = "404", description = "Short code not found")
    })
    @GetMapping("/{shortCode}/analytics")
    public ResponseEntity<AnalyticsResponse> getAnalytics(@PathVariable("shortCode") String shortCode) {
        AnalyticsResponse response = urlService.getAnalytics(shortCode);
        return ResponseEntity.ok(response);
    }
}
