package de.focusshift.zeiterfassung.tenancy.user;

import de.focusshift.zeiterfassung.security.SecurityRole;
import org.springframework.security.core.GrantedAuthority;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public record TenantUser(
    String id,
    Long localId,
    String givenName,
    String familyName,
    EMailAddress eMail,
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


    public List<GrantedAuthority> grantedAuthorities() {
        return authorities.stream()
            .map(SecurityRole::authority)
            .toList();
    }

}
