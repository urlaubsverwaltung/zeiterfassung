package de.focusshift.zeiterfassung.security.oidc;

import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.security.oauth2.core.oidc.IdTokenClaimNames.SUB;
import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.EMAIL;
import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.FAMILY_NAME;
import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.GIVEN_NAME;

@ExtendWith(MockitoExtension.class)
class OidcPersonAuthoritiesMapperTest {

    private OidcPersonAuthoritiesMapper sut;

    @Mock
    private UserManagementService userManagementService;

    @BeforeEach
    void setUp() {
        sut = new OidcPersonAuthoritiesMapper(userManagementService);
    }

    @Test
    void mapAuthoritiesFromIdToken() {
        final String uniqueID = "uniqueID";
        final String email = "test.me@example.com";

        final OidcUserAuthority oidcUserAuthority = getOidcUserAuthority(Map.of(
            SUB, uniqueID,
            EMAIL, email
        ));

        final UserId userId_1 = new UserId("uniqueID");
        final UserLocalId userLocalId_1 = new UserLocalId(1L);
        final UserIdComposite userIdComposite_1 = new UserIdComposite(userId_1, userLocalId_1);
        final User user_1 = new User(userIdComposite_1, "Bruce", "Wayne", new EMailAddress(""), Set.of(ZEITERFASSUNG_USER));

        when(userManagementService.findUserById(userId_1)).thenReturn(Optional.of(user_1));

        final Collection<? extends GrantedAuthority> grantedAuthorities = sut.mapAuthorities(List.of(oidcUserAuthority));

        assertThat(grantedAuthorities.stream().map(GrantedAuthority::getAuthority))
            .containsExactlyInAnyOrder("OIDC_USER", ZEITERFASSUNG_USER.name());
    }

    @Test
    void ensureFallbackToUserInfoIfGivenNameIsMissingInIdToken() {
        final String uniqueID = "uniqueID";
        final String givenName = "given name";

        final OidcUserAuthority oidcUserAuthorities = getOidcUserAuthority(
            Map.of(
                SUB, uniqueID
            ),
            Map.of(
                GIVEN_NAME, givenName
            )
        );

        when(userManagementService.findUserById(new UserId(uniqueID))).thenReturn(Optional.empty());

        final Collection<? extends GrantedAuthority> grantedAuthorities = sut.mapAuthorities(List.of(oidcUserAuthorities));

        assertThat(grantedAuthorities.stream().map(GrantedAuthority::getAuthority))
            .containsExactlyInAnyOrder("OIDC_USER", ZEITERFASSUNG_USER.name());
    }

    @Test
    void ensureFallbackToUserInfoIfLastnameIsMissingInIdToken() {
        final String uniqueID = "uniqueID";
        final String givenName = "given name";
        final String email = "test.me@example.com";

        final OidcUserAuthority oidcUserAuthorities = getOidcUserAuthority(
            Map.of(
                SUB, uniqueID,
                GIVEN_NAME, givenName,
                EMAIL, email
            )
        );

        when(userManagementService.findUserById(new UserId(uniqueID))).thenReturn(Optional.empty());

        final Collection<? extends GrantedAuthority> grantedAuthorities = sut.mapAuthorities(List.of(oidcUserAuthorities));

        assertThat(grantedAuthorities.stream().map(GrantedAuthority::getAuthority))
            .containsExactlyInAnyOrder("OIDC_USER", ZEITERFASSUNG_USER.name());
    }

    @Test
    void ensureFallbackToUserInfoIfEmailIsMissingInIdToken() {
        final String uniqueID = "uniqueID";
        final String givenName = "given name";
        final String familyName = "family name";
        final String email = "test.me@example.com";

        final OidcUserAuthority oidcUserAuthorities = getOidcUserAuthority(
            Map.of(
                SUB, uniqueID,
                GIVEN_NAME, givenName,
                FAMILY_NAME, familyName
            ),
            Map.of(
                EMAIL, email
            )
        );

        when(userManagementService.findUserById(new UserId(uniqueID))).thenReturn(Optional.empty());

        final Collection<? extends GrantedAuthority> grantedAuthorities = sut.mapAuthorities(List.of(oidcUserAuthorities));

        assertThat(grantedAuthorities.stream().map(GrantedAuthority::getAuthority))
            .containsExactlyInAnyOrder("OIDC_USER", ZEITERFASSUNG_USER.name());
    }

    @Test
    void mapAuthoritiesFromUserInfoByCreate() {
        final String uniqueID = "uniqueID";
        final String givenName = "test";
        final String familyName = "me";
        final String email = "test.me@example.com";

        final OidcUserAuthority oidcUserAuthority = getOidcUserAuthority(
            Map.of(
                SUB, uniqueID
            ),
            Map.of(
                GIVEN_NAME, givenName,
                FAMILY_NAME, familyName,
                EMAIL, email
            ));

        when(userManagementService.findUserById(new UserId(uniqueID))).thenReturn(Optional.empty());

        final Collection<? extends GrantedAuthority> grantedAuthorities = sut.mapAuthorities(List.of(oidcUserAuthority));

        assertThat(grantedAuthorities.stream().map(GrantedAuthority::getAuthority))
            .containsExactlyInAnyOrder("OIDC_USER", ZEITERFASSUNG_USER.name());
    }

    @Test
    void mapAuthoritiesFromUserInfoBySync() {
        final String uniqueID = "uniqueID";
        final String givenName = "test";
        final String familyName = "me";
        final String email = "test.me@example.com";

        final OidcUserAuthority oidcUserAuthority = getOidcUserAuthority(
            Map.of(
                SUB, uniqueID
            ),
            Map.of(
                GIVEN_NAME, givenName,
                FAMILY_NAME, familyName,
                EMAIL, email
            ));

        when(userManagementService.findUserById(new UserId(uniqueID))).thenReturn(Optional.empty());

        final Collection<? extends GrantedAuthority> grantedAuthorities = sut.mapAuthorities(List.of(oidcUserAuthority));

        assertThat(grantedAuthorities.stream().map(GrantedAuthority::getAuthority))
            .containsExactlyInAnyOrder("OIDC_USER", ZEITERFASSUNG_USER.name());
    }

    @Test
    void mapAuthoritiesWrongAuthority() {
        final List<SimpleGrantedAuthority> noRole = List.of(new SimpleGrantedAuthority("NO_ROLE"));
        assertThatThrownBy(() -> sut.mapAuthorities(noRole))
            .isInstanceOf(OidcPersonMappingException.class);
    }

    private OidcUserAuthority getOidcUserAuthority(Map<String, Object> idTokenClaims) {
        final OidcIdToken idToken = new OidcIdToken("tokenValue", Instant.now(), Instant.MAX, idTokenClaims);
        return new OidcUserAuthority(idToken);
    }

    private OidcUserAuthority getOidcUserAuthority(Map<String, Object> idTokenClaims, Map<String, Object> userInfoClaims) {
        final OidcIdToken idToken = new OidcIdToken("tokenValue", Instant.now(), Instant.MAX, idTokenClaims);
        final OidcUserInfo userInfo = new OidcUserInfo(userInfoClaims);
        return new OidcUserAuthority(idToken, userInfo);
    }
}
