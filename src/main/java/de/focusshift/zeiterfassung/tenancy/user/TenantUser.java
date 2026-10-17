package de.focusshift.zeiterfassung.tenancy.user;

import de.focusshift.zeiterfassung.security.SecurityRole;
import org.springframework.security.core.GrantedAuthority;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * @param eMailVerified whether the identity provider has verified the {@linkplain #eMail() email address}, {@code null} if unknown
 */
public record TenantUser(
    String id,
    Long localId,
    String givenName,
    String familyName,
    EMailAddress eMail,
    Boolean eMailVerified,
    Instant firstLoginAt,
    Set<SecurityRole> authorities,
    Instant createdAt,
    Instant updatedAt,
    Instant deactivatedAt,
    Instant deletedAt,
    UserStatus status
) {

    @Override
    public String toString() {
        return "TenantUser{" +
            "id='" + id + '\'' +
            ", localId=" + localId +
            '}';
    }

    /**
     * user is active when {@linkplain #status()} is {@linkplain UserStatus#ACTIVE} or {@linkplain UserStatus#UNKNOWN}.
     *
     * @return {@code true} when user is active, {@code false} otherwise
     */
    public boolean isActive() {
        return status == UserStatus.ACTIVE || status == UserStatus.UNKNOWN;
    }


    /**
     * @return {@code true} if the identity provider has verified the {@linkplain #eMail() email address},
     * {@code false} if it is not verified or unknown.
     */
    public boolean isEMailVerified() {
        return Boolean.TRUE.equals(eMailVerified);
    }

    public List<GrantedAuthority> grantedAuthorities() {
        return authorities.stream()
            .map(SecurityRole::authority)
            .toList();
    }

}
