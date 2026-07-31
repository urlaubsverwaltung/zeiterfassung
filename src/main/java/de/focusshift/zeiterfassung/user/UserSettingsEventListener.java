package de.focusshift.zeiterfassung.user;

import de.focusshift.zeiterfassung.security.oidc.OidcPersonMappingException;
import de.focusshift.zeiterfassung.tenancy.authentication.TenantIdProvider;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Optional.ofNullable;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.SUB;

/**
 * Event listener that listens to successful authentication events of a person
 * and maintains the locale of the person in the user settings as well as
 * in the browser.
 * In this case this listener knows how to extract tenantId from the OAuth2LoginAuthenticationToken
 *
 */
@Service
class UserSettingsEventListener {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final LocaleResolver localeResolver;
    private final UserManagementService userManagementService;
    private final UserSettingsService userSettingsService;
    private final TenantContextHolder tenantContextHolder;
    private final TenantIdProvider tenantIdProvider;


    public UserSettingsEventListener(LocaleResolver localeResolver, UserManagementService userManagementService, UserSettingsService userSettingsService, TenantContextHolder tenantContextHolder, TenantIdProvider tenantIdProvider) {
        this.localeResolver = localeResolver;
        this.userManagementService = userManagementService;
        this.userSettingsService = userSettingsService;
        this.tenantContextHolder = tenantContextHolder;
        this.tenantIdProvider = tenantIdProvider;
    }

    @EventListener
    public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
        if (event.getAuthentication().getPrincipal() instanceof Jwt) {
            return;
        }

        if (event.getAuthentication() instanceof OAuth2LoginAuthenticationToken oAuth2LoginAuthenticationToken) {

            tenantIdProvider.resolve(oAuth2LoginAuthenticationToken).ifPresent(tenantId ->
                tenantContextHolder.runInTenantIdContext(tenantId, passedTenantId -> {

                    if (oAuth2LoginAuthenticationToken.getPrincipal() instanceof OidcUser oidcUser) {
                        final String userName = extractIdentifier(oidcUser);

                        userManagementService.findUserById(new UserId(userName)).ifPresent(user -> {
                            updateUserSettingsWithLocaleBrowserSpecific(user.userIdComposite());
                            userSettingsService.getLocale(user.userIdComposite()).ifPresent(this::setLocale);
                        });
                    }
                }));
        }
    }

    private String extractIdentifier(OidcUser oidcUser) {
        return getClaimAsString(oidcUser, () -> SUB).orElseThrow(() -> {
            LOG.error("Can not retrieve the subject for oidc person mapping");
            return new OidcPersonMappingException("Can not retrieve the subject for oidc person mapping");
        });
    }

    private Optional<String> getClaimAsString(OidcUser oidcUser, Supplier<String> claimSupplier) {
        return ofNullable(oidcUser.getIdToken()).map(oidcIdToken -> oidcIdToken.getClaimAsString(claimSupplier.get())).or(() -> ofNullable(oidcUser.getUserInfo()).map(oidcIdToken -> oidcIdToken.getClaimAsString(claimSupplier.get())));
    }

    private void updateUserSettingsWithLocaleBrowserSpecific(UserIdComposite userIdComposite) {
        getRequest()
            .map(ServletRequest::getLocale)
            .ifPresent(locale -> userSettingsService.updateLocaleBrowserSpecific(userIdComposite, locale));
    }

    private void setLocale(Locale locale) {
        getRequest().ifPresent(request -> localeResolver.setLocale(request, null, locale));
    }

    private Optional<HttpServletRequest> getRequest() {
        HttpServletRequest request = null;

        final RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            request = ((ServletRequestAttributes) requestAttributes).getRequest();
        }

        return Optional.ofNullable(request);
    }
}
