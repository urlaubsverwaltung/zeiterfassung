package de.focusshift.zeiterfassung.integration.portal.user;


import de.focusshift.zeiterfassung.tenancy.tenant.Tenant;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantService;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantStatus;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.tenancy.user.TenantUser;
import de.focusshift.zeiterfassung.tenancy.user.TenantUserService;
import de.focusshift.zeiterfassung.tenancy.user.UserStatus;
import de.focusshift.zeiterfassung.user.UserId;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_USER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class PortalUserEventHandlerRabbitmqTest {

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private TenantContextHolder tenantContextHolder;

    @Mock
    private TenantUserService tenantUserService;

    @Mock
    private TenantService tenantService;

    @InjectMocks
    private PortalUserEventHandlerRabbitmq portalUserEventHandlerRabbitmq;

    private static Tenant myTenant() {
        return new Tenant("my-tenant-id", Instant.now(), Instant.now(), TenantStatus.ACTIVE);
    }

    private static TenantUser activeTenantUser(Instant now) {
        return new TenantUser("my-external-id-1", 1L, "batman", "batman", new EMailAddress("batman@batman.com"), now, Set.of(), now, now, null, null, UserStatus.ACTIVE);
    }

    private static TenantUser unknownTenantUser(Instant now) {
        return new TenantUser("my-external-id-1", 1L, "batman", "batman", new EMailAddress("batman@batman.com"), now, Set.of(), now, now, null, null, UserStatus.UNKNOWN);
    }

    @Nested
    class PortalUserCreatedEventTest {

        private static PortalUserCreatedEvent portalUserCreatedEvent() {
            return new PortalUserCreatedEvent("my-external-id-1", "my-tenant-id", "batman", "batman", "batman@batman.com", true, Instant.now(), "ACTIVE");
        }

        @Test
        void whenTenantDoesNotExist_nothingHappens() {
            PortalUserCreatedEvent event = portalUserCreatedEvent();
            when(tenantService.getTenantByTenantId(any())).thenReturn(Optional.empty());

            portalUserEventHandlerRabbitmq.on(event);

            verify(tenantService, times(1)).getTenantByTenantId(event.tenantId());
            verifyNoInteractions(tenantUserService);
            verifyNoInteractions(tenantContextHolder);
        }

        @Test
        void whenUserAlreadyExists_nothingHappens() {
            PortalUserCreatedEvent event = portalUserCreatedEvent();
            when(tenantService.getTenantByTenantId(any())).thenReturn(Optional.of(myTenant()));
            when(tenantUserService.findById(any())).thenReturn(Optional.of(activeTenantUser(Instant.now())));

            portalUserEventHandlerRabbitmq.on(event);

            verify(tenantService).getTenantByTenantId(event.tenantId());
            verify(tenantUserService).findById(new UserId(event.uuid()));
            verify(tenantUserService, never()).createNewUser(any(), any(), any(), any(), any());
            InOrder inOrder = Mockito.inOrder(tenantContextHolder);
            inOrder.verify(tenantContextHolder).setTenantId(new TenantId(event.tenantId()));
            inOrder.verify(tenantContextHolder).clear();
        }

        @Test
        void whenUserDoesntExists_UserIsCreated() {
            PortalUserCreatedEvent event = portalUserCreatedEvent();
            when(tenantService.getTenantByTenantId(any())).thenReturn(Optional.of(myTenant()));
            when(tenantUserService.findById(any())).thenReturn(Optional.empty());

            portalUserEventHandlerRabbitmq.on(event);

            verify(tenantService).getTenantByTenantId(event.tenantId());
            verify(tenantUserService).findById(new UserId(event.uuid()));
            verify(tenantUserService).createNewUser(event.uuid(), event.firstName(), event.lastName(), new EMailAddress(event.email()), Set.of(ZEITERFASSUNG_USER));
            InOrder inOrder = Mockito.inOrder(tenantContextHolder);
            inOrder.verify(tenantContextHolder).setTenantId(new TenantId(event.tenantId()));
            inOrder.verify(tenantContextHolder).clear();
        }
    }

    @Nested
    class PortalUserUpdatedEventTest {

        private static PortalUserUpdatedEvent portalUserUpdatedEvent() {
            return new PortalUserUpdatedEvent("my-external-id-1", "my-tenant-id", "Catwoman", "Catwoman", "Catwoman@batman.com", true, Instant.now(), Instant.now(), "ACTIVE");
        }

        @Test
        void whenTenantDoesNotExist_nothingHappens() {
            PortalUserUpdatedEvent event = portalUserUpdatedEvent();
            when(tenantService.getTenantByTenantId(any())).thenReturn(Optional.empty());

            portalUserEventHandlerRabbitmq.on(event);

            verify(tenantService).getTenantByTenantId(event.tenantId());
            verifyNoInteractions(tenantUserService);
            verifyNoInteractions(tenantContextHolder);
        }

        @Test
        void whenUserDoesNotExist_UserIsCreated() {
            PortalUserUpdatedEvent event = portalUserUpdatedEvent();
            when(tenantService.getTenantByTenantId(any())).thenReturn(Optional.of(myTenant()));
            when(tenantUserService.findById(any())).thenReturn(Optional.empty());

            portalUserEventHandlerRabbitmq.on(event);

            verify(tenantService).getTenantByTenantId(event.tenantId());
            verify(tenantUserService).findById(new UserId(event.uuid()));
            verify(tenantUserService).createNewUser(event.uuid(), event.lastName(), event.firstName(), new EMailAddress(event.email()), Set.of(ZEITERFASSUNG_USER));
            InOrder inOrder = Mockito.inOrder(tenantContextHolder);
            inOrder.verify(tenantContextHolder).setTenantId(new TenantId(event.tenantId()));
            inOrder.verify(tenantContextHolder).clear();
        }

        @Test
        void whenUserExists_UserIsUpdated() {
            PortalUserUpdatedEvent event = portalUserUpdatedEvent();
            when(tenantService.getTenantByTenantId(any())).thenReturn(Optional.of(myTenant()));
            TenantUser existingUser = activeTenantUser(Instant.now());
            when(tenantUserService.findById(any())).thenReturn(Optional.of(existingUser));

            portalUserEventHandlerRabbitmq.on(event);

            verify(tenantService).getTenantByTenantId(event.tenantId());
            verify(tenantUserService).findById(new UserId(event.uuid()));
            verify(tenantUserService).updateUser(new TenantUser(existingUser.id(), existingUser.localId(), event.firstName(), event.lastName(), new EMailAddress(event.email()), existingUser.firstLoginAt(), existingUser.authorities(), existingUser.createdAt(), existingUser.updatedAt(), existingUser.deactivatedAt(), existingUser.deletedAt(), existingUser.status()));
            InOrder inOrder = Mockito.inOrder(tenantContextHolder);
            inOrder.verify(tenantContextHolder).setTenantId(new TenantId(event.tenantId()));
            inOrder.verify(tenantContextHolder).clear();
        }

        @Test
        void whenUserExistsWithUnknownStatus_UserIsUpdatedAndActivated() {
            PortalUserUpdatedEvent event = portalUserUpdatedEvent();
            when(tenantService.getTenantByTenantId(any())).thenReturn(Optional.of(myTenant()));
            TenantUser existingUserStatusUnknown = unknownTenantUser(Instant.now());
            when(tenantUserService.findById(any())).thenReturn(Optional.of(existingUserStatusUnknown));

            portalUserEventHandlerRabbitmq.on(event);

            verify(tenantService).getTenantByTenantId(event.tenantId());
            verify(tenantUserService).findById(new UserId(event.uuid()));
            verify(tenantUserService).updateUser(any(TenantUser.class));
            verify(tenantUserService).activateUser(existingUserStatusUnknown.localId());
            InOrder inOrder = Mockito.inOrder(tenantContextHolder);
            inOrder.verify(tenantContextHolder).setTenantId(new TenantId(event.tenantId()));
            inOrder.verify(tenantContextHolder).clear();
        }
    }

    @Nested
    class PortalUserDeletedEventTest {

        private static PortalUserDeletedEvent portalUserDeletedEvent() {
            return new PortalUserDeletedEvent("my-external-id-1", "my-tenant-id", "batman", "batman", "batman@batman.com", true, Instant.now(), Instant.now(), Instant.now(), "DELETED");
        }

        @Test
        void whenTenantDoesNotExist_nothingHappens() {
            PortalUserDeletedEvent event = portalUserDeletedEvent();
            when(tenantService.getTenantByTenantId(any())).thenReturn(Optional.empty());

            portalUserEventHandlerRabbitmq.on(event);

            verify(tenantService).getTenantByTenantId(event.tenantId());
            verifyNoInteractions(tenantUserService);
            verifyNoInteractions(tenantContextHolder);
        }

        @Test
        void whenUserDoesNotExist_nothingHappens() {
            PortalUserDeletedEvent event = portalUserDeletedEvent();
            when(tenantService.getTenantByTenantId(any())).thenReturn(Optional.of(myTenant()));
            when(tenantUserService.findById(any())).thenReturn(Optional.empty());

            portalUserEventHandlerRabbitmq.on(event);

            verify(tenantService).getTenantByTenantId(event.tenantId());
            verify(tenantUserService).findById(new UserId(event.uuid()));
            verify(tenantUserService, never()).deleteUser(any());
            InOrder inOrder = Mockito.inOrder(tenantContextHolder);
            inOrder.verify(tenantContextHolder).setTenantId(new TenantId(event.tenantId()));
            inOrder.verify(tenantContextHolder).clear();
        }

        @Test
        void whenUserExists_UserIsDeleted() {
            PortalUserDeletedEvent event = portalUserDeletedEvent();
            when(tenantService.getTenantByTenantId(any())).thenReturn(Optional.of(myTenant()));
            TenantUser existingUser = activeTenantUser(Instant.now());
            when(tenantUserService.findById(any())).thenReturn(Optional.of(existingUser));

            portalUserEventHandlerRabbitmq.on(event);

            verify(tenantService).getTenantByTenantId(event.tenantId());
            verify(tenantUserService).findById(new UserId(event.uuid()));
            verify(tenantUserService).deleteUser(existingUser.localId());
            InOrder inOrder = Mockito.inOrder(tenantContextHolder);
            inOrder.verify(tenantContextHolder).setTenantId(new TenantId(event.tenantId()));
            inOrder.verify(tenantContextHolder).clear();
        }
    }

    @Nested
    class PortalUserActivatedEventTest {

        private static PortalUserActivatedEvent portalUserActivatedEvent() {
            return new PortalUserActivatedEvent("my-external-id-1", "my-tenant-id", "batman", "batman", "batman@batman.com", true, Instant.now(), Instant.now(), null, null, "ACTIVE");
        }

        @Test
        void whenTenantDoesNotExist_nothingHappens() {
            PortalUserActivatedEvent event = portalUserActivatedEvent();
            when(tenantService.getTenantByTenantId(any())).thenReturn(Optional.empty());

            portalUserEventHandlerRabbitmq.on(event);

            verify(tenantService).getTenantByTenantId(event.tenantId());
            verifyNoInteractions(tenantUserService);
            verifyNoInteractions(tenantContextHolder);
        }

        @Test
        void whenUserDoesNotExist_nothingHappens() {
            PortalUserActivatedEvent event = portalUserActivatedEvent();
            when(tenantService.getTenantByTenantId(any())).thenReturn(Optional.of(myTenant()));
            when(tenantUserService.findById(any())).thenReturn(Optional.empty());

            portalUserEventHandlerRabbitmq.on(event);

            verify(tenantService).getTenantByTenantId(event.tenantId());
            verify(tenantUserService).findById(new UserId(event.uuid()));
            verify(tenantUserService, never()).activateUser(any());
            InOrder inOrder = Mockito.inOrder(tenantContextHolder);
            inOrder.verify(tenantContextHolder).setTenantId(new TenantId(event.tenantId()));
            inOrder.verify(tenantContextHolder).clear();
        }

        @Test
        void whenUserExists_UserIsActivated() {
            PortalUserActivatedEvent event = portalUserActivatedEvent();
            when(tenantService.getTenantByTenantId(any())).thenReturn(Optional.of(myTenant()));
            TenantUser existingUser = activeTenantUser(Instant.now());
            when(tenantUserService.findById(any())).thenReturn(Optional.of(existingUser));

            portalUserEventHandlerRabbitmq.on(event);

            verify(tenantService).getTenantByTenantId(event.tenantId());
            verify(tenantUserService).findById(new UserId(event.uuid()));
            verify(tenantUserService).activateUser(existingUser.localId());
            InOrder inOrder = Mockito.inOrder(tenantContextHolder);
            inOrder.verify(tenantContextHolder).setTenantId(new TenantId(event.tenantId()));
            inOrder.verify(tenantContextHolder).clear();
        }

    }

    @Nested
    class PortalUserDeactivatedEventTest {

        private static PortalUserDeactivatedEvent portalUserDeactivatedEvent() {
            return new PortalUserDeactivatedEvent("my-external-id-1", "my-tenant-id", "batman", "batman", "batman@batman.com", true, Instant.now(), Instant.now(), Instant.now(), null, "DEACTIVATED");
        }

        @Test
        void whenTenantDoesNotExist_nothingHappens() {
            PortalUserDeactivatedEvent event = portalUserDeactivatedEvent();
            when(tenantService.getTenantByTenantId(any())).thenReturn(Optional.empty());

            portalUserEventHandlerRabbitmq.on(event);

            verify(tenantService).getTenantByTenantId(event.tenantId());
            verifyNoInteractions(tenantUserService);
            verifyNoInteractions(tenantContextHolder);
        }

        @Test
        void whenUserDoesNotExist_nothingHappens() {
            PortalUserDeactivatedEvent event = portalUserDeactivatedEvent();
            when(tenantService.getTenantByTenantId(any())).thenReturn(Optional.of(myTenant()));
            when(tenantUserService.findById(any())).thenReturn(Optional.empty());

            portalUserEventHandlerRabbitmq.on(event);

            verify(tenantService).getTenantByTenantId(event.tenantId());
            verify(tenantUserService).findById(new UserId(event.uuid()));
            verify(tenantUserService, never()).deactivateUser(any());
            InOrder inOrder = Mockito.inOrder(tenantContextHolder);
            inOrder.verify(tenantContextHolder).setTenantId(new TenantId(event.tenantId()));
            inOrder.verify(tenantContextHolder).clear();
        }

        @Test
        void whenUserExists_UserIsDeactivated() {
            PortalUserDeactivatedEvent event = portalUserDeactivatedEvent();
            when(tenantService.getTenantByTenantId(any())).thenReturn(Optional.of(myTenant()));
            TenantUser existingUser = activeTenantUser(Instant.now());
            when(tenantUserService.findById(any())).thenReturn(Optional.of(existingUser));

            portalUserEventHandlerRabbitmq.on(event);

            verify(tenantService).getTenantByTenantId(event.tenantId());
            verify(tenantUserService).findById(new UserId(event.uuid()));
            verify(tenantUserService).deactivateUser(existingUser.localId());
            InOrder inOrder = Mockito.inOrder(tenantContextHolder);
            inOrder.verify(tenantContextHolder).setTenantId(new TenantId(event.tenantId()));
            inOrder.verify(tenantContextHolder).clear();
        }

    }

}
