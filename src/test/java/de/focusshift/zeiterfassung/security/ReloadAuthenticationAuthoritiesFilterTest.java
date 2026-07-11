package de.focusshift.zeiterfassung.security;

import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.tenancy.authentication.TenantIdProvider;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
    @Mock
    private TenantIdProvider tenantIdProvider;

    @BeforeEach
    void setUp() {
        sut = new ReloadAuthenticationAuthoritiesFilter(userManagementService, sessionService, securityContextRepository, tenantContextHolder, tenantIdProvider);
    }

    @Test
    void ensuresFilterSetsOAuth2AuthenticationWithNewAuthorities() throws ServletException, IOException {

        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final FilterChain filterChain = mock(FilterChain.class);

        request.getSession().setAttribute("reloadAuthorities", true);

        final SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(prepareOAuth2Authentication("username"));

        when(tenantIdProvider.resolve(any(OAuth2AuthenticationToken.class)))
            .thenReturn(Optional.of(new TenantId("authorizedClientRegistrationId")));

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

    // ---- shouldNotFilter tests ----

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

    @Test
    void ensuresFilterSetsAuthenticationWithNewAuthoritiesButSessionIsNullDoNothing() {

        final MockHttpServletRequest request = mock(MockHttpServletRequest.class);
        when(request.getSession()).thenReturn(null);

        final boolean shouldNotFilter = sut.shouldNotFilter(request);
        assertThat(shouldNotFilter).isTrue();
    }

    // ---- doFilterInternal defensive guard clause tests ----

    @Test
    void doFilterInternalWithNullAuthenticationContinuesChainWithoutError() throws ServletException, IOException {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("reloadAuthorities", true);

        final MockHttpServletResponse response = new MockHttpServletResponse();
        final FilterChain filterChain = mock(FilterChain.class);

        final SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(null);

        sut.doFilterInternal(request, response, filterChain);

        verify(sessionService).unmarkSessionToReloadAuthorities(request.getSession().getId());
        verify(securityContextRepository, never()).saveContext(context, request, response);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternalWithNonOAuth2AuthenticationContinuesChainWithoutError() throws ServletException, IOException {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("reloadAuthorities", true);

        final MockHttpServletResponse response = new MockHttpServletResponse();
        final FilterChain filterChain = mock(FilterChain.class);

        final SecurityContext context = SecurityContextHolder.getContext();
        // Use a mock Authentication that is NOT an OAuth2AuthenticationToken
        final Authentication authentication = mock(Authentication.class);
        context.setAuthentication(authentication);

        sut.doFilterInternal(request, response, filterChain);

        verify(sessionService).unmarkSessionToReloadAuthorities(request.getSession().getId());
        verify(securityContextRepository, never()).saveContext(context, request, response);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternalWithPrincipalNotCurrentOidcUserContinuesChainWithoutError() throws ServletException, IOException {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("reloadAuthorities", true);

        final MockHttpServletResponse response = new MockHttpServletResponse();
        final FilterChain filterChain = mock(FilterChain.class);

        final SecurityContext context = SecurityContextHolder.getContext();

        // Create a DefaultOidcUser (not CurrentOidcUser) as the principal
        final DefaultOidcUser plainOidcUser = new DefaultOidcUser(
            List.of(),
            OidcIdToken.withTokenValue("token-value").claim("claim", "not-empty").subject("username").build()
        );

        final OAuth2AuthenticationToken authentication = mock(OAuth2AuthenticationToken.class);
        when(authentication.getPrincipal()).thenReturn(plainOidcUser);
        context.setAuthentication(authentication);

        sut.doFilterInternal(request, response, filterChain);

        verify(sessionService).unmarkSessionToReloadAuthorities(request.getSession().getId());
        verify(securityContextRepository, never()).saveContext(context, request, response);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternalWithNullPrincipalContinuesChainWithoutError() throws ServletException, IOException {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("reloadAuthorities", true);

        final MockHttpServletResponse response = new MockHttpServletResponse();
        final FilterChain filterChain = mock(FilterChain.class);

        final SecurityContext context = SecurityContextHolder.getContext();

        final OAuth2AuthenticationToken authentication = mock(OAuth2AuthenticationToken.class);
        doAnswer(invocation -> null).when(authentication).getPrincipal();
        context.setAuthentication(authentication);

        sut.doFilterInternal(request, response, filterChain);

        verify(sessionService).unmarkSessionToReloadAuthorities(request.getSession().getId());
        verify(securityContextRepository, never()).saveContext(context, request, response);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternalWithUnresolvableTenantIdContinuesChainWithoutError() throws ServletException, IOException {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("reloadAuthorities", true);

        final MockHttpServletResponse response = new MockHttpServletResponse();
        final FilterChain filterChain = mock(FilterChain.class);

        final SecurityContext context = SecurityContextHolder.getContext();

        final DefaultOidcUser oidcUser = new DefaultOidcUser(
            List.of(),
            OidcIdToken.withTokenValue("token-value").claim("claim", "not-empty").subject("username").build()
        );
        final CurrentOidcUser currentOidcUser = new CurrentOidcUser(oidcUser, List.of(), List.of(), new UserLocalId(1L));

        final OAuth2AuthenticationToken authentication = mock(OAuth2AuthenticationToken.class);
        when(authentication.getPrincipal()).thenReturn(currentOidcUser);
        context.setAuthentication(authentication);

        when(tenantIdProvider.resolve(any(OAuth2AuthenticationToken.class))).thenReturn(Optional.empty());

        sut.doFilterInternal(request, response, filterChain);

        verify(sessionService).unmarkSessionToReloadAuthorities(request.getSession().getId());
        verify(securityContextRepository, never()).saveContext(context, request, response);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternalWithNullRegistrationIdContinuesChainWithoutError() throws ServletException, IOException {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("reloadAuthorities", true);

        final MockHttpServletResponse response = new MockHttpServletResponse();
        final FilterChain filterChain = mock(FilterChain.class);

        final SecurityContext context = SecurityContextHolder.getContext();

        final DefaultOidcUser oidcUser = new DefaultOidcUser(
            List.of(),
            OidcIdToken.withTokenValue("token-value").claim("claim", "not-empty").subject("username").build()
        );
        final CurrentOidcUser currentOidcUser = new CurrentOidcUser(oidcUser, List.of(), List.of(), new UserLocalId(1L));

        final OAuth2AuthenticationToken authentication = mock(OAuth2AuthenticationToken.class);
        when(authentication.getPrincipal()).thenReturn(currentOidcUser);
        when(authentication.getAuthorizedClientRegistrationId()).thenReturn(null);
        context.setAuthentication(authentication);

        when(tenantIdProvider.resolve(any(OAuth2AuthenticationToken.class))).thenReturn(Optional.of(new TenantId("a154bc4e")));

        sut.doFilterInternal(request, response, filterChain);

        verify(sessionService).unmarkSessionToReloadAuthorities(request.getSession().getId());
        verify(securityContextRepository, never()).saveContext(context, request, response);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternalWithEmptyUserLocalIdContinuesChainWithoutError() throws ServletException, IOException {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("reloadAuthorities", true);

        final MockHttpServletResponse response = new MockHttpServletResponse();
        final FilterChain filterChain = mock(FilterChain.class);

        final SecurityContext context = SecurityContextHolder.getContext();

        // Create CurrentOidcUser with null UserLocalId (using 3-arg constructor)
        final DefaultOidcUser oidcUser = new DefaultOidcUser(
            List.of(),
            OidcIdToken.withTokenValue("token-value").claim("claim", "not-empty").subject("username").build()
        );
        final CurrentOidcUser currentOidcUser = new CurrentOidcUser(oidcUser, List.of(), List.of());

        final OAuth2AuthenticationToken authentication = mock(OAuth2AuthenticationToken.class);
        when(authentication.getPrincipal()).thenReturn(currentOidcUser);
        context.setAuthentication(authentication);

        when(tenantIdProvider.resolve(any(OAuth2AuthenticationToken.class)))
            .thenReturn(Optional.of(new TenantId("some-id")));

        sut.doFilterInternal(request, response, filterChain);

        verify(sessionService).unmarkSessionToReloadAuthorities(request.getSession().getId());
        verify(securityContextRepository, never()).saveContext(context, request, response);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternalWithNullOidcAuthoritiesHandlesGracefully() throws ServletException, IOException {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("reloadAuthorities", true);

        final MockHttpServletResponse response = new MockHttpServletResponse();
        final FilterChain filterChain = mock(FilterChain.class);

        final SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(prepareOAuth2Authentication("username"));

        when(tenantIdProvider.resolve(any(OAuth2AuthenticationToken.class)))
            .thenReturn(Optional.of(new TenantId("authorizedClientRegistrationId")));

        final UserId userId = new UserId("username");
        final User user = new User(
            new UserIdComposite(userId, new UserLocalId(42L)),
            "givenName",
            "familyName",
            new EMailAddress("email@example.org"),
            Set.of(SecurityRole.ZEITERFASSUNG_USER)
        );
        when(userManagementService.findUserById(userId))
            .thenReturn(Optional.of(user));

        sut.doFilterInternal(request, response, filterChain);

        // Should complete without exception even when oidcAuthorities is an empty list
        final List<String> updatedAuthorities = context.getAuthentication().getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .toList();
        assertThat(updatedAuthorities).contains("ZEITERFASSUNG_USER");
        verify(securityContextRepository).saveContext(context, request, response);
    }

    private OAuth2AuthenticationToken prepareOAuth2Authentication(String subject) {

        final DefaultOidcUser oidcUser = new DefaultOidcUser(
            List.of(),
            OidcIdToken.withTokenValue("token-value").claim("claim", "not-empty").subject(subject).build()
        );

        final CurrentOidcUser currentOidcUser = new CurrentOidcUser(oidcUser, List.of(), List.of(), new UserLocalId(1L));

        final OAuth2AuthenticationToken authentication = mock(OAuth2AuthenticationToken.class);
        when(authentication.getPrincipal()).thenReturn(currentOidcUser);
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
