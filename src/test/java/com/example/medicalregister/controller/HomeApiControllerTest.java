package com.example.medicalregister.controller;

import com.example.medicalregister.config.SecurityConfig;

import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HomeApiController.class)
@Import(SecurityConfig.class) // Import security config to apply security filters
@ActiveProfiles("test") // Ensure test properties are loaded
@DisplayName("HomeApiController Tests")
class HomeApiControllerTest {

    private static final String NAME_ATTRIBUTE_KEY = "name";
    private static final String SUB_ATTRIBUTE_KEY = "sub";
    private static final String DEFAULT_TEST_SUB = "test-sub";
    private static final String DEFAULT_TEST_API_USER_NAME = "Test API User";

    @Autowired
    private MockMvc mockMvc;

    // The h2ConsoleEnabled field in HomeApiController is now @Value annotated,
    // so its value will be injected by Spring based on properties.
    // We can use @TestPropertySource to override it for specific tests if needed.

    private OAuth2User createMockOAuth2User(String name, String sub) {
        Map<String, Object> attributes = new HashMap<>();
        if (name != null) {
            attributes.put(NAME_ATTRIBUTE_KEY, name);
        }
        // "sub" is often used as a stable, non-null identifier and a good candidate for
        // nameAttributeKey
        attributes.put(SUB_ATTRIBUTE_KEY, sub);
        return new DefaultOAuth2User(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                SUB_ATTRIBUTE_KEY); // Use "sub" as the nameAttributeKey, which must be non-null
    }

    private OAuth2User createMockOAuth2UserWithSpecificName(String name) {
        return createMockOAuth2User(name, DEFAULT_TEST_SUB); // Provide a default "sub"
    }

    @Test
    @DisplayName("GET /api/v1 - Unauthenticated - Should return welcome message and isAuthenticated false")
    void apiRoot_unauthenticated_shouldReturnWelcomeAndNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Welcome to the Medical Register API v1")))
                .andExpect(jsonPath("$.h2ConsoleEnabled", is(false))) // Default from application-test.properties
                .andExpect(jsonPath("$.isAuthenticated", is(false)))
                .andExpect(jsonPath("$.authenticatedUser").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/v1 - Authenticated - Should return welcome message, user details, and isAuthenticated true")
    void apiRoot_authenticated_shouldReturnWelcomeAndUserDetails() throws Exception {
        OAuth2User principal = createMockOAuth2UserWithSpecificName(DEFAULT_TEST_API_USER_NAME);

        mockMvc.perform(get("/api/v1")
                .with(oauth2Login().oauth2User(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Welcome to the Medical Register API v1")))
                .andExpect(jsonPath("$.h2ConsoleEnabled", is(false))) // Default from application-test.properties
                .andExpect(jsonPath("$.isAuthenticated", is(true)))
                .andExpect(jsonPath("$.authenticatedUser", is(DEFAULT_TEST_API_USER_NAME)));
    }

    @Test
    @DisplayName("GET /api/v1 - Authenticated with null name attribute - Should return null authenticatedUser")
    void apiRoot_authenticatedWithNullNameAttribute_shouldReturnNullAuthenticatedUser() throws Exception {
        // Create a user where the "name" attribute is not present or explicitly null in
        // the map
        OAuth2User principalWithNullName = createMockOAuth2User(null, DEFAULT_TEST_SUB + "-no-name");

        mockMvc.perform(get("/api/v1")
                .with(oauth2Login().oauth2User(principalWithNullName)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isAuthenticated", is(true)))
                .andExpect(jsonPath("$.authenticatedUser").value(IsNull.nullValue()));
    }
}