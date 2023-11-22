package de.focusshift.zeiterfassung.security.oidc.claimmapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.oauth2.core.oidc.IdTokenClaimNames.SUB;
import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.EMAIL;
import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.FAMILY_NAME;
import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.GIVEN_NAME;

@ExtendWith(MockitoExtension.class)
class RolesFromResourceAccessClaimMapperTest {

    @Test
    void ensureToMapClaimsFromResourceAccessClaim() {

        final RolesFromClaimMappersProperties properties = new RolesFromClaimMappersProperties();
        final RolesFromClaimMapperConverter converter = new RolesFromClaimMapperConverter();
        final RolesFromResourceAccessClaimMapper sut = new RolesFromResourceAccessClaimMapper(converter, properties);

        final Map<String, Object> claims = Map.of(
            SUB, "uniqueID",
            GIVEN_NAME, "givenName",
            FAMILY_NAME, "familyNam",
            EMAIL, "email",
            "resource_access", Map.of("zeiterfassung", Map.of("roles",
                List.of("zeiterfassung_user", "zeiterfassung_view_report_all")
            ))
        );

        final List<GrantedAuthority> authorities = sut.mapClaimToRoles(claims);
        assertThat(authorities.stream().map(GrantedAuthority::getAuthority))
            .containsExactly("ZEITERFASSUNG_USER", "ZEITERFASSUNG_VIEW_REPORT_ALL");
    }

    @Test
    void ensureToThrowExceptionIfNeededAccessRoleIsNotGiven() {

        final RolesFromClaimMappersProperties properties = new RolesFromClaimMappersProperties();
        final RolesFromClaimMapperConverter converter = new RolesFromClaimMapperConverter();
        final RolesFromResourceAccessClaimMapper sut = new RolesFromResourceAccessClaimMapper(converter, properties);

        final Map<String, Object> claims = Map.of(
            SUB, "uniqueID",
            GIVEN_NAME, "givenName",
            FAMILY_NAME, "familyNam",
            EMAIL, "email",
            "resource_access", Map.of("zeiterfassung", Map.of("roles",
                List.of("ZEITERFASSUNG_VIEW_REPORT_ALL")
            ))
        );

        assertThatThrownBy(() -> sut.mapClaimToRoles(claims))
            .isInstanceOf(MissingClaimAuthorityException.class)
            .hasMessageContaining("User has not required permission 'zeiterfassung_user' to access zeiterfassung!");
    }

    @Test
    void ensureToMapClaimsFromResourceAccessClaimConfiguredResourceApp() {

        final RolesFromClaimMappersProperties properties = new RolesFromClaimMappersProperties();
        properties.getResourceAccessClaim().setResourceApp("otherResourceApp");
        final RolesFromClaimMapperConverter converter = new RolesFromClaimMapperConverter();
        final RolesFromResourceAccessClaimMapper sut = new RolesFromResourceAccessClaimMapper(converter, properties);

        final Map<String, Object> claims = Map.of(
            SUB, "uniqueID",
            GIVEN_NAME, "givenName",
            FAMILY_NAME, "familyNam",
            EMAIL, "email",
            "resource_access", Map.of("otherResourceApp", Map.of("roles",
                List.of("zeiterfassung_user", "zeiterfassung_view_report_all")
            ))
        );

        final List<GrantedAuthority> authorities = sut.mapClaimToRoles(claims);
        assertThat(authorities.stream().map(GrantedAuthority::getAuthority))
            .containsExactly("ZEITERFASSUNG_USER", "ZEITERFASSUNG_VIEW_REPORT_ALL");
    }
}
