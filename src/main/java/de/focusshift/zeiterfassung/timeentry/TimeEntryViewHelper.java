package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import org.slf4j.Logger;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.util.StringUtils.hasText;

@Component
class TimeEntryViewHelper {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final TimeEntryService timeEntryService;
    private final UserSettingsProvider userSettingsProvider;

    TimeEntryViewHelper(TimeEntryService timeEntryService, UserSettingsProvider userSettingsProvider) {
        this.timeEntryService = timeEntryService;
        this.userSettingsProvider = userSettingsProvider;
    }

    public void addTimeEntryToModel(Model model, TimeEntryDTO timeEntryDTO) {
        model.addAttribute("timeEntry", timeEntryDTO);
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

    public void saveTimeEntry(TimeEntryDTO dto, BindingResult bindingResult, Model model, OidcUser principal) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("timeEntryErrorId", dto.getId());

            final boolean hasErrorStart = bindingResult.hasFieldErrors("start");
            final boolean hasErrorEnd = bindingResult.hasFieldErrors("end");
            final boolean hasErrorDuration = bindingResult.hasFieldErrors("value");

            if (hasErrorStart && hasErrorEnd && !hasErrorDuration) {
                bindingResult.reject("time-entry.validation.startOrEnd.required");
            } else if (hasErrorStart && !hasErrorEnd && hasErrorDuration) {
                bindingResult.reject("time-entry.validation.startOrDuration.required");
            } else if (!hasErrorStart && hasErrorEnd && hasErrorDuration) {
                bindingResult.reject("time-entry.validation.endOrDuration.required");
            }

            return;
        }

        final UserId userId = new UserId(principal.getUserInfo().getSubject());
        final ZoneId zoneId = userSettingsProvider.zoneId();

        if (dto.getId() == null) {
            createTimeEntry(dto, userId, zoneId);
            return;
        }

        try {
            updateTimeEntry(dto, zoneId);
        } catch (TimeEntryUpdateNotPlausibleException e) {
            LOG.debug("could not update time-entry", e);

            bindingResult.reject("time-entry.validation.plausible");
            bindingResult.rejectValue("start", "");
            bindingResult.rejectValue("end", "");
            bindingResult.rejectValue("duration", "");

            model.addAttribute("timeEntryErrorId", dto.getId());
        }
    }

    private void createTimeEntry(TimeEntryDTO dto, UserId userId, ZoneId zoneId) {

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

        timeEntryService.createTimeEntry(userId, dto.getComment(), start, end, dto.isBreak());
    }

    private void updateTimeEntry(TimeEntryDTO dto, ZoneId zoneId) throws TimeEntryUpdateNotPlausibleException {

        final Duration duration = toDuration(dto.getDuration());
        final ZonedDateTime start = dto.getStart() == null ? null : ZonedDateTime.of(LocalDateTime.of(dto.getDate(), dto.getStart()), zoneId);
        final ZonedDateTime end = getEndDate(dto, zoneId);

        timeEntryService.updateTimeEntry(new TimeEntryId(dto.getId()), dto.getComment(), start, end, duration, dto.isBreak());
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
