package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.slf4j.Logger;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.util.StringUtils.hasText;

@Component
public class TimeEntryViewHelper {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    public static final String TIME_ENTRY_MODEL_NAME = "timeEntry";

    private final TimeEntryService timeEntryService;
    private final TimeEntryLockService timeEntryLockService;
    private final UserSettingsProvider userSettingsProvider;

    public TimeEntryViewHelper(TimeEntryService timeEntryService, TimeEntryLockService timeEntryLockService, UserSettingsProvider userSettingsProvider) {
        this.timeEntryService = timeEntryService;
        this.timeEntryLockService = timeEntryLockService;
        this.userSettingsProvider = userSettingsProvider;
    }

    public void addTimeEntryToModel(Model model, TimeEntryDTO timeEntryDTO) {
        model.addAttribute(TIME_ENTRY_MODEL_NAME, timeEntryDTO);
    }

    public TimeEntryDTO toTimeEntryDto(TimeEntry timeEntry) {

        final ZonedDateTime start = timeEntry.start();
        final ZonedDateTime end = timeEntry.end();

        final LocalDate date = start.toLocalDate();
        final LocalTime startTime = start.toLocalTime();
        final LocalTime endTime = end.toLocalTime();

        final Duration duration = timeEntry.durationInMinutes();
        final String durationString = toTimeEntryDTODurationString(duration);

        return TimeEntryDTO.builder()
            .id(timeEntry.id().value())
            .userLocalId(timeEntry.userIdComposite().localId().value())
            .date(date)
            .start(startTime)
            .end(endTime)
            .duration(durationString)
            .comment(timeEntry.comment())
            .isBreak(timeEntry.isBreak())
            .build();
    }

    /**
     * Handles view related actions to update a {@link TimeEntry} if valid.
     *
     * <p>
     * Editing {@link TimeEntry} is possible on the TimeEntry page or with the TimeEntryDialog.
     *
     * <p>
     * This method validates whether the desired timespan is locked or not. Updating the timeEntry is rejected
     * via bindingResult if timespan is locked and current user is not allowed to bypass it.
     *
     * @param currentUser        currently logged-in user updating the time entry
     * @param dto                time entry information
     * @param bindingResult      {@link BindingResult} containing constraint violations
     * @param model              view model
     * @param redirectAttributes {@link RedirectAttributes}
     * @throws TimeEntryNotFoundException when time entry does not exist
     * @throws AccessDeniedException      when current user is not allowed to edit the time entry
     */
    public void updateTimeEntry(CurrentOidcUser currentUser, TimeEntryDTO dto, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {

        if (dto.getId() == null) {
            throw new IllegalStateException("Expected timeEntry id to have value. Did you meant to create a time entry?");
        }

        final TimeEntryId timeEntryId = new TimeEntryId(dto.getId());

        final TimeEntry timeEntry = timeEntryService.findTimeEntry(timeEntryId)
            .orElseThrow(() -> new TimeEntryNotFoundException(timeEntryId));

        final UserLocalId currentUserLocalId = currentUser.getUserIdComposite().localId();
        final boolean isOwner = timeEntry.userIdComposite().localId().equals(currentUserLocalId);
        final boolean allowedToEdit = currentUser.hasRole(ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL);
        if (!allowedToEdit && !isOwner) {
            throw new AccessDeniedException("Not allowed to edit timeEntry %s.".formatted(timeEntryId));
        }

        final ZoneId zoneId = userSettingsProvider.zoneId();
        final Duration duration = toDuration(dto.getDuration());
        final ZonedDateTime start = dto.getStart() == null ? null : ZonedDateTime.of(LocalDateTime.of(dto.getDate(), dto.getStart()), zoneId);
        final ZonedDateTime end = getEndDate(dto, zoneId);

        final boolean timespanLocked = timeEntryLockService.isTimespanLocked(start, end);
        if (timespanLocked && !timeEntryLockService.isUserAllowedToBypassLock(currentUser.getRoles())) {
            LOG.info("Updating TimeEntry is not allowed since currentUser is not privileged to bypass timespan lock.");
            bindingResult.reject("time-entry.validation.timespan.locked");
        }

        if (bindingResult.hasErrors()) {
            handleCrudTimeEntryErrors(dto, bindingResult, model, redirectAttributes);
            return;
        }

        try {
            timeEntryService.updateTimeEntry(timeEntryId, dto.getComment(), start, end, duration, dto.isBreak());
        } catch (TimeEntryUpdateNotPlausibleException e) {
            LOG.debug("Could not update timeEntry.", e);

            bindingResult.reject("time-entry.validation.plausible");
            bindingResult.rejectValue("start", "");
            bindingResult.rejectValue("end", "");
            bindingResult.rejectValue("duration", "");

            setTimeEntryErrorRedirectAttributes(redirectAttributes, dto, bindingResult);
        }
    }

    /**
     * Adds error handling related information to the view model.
     *
     * @param timeEntryDto       user input for the time entry
     * @param bindingResult      constraint validation errors
     * @param model              view model
     * @param redirectAttributes to set flash attributes for the redirect
     */
    void handleCrudTimeEntryErrors(TimeEntryDTO timeEntryDto, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {

        model.addAttribute("timeEntryErrorId", timeEntryDto.getId());

        final boolean hasErrorStart = bindingResult.hasFieldErrors("start");
        final boolean hasErrorEnd = bindingResult.hasFieldErrors("end");
        final boolean hasErrorDuration = bindingResult.hasFieldErrors("duration");

        if (hasErrorStart && hasErrorEnd && !hasErrorDuration) {
            bindingResult.reject("time-entry.validation.startOrEnd.required");
        } else if (hasErrorStart && !hasErrorEnd && hasErrorDuration) {
            bindingResult.reject("time-entry.validation.startOrDuration.required");
        } else if (!hasErrorStart && hasErrorEnd && hasErrorDuration) {
            bindingResult.reject("time-entry.validation.endOrDuration.required");
        }

        setTimeEntryErrorRedirectAttributes(redirectAttributes, timeEntryDto, bindingResult);
    }

    private void setTimeEntryErrorRedirectAttributes(RedirectAttributes redirectAttributes, TimeEntryDTO timeEntryDto, BindingResult bindingResult) {
        redirectAttributes.addFlashAttribute(BindingResult.MODEL_KEY_PREFIX + TIME_ENTRY_MODEL_NAME, bindingResult);
        redirectAttributes.addFlashAttribute(TIME_ENTRY_MODEL_NAME, timeEntryDto);
        redirectAttributes.addFlashAttribute("timeEntryErrorId", timeEntryDto.getId());
    }

    /**
     * Creates the {@link TimeEntry} if valid. Please ensure authorization yourself, upfront!
     *
     * <p>
     * This method validates whether the desired timespan is locked or not. Creating the timeEntry is rejected
     * via bindingResult if timespan is locked and current user is not allowed to bypass it.
     *
     * @param timeEntryDto user input for the time entry
     */
    void createTimeEntry(TimeEntryDTO timeEntryDto, BindingResult bindingResult, CurrentOidcUser currentUser) {

        if (bindingResult.hasErrors()) {
            // nothing to do here if there are input errors already
            return;
        }

        final ZonedDateTime start;
        final ZonedDateTime end;
        final Duration duration = toDuration(timeEntryDto.getDuration());

        final ZoneId zoneId = userSettingsProvider.zoneId();

        if (duration.equals(Duration.ZERO)) {
            LOG.info("No duration is set by user. Read start and end.");
            start = ZonedDateTime.of(LocalDateTime.of(timeEntryDto.getDate(), timeEntryDto.getStart()), zoneId);
            end = getEndDate(timeEntryDto, zoneId);
        } else if (timeEntryDto.getStart() == null) {
            LOG.info("No start is set by user. Read end and calculate start with given duration.");
            end = ZonedDateTime.of(LocalDateTime.of(timeEntryDto.getDate(), timeEntryDto.getEnd()), zoneId);
            start = end.minusMinutes(duration.toMinutes());
        } else {
            LOG.info("No end is set by user. Read start and calculate end with given duration.");
            start = ZonedDateTime.of(LocalDateTime.of(timeEntryDto.getDate(), timeEntryDto.getStart()), zoneId);
            end = start.plusMinutes(duration.toMinutes());
        }

        final boolean timespanLocked = timeEntryLockService.isTimespanLocked(start, end);
        if (timespanLocked && !timeEntryLockService.isUserAllowedToBypassLock(currentUser.getRoles())) {
            LOG.info("Creating TimeEntry is not allowed since currentUser is not privileged to bypass timespan lock.");
            bindingResult.reject("time-entry.validation.timespan.locked");
        } else {
            final UserLocalId ownerLocalId = new UserLocalId(timeEntryDto.getUserLocalId());
            timeEntryService.createTimeEntry(ownerLocalId, timeEntryDto.getComment(), start, end, timeEntryDto.isBreak());
        }
    }

    private ZonedDateTime getEndDate(TimeEntryDTO dto, ZoneId zoneId) {
        if (dto.getEnd() == null) {
            return null;
        } else if (dto.getStart() == null) {
            return ZonedDateTime.of(LocalDateTime.of(dto.getDate(), dto.getEnd()), zoneId);
        } else if (dto.getEnd().isBefore(dto.getStart())) {
            // end is on next day
            return ZonedDateTime.of(LocalDateTime.of(dto.getDate().plusDays(1), dto.getEnd()), zoneId);
        } else {
            return ZonedDateTime.of(LocalDateTime.of(dto.getDate(), dto.getEnd()), zoneId);
        }
    }

    private static Duration toDuration(String timeEntryDTODurationString) {
        if (hasText(timeEntryDTODurationString)) {
            final String[] split = timeEntryDTODurationString.split(":");
            return Duration.ofHours(Integer.parseInt(split[0])).plusMinutes(Integer.parseInt(split[1]));
        }
        return Duration.ZERO;
    }

    private static String toTimeEntryDTODurationString(Duration duration) {
        if (duration == null) {
            return "00:00";
        }
        return String.format("%02d:%02d", duration.toHours(), duration.toMinutes() % 60);
    }
}
