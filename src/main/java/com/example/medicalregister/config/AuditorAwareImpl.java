package com.example.medicalregister.config;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * Implementation of {@link AuditorAware} to provide the current auditor's
 * identifier (username/ID). This is used by Spring Data JPA auditing to
 * automatically populate `createdBy` and `lastModifiedBy` fields. It attempts
 * to use the 'sub' claim from an {@link OAuth2User} principal.
 */
public class AuditorAwareImpl implements AuditorAware<String> {

    private static final Logger logger = LoggerFactory.getLogger(AuditorAwareImpl.class);

    @SuppressWarnings("null")
    @Override
    public Optional<String> getCurrentAuditor() {
        // Retrieve the current authentication object from the security context. This
        // context is thread-local and holds security information for the current
        // request.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            logger.debug("No authenticated user found, returning empty for auditor.");
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        // If the principal is an OAuth2User, extract the 'sub' (subject) claim as the
        // auditor.
        if (principal instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) principal;
            String sub = oauth2User.getAttribute("sub");
            if (sub == null) {
                logger.warn("'sub' attribute is null for OAuth2User during audit. Attributes: {}",
                        oauth2User.getAttributes().keySet());
                return Optional.empty();
            }
            logger.debug("Current auditor identified by 'sub': {}", sub);
            return Optional.of(sub);
        }
        // Fallback: If not an OAuth2User or 'sub' is unavailable, use the principal's
        // name. This might be a username for other authentication types or a generic
        // identifier.
        logger.warn(
                "Principal is not an instance of OAuth2User, cannot determine auditor 'sub'. Principal type: {}. Returning principal name: {}",
                principal.getClass().getName(), authentication.getName());
        return Optional.ofNullable(authentication.getName());
    }
}
