package de.focusshift.zeiterfassung.security;

import de.focusshift.zeiterfassung.user.CurrentUserProvider;
import de.focusshift.zeiterfassung.usermanagement.User;
import org.slf4j.Logger;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import static de.focusshift.zeiterfassung.security.SessionService.RELOAD_AUTHORITIES;
import static java.lang.Boolean.TRUE;
import static java.lang.invoke.MethodHandles.lookup;
import static java.util.function.Predicate.not;
import static org.slf4j.LoggerFactory.getLogger;

class ReloadAuthenticationAuthoritiesFilter extends OncePerRequestFilter {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final CurrentUserProvider currentUserProvider;
    private final SessionService sessionService;

    ReloadAuthenticationAuthoritiesFilter(CurrentUserProvider currentUserProvider, SessionService sessionService) {
        this.currentUserProvider = currentUserProvider;
        this.sessionService = sessionService;
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

        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        final User user = currentUserProvider.getCurrentUser();
        final List<GrantedAuthority> updatedAuthorities = getUpdatedAuthorities(authentication, user);

        try {
            final Authentication updatedAuthentication = getUpdatedAuthentication(updatedAuthorities, authentication);
            SecurityContextHolder.getContext().setAuthentication(updatedAuthentication);
            LOG.info("Updated authorities of person with the id {} from {} to {}", user.id(), authentication.getAuthorities(), updatedAuthorities);
        } catch (ReloadAuthenticationException e) {
            LOG.error(e.getMessage());
        }

        chain.doFilter(request, response);
    }

    private List<GrantedAuthority> getUpdatedAuthorities(Authentication authentication, User user) {

        final Stream<? extends GrantedAuthority> otherAuthorities = authentication.getAuthorities()
            .stream()
            .filter(not(grantedAuthority -> grantedAuthority.getAuthority().startsWith("ROLE_ZEITERFASSUNG")));

        final Stream<SimpleGrantedAuthority> zeiterfassungAuthorities = user.authorities().stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()));

        return Stream.concat(otherAuthorities, zeiterfassungAuthorities).toList();
    }

    private Authentication getUpdatedAuthentication(List<GrantedAuthority> updatedAuthorities, Authentication authentication) throws ReloadAuthenticationException {
        final Authentication updatedAuthentication;
        if (authentication instanceof OAuth2AuthenticationToken) {
            final OAuth2AuthenticationToken oAuth2Auth = (OAuth2AuthenticationToken) authentication;
            updatedAuthentication = new OAuth2AuthenticationToken(oAuth2Auth.getPrincipal(), updatedAuthorities, oAuth2Auth.getAuthorizedClientRegistrationId());
        } else if (authentication instanceof UsernamePasswordAuthenticationToken) {
            updatedAuthentication = new UsernamePasswordAuthenticationToken(authentication.getPrincipal(), authentication.getCredentials(), updatedAuthorities);
        } else {
            throw new ReloadAuthenticationException("Could not update authentication with updated authorities, because of type mismatch");
        }

        return updatedAuthentication;
    }
}
