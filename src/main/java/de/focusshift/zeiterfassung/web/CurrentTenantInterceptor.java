package de.focusshift.zeiterfassung.web;

import de.focusshift.zeiterfassung.tenancy.authentication.TenantIdProvider;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.security.Principal;

import static java.lang.invoke.MethodHandles.lookup;

@Component
class CurrentTenantInterceptor implements HandlerInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    private final TenantContextHolder tenantContextHolder;
    private final TenantIdProvider tenantIdProvider;

    CurrentTenantInterceptor(TenantContextHolder tenantContextHolder, TenantIdProvider tenantIdProvider) {
        this.tenantContextHolder = tenantContextHolder;
        this.tenantIdProvider = tenantIdProvider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        final Principal userPrincipal = request.getUserPrincipal();

        if (userPrincipal instanceof final OAuth2AuthenticationToken oauthToken) {
            tenantIdProvider.resolve(oauthToken).ifPresentOrElse(tenantContextHolder::setTenantId, () -> {
                LOG.warn("could not resolve tenantId for oauthToken");
                tenantContextHolder.clear();
            });
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        tenantContextHolder.clear();
    }
}
