package de.focusshift.zeiterfassung.security.oidc;

import de.focusshift.zeiterfassung.security.SecurityRole;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;

/**
 * {@link OidcUser} enhanced with application specifics like authorities coming from Oidc claims and authorities
 * known in application. Useful since oidc provided authorities could not be synced to the application yet.
 */
public class CurrentOidcUser implements OidcUser {

    private final OidcUser oidcUser;
    private final Collection<? extends GrantedAuthority> applicationAuthorities;
    private final Collection<? extends GrantedAuthority> oidcAuthorities;
    private final UserId userId;
    private final UserLocalId userLocalId;

    public CurrentOidcUser(OidcUser oidcUser,
                           Collection<? extends GrantedAuthority> applicationAuthorities,
                           Collection<? extends GrantedAuthority> oidcAuthorities) {

        this(oidcUser, applicationAuthorities, oidcAuthorities, null);
    }

    public CurrentOidcUser(OidcUser oidcUser,
                           Collection<? extends GrantedAuthority> applicationAuthorities,
                           Collection<? extends GrantedAuthority> oidcAuthorities,
                           UserLocalId userLocalId) {

        this.oidcUser = oidcUser;
        this.applicationAuthorities = applicationAuthorities;
        this.oidcAuthorities = oidcAuthorities;
        this.userId = new UserId(oidcUser.getSubject());
        this.userLocalId = userLocalId;
    }

    public OidcUser getOidcUser() {
        return oidcUser;
    }

    public List<SecurityRole> getRoles() {
        return getAuthorities().stream()
            .map(SecurityRole::fromAuthority)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();
    }

    public boolean hasRole(SecurityRole role) {
        return getAuthorities().contains(role.authority());
    }

    /**
     * Returns the {@link UserIdComposite} of the user.
     *
     * @return the {@link UserIdComposite} of the user
     * @throws IllegalStateException when there is no {@link UserLocalId} present
     */
    public UserIdComposite getUserIdComposite() {
        final UserLocalId localId = getUserLocalId().orElseThrow(() -> new IllegalStateException("expected userLocalId to be present for " + userId));
        return new UserIdComposite(userId, localId);
    }

    public UserId getUserId() {
        return userId;
    }

    public Optional<UserLocalId> getUserLocalId() {
        return Optional.ofNullable(userLocalId);
    }

    public Collection<? extends GrantedAuthority> getApplicationAuthorities() {
        return applicationAuthorities;
    }

    public Collection<? extends GrantedAuthority> getOidcAuthorities() {
        return oidcAuthorities;
    }

    @Override
    public Map<String, Object> getClaims() {
        return oidcUser.getClaims();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return oidcUser.getUserInfo();
    }

    @Override
    public OidcIdToken getIdToken() {
        return oidcUser.getIdToken();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return oidcUser.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return concat(applicationAuthorities.stream(), oidcAuthorities.stream()).collect(toSet());
    }

    @Override
    public String getName() {
        return oidcUser.getName();
    }
}
