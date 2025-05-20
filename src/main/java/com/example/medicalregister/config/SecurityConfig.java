package com.example.medicalregister.config;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.DispatcherType;

/**
 * Configures web security for the application, including OAuth2 login with
 * Auth0, logout handling, CSRF protection, and access control rules.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${spring.security.oauth2.client.registration.auth0.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.provider.auth0.issuer-uri}")
    private String issuerUri;

    /**
     * Configures Spring Security to ignore requests to static resources. These
     * requests will bypass the security filter chain entirely for performance.
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        logger.info("Configuring WebSecurityCustomizer to ignore static resources like webjars.");
        return (web) -> web.ignoring().requestMatchers(
                "/webjars/**" // Static resources like webjars can bypass the security filter chain.
        );
    }

    /**
     * Defines the main security filter chain for HTTP requests.
     * 
     * @param http HttpSecurity to configure.
     * @return The configured SecurityFilterChain.
     * @throws Exception if an error occurs during configuration.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        logger.info("Configuring main SecurityFilterChain.");
        http
                .authorizeHttpRequests(authorizeRequests -> {
                    authorizeRequests
                            .requestMatchers("/", "/error", "/public/**", "/test/login",
                                    "/h2-console/**" // Allow access to H2 console (intended for development)
                    ).permitAll()
                            // Allow unauthenticated access to the health endpoint
                            .requestMatchers(EndpointRequest.to("health")).permitAll()
                            // Permit requests dispatched via FORWARD (e.g., to error pages) or ERROR.
                            // This is crucial for Spring Boot's default error handling to function
                            // correctly with security.
                            .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
                            // Any other request must be authenticated.
                            .anyRequest().authenticated();
                })
                .oauth2Login(oauth2Login -> oauth2Login
                        .defaultSuccessUrl("/records", true) // Always redirect to records list after login
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout")) // Spring Security handles this URL
                        .addLogoutHandler(logoutHandler()) // Custom Auth0 logout handler for RP-initiated logout
                )
                // CSRF protection is enabled by default.
                // H2 console requires CSRF to be disabled for its path.
                // Static resources (webjars) are ignored by WebSecurityCustomizer, so they
                // don't interact with CsrfFilter.
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                                "/h2-console/**"))
                // Allow H2 console to be embedded in a frame from the same origin (e.g., for
                // its own UI).
                .headers(headers -> headers.frameOptions(customizer -> customizer.sameOrigin()));

        return http.build();
    }

    /**
     * Creates a custom LogoutHandler for Auth0. This handler redirects the user to
     * the Auth0 logout endpoint to terminate the Auth0 session, and then redirects
     * back to the application.
     * 
     * @return The configured LogoutHandler.
     */
    private LogoutHandler logoutHandler() {
        return (request, response, authentication) -> {
            try {
                // Construct the 'returnTo' URL, which is where Auth0 will redirect the user
                // back to after they have logged out from Auth0.
                String returnTo = ServletUriComponentsBuilder
                        .fromCurrentContextPath() // Gets scheme://host:port/contextPath
                        .path("/") // Use root path which HomeController maps to index.xhtml
                        .queryParam("auth0logout", "true") // Indicates successful Auth0 logout
                        .build()
                        .toUriString();

                // Construct the Auth0 logout URL.
                String baseLogoutUrl = UriComponentsBuilder.fromUriString(issuerUri)
                        .path("/v2/logout") // .path() handles leading/trailing slashes correctly
                        .build()
                        .toUriString();

                String logoutUrl = baseLogoutUrl + "?client_id=" + clientId + "&returnTo="
                        + java.net.URLEncoder.encode(returnTo, "UTF-8");
                logger.info("Redirecting user to Auth0 logout URL: {}", logoutUrl);
                response.sendRedirect(logoutUrl);
            } catch (IOException e) {
                logger.error("Error during Auth0 logout redirection", e);
            }
        };
    }
}
