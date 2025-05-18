package com.example.medicalregister.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for handling requests to the home page.
 */
@Controller
public class HomeController {

    @Value("${spring.h2.console.enabled:false}") // Default to false if not set
    private boolean h2ConsoleEnabled;

    /**
     * Handles requests to the root URL ("/"). Populates the model with user
     * authentication status and H2 console availability.
     *
     * @param model     The Spring MVC model to add attributes to.
     * @param principal The authenticated user details (OAuth2User), or null if not
     *                  authenticated.
     * @return The name of the view to render (index).
     */
    @GetMapping("/")
    public String home(Model model, @AuthenticationPrincipal OAuth2User principal) {
        if (principal != null) {
            String name = principal.getAttribute("name");
            // Use "User" as a fallback if name attribute is missing
            model.addAttribute("userName", name != null ? name : "User");
            model.addAttribute("isAuthenticated", true);
        } else {
            model.addAttribute("isAuthenticated", false);
        }
        model.addAttribute("h2ConsoleEnabled", h2ConsoleEnabled);
        return "index"; // Renders src/main/resources/templates/index.xhtml (or .html)
    }
}
