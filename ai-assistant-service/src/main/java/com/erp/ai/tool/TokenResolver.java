package com.erp.ai.tool;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Tools run inside the model loop, maybe on another thread. So the chat code puts the token and
 * username into the ToolContext. When a tool is called from the MCP server there is no such
 * context, so we fall back to the request's own security context.
 */
public final class TokenResolver {

    public static final String TOKEN_KEY = "token";
    public static final String USERNAME_KEY = "username";

    private TokenResolver() {}

    public static String token(ToolContext context) {
        if (context != null && context.getContext().get(TOKEN_KEY) instanceof String token) {
            return token;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt) {
            return "Bearer " + jwt.getToken().getTokenValue();
        }
        throw new IllegalStateException("No user token available for this tool call");
    }

    public static String username(ToolContext context) {
        if (context != null && context.getContext().get(USERNAME_KEY) instanceof String name) {
            return name;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            return auth.getName();
        }
        throw new IllegalStateException("No user for this tool call");
    }
}
