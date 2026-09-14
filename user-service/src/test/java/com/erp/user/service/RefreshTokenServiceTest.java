package com.erp.user.service;

import com.erp.common.exception.BadRequestException;
import com.erp.user.entity.RefreshToken;
import com.erp.user.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository);
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpirationMs", 604800000L);
    }

    @Test
    void createRefreshToken_savesUnrevokedTokenWithFutureExpiry() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken token = refreshTokenService.createRefreshToken("user@example.com");

        assertThat(token.getUsername()).isEqualTo("user@example.com");
        assertThat(token.isRevoked()).isFalse();
        assertThat(token.getExpiryDate()).isAfter(LocalDateTime.now());
        assertThat(token.getToken()).isNotBlank();
    }

    @Test
    void rotate_revokesOldTokenAndIssuesNewOne() {
        RefreshToken stored = new RefreshToken();
        stored.setToken("old-token");
        stored.setUsername("user@example.com");
        stored.setExpiryDate(LocalDateTime.now().plusDays(1));
        stored.setRevoked(false);

        when(refreshTokenRepository.findByToken("old-token")).thenReturn(Optional.of(stored));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken newToken = refreshTokenService.rotate("old-token");

        assertThat(stored.isRevoked()).isTrue();
        assertThat(newToken.getToken()).isNotEqualTo("old-token");
        assertThat(newToken.getUsername()).isEqualTo("user@example.com");
        assertThat(newToken.isRevoked()).isFalse();
    }

    @Test
    void rotate_rejectsAlreadyRevokedToken() {
        RefreshToken stored = new RefreshToken();
        stored.setToken("revoked-token");
        stored.setUsername("user@example.com");
        stored.setExpiryDate(LocalDateTime.now().plusDays(1));
        stored.setRevoked(true);

        when(refreshTokenRepository.findByToken("revoked-token")).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> refreshTokenService.rotate("revoked-token"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rotate_rejectsExpiredToken() {
        RefreshToken stored = new RefreshToken();
        stored.setToken("expired-token");
        stored.setUsername("user@example.com");
        stored.setExpiryDate(LocalDateTime.now().minusMinutes(1));
        stored.setRevoked(false);

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> refreshTokenService.rotate("expired-token"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rotate_rejectsUnknownToken() {
        when(refreshTokenRepository.findByToken("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.rotate("unknown"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void revoke_marksMatchingTokenAsRevoked() {
        RefreshToken stored = new RefreshToken();
        stored.setToken("token-to-revoke");
        stored.setRevoked(false);
        when(refreshTokenRepository.findByToken("token-to-revoke")).thenReturn(Optional.of(stored));

        refreshTokenService.revoke("token-to-revoke");

        assertThat(stored.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(stored);
    }

    @Test
    void revoke_doesNothing_whenTokenNotFound() {
        when(refreshTokenRepository.findByToken("missing")).thenReturn(Optional.empty());

        refreshTokenService.revoke("missing");
        // No exception, no save call expected on a nonexistent token.
    }
}
