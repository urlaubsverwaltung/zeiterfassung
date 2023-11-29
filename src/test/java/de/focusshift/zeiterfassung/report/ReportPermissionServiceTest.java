package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.user.CurrentUserProvider;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
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
import java.util.Set;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_VIEW_REPORT_ALL;
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

        final UserId userId = new UserId("");
        final UserLocalId userLocalId = new UserLocalId(2L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        when(currentUserProvider.getCurrentUser())
            .thenReturn(new User(userIdComposite, "", "", new EMailAddress(""), Set.of()));

        final List<UserLocalId> actual = sut.filterUserLocalIdsByCurrentUserHasPermissionFor(
            List.of(new UserLocalId(1L), userLocalId, new UserLocalId(3L)));

        assertThat(actual).containsOnly(userLocalId);
    }

    @Test
    void ensureFilterUserLocalIdsByCurrentUserHasPermissionForReturnsEmptyList() {

        when(currentUserProvider.getCurrentAuthentication())
            .thenReturn(new TestingAuthenticationToken("", "", List.of()));

        final UserId userId = new UserId("");
        final UserLocalId userLocalId = new UserLocalId(2L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        when(currentUserProvider.getCurrentUser())
            .thenReturn(new User(userIdComposite, "", "", new EMailAddress(""), Set.of()));

        final List<UserLocalId> actual = sut.filterUserLocalIdsByCurrentUserHasPermissionFor(
            List.of(new UserLocalId(1L), new UserLocalId(3L)));

        assertThat(actual).isEmpty();
    }

    @Test
    void ensureFindAllPermittedUserLocalIdsForCurrentUser() {

        when(currentUserProvider.getCurrentAuthentication())
            .thenReturn(new TestingAuthenticationToken("", "", List.of(ZEITERFASSUNG_VIEW_REPORT_ALL.authority())));

        final UserId userId_1 = new UserId("");
        final UserLocalId userLocalId_1 = new UserLocalId(1L);
        final UserIdComposite userIdComposite_1 = new UserIdComposite(userId_1, userLocalId_1);

        final UserId userId_2 = new UserId("");
        final UserLocalId userLocalId_2 = new UserLocalId(2L);
        final UserIdComposite userIdComposite_2 = new UserIdComposite(userId_2, userLocalId_2);

        final UserId userId_3 = new UserId("");
        final UserLocalId userLocalId_3 = new UserLocalId(3L);
        final UserIdComposite userIdComposite_3 = new UserIdComposite(userId_3, userLocalId_3);

        when(userManagementService.findAllUsers())
            .thenReturn(List.of(
                new User(userIdComposite_1, "", "", new EMailAddress(""), Set.of()),
                new User(userIdComposite_2, "", "", new EMailAddress(""), Set.of()),
                new User(userIdComposite_3, "", "", new EMailAddress(""), Set.of())
            ));

        assertThat(sut.findAllPermittedUserLocalIdsForCurrentUser())
            .containsExactly(userLocalId_1, userLocalId_2, userLocalId_3);
    }

    @Test
    void ensureFindAllPermittedUserLocalIdsForCurrentUserReturnsCurrentUserOnly() {

        when(currentUserProvider.getCurrentAuthentication())
            .thenReturn(new TestingAuthenticationToken("", "", List.of()));

        final UserId userId = new UserId("");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        when(currentUserProvider.getCurrentUser())
            .thenReturn(new User(userIdComposite, "", "", new EMailAddress(""), Set.of()));

        assertThat(sut.findAllPermittedUserLocalIdsForCurrentUser()).containsOnly(userLocalId);
    }

    @Test
    void ensureFindAllPermittedUsersForCurrentUser() {

        when(currentUserProvider.getCurrentAuthentication())
            .thenReturn(new TestingAuthenticationToken("", "", List.of(ZEITERFASSUNG_VIEW_REPORT_ALL.authority())));

        final UserId userId_1 = new UserId("");
        final UserLocalId userLocalId_1 = new UserLocalId(1L);
        final UserIdComposite userIdComposite_1 = new UserIdComposite(userId_1, userLocalId_1);

        final UserId userId_2 = new UserId("");
        final UserLocalId userLocalId_2 = new UserLocalId(2L);
        final UserIdComposite userIdComposite_2 = new UserIdComposite(userId_2, userLocalId_2);

        final UserId userId_3 = new UserId("");
        final UserLocalId userLocalId_3 = new UserLocalId(3L);
        final UserIdComposite userIdComposite_3 = new UserIdComposite(userId_3, userLocalId_3);

        final List<User> userList = List.of(
            new User(userIdComposite_1, "", "", new EMailAddress(""), Set.of()),
            new User(userIdComposite_2, "", "", new EMailAddress(""), Set.of()),
            new User(userIdComposite_3, "", "", new EMailAddress(""), Set.of())
        );

        when(userManagementService.findAllUsers()).thenReturn(userList);

        assertThat(sut.findAllPermittedUsersForCurrentUser()).isSameAs(userList);
    }

    @Test
    void ensureFindAllPermittedUsersForCurrentUserReturnsOnlyItself() {

        when(currentUserProvider.getCurrentAuthentication())
            .thenReturn(new TestingAuthenticationToken("", "", List.of()));

        final UserId userId = new UserId("");
        final UserLocalId userLocalId = new UserLocalId(2L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "", "", new EMailAddress(""), Set.of());
        when(currentUserProvider.getCurrentUser()).thenReturn(user);

        assertThat(sut.findAllPermittedUsersForCurrentUser()).containsOnly(user);
    }
}
