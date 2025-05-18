package com.example.medicalregister.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import com.example.medicalregister.util.SecurityTestUtils; // Import the utility
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest // Loads the full application context
@AutoConfigureMockMvc // Configures MockMvc
@ActiveProfiles("test") // Use the profile in application-test.properties for integration tests
@DisplayName("HomeController Integration Tests")
/**
 * Integration tests for the {@link HomeController}. This class tests the
 * controller methods to ensure they work as expected in a full application
 * context.
 */
class HomeControllerIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Value("${spring.h2.console.enabled:false}")
        private boolean h2ConsoleEnabledInConfig;

        @Test
        @DisplayName("GET / should return index view with correct attributes for unauthenticated user")
        void home_unauthenticated_shouldReturnIndexAndAttributes() throws Exception {
                mockMvc.perform(get("/"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("index"))
                                .andExpect(model().attribute("isAuthenticated", false))
                                .andExpect(model().attributeDoesNotExist("userName"))
                                .andExpect(model().attribute("h2ConsoleEnabled", h2ConsoleEnabledInConfig));
        }

        @Test
        @DisplayName("GET / should return index view with correct attributes for authenticated user")
        void home_authenticated_shouldReturnIndexAndAttributes() throws Exception {
                String expectedUserName = "Test User";
                // Create the mock principal
                var mockPrincipal = SecurityTestUtils.createOAuth2User(
                                Map.of("name", expectedUserName, "sub", "test-sub"),
                                "name");

                mockMvc.perform(get("/").with(oauth2Login().oauth2User(mockPrincipal)))
                                .andExpect(status().isOk())
                                .andExpect(view().name("index"))
                                .andExpect(model().attribute("isAuthenticated", true))
                                .andExpect(model().attribute("userName", expectedUserName))
                                .andExpect(model().attribute("h2ConsoleEnabled", h2ConsoleEnabledInConfig));
        }

        @Test
        @DisplayName("GET / should use fallback 'User' if name attribute is missing from principal")
        void home_authenticated_missingNameAttribute_shouldUseFallbackUserName() throws Exception {
                // Create the mock principal, 'sub' is the nameAttributeKey here
                var mockPrincipalWithMissingName = SecurityTestUtils.createOAuth2User(
                                Map.of("sub", "test-sub"), // "name" attribute is missing
                                "sub"); // Assuming 'sub' is the nameAttributeKey if 'name' is missing

                mockMvc.perform(get("/").with(oauth2Login().oauth2User(mockPrincipalWithMissingName)))
                                .andExpect(status().isOk())
                                .andExpect(view().name("index"))
                                .andExpect(model().attribute("isAuthenticated", true))
                                .andExpect(model().attribute("userName", "User")) // Expecting fallback
                                .andExpect(model().attribute("h2ConsoleEnabled", h2ConsoleEnabledInConfig));
        }
}
