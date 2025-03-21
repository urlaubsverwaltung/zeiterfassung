package de.focusshift.zeiterfassung.security;

import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static de.focusshift.zeiterfassung.security.SessionServiceImpl.RELOAD_AUTHORITIES;
import static java.lang.Boolean.TRUE;
import static java.lang.invoke.MethodHandles.lookup;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;
import static org.slf4j.LoggerFactory.getLogger;

class ReloadAuthenticationAuthoritiesFilter extends OncePerRequestFilter {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final UserManagementService userManagementService;
    private final SessionService sessionService;
    private final DelegatingSecurityContextRepository securityContextRepository;
    private final TenantContextHolder tenantContextHolder;

    ReloadAuthenticationAuthoritiesFilter(
        UserManagementService userManagementService,
        SessionService sessionService,
        DelegatingSecurityContextRepository securityContextRepository,
        TenantContextHolder tenantContextHolder
    ) {
        this.userManagementService = userManagementService;
        this.sessionService = sessionService;
        this.securityContextRepository = securityContextRepository;
        this.tenantContextHolder = tenantContextHolder;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        final HttpSession session = request.getSession();
        if (session == null) {
            return true;
        }

        final Boolean reload = (Boolean) session.getAttribute(RELOAD_AUTHORITIES);
        return !TRUE.equals(reload);
    }

    @Override
    public void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain chain) throws ServletException, IOException {

        final HttpSession session = request.getSession();
        sessionService.unmarkSessionToReloadAuthorities(session.getId());

        final SecurityContext context = SecurityContextHolder.getContext();
        final Authentication authentication = context.getAuthentication();

        final OAuth2AuthenticationToken oAuth2Auth = (OAuth2AuthenticationToken) authentication;
        final CurrentOidcUser currentOidcUser = (CurrentOidcUser) oAuth2Auth.getPrincipal();

        tenantContextHolder.runInTenantIdContext(oAuth2Auth.getAuthorizedClientRegistrationId(), tenantId -> {
            final User user = getUserFromUserManagement(currentOidcUser);

            final List<SimpleGrantedAuthority> applicationAuthoritiesNew = getUserAuthorities(user);
            final Collection<? extends GrantedAuthority> oidcAuthorities = currentOidcUser.getOidcAuthorities();
            final Set<GrantedAuthority> updatedMergedAuthorities = concat(oidcAuthorities.stream(), applicationAuthoritiesNew.stream()).collect(toSet());

            final CurrentOidcUser updatedCurrentOidcUser = new CurrentOidcUser(currentOidcUser.getOidcUser(), applicationAuthoritiesNew, oidcAuthorities, currentOidcUser.getUserLocalId().orElseThrow());
            final OAuth2AuthenticationToken updatedAuthentication = new OAuth2AuthenticationToken(updatedCurrentOidcUser, updatedMergedAuthorities, tenantId);

            context.setAuthentication(updatedAuthentication);
            securityContextRepository.saveContext(context, request, response);
            LOG.info("Updated authorities of person with the id {} from {} to {}", user.userIdComposite(), authentication.getAuthorities(), updatedMergedAuthorities);
        });

        chain.doFilter(request, response);
    }

    private User getUserFromUserManagement(CurrentOidcUser currentOidcUser) {
        final String userId = currentOidcUser.getSubject();
        return userManagementService.findUserById(new UserId(userId))
            .orElseThrow(() -> new IllegalStateException("no user found with userId=" + userId));
    }

    private static List<SimpleGrantedAuthority> getUserAuthorities(User user) {
        return user.authorities().stream().map(role -> new SimpleGrantedAuthority(role.name())).toList();
    }
}
