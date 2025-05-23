package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL;
import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeEntryViewHelperTest {

    private TimeEntryViewHelper sut;

    @Mock
    private TimeEntryService timeEntryService;
    @Mock
    private TimeEntryLockService timeEntryLockService;
    @Mock
    private UserSettingsProvider userSettingsProvider;

    @BeforeEach
    void setUp() {
        sut = new TimeEntryViewHelper(timeEntryService, timeEntryLockService, userSettingsProvider);
    }

    @Test
    void ensureToTimeEntryDto() {

        final UserId userId = new UserId("userId");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final ZonedDateTime start = ZonedDateTime.parse("2025-02-16T09:00:00Z");
        final ZonedDateTime end = ZonedDateTime.parse("2025-02-16T17:00:00Z");

        final TimeEntry timeEntry = new TimeEntry(new TimeEntryId(1L), userIdComposite, "comment", start, end, false);

        final TimeEntryDTO actual = sut.toTimeEntryDto(timeEntry);

        assertThat(actual).isNotNull();
        assertThat(actual.getId()).isEqualTo(1L);
        assertThat(actual.getDate()).isEqualTo(LocalDate.parse("2025-02-16"));
        assertThat(actual.getStart()).isEqualTo(LocalTime.of(9, 0));
        assertThat(actual.getEnd()).isEqualTo(LocalTime.of(17, 0));
        assertThat(actual.getDuration()).isEqualTo("08:00");
        assertThat(actual.getComment()).isEqualTo("comment");
        assertThat(actual.isBreak()).isFalse();
    }

    @Nested
    class HandleCrudTimeEntryErrors {

        @Test
        void ensureStartOrEndRequired() {

            final TimeEntryDTO timeEntryDTO = new TimeEntryDTO();
            final BindingResult bindingResult = mock(BindingResult.class);
            final Model model = mock(Model.class);
            final RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

            when(bindingResult.hasFieldErrors("start")).thenReturn(true);
            when(bindingResult.hasFieldErrors("end")).thenReturn(true);
            when(bindingResult.hasFieldErrors("duration")).thenReturn(false);

            sut.handleCrudTimeEntryErrors(timeEntryDTO, bindingResult, model, redirectAttributes);

            verify(bindingResult).reject("time-entry.validation.startOrEnd.required");
            verifyNoMoreInteractions(bindingResult);
        }

        @Test
        void ensureStartOrDurationRequired() {

            final TimeEntryDTO timeEntryDTO = new TimeEntryDTO();
            final BindingResult bindingResult = mock(BindingResult.class);
            final Model model = mock(Model.class);
            final RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

            when(bindingResult.hasFieldErrors("start")).thenReturn(true);
            when(bindingResult.hasFieldErrors("end")).thenReturn(false);
            when(bindingResult.hasFieldErrors("duration")).thenReturn(true);

            sut.handleCrudTimeEntryErrors(timeEntryDTO, bindingResult, model, redirectAttributes);

            verify(bindingResult).reject("time-entry.validation.startOrDuration.required");
            verifyNoMoreInteractions(bindingResult);
        }

        @Test
        void ensureEndOrDurationRequired() {

            final TimeEntryDTO timeEntryDTO = new TimeEntryDTO();
            final BindingResult bindingResult = mock(BindingResult.class);
            final Model model = mock(Model.class);
            final RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

            when(bindingResult.hasFieldErrors("start")).thenReturn(false);
            when(bindingResult.hasFieldErrors("end")).thenReturn(true);
            when(bindingResult.hasFieldErrors("duration")).thenReturn(true);

            sut.handleCrudTimeEntryErrors(timeEntryDTO, bindingResult, model, redirectAttributes);

            verify(bindingResult).reject("time-entry.validation.endOrDuration.required");
            verifyNoMoreInteractions(bindingResult);
        }

        @Test
        void ensureRedirectAttributesAreSet() {

            final TimeEntryDTO timeEntryDTO = new TimeEntryDTO();
            timeEntryDTO.setId(1L);

            final BindingResult bindingResult = mock(BindingResult.class);
            final Model model = mock(Model.class);
            final RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

            sut.handleCrudTimeEntryErrors(timeEntryDTO, bindingResult, model, redirectAttributes);

            verify(redirectAttributes).addFlashAttribute(eq("org.springframework.validation.BindingResult.timeEntry"), same(bindingResult));
            verify(redirectAttributes).addFlashAttribute("timeEntry", timeEntryDTO);
            verify(redirectAttributes).addFlashAttribute("timeEntryErrorId", 1L);
            verifyNoMoreInteractions(redirectAttributes);
        }
    }

    @Nested
    class CreateTimeEntry {

        @Test
        void ensureCreateValidationErrorWhenTimespanIsLocked() {

            final UserLocalId userLocalId = new UserLocalId(1L);
            final ZoneOffset userZoneId = UTC;
            when(userSettingsProvider.zoneId()).thenReturn(userZoneId);

            final CurrentOidcUser currentUser = anyCurrentOidcUser(userLocalId);

            final LocalDate date = LocalDate.parse("2025-02-16");
            final LocalTime startTime = LocalTime.parse("09:00");
            final LocalTime endTime = LocalTime.parse("17:00");

            final TimeEntryDTO timeEntryDTO = new TimeEntryDTO();
            timeEntryDTO.setUserLocalId(userLocalId.value());
            timeEntryDTO.setDate(date);
            timeEntryDTO.setStart(startTime);
            timeEntryDTO.setEnd(endTime);
            timeEntryDTO.setComment("comment");

            final LocalDateTime start = LocalDateTime.of(date, startTime);
            final LocalDateTime end = LocalDateTime.of(date, endTime);
            when(timeEntryLockService.isTimespanLocked(ZonedDateTime.of(start, userZoneId), ZonedDateTime.of(end, userZoneId)))
                .thenReturn(true);

            final BindingResult bindingResult = mock(BindingResult.class);

            sut.createTimeEntry(timeEntryDTO, bindingResult, currentUser);

            verify(bindingResult).reject("time-entry.validation.timespan.locked");
        }

        @Test
        void ensureCreateAllowedForPrivilegedPersonDespiteTimespanIsLocked() {

            final UserLocalId userLocalId = new UserLocalId(1L);
            final ZoneOffset userZoneId = UTC;
            when(userSettingsProvider.zoneId()).thenReturn(userZoneId);

            final CurrentOidcUser privilegedUser = anyCurrentOidcUser(userLocalId, List.of(ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL.authority()));

            final LocalDate date = LocalDate.parse("2025-02-16");
            final LocalTime startTime = LocalTime.parse("09:00");
            final LocalTime endTime = LocalTime.parse("17:00");

            final TimeEntryDTO timeEntryDTO = new TimeEntryDTO();
            timeEntryDTO.setUserLocalId(userLocalId.value());
            timeEntryDTO.setDate(date);
            timeEntryDTO.setStart(startTime);
            timeEntryDTO.setEnd(endTime);
            timeEntryDTO.setComment("comment");

            final ZonedDateTime start = ZonedDateTime.of(LocalDateTime.of(date, startTime), userZoneId);
            final ZonedDateTime end = ZonedDateTime.of(LocalDateTime.of(date, endTime), userZoneId);

            when(timeEntryLockService.isTimespanLocked(start, end)).thenReturn(true);
            when(timeEntryLockService.isUserAllowedToBypassLock(List.of(ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL))).thenReturn(true);

            final BindingResult bindingResult = mock(BindingResult.class);

            sut.createTimeEntry(timeEntryDTO, bindingResult, privilegedUser);

            verify(bindingResult).hasErrors();
            verifyNoMoreInteractions(bindingResult);

            verify(timeEntryService).createTimeEntry(userLocalId, "comment", start, end, false);
        }

        @Test
        void ensureCreateTimeEntryWithStartAndEnd() {

            final UserLocalId userLocalId = new UserLocalId(1L);
            when(userSettingsProvider.zoneId()).thenReturn(UTC);

            final CurrentOidcUser currentUser = anyCurrentOidcUser(userLocalId);

            final TimeEntryDTO timeEntryDTO = new TimeEntryDTO();
            timeEntryDTO.setUserLocalId(userLocalId.value());
            timeEntryDTO.setId(null);
            timeEntryDTO.setDate(LocalDate.parse("2025-02-16"));
            timeEntryDTO.setStart(LocalTime.parse("09:00"));
            timeEntryDTO.setEnd(LocalTime.parse("17:00"));
            timeEntryDTO.setComment("comment");
            timeEntryDTO.setBreak(false);

            final BindingResult bindingResult = mock(BindingResult.class);

            sut.createTimeEntry(timeEntryDTO, bindingResult, currentUser);

            final ZonedDateTime start = ZonedDateTime.parse("2025-02-16T09:00:00Z");
            final ZonedDateTime end = ZonedDateTime.parse("2025-02-16T17:00:00Z");
            verify(timeEntryService).createTimeEntry(userLocalId, "comment", start, end, false);
        }

        @ParameterizedTest
        @ValueSource(strings = {"8:00", "08:00"})
        void ensureCreateTimeEntryWithStartAndDuration(String duration) {

            final UserLocalId userLocalId = new UserLocalId(1L);
            when(userSettingsProvider.zoneId()).thenReturn(UTC);

            final CurrentOidcUser currentUser = anyCurrentOidcUser(userLocalId);

            final TimeEntryDTO timeEntryDTO = new TimeEntryDTO();
            timeEntryDTO.setUserLocalId(userLocalId.value());
            timeEntryDTO.setId(null);
            timeEntryDTO.setDate(LocalDate.parse("2025-02-16"));
            timeEntryDTO.setStart(LocalTime.parse("09:00"));
            timeEntryDTO.setDuration(duration);
            timeEntryDTO.setBreak(false);

            final BindingResult bindingResult = mock(BindingResult.class);

            sut.createTimeEntry(timeEntryDTO, bindingResult, currentUser);

            final ZonedDateTime start = ZonedDateTime.parse("2025-02-16T09:00:00Z");
            final ZonedDateTime end = ZonedDateTime.parse("2025-02-16T17:00:00Z");
            verify(timeEntryService).createTimeEntry(userLocalId, null, start, end, false);
        }

        @ParameterizedTest
        @ValueSource(strings = {"8:00", "08:00"})
        void ensureCreateTimeEntryWithEndAndDuration(String duration) {

            final UserLocalId userLocalId = new UserLocalId(1L);
            when(userSettingsProvider.zoneId()).thenReturn(UTC);

            final CurrentOidcUser currentUser = anyCurrentOidcUser(userLocalId);

            final TimeEntryDTO timeEntryDTO = new TimeEntryDTO();
            timeEntryDTO.setUserLocalId(userLocalId.value());
            timeEntryDTO.setId(null);
            timeEntryDTO.setDate(LocalDate.parse("2025-02-16"));
            timeEntryDTO.setEnd(LocalTime.parse("17:00"));
            timeEntryDTO.setDuration(duration);
            timeEntryDTO.setBreak(false);

            final BindingResult bindingResult = mock(BindingResult.class);

            sut.createTimeEntry(timeEntryDTO, bindingResult, currentUser);

            final ZonedDateTime start = ZonedDateTime.parse("2025-02-16T09:00:00Z");
            final ZonedDateTime end = ZonedDateTime.parse("2025-02-16T17:00:00Z");
            verify(timeEntryService).createTimeEntry(userLocalId, null, start, end, false);
        }
    }

    @Nested
    class UpdateTimeEntry {

        @Test
        void ensureUpdateTimeEntryThrowsWhenThereIsNoTimeEntryId() {

            final UserLocalId userLocalId = new UserLocalId(1L);

            final TimeEntryDTO timeEntryDTO = new TimeEntryDTO();
            timeEntryDTO.setUserLocalId(userLocalId.value());

            final BindingResult bindingResult = mock(BindingResult.class);
            final CurrentOidcUser currentOidcUser = anyCurrentOidcUser(userLocalId);
            final Model model = mock(Model.class);
            final RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

            assertThatThrownBy(() -> sut.updateTimeEntry(currentOidcUser, timeEntryDTO, bindingResult, model, redirectAttributes))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Expected timeEntry id to have value. Did you meant to create a time entry?");

            verifyNoInteractions(timeEntryService);
            verifyNoInteractions(bindingResult);
            verifyNoInteractions(model);
            verifyNoInteractions(redirectAttributes);
        }

        @Test
        void ensureUpdateTimeEntryThrowsWhenTimeEntryIsNotFound() {

            final UserLocalId userLocalId = new UserLocalId(1L);

            final TimeEntryDTO timeEntryDTO = new TimeEntryDTO();
            timeEntryDTO.setUserLocalId(userLocalId.value());
            timeEntryDTO.setId(1L);

            final BindingResult bindingResult = mock(BindingResult.class);
            final CurrentOidcUser currentOidcUser = anyCurrentOidcUser(userLocalId);
            final Model model = mock(Model.class);
            final RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

            when(timeEntryService.findTimeEntry(new TimeEntryId(1L))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sut.updateTimeEntry(currentOidcUser, timeEntryDTO, bindingResult, model, redirectAttributes))
                .isInstanceOf(TimeEntryNotFoundException.class)
                .hasMessage("TimeEntry with TimeEntryId[value=1] not found.");

            verifyNoMoreInteractions(timeEntryService);
            verifyNoInteractions(bindingResult);
            verifyNoInteractions(model);
            verifyNoInteractions(redirectAttributes);
        }

        @Test
        void ensureUpdateTimeEntryThrowsWhenUserIsNotOwnerAndHasNoAuthority() {

            final UserLocalId loggedInUserLocalId = new UserLocalId(1L);

            final UserId timeEntryOwnerId = new UserId("other-user-id");
            final UserLocalId timeEntryOwnerLocalId = new UserLocalId(2L);
            final UserIdComposite timeEntryOwnerIdComposite = new UserIdComposite(timeEntryOwnerId, timeEntryOwnerLocalId);

            final TimeEntryDTO timeEntryDTO = new TimeEntryDTO();
            timeEntryDTO.setUserLocalId(loggedInUserLocalId.value());
            timeEntryDTO.setId(1L);

            final BindingResult bindingResult = mock(BindingResult.class);
            final CurrentOidcUser currentOidcUser = anyCurrentOidcUser(loggedInUserLocalId);
            final Model model = mock(Model.class);
            final RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

            when(timeEntryService.findTimeEntry(new TimeEntryId(1L))).thenReturn(Optional.of(anyTimeEntry(timeEntryOwnerIdComposite)));

            assertThatThrownBy(() -> sut.updateTimeEntry(currentOidcUser, timeEntryDTO, bindingResult, model, redirectAttributes))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Not allowed to edit timeEntry TimeEntryId[value=1].");

            verifyNoMoreInteractions(timeEntryService);
            verifyNoInteractions(bindingResult);
            verifyNoInteractions(model);
            verifyNoInteractions(redirectAttributes);
        }

        @Test
        void ensureUpdateTimeEntryHandlesErrorWhenOnlyDurationIsSet() {

            final UserId loggedInUserId = new UserId("user-id");
            final UserLocalId loggedInUserLocalId = new UserLocalId(42L);
            final UserIdComposite loggedInUserIdComposite = new UserIdComposite(loggedInUserId, loggedInUserLocalId);

            final TimeEntryDTO timeEntryDTO = new TimeEntryDTO();
            timeEntryDTO.setUserLocalId(loggedInUserLocalId.value());
            timeEntryDTO.setId(1L);

            final BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.hasErrors()).thenReturn(true);
            when(bindingResult.hasFieldErrors("start")).thenReturn(true);
            when(bindingResult.hasFieldErrors("end")).thenReturn(true);
            when(bindingResult.hasFieldErrors("duration")).thenReturn(false);

            when(timeEntryService.findTimeEntry(new TimeEntryId(1L))).thenReturn(Optional.of(anyTimeEntry(loggedInUserIdComposite)));

            final CurrentOidcUser currentOidcUser = anyCurrentOidcUser(loggedInUserLocalId);
            final Model model = new ConcurrentModel(bindingResult);
            final RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

            sut.updateTimeEntry(currentOidcUser, timeEntryDTO, bindingResult, model, redirectAttributes);

            verifyNoMoreInteractions(timeEntryService);
            verify(bindingResult).reject("time-entry.validation.startOrEnd.required");
            verifyNoMoreInteractions(bindingResult);
        }

        @Test
        void ensureUpdateTimeEntryHandlesErrorWhenOnlyStartIsSet() {

            final UserId loggedInUserId = new UserId("user-id");
            final UserLocalId loggedInUserLocalId = new UserLocalId(42L);
            final UserIdComposite loggedInUserIdComposite = new UserIdComposite(loggedInUserId, loggedInUserLocalId);

            final TimeEntryDTO timeEntryDTO = new TimeEntryDTO();
            timeEntryDTO.setUserLocalId(loggedInUserLocalId.value());
            timeEntryDTO.setId(1L);

            final BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.hasErrors()).thenReturn(true);
            when(bindingResult.hasFieldErrors("start")).thenReturn(false);
            when(bindingResult.hasFieldErrors("end")).thenReturn(true);
            when(bindingResult.hasFieldErrors("duration")).thenReturn(true);

            when(timeEntryService.findTimeEntry(new TimeEntryId(1L))).thenReturn(Optional.of(anyTimeEntry(loggedInUserIdComposite)));

            final CurrentOidcUser currentOidcUser = anyCurrentOidcUser(loggedInUserLocalId);
            final Model model = new ConcurrentModel(bindingResult);
            final RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

            sut.updateTimeEntry(currentOidcUser, timeEntryDTO, bindingResult, model, redirectAttributes);

            verifyNoMoreInteractions(timeEntryService);
            verify(bindingResult).reject("time-entry.validation.endOrDuration.required");
            verifyNoMoreInteractions(bindingResult);
        }

        @Test
        void ensureUpdateTimeEntryHandlesErrorWhenOnlyEndIsSet() {

            final UserId loggedInUserId = new UserId("user-id");
            final UserLocalId loggedInUserLocalId = new UserLocalId(42L);
            final UserIdComposite loggedInUserIdComposite = new UserIdComposite(loggedInUserId, loggedInUserLocalId);

            final TimeEntryDTO timeEntryDTO = new TimeEntryDTO();
            timeEntryDTO.setUserLocalId(loggedInUserLocalId.value());
            timeEntryDTO.setId(1L);

            final BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.hasErrors()).thenReturn(true);
            when(bindingResult.hasFieldErrors("start")).thenReturn(true);
            when(bindingResult.hasFieldErrors("end")).thenReturn(false);
            when(bindingResult.hasFieldErrors("duration")).thenReturn(true);

            when(timeEntryService.findTimeEntry(new TimeEntryId(1L))).thenReturn(Optional.of(anyTimeEntry(loggedInUserIdComposite)));

            final CurrentOidcUser currentOidcUser = anyCurrentOidcUser(loggedInUserLocalId);
            final Model model = new ConcurrentModel(bindingResult);
            final RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

            sut.updateTimeEntry(currentOidcUser, timeEntryDTO, bindingResult, model, redirectAttributes);

            verifyNoMoreInteractions(timeEntryService);
            verify(bindingResult).reject("time-entry.validation.startOrDuration.required");
            verifyNoMoreInteractions(bindingResult);
        }


        @Test
        void ensureUpdateTimeEntryErrorSetsRedirectAttributes() {

            final UserId loggedInUserId = new UserId("user-id");
            final UserLocalId loggedInUserLocalId = new UserLocalId(42L);
            final UserIdComposite loggedInUserIdComposite = new UserIdComposite(loggedInUserId, loggedInUserLocalId);

            final TimeEntryDTO timeEntryDTO = new TimeEntryDTO();
            timeEntryDTO.setUserLocalId(loggedInUserLocalId.value());
            timeEntryDTO.setId(1L);

            when(timeEntryService.findTimeEntry(new TimeEntryId(1L))).thenReturn(Optional.of(anyTimeEntry(loggedInUserIdComposite)));

            final BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.hasErrors()).thenReturn(true);

            final CurrentOidcUser currentOidcUser = anyCurrentOidcUser(loggedInUserLocalId);
            final Model model = new ConcurrentModel(bindingResult);
            final RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

            sut.updateTimeEntry(currentOidcUser, timeEntryDTO, bindingResult, model, redirectAttributes);

            verify(redirectAttributes).addFlashAttribute(BindingResult.MODEL_KEY_PREFIX + "timeEntry", bindingResult);
            verify(redirectAttributes).addFlashAttribute("timeEntry", timeEntryDTO);
            verify(redirectAttributes).addFlashAttribute("timeEntryErrorId", 1L);
        }

        @Test
        void ensureUpdateTimeEntryWithoutDurationSet() throws Exception {

            final UserId loggedInUserId = new UserId("user-id");
            final UserLocalId loggedInUserLocalId = new UserLocalId(42L);
            final UserIdComposite loggedInUserIdComposite = new UserIdComposite(loggedInUserId, loggedInUserLocalId);

            final TimeEntryId timeEntryId = new TimeEntryId(1L);

            final TimeEntryDTO timeEntryDto = new TimeEntryDTO();
            timeEntryDto.setUserLocalId(loggedInUserLocalId.value());
            timeEntryDto.setId(1L);
            timeEntryDto.setDate(LocalDate.parse("2025-02-14"));
            timeEntryDto.setStart(LocalTime.parse("12:15"));
            timeEntryDto.setEnd(LocalTime.parse("18:30"));
            timeEntryDto.setComment("comment-new");
            timeEntryDto.setBreak(false);

            when(timeEntryService.findTimeEntry(new TimeEntryId(1L))).thenReturn(Optional.of(anyTimeEntry(loggedInUserIdComposite)));
            when(userSettingsProvider.zoneId()).thenReturn(UTC);

            final BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.hasErrors()).thenReturn(false);

            final CurrentOidcUser currentOidcUser = anyCurrentOidcUser(loggedInUserLocalId);
            final Model model = new ConcurrentModel(bindingResult);
            final RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

            sut.updateTimeEntry(currentOidcUser, timeEntryDto, bindingResult, model, redirectAttributes);

            verifyNoMoreInteractions(bindingResult);
            verifyNoInteractions(redirectAttributes);

            final ZonedDateTime expectedStart = ZonedDateTime.parse("2025-02-14T12:15:00Z");
            final ZonedDateTime expectedEnd = ZonedDateTime.parse("2025-02-14T18:30:00Z");
            final Duration expectedDuration = Duration.ZERO;
            verify(timeEntryService).updateTimeEntry(timeEntryId, "comment-new", expectedStart, expectedEnd, expectedDuration, false);
        }

        @Test
        void ensureUpdateTimeEntrySuccessForAuthorizedUser() throws Exception {

            final UserLocalId loggedInUserLocalId = new UserLocalId(1L);

            final UserId timeEntryOwnerId = new UserId("other-user-id");
            final UserLocalId timeEntryOwnerLocalId = new UserLocalId(2L);
            final UserIdComposite timeEntryOwnerIdComposite = new UserIdComposite(timeEntryOwnerId, timeEntryOwnerLocalId);

            final TimeEntryId timeEntryId = new TimeEntryId(1L);

            final TimeEntryDTO timeEntryDto = new TimeEntryDTO();
            timeEntryDto.setUserLocalId(timeEntryOwnerLocalId.value());
            timeEntryDto.setId(1L);
            timeEntryDto.setDate(LocalDate.parse("2025-02-14"));
            timeEntryDto.setStart(LocalTime.parse("12:15"));
            timeEntryDto.setEnd(LocalTime.parse("18:30"));
            timeEntryDto.setComment("comment-new");
            timeEntryDto.setBreak(false);

            when(timeEntryService.findTimeEntry(new TimeEntryId(1L))).thenReturn(Optional.of(anyTimeEntry(timeEntryOwnerIdComposite)));
            when(userSettingsProvider.zoneId()).thenReturn(UTC);

            final BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.hasErrors()).thenReturn(false);

            final CurrentOidcUser currentOidcUser = anyAuthenticatedCurrentOidcUser(loggedInUserLocalId);
            final Model model = new ConcurrentModel(bindingResult);
            final RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

            sut.updateTimeEntry(currentOidcUser, timeEntryDto, bindingResult, model, redirectAttributes);

            final ZonedDateTime expectedStart = ZonedDateTime.parse("2025-02-14T12:15:00Z");
            final ZonedDateTime expectedEnd = ZonedDateTime.parse("2025-02-14T18:30:00Z");
            final Duration expectedDuration = Duration.ZERO;
            verify(timeEntryService).updateTimeEntry(timeEntryId, "comment-new", expectedStart, expectedEnd, expectedDuration, false);
        }

        @Test
        void ensureUpdateTimeEntryHandlesUpdateNotPlausibleException() throws Exception {

            final UserId loggedInUserId = new UserId("user-id");
            final UserLocalId loggedInUserLocalId = new UserLocalId(42L);
            final UserIdComposite loggedInUserIdComposite = new UserIdComposite(loggedInUserId, loggedInUserLocalId);

            final TimeEntryDTO timeEntryDto = new TimeEntryDTO();
            timeEntryDto.setId(1L);
            timeEntryDto.setDate(LocalDate.parse("2025-02-14"));
            timeEntryDto.setStart(LocalTime.parse("12:15"));
            timeEntryDto.setEnd(LocalTime.parse("18:30"));
            timeEntryDto.setComment("comment-new");
            timeEntryDto.setBreak(false);

            when(timeEntryService.findTimeEntry(new TimeEntryId(1L))).thenReturn(Optional.of(anyTimeEntry(loggedInUserIdComposite)));
            when(userSettingsProvider.zoneId()).thenReturn(UTC);

            when(timeEntryService.updateTimeEntry(any(TimeEntryId.class), anyString(), any(ZonedDateTime.class), any(ZonedDateTime.class), any(Duration.class), anyBoolean()))
                .thenThrow(new TimeEntryUpdateNotPlausibleException("message"));

            final BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.hasErrors()).thenReturn(false);

            final CurrentOidcUser currentOidcUser = anyCurrentOidcUser(loggedInUserLocalId);
            final Model model = mock(Model.class);
            final RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

            sut.updateTimeEntry(currentOidcUser, timeEntryDto, bindingResult, model, redirectAttributes);

            verify(bindingResult).reject("time-entry.validation.plausible");
            verify(bindingResult).rejectValue("start", "");
            verify(bindingResult).rejectValue("end", "");
            verify(bindingResult).rejectValue("duration", "");
            verifyNoMoreInteractions(bindingResult);

            verify(redirectAttributes).addFlashAttribute(BindingResult.MODEL_KEY_PREFIX + "timeEntry", bindingResult);
            verify(redirectAttributes).addFlashAttribute("timeEntry", timeEntryDto);
            verify(redirectAttributes).addFlashAttribute("timeEntryErrorId", 1L);
        }

        @Test
        void ensureUpdateValidationErrorWhenTimespanIsLocked() {

            final UserId loggedInUserId = new UserId("user-id");
            final UserLocalId loggedInUserLocalId = new UserLocalId(42L);
            final UserIdComposite loggedInUserIdComposite = new UserIdComposite(loggedInUserId, loggedInUserLocalId);
            final ZoneOffset userZoneId = UTC;

            final LocalDate date = LocalDate.parse("2025-02-14");
            final LocalTime startTime = LocalTime.parse("12:15");
            final LocalTime endTime = LocalTime.parse("18:30");

            final TimeEntryDTO timeEntryDto = new TimeEntryDTO();
            timeEntryDto.setId(1L);
            timeEntryDto.setDate(date);
            timeEntryDto.setStart(startTime);
            timeEntryDto.setEnd(endTime);
            timeEntryDto.setComment("comment-new");
            timeEntryDto.setBreak(false);

            when(timeEntryService.findTimeEntry(new TimeEntryId(1L))).thenReturn(Optional.of(anyTimeEntry(loggedInUserIdComposite)));
            when(userSettingsProvider.zoneId()).thenReturn(userZoneId);

            final LocalDateTime start = LocalDateTime.of(date, startTime);
            final LocalDateTime end = LocalDateTime.of(date, endTime);
            when(timeEntryLockService.isTimespanLocked(ZonedDateTime.of(start, userZoneId), ZonedDateTime.of(end, userZoneId)))
                .thenReturn(true);

            final BindingResult bindingResult = mock(BindingResult.class);

            final CurrentOidcUser currentOidcUser = anyCurrentOidcUser(loggedInUserLocalId);
            final Model model = mock(Model.class);
            final RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

            sut.updateTimeEntry(currentOidcUser, timeEntryDto, bindingResult, model, redirectAttributes);

            verify(bindingResult).reject("time-entry.validation.timespan.locked");
        }

        @Test
        void ensureUpdateAllowedForPrivilegedPersonDespiteTimespanIsLocked() throws Exception {

            final UserId loggedInUserId = new UserId("user-id");
            final UserLocalId loggedInUserLocalId = new UserLocalId(42L);
            final UserIdComposite loggedInUserIdComposite = new UserIdComposite(loggedInUserId, loggedInUserLocalId);
            final ZoneOffset userZoneId = UTC;

            final LocalDate date = LocalDate.parse("2025-02-14");
            final LocalTime startTime = LocalTime.parse("13:00");
            final LocalTime endTime = LocalTime.parse("17:00");

            final TimeEntryDTO timeEntryDto = new TimeEntryDTO();
            timeEntryDto.setId(1L);
            timeEntryDto.setDate(date);
            timeEntryDto.setStart(startTime);
            timeEntryDto.setEnd(endTime);
            timeEntryDto.setComment("comment-new");
            timeEntryDto.setBreak(false);

            when(timeEntryService.findTimeEntry(new TimeEntryId(1L))).thenReturn(Optional.of(anyTimeEntry(loggedInUserIdComposite)));
            when(userSettingsProvider.zoneId()).thenReturn(userZoneId);

            final ZonedDateTime start = ZonedDateTime.of(LocalDateTime.of(date, startTime), userZoneId);
            final ZonedDateTime end = ZonedDateTime.of(LocalDateTime.of(date, endTime), userZoneId);
            when(timeEntryLockService.isTimespanLocked(start, end)).thenReturn(true);
            when(timeEntryLockService.isUserAllowedToBypassLock(List.of())).thenReturn(true);

            final BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.hasErrors()).thenReturn(false);

            final CurrentOidcUser currentOidcUser = anyCurrentOidcUser(loggedInUserLocalId);
            final Model model = mock(Model.class);
            final RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

            sut.updateTimeEntry(currentOidcUser, timeEntryDto, bindingResult, model, redirectAttributes);

            verifyNoMoreInteractions(bindingResult);
            verify(timeEntryService).updateTimeEntry(new TimeEntryId(1L), "comment-new", start, end, Duration.ZERO, false);
        }
    }

    private static CurrentOidcUser anyAuthenticatedCurrentOidcUser(UserLocalId userLocalId) {
        final List<GrantedAuthority> authorities = List.of(ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL.authority());
        return anyCurrentOidcUser(userLocalId, authorities);
    }

    private static CurrentOidcUser anyCurrentOidcUser(UserLocalId userLocalId) {
        return anyCurrentOidcUser(userLocalId, List.of());
    }

    private static CurrentOidcUser anyCurrentOidcUser(UserLocalId userLocalId, List<GrantedAuthority> authorities) {
        return new CurrentOidcUser(new DefaultOidcUser(authorities, OidcIdToken.withTokenValue("value").subject("subject").build()), authorities, authorities, userLocalId);
    }

    private static TimeEntry anyTimeEntry(UserIdComposite userIdComposite) {
        final TimeEntryId timeEntryId = new TimeEntryId(1L);
        final ZonedDateTime start = ZonedDateTime.parse("2025-02-16T09:00:00Z");
        final ZonedDateTime end = ZonedDateTime.parse("2025-02-16T17:00:00Z");
        return new TimeEntry(timeEntryId, userIdComposite, "comment", start, end, false);
    }
}
