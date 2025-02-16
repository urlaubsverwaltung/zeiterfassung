package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.security.AuthenticationService;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Optional;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL;
import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
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
    private UserSettingsProvider userSettingsProvider;
    @Mock
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        sut = new TimeEntryViewHelper(timeEntryService, userSettingsProvider, authenticationService);
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
    class CreateTimeEntry {

        @Test
        void ensureCreateTimeEntryHandlesErrorWhenOnlyDurationIsSet() {

            final TimeEntryDTO timeEntryDTO = new TimeEntryDTO();

            final BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.hasErrors()).thenReturn(true);
            when(bindingResult.hasFieldErrors("start")).thenReturn(true);
            when(bindingResult.hasFieldErrors("end")).thenReturn(true);
            when(bindingResult.hasFieldErrors("duration")).thenReturn(false);

            final Model model = new ConcurrentModel(bindingResult);
            final RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

            sut.createTimeEntry(timeEntryDTO, bindingResult, model, redirectAttributes);

            verify(bindingResult).reject("time-entry.validation.startOrEnd.required");
            verifyNoMoreInteractions(bindingResult);
        }

        @Test
        void ensureCreateTimeEntryHandlesErrorWhenOnlyStartIsSet() {

            final TimeEntryDTO timeEntryDTO = new TimeEntryDTO();

            final BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.hasErrors()).thenReturn(true);
            when(bindingResult.hasFieldErrors("start")).thenReturn(false);
            when(bindingResult.hasFieldErrors("end")).thenReturn(true);
            when(bindingResult.hasFieldErrors("duration")).thenReturn(true);

            final Model model = new ConcurrentModel(bindingResult);
            final RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

            sut.createTimeEntry(timeEntryDTO, bindingResult, model, redirectAttributes);

            verify(bindingResult).reject("time-entry.validation.endOrDuration.required");
            verifyNoMoreInteractions(bindingResult);
        }

        @Test
        void ensureCreateTimeEntryHandlesErrorWhenOnlyEndIsSet() {

            final TimeEntryDTO timeEntryDTO = new TimeEntryDTO();

            final BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.hasErrors()).thenReturn(true);
            when(bindingResult.hasFieldErrors("start")).thenReturn(true);
            when(bindingResult.hasFieldErrors("end")).thenReturn(false);
            when(bindingResult.hasFieldErrors("duration")).thenReturn(true);

            final Model model = new ConcurrentModel(bindingResult);
            final RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

            sut.createTimeEntry(timeEntryDTO, bindingResult, model, redirectAttributes);

            verify(bindingResult).reject("time-entry.validation.startOrDuration.required");
            verifyNoMoreInteractions(bindingResult);
        }

        @Test
        void ensureCreateTimeEntryErrorSetsRedirectAttributes() {

            final TimeEntryDTO timeEntryDTO = new TimeEntryDTO();

            final BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.hasErrors()).thenReturn(true);

            final Model model = new ConcurrentModel(bindingResult);
            final RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

            sut.createTimeEntry(timeEntryDTO, bindingResult, model, redirectAttributes);

            verify(redirectAttributes).addFlashAttribute(BindingResult.MODEL_KEY_PREFIX + "timeEntry", bindingResult);
            verify(redirectAttributes).addFlashAttribute("timeEntry", timeEntryDTO);
            verify(redirectAttributes).addFlashAttribute("timeEntryErrorId", timeEntryDTO.getId());
        }

        @Test
        void ensureCreateTimeEntryThrowsWhenThereIsATimeEntryId() {

            final TimeEntryDTO timeEntryDTO = new TimeEntryDTO();
            timeEntryDTO.setId(1L);

            final BindingResult bindingResult = mock(BindingResult.class);
            final Model model = new ConcurrentModel(bindingResult);
            final RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

            when(authenticationService.getCurrentUserIdComposite()).thenReturn(anyUserIdComposite());

            assertThatThrownBy(() -> sut.createTimeEntry(timeEntryDTO, bindingResult, model, redirectAttributes))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Expected timeEntry id not to be defined but has value. Did you meant to update the time entry?");
        }

        @Test
        void ensureCreateTimeEntry() {

            final UserId userId = new UserId("user-id");
            when(authenticationService.getCurrentUserIdComposite()).thenReturn(new UserIdComposite(userId, new UserLocalId(1L)));
            when(userSettingsProvider.zoneId()).thenReturn(UTC);

            final TimeEntryDTO timeEntryDTO = new TimeEntryDTO();
            timeEntryDTO.setId(null);
            timeEntryDTO.setDate(LocalDate.parse("2025-02-16"));
            timeEntryDTO.setStart(LocalTime.parse("09:00"));
            timeEntryDTO.setEnd(LocalTime.parse("17:00"));
            timeEntryDTO.setComment("comment");
            timeEntryDTO.setBreak(false);

            final BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.hasErrors()).thenReturn(false);

            final Model model = new ConcurrentModel(bindingResult);
            final RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

            sut.createTimeEntry(timeEntryDTO, bindingResult, model, redirectAttributes);

            final ZonedDateTime start = ZonedDateTime.parse("2025-02-16T09:00:00Z");
            final ZonedDateTime end = ZonedDateTime.parse("2025-02-16T17:00:00Z");

            verify(timeEntryService).createTimeEntry(userId, "comment", start, end, false);
            verifyNoMoreInteractions(bindingResult);
        }
    }

    @Nested
    class UpdateTimeEntry {

        @Test
        void ensureUpdateTimeEntryThrowsWhenThereIsNoTimeEntryId() {

            final TimeEntryDTO timeEntryDTO = new TimeEntryDTO();

            final BindingResult bindingResult = mock(BindingResult.class);
            final Model model = mock(Model.class);
            final RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

            assertThatThrownBy(() -> sut.updateTimeEntry(timeEntryDTO, bindingResult, model, redirectAttributes))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Expected timeEntry id to have value. Did you meant to create a time entry?");

            verifyNoInteractions(timeEntryService);
            verifyNoInteractions(bindingResult);
            verifyNoInteractions(model);
            verifyNoInteractions(redirectAttributes);
        }

        @Test
        void ensureUpdateTimeEntryThrowsWhenTimeEntryIsNotFound() {

            final TimeEntryDTO timeEntryDTO = new TimeEntryDTO();
            timeEntryDTO.setId(1L);

            final BindingResult bindingResult = mock(BindingResult.class);
            final Model model = mock(Model.class);
            final RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

            when(timeEntryService.findTimeEntry(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sut.updateTimeEntry(timeEntryDTO, bindingResult, model, redirectAttributes))
                .isInstanceOf(TimeEntryNotFoundException.class)
                .hasMessage("TimeEntry with TimeEntryId[value=1] not found.");

            verifyNoMoreInteractions(timeEntryService);
            verifyNoInteractions(bindingResult);
            verifyNoInteractions(model);
            verifyNoInteractions(redirectAttributes);
        }

        @Test
        void ensureUpdateTimeEntryThrowsWhenUserIsNotOwnerAndHasNoAuthority() {

            final UserId loggedInUserId = new UserId("user-id");
            final UserLocalId loggedInUserLocalId = new UserLocalId(1L);
            final UserIdComposite loggedInUserIdComposite = new UserIdComposite(loggedInUserId, loggedInUserLocalId);

            final UserId timeEntryOwnerId = new UserId("other-user-id");
            final UserLocalId timeEntryOwnerLocalId = new UserLocalId(2L);
            final UserIdComposite timeEntryOwnerIdComposite = new UserIdComposite(timeEntryOwnerId, timeEntryOwnerLocalId);

            final TimeEntryDTO timeEntryDTO = new TimeEntryDTO();
            timeEntryDTO.setId(1L);

            final BindingResult bindingResult = mock(BindingResult.class);
            final Model model = mock(Model.class);
            final RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

            when(timeEntryService.findTimeEntry(1L)).thenReturn(Optional.of(anyTimeEntry(timeEntryOwnerIdComposite)));
            when(authenticationService.getCurrentUserIdComposite()).thenReturn(loggedInUserIdComposite);
            when(authenticationService.hasSecurityRole(ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL)).thenReturn(false);

            assertThatThrownBy(() -> sut.updateTimeEntry(timeEntryDTO, bindingResult, model, redirectAttributes))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Not allowed to edit time entry with TimeEntryId[value=1].");

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
            timeEntryDTO.setId(1L);

            final BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.hasErrors()).thenReturn(true);
            when(bindingResult.hasFieldErrors("start")).thenReturn(true);
            when(bindingResult.hasFieldErrors("end")).thenReturn(true);
            when(bindingResult.hasFieldErrors("duration")).thenReturn(false);

            when(timeEntryService.findTimeEntry(1L)).thenReturn(Optional.of(anyTimeEntry(loggedInUserIdComposite)));
            when(authenticationService.getCurrentUserIdComposite()).thenReturn(loggedInUserIdComposite);

            final Model model = new ConcurrentModel(bindingResult);
            final RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

            sut.updateTimeEntry(timeEntryDTO, bindingResult, model, redirectAttributes);

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
            timeEntryDTO.setId(1L);

            final BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.hasErrors()).thenReturn(true);
            when(bindingResult.hasFieldErrors("start")).thenReturn(false);
            when(bindingResult.hasFieldErrors("end")).thenReturn(true);
            when(bindingResult.hasFieldErrors("duration")).thenReturn(true);

            when(timeEntryService.findTimeEntry(1L)).thenReturn(Optional.of(anyTimeEntry(loggedInUserIdComposite)));
            when(authenticationService.getCurrentUserIdComposite()).thenReturn(loggedInUserIdComposite);

            final Model model = new ConcurrentModel(bindingResult);
            final RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

            sut.updateTimeEntry(timeEntryDTO, bindingResult, model, redirectAttributes);

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
            timeEntryDTO.setId(1L);

            final BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.hasErrors()).thenReturn(true);
            when(bindingResult.hasFieldErrors("start")).thenReturn(true);
            when(bindingResult.hasFieldErrors("end")).thenReturn(false);
            when(bindingResult.hasFieldErrors("duration")).thenReturn(true);

            when(timeEntryService.findTimeEntry(1L)).thenReturn(Optional.of(anyTimeEntry(loggedInUserIdComposite)));
            when(authenticationService.getCurrentUserIdComposite()).thenReturn(loggedInUserIdComposite);

            final Model model = new ConcurrentModel(bindingResult);
            final RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

            sut.updateTimeEntry(timeEntryDTO, bindingResult, model, redirectAttributes);

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
            timeEntryDTO.setId(1L);

            when(timeEntryService.findTimeEntry(1L)).thenReturn(Optional.of(anyTimeEntry(loggedInUserIdComposite)));
            when(authenticationService.getCurrentUserIdComposite()).thenReturn(loggedInUserIdComposite);

            final BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.hasErrors()).thenReturn(true);

            final Model model = new ConcurrentModel(bindingResult);
            final RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

            sut.updateTimeEntry(timeEntryDTO, bindingResult, model, redirectAttributes);

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
            timeEntryDto.setId(1L);
            timeEntryDto.setDate(LocalDate.parse("2025-02-14"));
            timeEntryDto.setStart(LocalTime.parse("12:15"));
            timeEntryDto.setEnd(LocalTime.parse("18:30"));
            timeEntryDto.setComment("comment-new");
            timeEntryDto.setBreak(false);

            when(timeEntryService.findTimeEntry(1L)).thenReturn(Optional.of(anyTimeEntry(loggedInUserIdComposite)));
            when(authenticationService.getCurrentUserIdComposite()).thenReturn(loggedInUserIdComposite);
            when(userSettingsProvider.zoneId()).thenReturn(UTC);

            final BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.hasErrors()).thenReturn(false);

            final Model model = new ConcurrentModel(bindingResult);
            final RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

            sut.updateTimeEntry(timeEntryDto, bindingResult, model, redirectAttributes);

            verifyNoMoreInteractions(bindingResult);
            verifyNoInteractions(redirectAttributes);

            final ZonedDateTime expectedStart = ZonedDateTime.parse("2025-02-14T12:15:00Z");
            final ZonedDateTime expectedEnd = ZonedDateTime.parse("2025-02-14T18:30:00Z");
            final Duration expectedDuration = Duration.ZERO;
            verify(timeEntryService).updateTimeEntry(timeEntryId, "comment-new", expectedStart, expectedEnd, expectedDuration, false);
        }

        @Test
        void ensureUpdateTimeEntrySuccessForAuthorizedUser() throws Exception {

            final UserId timeEntryOwnerId = new UserId("other-user-id");
            final UserLocalId timeEntryOwnerLocalId = new UserLocalId(1L);
            final UserIdComposite timeEntryOwnerIdComposite = new UserIdComposite(timeEntryOwnerId, timeEntryOwnerLocalId);

            final UserId loggedInUserId = new UserId("user-id");
            final UserLocalId loggedInUserLocalId = new UserLocalId(2L);
            final UserIdComposite loggedInUserIdComposite = new UserIdComposite(loggedInUserId, loggedInUserLocalId);

            final TimeEntryId timeEntryId = new TimeEntryId(1L);

            final TimeEntryDTO timeEntryDto = new TimeEntryDTO();
            timeEntryDto.setId(1L);
            timeEntryDto.setDate(LocalDate.parse("2025-02-14"));
            timeEntryDto.setStart(LocalTime.parse("12:15"));
            timeEntryDto.setEnd(LocalTime.parse("18:30"));
            timeEntryDto.setComment("comment-new");
            timeEntryDto.setBreak(false);

            when(timeEntryService.findTimeEntry(1L)).thenReturn(Optional.of(anyTimeEntry(timeEntryOwnerIdComposite)));
            when(authenticationService.getCurrentUserIdComposite()).thenReturn(loggedInUserIdComposite);
            when(authenticationService.hasSecurityRole(ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL)).thenReturn(true);
            when(userSettingsProvider.zoneId()).thenReturn(UTC);

            final BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.hasErrors()).thenReturn(false);

            final Model model = new ConcurrentModel(bindingResult);
            final RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

            sut.updateTimeEntry(timeEntryDto, bindingResult, model, redirectAttributes);

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

            when(timeEntryService.findTimeEntry(1L)).thenReturn(Optional.of(anyTimeEntry(loggedInUserIdComposite)));
            when(authenticationService.getCurrentUserIdComposite()).thenReturn(loggedInUserIdComposite);
            when(userSettingsProvider.zoneId()).thenReturn(UTC);

            when(timeEntryService.updateTimeEntry(any(TimeEntryId.class), anyString(), any(ZonedDateTime.class), any(ZonedDateTime.class), any(Duration.class), anyBoolean()))
                .thenThrow(new TimeEntryUpdateNotPlausibleException("message"));

            final BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.hasErrors()).thenReturn(false);

            final Model model = mock(Model.class);
            final RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

            sut.updateTimeEntry(timeEntryDto, bindingResult, model, redirectAttributes);

            verify(bindingResult).reject("time-entry.validation.plausible");
            verify(bindingResult).rejectValue("start", "");
            verify(bindingResult).rejectValue("end", "");
            verify(bindingResult).rejectValue("duration", "");
            verifyNoMoreInteractions(bindingResult);

            verify(redirectAttributes).addFlashAttribute(BindingResult.MODEL_KEY_PREFIX + "timeEntry", bindingResult);
            verify(redirectAttributes).addFlashAttribute("timeEntry", timeEntryDto);
            verify(redirectAttributes).addFlashAttribute("timeEntryErrorId", 1L);
        }
    }

    private static UserIdComposite anyUserIdComposite() {
        final UserId userId = new UserId("user-id");
        final UserLocalId userLocalId = new UserLocalId(1L);
        return new UserIdComposite(userId, userLocalId);
    }

    private static TimeEntry anyTimeEntry(UserIdComposite userIdComposite) {
        final TimeEntryId timeEntryId = new TimeEntryId(1L);
        final ZonedDateTime start = ZonedDateTime.parse("2025-02-16T09:00:00Z");
        final ZonedDateTime end = ZonedDateTime.parse("2025-02-16T17:00:00Z");
        return new TimeEntry(timeEntryId, userIdComposite, "comment", start, end, false);
    }
}
