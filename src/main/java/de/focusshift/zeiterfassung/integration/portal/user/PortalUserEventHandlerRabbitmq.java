package de.focusshift.zeiterfassung.integration.portal.user;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantService;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.tenancy.user.TenantUser;
import de.focusshift.zeiterfassung.tenancy.user.TenantUserService;
import de.focusshift.zeiterfassung.tenancy.user.UserStatus;
import de.focusshift.zeiterfassung.user.UserId;
import org.slf4j.Logger;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import java.util.Set;

import static de.focusshift.zeiterfassung.integration.portal.user.PortalUserRabbitmqConfiguration.ZEITERFASSUNG_PORTAL_USER_ACTIVATED_QUEUE;
import static de.focusshift.zeiterfassung.integration.portal.user.PortalUserRabbitmqConfiguration.ZEITERFASSUNG_PORTAL_USER_CREATED_QUEUE;
import static de.focusshift.zeiterfassung.integration.portal.user.PortalUserRabbitmqConfiguration.ZEITERFASSUNG_PORTAL_USER_DEACTIVATED_QUEUE;
import static de.focusshift.zeiterfassung.integration.portal.user.PortalUserRabbitmqConfiguration.ZEITERFASSUNG_PORTAL_USER_DELETED_QUEUE;
import static de.focusshift.zeiterfassung.integration.portal.user.PortalUserRabbitmqConfiguration.ZEITERFASSUNG_PORTAL_USER_UPDATED_QUEUE;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_USER;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

public class PortalUserEventHandlerRabbitmq {
    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final TenantContextHolder tenantContextHolder;
    private final TenantUserService tenantUserService;
    private final TenantService tenantService;

    public PortalUserEventHandlerRabbitmq(TenantContextHolder tenantContextHolder,
                                          TenantUserService tenantUserService, TenantService tenantService) {
        this.tenantContextHolder = tenantContextHolder;
        this.tenantUserService = tenantUserService;
        this.tenantService = tenantService;
    }

    @RabbitListener(queues = {ZEITERFASSUNG_PORTAL_USER_CREATED_QUEUE})
    void on(PortalUserCreatedEvent event) {
        LOG.info("Received PortalUserCreatedEvent for userId={} and tenantId={}", event.uuid(), event.tenantId());

        if (tenantService.getTenantByTenantId(event.tenantId()).isEmpty()) {
            LOG.info("Can not create user with userId={} of tenantId={} - tenant does not exist", event.uuid(), event.tenantId());
            return;
        }

        tenantContextHolder.runInTenantIdContext(new TenantId(event.tenantId()), tenantId -> {
            if (tenantUserService.findById(new UserId(event.uuid())).isPresent()) {
                LOG.info("Can not create user with userId={} of tenantId={} - user already exists", event.uuid(), tenantId);
                return;
            }

            LOG.info("Creating new user with userId={} of tenantId={}", event.uuid(), tenantId);
            tenantUserService.createNewUser(event.uuid(), event.firstName(), event.lastName(), new EMailAddress(event.email()), Set.of(ZEITERFASSUNG_USER));
        });
    }

    @RabbitListener(queues = {ZEITERFASSUNG_PORTAL_USER_UPDATED_QUEUE})
    void on(PortalUserUpdatedEvent event) {
        LOG.info("Received PortalUserUpdatedEvent for userId={} and tenantId={}", event.uuid(), event.tenantId());

        if (tenantService.getTenantByTenantId(event.tenantId()).isEmpty()) {
            LOG.info("Can not update user with userId={} of tenantId={} - tenant does not exist", event.uuid(), event.tenantId());
            return;
        }

        tenantContextHolder.runInTenantIdContext(new TenantId(event.tenantId()), tenantId -> tenantUserService.findById(new UserId(event.uuid()))
            .ifPresentOrElse(existing -> {
                LOG.info("Found existing user with userId={} of tenantId={} - updating user ...", event.uuid(), tenantId);
                tenantUserService.updateUser(new TenantUser(existing.id(), existing.localId(), event.firstName(), event.lastName(), new EMailAddress(event.email()), existing.firstLoginAt(), existing.authorities(), existing.createdAt(), existing.updatedAt(), existing.deactivatedAt(), existing.deletedAt(), existing.status()));
                if(UserStatus.UNKNOWN.equals(existing.status())) {
                    LOG.info("Found existing user with userId={} of tenantId={} with status=UNKNOWN - activating user ...", event.uuid(), tenantId);
                    tenantUserService.activateUser(existing.localId());
                }
            }, () -> {
                LOG.info("No user found for userId={} of tenantId={} - going to create user ...", event.uuid(), tenantId);
                tenantUserService.createNewUser(event.uuid(), event.lastName(), event.firstName(), new EMailAddress(event.email()), Set.of(ZEITERFASSUNG_USER));
            }));
    }

    @RabbitListener(queues = {ZEITERFASSUNG_PORTAL_USER_DELETED_QUEUE})
    void on(PortalUserDeletedEvent event) {
        LOG.info("Received PortalUserDeletedEvent for userId={} and tenantId={}", event.uuid(), event.tenantId());

        if (tenantService.getTenantByTenantId(event.tenantId()).isEmpty()) {
            LOG.info("Can not delete user with userId={} of tenantId={} - tenant does not exist", event.uuid(), event.tenantId());
            return;
        }

        tenantContextHolder.runInTenantIdContext(new TenantId(event.tenantId()), tenantId -> tenantUserService.findById(new UserId(event.uuid()))
            .ifPresentOrElse(existing -> {
                LOG.info("Found existing user with userId={} of tenantId={} - deleting user ...", event.uuid(), tenantId);
                tenantUserService.deleteUser(existing.localId());
            }, () -> LOG.info("No user found for userId={} of tenantId={} - skipping deletion ...", event.uuid(), tenantId)));
    }

    @RabbitListener(queues = {ZEITERFASSUNG_PORTAL_USER_ACTIVATED_QUEUE})
    void on(PortalUserActivatedEvent event) {
        LOG.info("Received PortalUserActivatedEvent for userId={} and tenantId={}", event.uuid(), event.tenantId());

        if (tenantService.getTenantByTenantId(event.tenantId()).isEmpty()) {
            LOG.info("Can not activate user with userId={} of tenantId={} - tenant does not exist", event.uuid(), event.tenantId());
            return;
        }

        tenantContextHolder.runInTenantIdContext(new TenantId(event.tenantId()), tenantId -> tenantUserService.findById(new UserId(event.uuid()))
            .ifPresentOrElse(existing -> {
                LOG.info("Found existing user with userId={} of tenantId={} - activating user ...", event.uuid(), tenantId);
                tenantUserService.activateUser(existing.localId());
            }, () -> LOG.info("No user found for userId={} of tenantId={} - skipping activation ...", event.uuid(), tenantId)));
    }

    @RabbitListener(queues = {ZEITERFASSUNG_PORTAL_USER_DEACTIVATED_QUEUE})
    void on(PortalUserDeactivatedEvent event) {
        LOG.info("Received PortalUserDeactivatedEvent for userId={} and tenantId={}", event.uuid(), event.tenantId());

        if (tenantService.getTenantByTenantId(event.tenantId()).isEmpty()) {
            LOG.info("Can not deactivate user with userId={} of tenantId={} - tenant does not exist", event.uuid(), event.tenantId());
            return;
        }

        tenantContextHolder.runInTenantIdContext(new TenantId(event.tenantId()), tenantId -> tenantUserService.findById(new UserId(event.uuid()))
            .ifPresentOrElse(existing -> {
                LOG.info("Found existing user with userId={} of tenantId={} - deactivating user ...", event.uuid(), tenantId);
                tenantUserService.deactivateUser(existing.localId());
            }, () -> LOG.info("No user found for userId={} of tenantId={} - skipping deactivation ...", event.uuid(), tenantId)));
    }


}
