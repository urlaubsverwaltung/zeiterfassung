package de.focusshift.zeiterfassung.integration.urlaubsverwaltung.person;

import de.focus_shift.urlaubsverwaltung.extension.api.person.PersonCreatedEventDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.person.PersonDeletedEventDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.person.PersonDisabledEventDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.person.PersonUpdatedEventDTO;
import de.focusshift.zeiterfassung.security.SecurityRole;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantService;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.tenancy.user.TenantUser;
import de.focusshift.zeiterfassung.tenancy.user.TenantUserService;
import de.focusshift.zeiterfassung.user.UserId;
import org.slf4j.Logger;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import java.util.Objects;
import java.util.Set;

import static de.focusshift.zeiterfassung.integration.urlaubsverwaltung.person.PersonRabbitmqConfiguration.ZEITERFASSUNG_URLAUBSVERWALTUNG_PERSON_CREATED_QUEUE;
import static de.focusshift.zeiterfassung.integration.urlaubsverwaltung.person.PersonRabbitmqConfiguration.ZEITERFASSUNG_URLAUBSVERWALTUNG_PERSON_DELETED_QUEUE;
import static de.focusshift.zeiterfassung.integration.urlaubsverwaltung.person.PersonRabbitmqConfiguration.ZEITERFASSUNG_URLAUBSVERWALTUNG_PERSON_DISABLED_QUEUE;
import static de.focusshift.zeiterfassung.integration.urlaubsverwaltung.person.PersonRabbitmqConfiguration.ZEITERFASSUNG_URLAUBSVERWALTUNG_PERSON_UPDATED_QUEUE;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;


/**
 * Maintain users of zeiterfassung based on person events from urlaubsverwaltung
 */
class PersonEventHandlerRabbitmq {

    private static final Logger LOG = getLogger(lookup().lookupClass());
    private static final Set<SecurityRole> DEFAULT_SECURITY_ROLE = Set.of(SecurityRole.ZEITERFASSUNG_USER);

    private final TenantContextHolder tenantContextHolder;
    private final TenantService tenantService;
    private final TenantUserService tenantUserService;

    public PersonEventHandlerRabbitmq(TenantContextHolder tenantContextHolder, TenantService tenantService, TenantUserService tenantUserService) {
        this.tenantContextHolder = tenantContextHolder;
        this.tenantService = tenantService;
        this.tenantUserService = tenantUserService;
    }

    private static UserId toUserId(String username) {
        return new UserId(Objects.requireNonNull(username));
    }

    private static TenantId toTenantId(String tenantId) {
        return new TenantId(Objects.requireNonNull(tenantId));
    }

    @RabbitListener(queues = {ZEITERFASSUNG_URLAUBSVERWALTUNG_PERSON_CREATED_QUEUE})
    void on(PersonCreatedEventDTO event) {
        TenantId tenantId = toTenantId(event.getTenantId());
        if (tenantService.getTenantByTenantId(tenantId.tenantId()).isEmpty()) {
            LOG.info("Received PersonCreatedEvent={} of tenantId={}, but tenant is unknown - nothing todo!", event.getId(), tenantId.tenantId());
            return;
        }

        try {
            tenantContextHolder.setTenantId(tenantId);
            tenantUserService.findById(toUserId(event.getUsername())).ifPresentOrElse(existingUser -> {
                LOG.info("Found existing User with id={} for PersonCreatedEvent={} of tenantId={} - nothing todo!", event.getUsername(), event.getId(), tenantId.tenantId());
            }, () -> {
                LOG.info("No User with id={} found for PersonCreatedEvent={} of tenantId={} - going to create new user!", event.getUsername(), event.getId(), tenantId.tenantId());
                TenantUser newUser = tenantUserService.createNewUser(event.getUsername(), event.getFirstName(), event.getLastName(), new EMailAddress(event.getEmail()), DEFAULT_SECURITY_ROLE);
                LOG.info("Created new User with id={} for PersonCreatedEvent={} of tenantId={}!", newUser.id(), event.getId(), tenantId.tenantId());
            });
        } finally {
            tenantContextHolder.clear();
        }
    }

    @RabbitListener(queues = {ZEITERFASSUNG_URLAUBSVERWALTUNG_PERSON_UPDATED_QUEUE})
    void on(PersonUpdatedEventDTO event) {
        TenantId tenantId = toTenantId(event.getTenantId());
        if (tenantService.getTenantByTenantId(tenantId.tenantId()).isEmpty()) {
            LOG.info("Received PersonUpdatedEventDTO={} of tenantId={}, but tenant is unknown - nothing todo!", event.getId(), tenantId.tenantId());
            return;
        }

        try {
            tenantContextHolder.setTenantId(tenantId);
            tenantUserService.findById(toUserId(event.getUsername())).ifPresentOrElse(existingUser -> {
                LOG.info("Found existing User with id={} for PersonUpdatedEventDTO={} of tenantId={} - nothing todo!", existingUser.id(), event.getId(), tenantId.tenantId());
                TenantUser update = new TenantUser(existingUser.id(), existingUser.localId(), event.getFirstName(), event.getLastName(), new EMailAddress(event.getEmail()), existingUser.firstLoginAt(), existingUser.authorities());
                tenantUserService.updateUser(update);
                LOG.info("Updated existing User with id={} for PersonUpdatedEventDTO={} of tenantId={}!", existingUser.id(), event.getId(), tenantId.tenantId());
            }, () -> {
                LOG.info("No User with id={} found for PersonUpdatedEventDTO={} of tenantId={} - going to create new user!", event.getUsername(), event.getId(), tenantId.tenantId());
                TenantUser newUser = tenantUserService.createNewUser(event.getUsername(), event.getFirstName(), event.getLastName(), new EMailAddress(event.getEmail()), DEFAULT_SECURITY_ROLE);
                LOG.info("Created new User with id={} for PersonCreatedEvent={} of tenantId={}!", newUser.id(), event.getId(), tenantId.tenantId());
            });
        } finally {
            tenantContextHolder.clear();
        }
    }

    @RabbitListener(queues = {ZEITERFASSUNG_URLAUBSVERWALTUNG_PERSON_DISABLED_QUEUE})
    void on(PersonDisabledEventDTO event) {
        TenantId tenantId = toTenantId(event.getTenantId());
        if (tenantService.getTenantByTenantId(tenantId.tenantId()).isEmpty()) {
            LOG.info("Received PersonDisabledEventDTO={} of tenantId={}, but tenant is unknown - nothing todo!", event.getId(), tenantId.tenantId());
            return;
        }

        try {
            tenantContextHolder.setTenantId(tenantId);
            tenantUserService.findById(toUserId(event.getUsername())).ifPresentOrElse(existingUser -> {
                LOG.info("Going to disable user with id={} of tenantId={} for PersonDisabledEventDTO={} - but zeiterfassung doesn't support disable a user at the moment!", existingUser.id(), tenantId.tenantId(), event.getId());
            }, () -> LOG.info("No user with id={} found for PersonDisabledEventDTO={} of tenantId={} - nothing todo!", event.getUsername(), event.getId(), tenantId.tenantId()));
        } finally {
            tenantContextHolder.clear();
        }
    }

    @RabbitListener(queues = {ZEITERFASSUNG_URLAUBSVERWALTUNG_PERSON_DELETED_QUEUE})
    void on(PersonDeletedEventDTO event) {
        TenantId tenantId = toTenantId(event.getTenantId());
        if (tenantService.getTenantByTenantId(tenantId.tenantId()).isEmpty()) {
            LOG.info("Received PersonDeletedEventDTO={} of tenantId={}, but tenant is unknown - nothing todo!", event.getId(), tenantId.tenantId());
            return;
        }

        try {
            tenantContextHolder.setTenantId(tenantId);
            tenantUserService.findById(toUserId(event.getUsername())).ifPresentOrElse(existingUser -> {
                LOG.info("Going to delete user with id={} of tenantId={} for PersonDeletedEvent={}!", existingUser.id(), tenantId.tenantId(), event.getId());
                tenantUserService.deleteUser(existingUser.localId());
                LOG.info("Deleted existing User with id={} of tenantId={} for PersonDeletedEvent={}!", existingUser.id(), tenantId.tenantId(), event.getId());
            }, () -> LOG.info("No user with id={} found for PersonDeletedEvent={} of tenantId={} - nothing todo!", event.getUsername(), event.getId(), tenantId.tenantId()));
        } finally {
            tenantContextHolder.clear();
        }
    }

}
