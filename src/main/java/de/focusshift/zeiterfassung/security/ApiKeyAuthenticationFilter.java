package de.focusshift.zeiterfassung.security;

import de.focusshift.zeiterfassung.apikey.ApiKeyService;
import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final ApiKeyService apiKeyService;
    private final UserManagementService userManagementService;

    public ApiKeyAuthenticationFilter(ApiKeyService apiKeyService, UserManagementService userManagementService) {
        this.apiKeyService = apiKeyService;
        this.userManagementService = userManagementService;
    }

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String header = request.getHeader(AUTHORIZATION_HEADER);

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            final String apiKey = header.substring(BEARER_PREFIX.length());

            // Validiere API-Key und lade User
            // TODO: ApiKeyService muss User zurückgeben
            // Optional<User> userOpt = apiKeyService.validateApiKey(apiKey);
            //
            // userOpt.ifPresent(user -> {
            //     CurrentOidcUser oidcUser = createOidcUser(user);
            //     UsernamePasswordAuthenticationToken authentication =
            //         new UsernamePasswordAuthenticationToken(oidcUser, null, oidcUser.getAuthorities());
            //     SecurityContextHolder.getContext().setAuthentication(authentication);
            // });
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }
}
