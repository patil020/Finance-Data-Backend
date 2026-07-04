package com.financeapp.auth.security;

import com.financeapp.auth.entity.RefreshToken;
import com.financeapp.auth.repository.RefreshTokenRepository;
import com.financeapp.common.ServiceException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public void store(String refreshToken, String userId, String tokenId, Date expiresAt) {
        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setTokenId(tokenId);
        token.setTokenHash(hash(refreshToken));
        token.setExpiresAt(LocalDateTime.ofInstant(expiresAt.toInstant(), ZoneId.systemDefault()));
        refreshTokenRepository.save(token);
    }

    @Transactional(readOnly = true)
    public RefreshToken requireActive(String refreshToken) {
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(hash(refreshToken))
                .orElseThrow(() -> new ServiceException("Invalid refresh token", "INVALID_TOKEN", 401));

        if (storedToken.isRevoked()) {
            throw new ServiceException("Refresh token has been revoked", "TOKEN_REVOKED", 401);
        }
        if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ServiceException("Refresh token has expired", "TOKEN_EXPIRED", 401);
        }
        return storedToken;
    }

    @Transactional
    public void rotate(RefreshToken oldToken, String newRefreshToken, String newTokenId, Date newExpiresAt) {
        String newHash = hash(newRefreshToken);
        oldToken.setRevoked(true);
        oldToken.setRevokedAt(LocalDateTime.now());
        oldToken.setReplacedByTokenHash(newHash);
        refreshTokenRepository.save(oldToken);

        RefreshToken replacement = new RefreshToken();
        replacement.setUserId(oldToken.getUserId());
        replacement.setTokenId(newTokenId);
        replacement.setTokenHash(newHash);
        replacement.setExpiresAt(LocalDateTime.ofInstant(newExpiresAt.toInstant(), ZoneId.systemDefault()));
        refreshTokenRepository.save(replacement);
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available for refresh token hashing", e);
        }
    }
}
