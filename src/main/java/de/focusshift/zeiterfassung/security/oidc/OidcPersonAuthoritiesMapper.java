package de.focusshift.zeiterfassung.security.oidc;

import de.focusshift.zeiterfassung.security.SecurityRole;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import org.slf4j.Logger;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_USER;
import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Optional.ofNullable;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.SUB;

public class OidcPersonAuthoritiesMapper implements GrantedAuthoritiesMapper {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final UserManagementService userManagementService;

    public OidcPersonAuthoritiesMapper(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @Override
    public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {

        final Collection<? extends GrantedAuthority> applicationAuthorities = authorities
            .stream()
            .filter(OidcUserAuthority.class::isInstance)
            .findFirst()
            .map(OidcUserAuthority.class::cast)
            .map(this::mapAuthorities)
            .orElseThrow(() -> new OidcPersonMappingException("oidc: The granted authority was not a 'OidcUserAuthority' and the user cannot be mapped."));

        return Stream.concat(applicationAuthorities.stream(), authorities.stream()).toList();
    }

    private Collection<? extends GrantedAuthority> mapAuthorities(OidcUserAuthority oidcUserAuthority) {
        return resolvePerson(oidcUserAuthority)
            .map(this::extractPermissions).orElseGet(this::generateListOfRoles)
            .stream()
            .map(SecurityRole::name)
            .map(SimpleGrantedAuthority::new)
            .toList();
    }

    private List<SecurityRole> generateListOfRoles() {
        return List.of(ZEITERFASSUNG_USER);
    }

    private Collection<SecurityRole> extractPermissions(User user) {
        return user.authorities();
    }

    private Optional<User> resolvePerson(OidcUserAuthority oidcUserAuthority) {
        return userManagementService.findUserById(extractUserId(oidcUserAuthority));
    }

    private UserId extractUserId(OidcUserAuthority authority) {
        return getClaimAsString(authority, () -> SUB)
            .map(UserId::new)
            .orElseThrow(() -> {
                LOG.error("Can not retrieve the subject of the id token for oidc person mapping on {} ", authority);
                return new OidcPersonMappingException("Can not retrieve the subject of the id token for oidc person mapping");
            });
    }

    private Optional<String> getClaimAsString(OidcUserAuthority authority, Supplier<String> claimAccessor) {
        return ofNullable(authority.getIdToken()).map(oidcIdToken -> oidcIdToken.getClaimAsString(claimAccessor.get()))
            .or(() -> ofNullable(authority.getUserInfo()).map(oidcIdToken -> oidcIdToken.getClaimAsString(claimAccessor.get())));
    }
}
