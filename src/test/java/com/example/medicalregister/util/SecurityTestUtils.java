package com.example.medicalregister.util;

import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collections;
import java.util.Map;

/**
 * Utility class for creating mock OAuth2User instances for testing.
 */
public class SecurityTestUtils {

    /**
     * Creates a mock OAuth2User with 'sub' and 'name' attributes. The 'sub'
     * attribute is used as the nameAttributeKey. This is common for services that
     * rely on 'sub' as the primary identifier.
     *
     * @param sub  The subject identifier.
     * @param name The user's display name.
     * @return A mock OAuth2User.
     */
    public static OAuth2User createOAuth2UserWithSubAndName(String sub, String name) {
        return new DefaultOAuth2User(
                Collections.emptyList(),
                Map.of("sub", sub, "name", name),
                "sub" // nameAttributeKey
        );
    }

    /**
     * Creates a mock OAuth2User with custom attributes and a specific
     * nameAttributeKey. This is a general-purpose factory method.
     *
     * @param attributes       The attributes for the user.
     * @param nameAttributeKey The key in the attributes map that represents the
     *                         principal's name.
     * @return A mock OAuth2User.
     */
    public static OAuth2User createOAuth2User(Map<String, Object> attributes, String nameAttributeKey) {
        return new DefaultOAuth2User(
                Collections.emptyList(), // Defaulting to no authorities
                attributes,
                nameAttributeKey);
    }

}
