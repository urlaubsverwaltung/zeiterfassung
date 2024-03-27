package de.focusshift.zeiterfassung.integration.portal.user;

import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.tenancy.user.TenantUser;
import de.focusshift.zeiterfassung.tenancy.user.TenantUserService;
import de.focusshift.zeiterfassung.user.UserId;
import org.slf4j.Logger;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static de.focusshift.zeiterfassung.integration.portal.user.PortalUserRabbitmqConfiguration.ZEITERFASSUNG_PORTAL_USER_CREATED_QUEUE;
import static de.focusshift.zeiterfassung.integration.portal.user.PortalUserRabbitmqConfiguration.ZEITERFASSUNG_PORTAL_USER_DELETED_QUEUE;
import static de.focusshift.zeiterfassung.integration.portal.user.PortalUserRabbitmqConfiguration.ZEITERFASSUNG_PORTAL_USER_UPDATED_QUEUE;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_USER;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

public class PortalUserEventHandlerRabbitmq {
    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final TenantUserService tenantUserService;

    public PortalUserEventHandlerRabbitmq(TenantUserService tenantUserService) {
        this.tenantUserService = tenantUserService;
    }

    @RabbitListener(queues = {ZEITERFASSUNG_PORTAL_USER_CREATED_QUEUE})
    void on(PortalUserCreatedEvent event) {
        LOG.info("Received PortalUserCreatedEvent for userId={} and tenantId={}", event.uuid(), event.tenantId());
        guardWithTenantIdInSecurityContext(event.tenantId(), () -> handleUserCreation(event));
    }

    @RabbitListener(queues = {ZEITERFASSUNG_PORTAL_USER_UPDATED_QUEUE})
    void on(PortalUserUpdatedEvent event) {
        LOG.info("Received PortalUserUpdatedEvent for userId={} and tenantId={}", event.uuid(), event.tenantId());
        guardWithTenantIdInSecurityContext(event.tenantId(), () -> handleUserUpdate(event));
    }

    @RabbitListener(queues = {ZEITERFASSUNG_PORTAL_USER_DELETED_QUEUE})
    void on(PortalUserDeletedEvent event) {
        LOG.info("Received PortalUserDeletedEvent for userId={} and tenantId={}", event.uuid(), event.tenantId());
        guardWithTenantIdInSecurityContext(event.tenantId(), () -> handleUserDelete(event));
    }

    private void handleUserCreation(PortalUserCreatedEvent event) {
        if (tenantUserService.findById(new UserId(event.uuid())).isPresent()) {
            LOG.info("Can not create user with userId={} of tenantId={} - user already exists", event.uuid(), event.tenantId());
            return;
        }

        LOG.info("Creating new user with userId={} of tenantId={}", event.uuid(), event.tenantId());
        tenantUserService.createNewUser(event.uuid(), event.firstName(), event.lastName(), new EMailAddress(event.email()), Set.of(ZEITERFASSUNG_USER));
    }

    private void handleUserUpdate(PortalUserUpdatedEvent event) {
        tenantUserService.findById(new UserId(event.uuid()))
            .ifPresentOrElse(existing -> {
                tenantUserService.updateUser(new TenantUser(existing.id(), existing.localId(), event.firstName(), event.lastName(), new EMailAddress(event.email()), existing.authorities()));
            }, () -> {
                LOG.info("No user found for userId={} of tenantId={} - going to create user ...", event.uuid(), event.tenantId());
                tenantUserService.createNewUser(event.uuid(), event.lastName(), event.firstName(), new EMailAddress(event.email()), Set.of(ZEITERFASSUNG_USER));
            });
    }

    private void handleUserDelete(PortalUserDeletedEvent event) {
        tenantUserService.findById(new UserId(event.uuid()))
            .ifPresentOrElse(existing -> {
                LOG.info("Found existing user with userId={} of tenantId={} - deleting user ...", event.uuid(), event.tenantId());
                tenantUserService.deleteUser(existing.localId());
            }, () -> {
                LOG.info("No user found for userId={} of tenantId={} - skipping deletion ...", event.uuid(), event.tenantId());
            });
    }

    private void guardWithTenantIdInSecurityContext(String tenantId, Runnable runnable) {
        try {
            // reading stuff from database requires an authentication in the securityContext
            // in order to get the clientRegistration for database row security queries.
            prepareSecurityContext(tenantId, new DummyOidcUser());
            runnable.run();
        } finally {
            clearSecurityContext();
        }
    }


    private void prepareSecurityContext(String tenantId, OidcUser oidcUser) {

        final OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), tenantId);

        final SecurityContext securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private void clearSecurityContext() {
        SecurityContextHolder.setContext(new SecurityContextImpl());
    }

    private class DummyOidcUser implements OidcUser {

        @Override
        public Map<String, Object> getClaims() {
            return null;
        }

        @Override
        public OidcUserInfo getUserInfo() {
            return null;
        }

        @Override
        public OidcIdToken getIdToken() {
            return null;
        }

        @Override
        public Map<String, Object> getAttributes() {
            return null;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return Set.of(ZEITERFASSUNG_USER.authority());
        }

        @Override
        public String getName() {
            return null;
        }
    }


}
