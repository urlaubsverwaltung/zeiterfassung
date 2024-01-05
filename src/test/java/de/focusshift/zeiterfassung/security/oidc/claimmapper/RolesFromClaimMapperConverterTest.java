package de.focusshift.zeiterfassung.security.oidc.claimmapper;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;

class RolesFromClaimMapperConverterTest {

    @Test
    void ensureToConvertStringToGrantedAuthority() {
        final RolesFromClaimMapperConverter converter = new RolesFromClaimMapperConverter();
        final GrantedAuthority grantedAuthority = converter.convert("zeiterfassung_user");
        assertThat(grantedAuthority.getAuthority()).isEqualTo("ZEITERFASSUNG_USER");
    }

    @Test
    void ensureToReturnNullIfNotARole() {
        final RolesFromClaimMapperConverter converter = new RolesFromClaimMapperConverter();
        final GrantedAuthority grantedAuthority = converter.convert("urlaubsverwaltung_NotARole");
        assertThat(grantedAuthority).isNull();
    }
}
