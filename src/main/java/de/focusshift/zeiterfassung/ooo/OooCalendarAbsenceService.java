package de.focusshift.zeiterfassung.ooo;

import de.focusshift.zeiterfassung.absence.Absence;
import de.focusshift.zeiterfassung.absence.AbsenceColor;
import de.focusshift.zeiterfassung.absence.AbsenceTypeCategory;
import de.focusshift.zeiterfassung.absence.DayLength;
import de.focusshift.zeiterfassung.settings.OooCalendarSettingsService;
import de.focusshift.zeiterfassung.timeentry.TimeEntryLockService;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Attendee;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

@Service
public class OooCalendarAbsenceService {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final OooCalendarSettingsService oooCalendarSettingsService;
    private final TimeEntryLockService timeEntryLockService;
    private final UserManagementService userManagementService;
    private final HttpClient httpClient;

    public OooCalendarAbsenceService(
        OooCalendarSettingsService oooCalendarSettingsService,
        TimeEntryLockService timeEntryLockService,
        UserManagementService userManagementService
    ) {
        this.oooCalendarSettingsService = oooCalendarSettingsService;
        this.timeEntryLockService = timeEntryLockService;
        this.userManagementService = userManagementService;
        this.httpClient = HttpClient.newHttpClient();
    }

    public Map<UserIdComposite, List<Absence>> getAbsencesForAllUsers(LocalDate from, LocalDate toExclusive) {
        return getAbsences(from, toExclusive, userManagementService.findAllUsers());
    }

    public Map<UserIdComposite, List<Absence>> getAbsencesByUserIds(Collection<UserLocalId> userLocalIds, LocalDate from, LocalDate toExclusive) {
        return getAbsences(from, toExclusive, userManagementService.findAllUsersByLocalIds(userLocalIds));
    }

    private Map<UserIdComposite, List<Absence>> getAbsences(LocalDate from, LocalDate toExclusive, List<User> users) {
        if (!oooCalendarSettingsService.getOooCalendarSettings().isConfigured()) {
            return Map.of();
        }

        final LocalDate cutoff = timeEntryLockService.getMinValidTimeEntryDate(ZoneId.systemDefault())
            .orElse(LocalDate.now());
        final LocalDate effectiveFrom = from.isBefore(cutoff) ? cutoff : from;

        if (!effectiveFrom.isBefore(toExclusive)) {
            return Map.of();
        }

        final Map<String, User> usersByMailto = new HashMap<>();
        for (User user : users) {
            usersByMailto.put("mailto:" + user.email().value().toLowerCase(), user);
        }

        final String calendarUrl = oooCalendarSettingsService.getOooCalendarSettings().calendarUrl();

        try {
            final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(calendarUrl))
                .GET()
                .build();

            final HttpResponse<InputStream> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                LOG.warn("Unexpected status {} fetching ooo calendar", response.statusCode());
                return Map.of();
            }

            final CalendarBuilder builder = new CalendarBuilder();
            final Calendar calendar = builder.build(response.body());

            final Map<UserIdComposite, List<Absence>> result = new HashMap<>();

            for (final Object component : calendar.getComponents(Component.VEVENT)) {
                final VEvent event = (VEvent) component;

                if (event.getStartDate() == null) continue;

                final LocalDate eventStart = toLocalDate(event.getStartDate().getDate());
                // iCal DTEND is exclusive for all-day events
                final LocalDate eventEndExclusive = event.getEndDate() != null
                    ? toLocalDate(event.getEndDate().getDate())
                    : eventStart.plusDays(1);

                // clamp start to cutoff; Absence uses inclusive end date
                final LocalDate absenceStart = eventStart.isBefore(effectiveFrom) ? effectiveFrom : eventStart;
                final LocalDate absenceEnd = eventEndExclusive.minusDays(1);

                if (!absenceStart.isBefore(toExclusive) || absenceEnd.isBefore(absenceStart)) continue;

                final String summary = event.getSummary() != null && !event.getSummary().getValue().isBlank()
                    ? event.getSummary().getValue()
                    : "Vacation";

                for (final Object prop : event.getProperties(Property.ATTENDEE)) {
                    final Attendee attendee = (Attendee) prop;
                    final User user = usersByMailto.get(attendee.getValue().toLowerCase());
                    if (user == null) continue;

                    final Absence absence = new Absence(
                        user.userIdComposite().id(),
                        absenceStart.atStartOfDay(ZoneOffset.UTC).toInstant(),
                        absenceEnd.atStartOfDay(ZoneOffset.UTC).toInstant(),
                        DayLength.FULL,
                        locale -> summary,
                        AbsenceColor.YELLOW,
                        AbsenceTypeCategory.HOLIDAY
                    );

                    result.computeIfAbsent(user.userIdComposite(), k -> new ArrayList<>()).add(absence);
                }
            }

            return result;

        } catch (Exception e) {
            LOG.warn("Could not fetch or parse ooo calendar: {}", e.getMessage());
            return Map.of();
        }
    }

    private static LocalDate toLocalDate(java.util.Date date) {
        if (date instanceof net.fortuna.ical4j.model.DateTime) {
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        return LocalDate.parse(new SimpleDateFormat("yyyy-MM-dd").format(date));
    }
}
