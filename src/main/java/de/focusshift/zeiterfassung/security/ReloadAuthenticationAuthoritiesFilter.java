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
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
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

        final HttpSession session = request.getSession(false);
        if (session == null) {
            LOG.debug("No session available, skipping authority reload and continuing filter chain.");
            chain.doFilter(request, response);
            return;
        }

        sessionService.unmarkSessionToReloadAuthorities(session.getId());

        final SecurityContext context = SecurityContextHolder.getContext();
        final Authentication authentication = context.getAuthentication();

        if (authentication == null) {
            LOG.warn("SecurityContext has no authentication, cannot reload authorities. Session: {}", session.getId());
            chain.doFilter(request, response);
            return;
        }

        if (!(authentication instanceof OAuth2AuthenticationToken oAuth2Auth)) {
            LOG.warn("Authentication is not an OAuth2AuthenticationToken (type={}), cannot reload authorities.", authentication.getClass().getSimpleName());
            chain.doFilter(request, response);
            return;
        }

        final Object principal = oAuth2Auth.getPrincipal();
        if (!(principal instanceof CurrentOidcUser currentOidcUser)) {
            LOG.warn("Principal is not a CurrentOidcUser (type={}), cannot reload authorities.",
                principal != null ? principal.getClass().getSimpleName() : "null");
            chain.doFilter(request, response);
            return;
        }

        final String registrationId = oAuth2Auth.getAuthorizedClientRegistrationId();
        if (registrationId == null) {
            LOG.warn("No authorized client registration ID found, cannot determine tenant context.");
            chain.doFilter(request, response);
            return;
        }

        final var userLocalIdOptional = currentOidcUser.getUserLocalId();
        if (userLocalIdOptional.isEmpty()) {
            LOG.warn("CurrentOidcUser has no UserLocalId, cannot reload authorities.");
            chain.doFilter(request, response);
            return;
        }

        tenantContextHolder.runInTenantIdContext(registrationId, tenantId -> {
            final User user = getUserFromUserManagement(currentOidcUser);

            // Defensive: treat null authority collections as empty
            final var safeOidcAuthorities = currentOidcUser.getOidcAuthorities() != null ? currentOidcUser.getOidcAuthorities() : List.<GrantedAuthority>of();
            final var safeAppAuthorities = user.grantedAuthorities();

            final Set<GrantedAuthority> updatedMergedAuthorities = concat(safeOidcAuthorities.stream(), safeAppAuthorities.stream()).collect(toSet());

            final CurrentOidcUser updatedCurrentOidcUser = new CurrentOidcUser(
                currentOidcUser.getOidcUser(),
                safeAppAuthorities,
                safeOidcAuthorities,
                userLocalIdOptional.orElseThrow(() -> new IllegalStateException("UserLocalId disappeared after null-check"))
            );
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

}
