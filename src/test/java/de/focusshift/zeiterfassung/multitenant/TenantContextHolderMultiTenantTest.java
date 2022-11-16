package de.focusshift.zeiterfassung.multitenant;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TenantContextHolderMultiTenantTest {

    private TenantContextHolderMultiTenant sut = new TenantContextHolderMultiTenant();

    @Test
    void ensureTenantForOAuth2AuthenticationToken() {

        final OAuth2User oAuth2User = mock(OAuth2User.class);
        final Authentication authentication = new OAuth2AuthenticationToken(oAuth2User, List.of(), "a154bc4e");

        final SecurityContext securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        assertThat(sut.getCurrentTenantId()).hasValue(new TenantId("a154bc4e"));

    }

    private static Stream<Authentication> authenticationSource() {
        return Stream.of(
            null,
            UsernamePasswordAuthenticationToken.authenticated(null, null, List.of())
        );
    }

    @ParameterizedTest
    @MethodSource("authenticationSource")
    void ensureExceptionWithoutOAuth2AuthenticationToken(final Authentication authentication) {

        final SecurityContext securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        assertThat(sut.getCurrentTenantId()).isNotPresent();
    }
}
