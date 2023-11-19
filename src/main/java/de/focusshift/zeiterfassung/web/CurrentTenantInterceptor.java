package de.focusshift.zeiterfassung.web;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
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

    CurrentTenantInterceptor(TenantContextHolder tenantContextHolder) {
        this.tenantContextHolder = tenantContextHolder;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        final Principal userPrincipal = request.getUserPrincipal();

        if (userPrincipal instanceof final OAuth2AuthenticationToken oauthToken) {
            final TenantId tenantId = new TenantId(oauthToken.getAuthorizedClientRegistrationId());
            if (tenantId.valid()) {
                tenantContextHolder.setTenantId(tenantId);
            } else {
                LOG.warn("invalid tenantId={}", tenantId);
                tenantContextHolder.clear();
            }
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        tenantContextHolder.clear();
    }
}
