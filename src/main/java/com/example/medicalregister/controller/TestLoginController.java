package com.example.medicalregister.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Controller for test purposes only to simulate user login. This controller
 * should only be active under "local" test profiles.
 * DO NOT ENABLE IN PRODUCTION.
 */
@RestController
@Profile("local")
public class TestLoginController {

    private static final Logger logger = LoggerFactory.getLogger(TestLoginController.class);

    @GetMapping("/test/login")
    public ResponseEntity<String> simulateLogin(
            @RequestParam(name = "username", defaultValue = "e2eTestUser") String username,
            @RequestParam(name = "sub", defaultValue = "auth0|e2e-test-sub") String sub,
            @RequestParam(name = "email", defaultValue = "e2e@example.com") String email) {

        logger.info("Simulating login for user: {}, sub: {}", username, sub);

        // Create OAuth2User attributes
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", sub); // Crucial for your MedicalRecordService
        attributes.put("name", username);
        attributes.put("email", email);
        // Add any other attributes your application might expect

        // Define authorities (roles)
        Set<GrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));

        // Create an OAuth2User principal
        OAuth2User principal = new DefaultOAuth2User(authorities, attributes, "sub"); // "sub" is the nameAttributeKey

        // Create an OAuth2AuthenticationToken (or UsernamePasswordAuthenticationToken
        // if simpler and sufficient) For consistency with your app's OAuth2 setup,
        // OAuth2AuthenticationToken is better.
        OAuth2AuthenticationToken authenticationToken = new OAuth2AuthenticationToken(
                principal,
                authorities,
                "auth0"); // "auth0" is the clientRegistrationId

        // Set the authentication in the SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        logger.info("Successfully simulated login for user: {}. SecurityContext updated.", username);
        // You could redirect here, but for an API call from Selenium, a simple response
        // is fine. The E2E test will then navigate to the desired page.
        return ResponseEntity.ok("Simulated login successful for " + username);
    }
}
