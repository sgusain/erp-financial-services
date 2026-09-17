package com.erp.user.controller;

import com.erp.common.exception.AccountLockedException;
import com.erp.user.dto.LoginRequest;
import com.erp.user.dto.LoginResponse;
import com.erp.user.dto.RefreshTokenRequest;
import com.erp.user.entity.RefreshToken;
import com.erp.user.entity.User;
import com.erp.user.security.JwtUtil;
import com.erp.user.security.LoginAttemptService;
import com.erp.user.security.TokenBlacklistService;
import com.erp.user.service.RefreshTokenService;
import com.erp.user.service.UserService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptService loginAttemptService;
    private final TokenBlacklistService tokenBlacklistService;

    @RateLimiter(name = "login")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        if (loginAttemptService.isLocked(request.getUsername())) {
            throw new AccountLockedException(
                    "Account is temporarily locked due to too many failed login attempts. Please try again later.");
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
        } catch (AuthenticationException ex) {
            loginAttemptService.recordFailure(request.getUsername());
            throw ex;
        }

        loginAttemptService.recordSuccess(request.getUsername());

        User user = userService.getUserByEmail(authentication.getName());
        String accessToken = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getEmail());

        return ResponseEntity.ok(new LoginResponse(accessToken, refreshToken.getToken(), user.getEmail(), user.getRole().name()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        RefreshToken newRefreshToken = refreshTokenService.rotate(request.getRefreshToken());
        User user = userService.getUserByEmail(newRefreshToken.getUsername());
        String accessToken = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        return ResponseEntity.ok(new LoginResponse(accessToken, newRefreshToken.getToken(), user.getEmail(), user.getRole().name()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest httpRequest) {
        refreshTokenService.revoke(request.getRefreshToken());

        String authHeader = httpRequest.getHeader(AUTH_HEADER);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String accessToken = authHeader.substring(BEARER_PREFIX.length());
            if (jwtUtil.isTokenValid(accessToken)) {
                long remainingTtlMillis = jwtUtil.extractExpiration(accessToken).getTime() - System.currentTimeMillis();
                tokenBlacklistService.blacklistToken(accessToken, remainingTtlMillis);
            }
        }

        return ResponseEntity.noContent().build();
    }
}
