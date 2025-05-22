package com.example.medicalregister.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * RESTful API controller for the home endpoint. Provides information about the
 * API and user authentication status.
 */
@RestController
@RequestMapping("/api/v1")
public class HomeApiController {

    @Value("${spring.h2.console.enabled:false}") // Default to false if not set
    private boolean h2ConsoleEnabled;

    /**
     * Handles GET requests to the root URL ("/api/v1"). Returns a JSON response
     * containing a welcome message and user authentication status.
     *
     * @param principal The authenticated user details (OAuth2User), or null if not
     *                  authenticated.
     * @return A ResponseEntity containing a map with the welcome message and user
     *         authentication status.
     */

    @GetMapping
    public ResponseEntity<Map<String, Object>> apiRoot(@AuthenticationPrincipal OAuth2User principal) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Welcome to the Medical Register API v1");
        response.put("h2ConsoleEnabled", h2ConsoleEnabled);
        if (principal != null) {
            response.put("authenticatedUser", principal.getAttribute("name"));
            response.put("isAuthenticated", true);
        } else {
            response.put("isAuthenticated", false);
        }
        return ResponseEntity.ok(response);
    }
}
