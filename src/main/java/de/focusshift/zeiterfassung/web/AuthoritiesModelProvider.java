package de.focusshift.zeiterfassung.web;

import de.focusshift.zeiterfassung.security.SecurityRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_PERMISSIONS_EDIT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_WORKING_TIME_EDIT_ALL;
import static java.lang.invoke.MethodHandles.lookup;

@Component
class AuthoritiesModelProvider implements HandlerInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    @Override
    public void postHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, ModelAndView modelAndView) {

        if (modelAndView != null && navigationHeaderVisible(modelAndView)) {

            final Principal userPrincipal = request.getUserPrincipal();

            if (userPrincipal instanceof OAuth2AuthenticationToken token) {
                final List<SecurityRole> roles = token.getAuthorities()
                    .stream()
                    .map(SecurityRole::fromAuthority)
                    .filter(Optional::isPresent)
                    .flatMap(Optional::stream)
                    .toList();
                setModelAttributes(modelAndView, roles);
            } else {
                LOG.info("could not recognize user roles. principal not of type OAuth2AuthenticationToken.");
            }
        }
    }

    private static void setModelAttributes(ModelAndView modelAndView, List<SecurityRole> roles) {

        modelAndView.addObject("showMainNavigationPersons",
            contains(roles, ZEITERFASSUNG_WORKING_TIME_EDIT_ALL, ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL, ZEITERFASSUNG_PERMISSIONS_EDIT_ALL));
    }

    private static boolean contains(Collection<SecurityRole> roles, SecurityRole... anyOf) {
        return Arrays.stream(anyOf).anyMatch(roles::contains);
    }

    private boolean navigationHeaderVisible(ModelAndView modelAndView) {

        final String viewName = modelAndView.getViewName();
        if (viewName == null) {
            return false;
        }

        return !viewName.startsWith("forward:")
            && !viewName.startsWith("redirect:");
    }
}
