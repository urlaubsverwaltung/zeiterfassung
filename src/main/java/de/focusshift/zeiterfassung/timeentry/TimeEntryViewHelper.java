package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.security.AuthenticationFacade;
import de.focusshift.zeiterfassung.user.UserId;
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
    private final UserSettingsProvider userSettingsProvider;
    private final AuthenticationFacade authenticationFacade;

    public TimeEntryViewHelper(TimeEntryService timeEntryService, UserSettingsProvider userSettingsProvider,
                               AuthenticationFacade authenticationFacade) {
        this.timeEntryService = timeEntryService;
        this.userSettingsProvider = userSettingsProvider;
        this.authenticationFacade = authenticationFacade;
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
            .date(date)
            .start(startTime)
            .end(endTime)
            .duration(durationString)
            .comment(timeEntry.comment())
            .isBreak(timeEntry.isBreak())
            .build();
    }

    /**
     * Handles view related actions to create a {@link TimeEntry}.
     *
     * <p>
     * Creating a {@link TimeEntry} is only possible on the TimeEntry page.
     *
     * @param dto time entry information
     * @param bindingResult {@link BindingResult} containing constraint violations
     * @param model view model
     * @param redirectAttributes {@link RedirectAttributes}
     */
    public void createTimeEntry(TimeEntryDTO dto, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            handleCrudTimeEntryErrors(dto, bindingResult, model, redirectAttributes);
            return;
        }

        final UserLocalId userLocalId = authenticationFacade.getCurrentUserIdComposite().localId();
        final ZoneId zoneId = userSettingsProvider.zoneId();

        if (dto.getId() == null) {
            createTimeEntry(dto, userLocalId, zoneId);
        } else {
            throw new IllegalStateException("Expected timeEntry id not to be defined but has value. Did you meant to update the time entry?");
        }
    }

    /**
     * Handles view related actions to update a {@link TimeEntry}.
     *
     * <p>
     * Editing {@link TimeEntry} is possible on the TimeEntry page or with the TimeEntryDialog.
     *
     * @param dto time entry information
     * @param bindingResult {@link BindingResult} containing constraint violations
     * @param model view model
     * @param redirectAttributes {@link RedirectAttributes}
     * @throws TimeEntryNotFoundException when time entry does not exist
     * @throws AccessDeniedException when current user is not allowed to edit the time entry
     */
    public void updateTimeEntry(TimeEntryDTO dto, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {

        if (dto.getId() == null) {
            throw new IllegalStateException("Expected timeEntry id to have value. Did you meant to create a time entry?");
        }

        final TimeEntryId timeEntryId = new TimeEntryId(dto.getId());

        final TimeEntry timeEntry = timeEntryService.findTimeEntry(timeEntryId)
            .orElseThrow(() -> new TimeEntryNotFoundException(timeEntryId));

        final UserId currentUserId = authenticationFacade.getCurrentUserIdComposite().id();
        final boolean idOwner = timeEntry.userIdComposite().id().equals(currentUserId);
        final boolean allowedToEdit = authenticationFacade.hasSecurityRole(ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL);

        if (!allowedToEdit && !idOwner) {
            throw new AccessDeniedException("Not allowed to edit time entry with %s.".formatted(timeEntryId));
        }

        if (bindingResult.hasErrors()) {
            handleCrudTimeEntryErrors(dto, bindingResult, model, redirectAttributes);
            return;
        }

        final ZoneId zoneId = userSettingsProvider.zoneId();
        final Duration duration = toDuration(dto.getDuration());
        final ZonedDateTime start = dto.getStart() == null ? null : ZonedDateTime.of(LocalDateTime.of(dto.getDate(), dto.getStart()), zoneId);
        final ZonedDateTime end = getEndDate(dto, zoneId);

        try {
            timeEntryService.updateTimeEntry(timeEntryId, dto.getComment(), start, end, duration, dto.isBreak());
        } catch (TimeEntryUpdateNotPlausibleException e) {
            LOG.debug("could not update time-entry", e);

            bindingResult.reject("time-entry.validation.plausible");
            bindingResult.rejectValue("start", "");
            bindingResult.rejectValue("end", "");
            bindingResult.rejectValue("duration", "");

            setTimeEntryErrorRedirectAttributes(redirectAttributes, dto, bindingResult);
        }
    }

    private void handleCrudTimeEntryErrors(TimeEntryDTO timeEntryDto, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {

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

    private void setTimeEntryErrorRedirectAttributes(RedirectAttributes redirectAttributes, TimeEntryDTO timeEntryDto, BindingResult  bindingResult) {
        redirectAttributes.addFlashAttribute(BindingResult.MODEL_KEY_PREFIX + TIME_ENTRY_MODEL_NAME, bindingResult);
        redirectAttributes.addFlashAttribute(TIME_ENTRY_MODEL_NAME, timeEntryDto);
        redirectAttributes.addFlashAttribute("timeEntryErrorId", timeEntryDto.getId());
    }

    private void createTimeEntry(TimeEntryDTO dto, UserLocalId userLocalId, ZoneId zoneId) {

        final ZonedDateTime start;
        final ZonedDateTime end;

        final Duration duration = toDuration(dto.getDuration());

        if (duration.equals(Duration.ZERO)) {
            // start and end should be given
            start = ZonedDateTime.of(LocalDateTime.of(dto.getDate(), dto.getStart()), zoneId);
            end = getEndDate(dto, zoneId);
        } else if (dto.getStart() == null) {
            // end and value should be given
            end = ZonedDateTime.of(LocalDateTime.of(dto.getDate(), dto.getEnd()), zoneId);
            start = end.minusMinutes(duration.toMinutes());
        } else {
            // start and value should be given
            start = ZonedDateTime.of(LocalDateTime.of(dto.getDate(), dto.getStart()), zoneId);
            end = start.plusMinutes(duration.toMinutes());
        }

        timeEntryService.createTimeEntry(userLocalId, dto.getComment(), start, end, dto.isBreak());
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
