package de.focusshift.zeiterfassung.security;

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
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static de.focusshift.zeiterfassung.security.SessionServiceImpl.RELOAD_AUTHORITIES;
import static java.lang.Boolean.TRUE;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

class ReloadAuthenticationAuthoritiesFilter extends OncePerRequestFilter {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final UserManagementService userManagementService;
    private final SessionService sessionService;
    private final DelegatingSecurityContextRepository securityContextRepository;

    ReloadAuthenticationAuthoritiesFilter(UserManagementService userManagementService,
                                          SessionService sessionService,
                                          DelegatingSecurityContextRepository securityContextRepository) {

        this.userManagementService = userManagementService;
        this.sessionService = sessionService;
        this.securityContextRepository = securityContextRepository;
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
        final OAuth2User oAuth2User = oAuth2Auth.getPrincipal();

        final User user = userManagementService.findUserById(new UserId(oAuth2User.getName()))
            .orElseThrow(() -> new IllegalStateException("no user found with userId=" + authentication.getName()));

        final Set<GrantedAuthority> updatedAuthorities = mergeAuthorities(oAuth2Auth, user);
        final Authentication updatedAuthentication = new OAuth2AuthenticationToken(oAuth2User, updatedAuthorities, oAuth2Auth.getAuthorizedClientRegistrationId());

        context.setAuthentication(updatedAuthentication);
        securityContextRepository.saveContext(context, request, response);
        LOG.info("Updated authorities of person with the id {} from {} to {}", user.userIdComposite(), authentication.getAuthorities(), updatedAuthorities);

        chain.doFilter(request, response);
    }

    private static Set<GrantedAuthority> mergeAuthorities(OAuth2AuthenticationToken token, User user) {

        // token.principal.authorities contains the original authorities coming from oidc provider
        // while token.authorities is a list already modified by us
        final HashSet<GrantedAuthority> updatedAuthorities = new HashSet<>(token.getPrincipal().getAuthorities());
        updatedAuthorities.addAll(user.authorities().stream().map(role -> new SimpleGrantedAuthority(role.name())).toList());

        return updatedAuthorities;
    }
}
