package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.data.history.EntityRevisionMetadata;
import de.focusshift.zeiterfassung.security.SecurityRole;
import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static de.focusshift.zeiterfassung.data.history.EntityRevisionType.CREATED;
import static de.focusshift.zeiterfassung.data.history.EntityRevisionType.UPDATED;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeEntryDialogHelperTest {

    private TimeEntryDialogHelper sut;

    @Mock
    private TimeEntryService timeEntryService;
    @Mock
    private TimeEntryLockService timeEntryLockService;
    @Mock
    private TimeEntryViewHelper timeEntryViewHelper;
    @Mock
    private UserSettingsProvider userSettingsProvider;
    @Mock
    private UserManagementService userManagementService;

    @BeforeEach
    void setUp() {
        sut = new TimeEntryDialogHelper(timeEntryService, timeEntryLockService, timeEntryViewHelper, userSettingsProvider, userManagementService);
    }

    @Nested
    class AddTimeEntryEditToModel {

        @Test
        void ensureTimeEntryModelIsNotAddedWhenItExists() {

            final UserIdComposite userIdComposite = anyUserIdComposite("batman");
            final CurrentOidcUser currentOidcUser = anyCurrentOidcUser(userIdComposite);

            final TimeEntry timeEntry = anyTimeEntry(userIdComposite);
            when(timeEntryService.findTimeEntry(new TimeEntryId(1L))).thenReturn(Optional.of(timeEntry));


            final User user = anyUser(userIdComposite);
            when(userManagementService.findUserById(userIdComposite.id())).thenReturn(Optional.of(user));

            final ConcurrentModel model = new ConcurrentModel();
            model.addAttribute("timeEntry", -1);

            sut.addTimeEntryEditToModel(model, currentOidcUser, 1L, "", "");

            assertThat(model.getAttribute("timeEntry")).isEqualTo(-1);
        }

        @Test
        void ensureTimeEntryModel() {

            final UserIdComposite userIdComposite = anyUserIdComposite("batman");
            final CurrentOidcUser currentOidcUser = anyCurrentOidcUser(userIdComposite);

            final TimeEntry timeEntry = anyTimeEntry(userIdComposite);
            when(timeEntryService.findTimeEntry(new TimeEntryId(1L))).thenReturn(Optional.of(timeEntry));

            final TimeEntryDTO timeEntryDTO = new TimeEntryDTO();
            when(timeEntryViewHelper.toTimeEntryDto(timeEntry)).thenReturn(timeEntryDTO);

            final User user = anyUser();
            when(userManagementService.findUserById(userIdComposite.id())).thenReturn(Optional.of(user));

            final ConcurrentModel model = new ConcurrentModel();
            sut.addTimeEntryEditToModel(model, currentOidcUser, 1L, "", "");

            assertThat(model.getAttribute("timeEntry")).isSameAs(timeEntryDTO);
        }

        @Test
        void ensureTimeEntryHistoryModelWithoutHistory() {

            final UserIdComposite userIdComposite = anyUserIdComposite("batman");
            final CurrentOidcUser currentOidcUser = anyCurrentOidcUser(userIdComposite);

            final TimeEntry timeEntry = anyTimeEntry(userIdComposite);
            when(timeEntryService.findTimeEntry(new TimeEntryId(1L))).thenReturn(Optional.of(timeEntry));

            final User user = anyUser(userIdComposite);
            when(userManagementService.findUserById(userIdComposite.id())).thenReturn(Optional.of(user));

            final TimeEntryHistory timeEntryHistory = new TimeEntryHistory(timeEntry.id(), List.of());
            when(timeEntryService.findTimeEntryHistory(timeEntry.id())).thenReturn(Optional.of(timeEntryHistory));

            when(timeEntryLockService.isLocked(timeEntry.start())).thenReturn(false);

            final ConcurrentModel model = new ConcurrentModel();
            model.addAttribute("timeEntry", -1);

            sut.addTimeEntryEditToModel(model, currentOidcUser, 1L, "edit-form-action",  "close-form-action");

            final TimeEntryDialogDto expected = new TimeEntryDialogDto(true, user.fullName(), List.of(), "edit-form-action", "close-form-action");
            assertThat(model.getAttribute("timeEntryDialog")).isEqualTo(expected);
        }

        @Test
        void ensureTimeEntryIsEditableForPrivilegedUserEvenIfLocked() {

            final UserIdComposite privilegeduserIdComposite = anyUserIdComposite("office");
            final CurrentOidcUser privilgegedOidcUser = anyCurrentOidcUser(privilegeduserIdComposite, List.of(ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL));

            final TimeEntry timeEntry = anyTimeEntry();
            when(timeEntryService.findTimeEntry(new TimeEntryId(1L))).thenReturn(Optional.of(timeEntry));

            final User user = anyUser();
            when(userManagementService.findUserById(timeEntry.userIdComposite().id())).thenReturn(Optional.of(user));

            final TimeEntryHistory timeEntryHistory = new TimeEntryHistory(timeEntry.id(), List.of());
            when(timeEntryService.findTimeEntryHistory(timeEntry.id())).thenReturn(Optional.of(timeEntryHistory));

            // locked but privileged user
            when(timeEntryLockService.isLocked(timeEntry.start())).thenReturn(true);
            when(timeEntryLockService.isUserAllowedToBypassLock(privilgegedOidcUser.getRoles())).thenReturn(true);

            final ConcurrentModel model = new ConcurrentModel();
            model.addAttribute("timeEntry", -1);

            sut.addTimeEntryEditToModel(model, privilgegedOidcUser, 1L, "edit-form-action",  "close-form-action");

            final TimeEntryDialogDto expected = new TimeEntryDialogDto(true, user.fullName(), List.of(), "edit-form-action", "close-form-action");
            assertThat(model.getAttribute("timeEntryDialog")).isEqualTo(expected);
        }

        @Test
        void ensureTimeEntryNotEditableBecauseLocked() {

            final UserIdComposite anyUserIdComposite = anyUserIdComposite("joker");
            final CurrentOidcUser currentOidcUser = anyCurrentOidcUser(anyUserIdComposite);

            final TimeEntry timeEntry = anyTimeEntry();
            when(timeEntryService.findTimeEntry(new TimeEntryId(1L))).thenReturn(Optional.of(timeEntry));

            final User user = anyUser();
            when(userManagementService.findUserById(timeEntry.userIdComposite().id())).thenReturn(Optional.of(user));

            final TimeEntryHistory timeEntryHistory = new TimeEntryHistory(timeEntry.id(), List.of());
            when(timeEntryService.findTimeEntryHistory(timeEntry.id())).thenReturn(Optional.of(timeEntryHistory));

            // locked and not privileged user
            when(timeEntryLockService.isLocked(timeEntry.start())).thenReturn(true);
            when(timeEntryLockService.isUserAllowedToBypassLock(currentOidcUser.getRoles())).thenReturn(false);

            final ConcurrentModel model = new ConcurrentModel();
            model.addAttribute("timeEntry", -1);

            sut.addTimeEntryEditToModel(model, currentOidcUser, 1L, "edit-form-action",  "close-form-action");

            final TimeEntryDialogDto expected = new TimeEntryDialogDto(false, user.fullName(), List.of(), "edit-form-action", "close-form-action");
            assertThat(model.getAttribute("timeEntryDialog")).isEqualTo(expected);
        }

        @Test
        void ensureTimeEntryHistoryModel() {

            final UserIdComposite batmanIdComposite = anyUserIdComposite("batman");
            final CurrentOidcUser batmanOidcUser = anyCurrentOidcUser(batmanIdComposite);

            final User user = anyUser(batmanIdComposite, "Bruce", "Wayne");
            final User otherUser = anyUser(anyUserIdComposite("pennyworth"), "Alfred", "Pennyworth");

            final TimeEntry createdTimeEntry = anyTimeEntry(batmanIdComposite, "workshop");
            final TimeEntry modifiedTimeEntry = anyTimeEntry(batmanIdComposite, "Kickoff Workshop");
            when(timeEntryService.findTimeEntry(new TimeEntryId(1L))).thenReturn(Optional.of(modifiedTimeEntry));

            when(userManagementService.findUserById(createdTimeEntry.userIdComposite().id())).thenReturn(Optional.of(user));
            when(userSettingsProvider.zoneId()).thenReturn(ZoneOffset.UTC);

            final Instant createdInstant = Instant.now();
            final EntityRevisionMetadata createdMetadata = new EntityRevisionMetadata(1, CREATED, createdInstant, Optional.of(user.userId()));
            final TimeEntryHistoryItem createdHistoryItem = new TimeEntryHistoryItem(createdMetadata, createdTimeEntry, true, true, true, true);

            final Instant modifiedInstant = Instant.now();
            final EntityRevisionMetadata modifiedMetadata = new EntityRevisionMetadata(2, UPDATED, modifiedInstant, Optional.of(otherUser.userId()));
            final TimeEntryHistoryItem modifiedHistoryItem = new TimeEntryHistoryItem(modifiedMetadata, modifiedTimeEntry, true, false, false, false);

            final TimeEntryHistory timeEntryHistory = new TimeEntryHistory(createdTimeEntry.id(), List.of(createdHistoryItem, modifiedHistoryItem));
            when(timeEntryService.findTimeEntryHistory(createdTimeEntry.id())).thenReturn(Optional.of(timeEntryHistory));

            when(userManagementService.findAllUsersByIds(List.of(user.userId(), otherUser.userId()))).thenReturn(List.of(otherUser, user));

            final TimeEntryDTO createdTimeEntryDto = new TimeEntryDTO();
            createdTimeEntryDto.setComment(createdTimeEntry.comment());
            when(timeEntryViewHelper.toTimeEntryDto(createdTimeEntry)).thenReturn(createdTimeEntryDto);

            final TimeEntryDTO modifiedTimeEntryDto = new TimeEntryDTO();
            modifiedTimeEntryDto.setComment(modifiedTimeEntry.comment());
            when(timeEntryViewHelper.toTimeEntryDto(modifiedTimeEntry)).thenReturn(modifiedTimeEntryDto);

            final ConcurrentModel model = new ConcurrentModel();
            sut.addTimeEntryEditToModel(model, batmanOidcUser, 1L, "edit-form-action", "close-form-action");

            assertThat(model.getAttribute("timeEntryDialog"))
                .isInstanceOf(TimeEntryDialogDto.class)
                .satisfies(dialogDto -> {
                    final TimeEntryDialogDto dto = (TimeEntryDialogDto) dialogDto;
                    assertThat(dto.dialogCloseFormAction()).isEqualTo("close-form-action");
                    assertThat(dto.editTimeEntryFormAction()).isEqualTo("edit-form-action");
                    assertThat(dto.owner()).isEqualTo("Bruce Wayne");
                    assertThat(dto.historyItems()).hasSize(2);
                    assertThat(dto.historyItems().get(0).timeEntry()).isSameAs(modifiedTimeEntryDto);
                    assertThat(dto.historyItems().get(1).timeEntry()).isSameAs(createdTimeEntryDto);
                });
        }
    }

    @Nested
    class SaveTimeEntry {

        @Test
        void ensureSaveTimeEntryDelegates() {

            final UserIdComposite userIdComposite = anyUserIdComposite("batman");

            final TimeEntryDTO timeEntryDto = new TimeEntryDTO();
            timeEntryDto.setId(1L);

            final BindingResult bindingResult = mock(BindingResult.class);
            final CurrentOidcUser currentOidcUser = anyCurrentOidcUser(userIdComposite);
            final Model model = mock(Model.class);
            final RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

            sut.saveTimeEntry(currentOidcUser, timeEntryDto, bindingResult, model, redirectAttributes);

            verify(timeEntryViewHelper).updateTimeEntry(currentOidcUser, timeEntryDto, bindingResult, model, redirectAttributes);
        }
    }

    private static User anyUser() {
        return anyUser(anyUserIdComposite("batman"));
    }

    private static CurrentOidcUser anyCurrentOidcUser(UserIdComposite userIdComposite) {
        return anyCurrentOidcUser(userIdComposite, List.of());
    }

    private static CurrentOidcUser anyCurrentOidcUser(UserIdComposite userIdComposite, List<SecurityRole> securityRoles) {
        final DefaultOidcUser defaultOidcUser = new DefaultOidcUser(List.of(), OidcIdToken.withTokenValue("token-value").claim("sub", userIdComposite.id().value()).build());
        return new CurrentOidcUser(defaultOidcUser, securityRoles.stream().map(SecurityRole::authority).toList(), List.of(), userIdComposite.localId());
    }

    private static User anyUser(UserIdComposite userIdComposite) {
        return anyUser(userIdComposite, "Bruce", "Wayne");
    }

    private static User anyUser(UserIdComposite userIdComposite, String givenName, String familyName) {
        return anyUser(userIdComposite, givenName, familyName, Set.of());
    }

    private static User anyUser(UserIdComposite userIdComposite, String givenName, String familyName, Set<SecurityRole> roles) {
        return new User(userIdComposite, givenName, familyName, new EMailAddress(""), roles);
    }

    private static UserIdComposite anyUserIdComposite(String userIdValue) {

        final UserId userId = new UserId(userIdValue);
        final UserLocalId userLocalId = new UserLocalId(ThreadLocalRandom.current().nextLong());

        return new UserIdComposite(userId, userLocalId);
    }

    private TimeEntry anyTimeEntry() {
        final TimeEntryId id = new TimeEntryId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(new UserId("abcdefg"), new UserLocalId(42L));
        return new TimeEntry(id, userIdComposite, "hack the planet", ZonedDateTime.now(), ZonedDateTime.now(), false);
    }

    private TimeEntry anyTimeEntry(UserIdComposite userIdComposite) {
        return anyTimeEntry(userIdComposite, "");
    }

    private TimeEntry anyTimeEntry(UserIdComposite userIdComposite, String comment) {
        final TimeEntryId id = new TimeEntryId(1L);
        return new TimeEntry(id, userIdComposite, comment, ZonedDateTime.now(), ZonedDateTime.now(), false);
    }
}
