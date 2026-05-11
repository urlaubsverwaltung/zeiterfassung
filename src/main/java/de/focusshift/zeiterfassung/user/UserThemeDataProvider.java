package de.focusshift.zeiterfassung.user;

import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.web.DataProviderInterface;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

import java.security.Principal;

import static java.lang.invoke.MethodHandles.lookup;

@Component
public class UserThemeDataProvider implements DataProviderInterface {

    private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    private static final Theme DEFAULT_THEME = Theme.SYSTEM;

    private final UserSettingsService userSettingsService;

    UserThemeDataProvider(UserSettingsService userSettingsService) {
        this.userSettingsService = userSettingsService;
    }

    @Override
    public void postHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, ModelAndView modelAndView) {
        if (addDataIf(modelAndView)) {

            final Principal userPrincipal = request.getUserPrincipal();

            final Theme theme;

            if (userPrincipal instanceof OAuth2AuthenticationToken token) {
                final OAuth2User oauth2User = token.getPrincipal();
                if (oauth2User instanceof CurrentOidcUser user) {
                    user.getUserIdComposite();
                    theme = userSettingsService.findTheme(user.getUserIdComposite()).orElse(DEFAULT_THEME);
                } else {
                    LOG.info("userPrincipal not of type {}. Using default system theme.", CurrentOidcUser.class.getName());
                    theme = DEFAULT_THEME;
                }
            } else {
                LOG.info("userPrincipal not of type OAuth2AuthenticationToken. Using default system theme.");
                theme = DEFAULT_THEME;
            }

            final String themeValueLowerCase = theme.name().toLowerCase();
            modelAndView.addObject("theme", themeValueLowerCase);
        }
    }
}
