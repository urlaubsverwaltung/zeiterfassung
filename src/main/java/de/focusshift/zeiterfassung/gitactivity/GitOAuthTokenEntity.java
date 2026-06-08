package de.focusshift.zeiterfassung.gitactivity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * Stores per-user OAuth tokens for external Git platforms (Bitbucket, GitLab, …).
 * One row per app user per platform.
 */
@Entity
@Table(name = "git_oauth_token", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"platform", "user_local_id"}),
    @UniqueConstraint(columnNames = {"platform", "platform_account_id"})
})
public class GitOAuthTokenEntity {

    @Id
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    @SequenceGenerator(name = "git_oauth_token_seq", sequenceName = "git_oauth_token_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "git_oauth_token_seq")
    private Long id;

    /** Platform identifier, e.g. "BITBUCKET" or "GITLAB". */
    @Column(name = "platform", nullable = false, length = 20)
    private String platform;

    /** The app's internal user ID (tenant_user local ID). */
    @Column(name = "user_local_id", nullable = false)
    private Long userLocalId;

    /** The user's account ID on the platform (Bitbucket account_id, GitLab username, …). */
    @Column(name = "platform_account_id", nullable = false, length = 255)
    private String platformAccountId;

    @Column(name = "access_token", nullable = false, columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    /** When the access token expires. Null means the token does not expire. */
    @Column(name = "expires_at")
    private Instant expiresAt;

    public Long getId() { return id; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public Long getUserLocalId() { return userLocalId; }
    public void setUserLocalId(Long userLocalId) { this.userLocalId = userLocalId; }

    public String getPlatformAccountId() { return platformAccountId; }
    public void setPlatformAccountId(String platformAccountId) { this.platformAccountId = platformAccountId; }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    /** Returns true when the token is expired or will expire within the next 60 seconds. */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt.minusSeconds(60));
    }
}
