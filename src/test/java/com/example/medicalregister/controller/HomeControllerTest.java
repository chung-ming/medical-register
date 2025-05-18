package com.example.medicalregister.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.Model;
import org.springframework.security.oauth2.core.user.OAuth2User;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("HomeController Tests")
/**
 * Unit tests for the {@link HomeController}. This class tests the controller
 * methods to ensure they work as expected.
 */
class HomeControllerTest {

    private HomeController homeController;

    @Mock
    private Model model;

    @Mock
    private OAuth2User principal;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        homeController = new HomeController(); // Create a real instance
    }

    @Test
    @DisplayName("Should return index view")
    void home_shouldReturnIndexView() {
        // Arrange
        setH2ConsoleEnabled(homeController, true); // Helper method to set private field

        // Act
        String viewName = homeController.home(model, null); // Test unauthenticated user

        // Assert
        assertEquals("index", viewName);
    }

    @Test
    @DisplayName("Should add isAuthenticated=false and h2ConsoleEnabled to model when not authenticated")
    void home_whenNotAuthenticated_shouldAddCorrectAttributes() {
        // Arrange
        setH2ConsoleEnabled(homeController, false); // Set h2ConsoleEnabled for this test

        // Act
        homeController.home(model, null); // No principal means not authenticated

        // Assert
        verify(model).addAttribute("isAuthenticated", false); // Key assertion for unauthenticated state
        verify(model, never()).addAttribute(eq("userName"), anyString()); // userName should not be added
        verify(model).addAttribute("h2ConsoleEnabled", false);
    }

    @Test
    @DisplayName("Should add isAuthenticated=true and userName from principal when authenticated")
    void home_whenAuthenticated_shouldAddCorrectAttributes() {
        // Arrange
        setH2ConsoleEnabled(homeController, true); // Set h2ConsoleEnabled for this test

        when(principal.getAttribute("name")).thenReturn("Test User");

        // Act
        homeController.home(model, principal);

        // Assert
        verify(model).addAttribute("isAuthenticated", true);
        verify(model).addAttribute("userName", "Test User");
        verify(model).addAttribute("h2ConsoleEnabled", true);
    }

    @Test
    @DisplayName("Should use 'User' as userName fallback if principal name is null")
    void home_whenAuthenticatedAndPrincipalNameIsNull_shouldUseFallbackUserName() {
        // Arrange
        setH2ConsoleEnabled(homeController, true); // Set h2ConsoleEnabled for this test

        when(principal.getAttribute("name")).thenReturn(null); // Simulate null name attribute

        // Act
        homeController.home(model, principal);

        // Assert
        verify(model).addAttribute("isAuthenticated", true);
        verify(model).addAttribute("userName", "User"); // Verify fallback
        verify(model).addAttribute("h2ConsoleEnabled", true);
    }

    /**
     * Helper method to set the private {@code @Value} annotated field
     * 'h2ConsoleEnabled'
     * using reflection, as it's not directly injectable in a plain unit test.
     */
    private void setH2ConsoleEnabled(HomeController controller, boolean value) {
        try {
            java.lang.reflect.Field field = HomeController.class.getDeclaredField("h2ConsoleEnabled");
            field.setAccessible(true);
            field.set(controller, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set h2ConsoleEnabled field for testing", e);
        }
    }
}