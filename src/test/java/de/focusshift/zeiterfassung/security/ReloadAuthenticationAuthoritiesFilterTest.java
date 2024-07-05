package de.focusshift.zeiterfassung.security;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReloadAuthenticationAuthoritiesFilterTest {

    private ReloadAuthenticationAuthoritiesFilter sut;

    @Mock
    private UserManagementService userManagementService;
    @Mock
    private SessionService sessionService;
    @Mock
    private DelegatingSecurityContextRepository securityContextRepository;
    @Mock(answer = CALLS_REAL_METHODS)
    private TenantContextHolder tenantContextHolder;

    @BeforeEach
    void setUp() {
        sut = new ReloadAuthenticationAuthoritiesFilter(userManagementService, sessionService, securityContextRepository, tenantContextHolder);
    }

    @Test
    void ensuresFilterSetsOAuth2AuthenticationWithNewAuthorities() throws ServletException, IOException {

        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockFilterChain filterChain = new MockFilterChain();

        request.getSession().setAttribute("reloadAuthorities", true);

        final SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(prepareOAuth2Authentication("username"));

        final UserId userId = new UserId("username");
        when(userManagementService.findUserById(userId))
            .thenReturn(Optional.of(anyUser(userId, Set.of(SecurityRole.ZEITERFASSUNG_VIEW_REPORT_ALL))));

        sut.doFilterInternal(request, response, filterChain);

        final List<String> updatedAuthorities = context.getAuthentication().getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .toList();
        assertThat(updatedAuthorities).containsExactly("ZEITERFASSUNG_VIEW_REPORT_ALL");

        verify(sessionService).unmarkSessionToReloadAuthorities(request.getSession().getId());
        verify(securityContextRepository).saveContext(context, request, response);
    }

    @Test
    void ensuresFilterSetsAuthenticationWithNewAuthoritiesButSessionIsNullDoNothing() {

        final MockHttpServletRequest request = mock(MockHttpServletRequest.class);
        when(request.getSession()).thenReturn(null);

        final boolean shouldNotFilter = sut.shouldNotFilter(request);
        assertThat(shouldNotFilter).isTrue();
    }

    @Test
    void ensuresFilterSetsNoNewAuthenticationIfReloadIsNotDefined() {

        final MockHttpServletRequest request = new MockHttpServletRequest();

        final boolean shouldNotFilter = sut.shouldNotFilter(request);
        assertThat(shouldNotFilter).isTrue();
    }

    @Test
    void ensuresFilterSetsNoNewAuthenticationIfReloadIsFalse() {

        final MockHttpServletRequest request = new MockHttpServletRequest();

        request.getSession().setAttribute("reloadAuthorities", false);

        final boolean shouldNotFilter = sut.shouldNotFilter(request);
        assertThat(shouldNotFilter).isTrue();
    }

    private OAuth2AuthenticationToken prepareOAuth2Authentication(String subject) {

        final DefaultOidcUser oidcUser = new DefaultOidcUser(
            List.of(),
            OidcIdToken.withTokenValue("token-value").claim("claim", "not-empty").subject(subject).build()
        );

        final OAuth2AuthenticationToken authentication = mock(OAuth2AuthenticationToken.class);
        when(authentication.getPrincipal()).thenReturn(oidcUser);
        when(authentication.getAuthorizedClientRegistrationId()).thenReturn("authorizedClientRegistrationId");

        return authentication;
    }

    private static User anyUser(UserId userId, Set<SecurityRole> permissions) {
        return new User(
            new UserIdComposite(userId, new UserLocalId(42L)),
            "givenName",
            "familyName",
            new EMailAddress("email@example.org"),
            permissions
        );
    }
}
