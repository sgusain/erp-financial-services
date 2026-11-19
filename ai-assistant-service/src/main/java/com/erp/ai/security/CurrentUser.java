package com.erp.ai.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Small helper to read the logged-in user from the request's JWT. */
public final class CurrentUser {

    private CurrentUser() {}

    /** The JWT subject, which user-service sets to the username. */
    public static String username() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new IllegalStateException("No authenticated user");
        }
        return auth.getName();
    }

    /** The raw token, as "Bearer xxx", ready to forward to other ERP services. */
    public static String bearerToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt) {
            return "Bearer " + jwt.getToken().getTokenValue();
        }
        throw new IllegalStateException("No JWT on the current request");
    }
}
