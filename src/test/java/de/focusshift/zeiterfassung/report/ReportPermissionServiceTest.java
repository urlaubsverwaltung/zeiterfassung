package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.tenantuser.EMailAddress;
import de.focusshift.zeiterfassung.user.CurrentUserProvider;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.util.List;

import static de.focusshift.zeiterfassung.security.SecurityRoles.ZEITERFASSUNG_VIEW_REPORT_ALL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportPermissionServiceTest {

    private ReportPermissionService sut;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private UserManagementService userManagementService;

    @BeforeEach
    void setUp() {
        sut = new ReportPermissionService(currentUserProvider, userManagementService);
    }

    @Test
    void ensureCurrentUserHasPermissionForAllUsers() {

        when(currentUserProvider.getCurrentAuthentication())
            .thenReturn(new TestingAuthenticationToken("", "", List.of(ZEITERFASSUNG_VIEW_REPORT_ALL.authority())));

        assertThat(sut.currentUserHasPermissionForAllUsers()).isTrue();
    }

    @Test
    void ensureCurrentUserHasPermissionForAllUsersIsFalseWhenAuthorityIsNotGiven() {

        when(currentUserProvider.getCurrentAuthentication())
            .thenReturn(new TestingAuthenticationToken("", "", List.of()));

        assertThat(sut.currentUserHasPermissionForAllUsers()).isFalse();
    }

    @Test
    void ensureFilterUserLocalIdsByCurrentUserHasPermissionForReturnsTheListWhenUserHasPermissionForAll() {

        when(currentUserProvider.getCurrentAuthentication())
            .thenReturn(new TestingAuthenticationToken("", "", List.of(ZEITERFASSUNG_VIEW_REPORT_ALL.authority())));

        final List<UserLocalId> userLocalIds = List.of(new UserLocalId(1L), new UserLocalId(2L));

        final List<UserLocalId> actual = sut.filterUserLocalIdsByCurrentUserHasPermissionFor(userLocalIds);
        assertThat(actual).isSameAs(userLocalIds);
    }

    @Test
    void ensureFilterUserLocalIdsByCurrentUserHasPermissionForReturnsListForCurrentUserOnly() {

        when(currentUserProvider.getCurrentAuthentication())
            .thenReturn(new TestingAuthenticationToken("", "", List.of()));

        when(currentUserProvider.getCurrentUser())
            .thenReturn(new User(new UserId(""), new UserLocalId(2L), "", "", new EMailAddress("")));

        final List<UserLocalId> actual = sut.filterUserLocalIdsByCurrentUserHasPermissionFor(
            List.of(new UserLocalId(1L), new UserLocalId(2L), new UserLocalId(3L)));

        assertThat(actual).containsOnly(new UserLocalId(2L));
    }

    @Test
    void ensureFilterUserLocalIdsByCurrentUserHasPermissionForReturnsEmptyList() {

        when(currentUserProvider.getCurrentAuthentication())
            .thenReturn(new TestingAuthenticationToken("", "", List.of()));

        when(currentUserProvider.getCurrentUser())
            .thenReturn(new User(new UserId(""), new UserLocalId(2L), "", "", new EMailAddress("")));

        final List<UserLocalId> actual = sut.filterUserLocalIdsByCurrentUserHasPermissionFor(
            List.of(new UserLocalId(1L), new UserLocalId(3L)));

        assertThat(actual).isEmpty();
    }

    @Test
    void ensureFindAllPermittedUserLocalIdsForCurrentUser() {

        when(currentUserProvider.getCurrentAuthentication())
            .thenReturn(new TestingAuthenticationToken("", "", List.of(ZEITERFASSUNG_VIEW_REPORT_ALL.authority())));

        when(userManagementService.findAllUsers())
            .thenReturn(List.of(
                new User(new UserId(""), new UserLocalId(1L), "", "", new EMailAddress("")),
                new User(new UserId(""), new UserLocalId(2L), "", "", new EMailAddress("")),
                new User(new UserId(""), new UserLocalId(3L), "", "", new EMailAddress(""))
            ));

        assertThat(sut.findAllPermittedUserLocalIdsForCurrentUser())
            .containsExactly(new UserLocalId(1L), new UserLocalId(2L), new UserLocalId(3L));
    }

    @Test
    void ensureFindAllPermittedUserLocalIdsForCurrentUserReturnsCurrentUserOnly() {

        when(currentUserProvider.getCurrentAuthentication())
            .thenReturn(new TestingAuthenticationToken("", "", List.of()));

        when(currentUserProvider.getCurrentUser())
            .thenReturn(new User(new UserId(""), new UserLocalId(2L), "", "", new EMailAddress("")));

        assertThat(sut.findAllPermittedUserLocalIdsForCurrentUser()).containsOnly(new UserLocalId(2L));
    }

    @Test
    void ensureFindAllPermittedUsersForCurrentUser() {

        when(currentUserProvider.getCurrentAuthentication())
            .thenReturn(new TestingAuthenticationToken("", "", List.of(ZEITERFASSUNG_VIEW_REPORT_ALL.authority())));

        final List<User> userList = List.of(
            new User(new UserId(""), new UserLocalId(1L), "", "", new EMailAddress("")),
            new User(new UserId(""), new UserLocalId(2L), "", "", new EMailAddress("")),
            new User(new UserId(""), new UserLocalId(3L), "", "", new EMailAddress(""))
        );

        when(userManagementService.findAllUsers()).thenReturn(userList);

        assertThat(sut.findAllPermittedUsersForCurrentUser()).isSameAs(userList);
    }

    @Test
    void ensureFindAllPermittedUsersForCurrentUserReturnsOnlyItself() {

        when(currentUserProvider.getCurrentAuthentication())
            .thenReturn(new TestingAuthenticationToken("", "", List.of()));

        when(currentUserProvider.getCurrentUser())
            .thenReturn(new User(new UserId(""), new UserLocalId(2L), "", "", new EMailAddress("")));

        assertThat(sut.findAllPermittedUsersForCurrentUser())
            .containsOnly(new User(new UserId(""), new UserLocalId(2L), "", "", new EMailAddress("")));
    }
}
