package de.focusshift.zeiterfassung.suggestion;

import de.focusshift.zeiterfassung.settings.OooCalendarSettingsService;
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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

@Service
public class CalendarSuggestionService {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final OooCalendarSettingsService oooCalendarSettingsService;
    private final HttpClient httpClient;

    public CalendarSuggestionService(OooCalendarSettingsService oooCalendarSettingsService) {
        this.oooCalendarSettingsService = oooCalendarSettingsService;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Returns suggestions (absence events) for the given user on the given date.
     *
     * <p>Matching is done via the ATTENDEE email address (e.g. {@code mailto:aurindam.jana@slint.dev})
     * which is more reliable than name matching.
     *
     * @param userEmail the user's email address, e.g. "aurindam.jana@slint.dev"
     * @param date      the date to check
     * @return list of suggestions; empty if none, or if the calendar URL is not configured
     */
    public List<CalendarSuggestionDto> getSuggestionsForDate(String userEmail, LocalDate date) {
        if (!oooCalendarSettingsService.getOooCalendarSettings().isConfigured()) {
            return List.of();
        }

        final String calendarUrl = oooCalendarSettingsService.getOooCalendarSettings().calendarUrl();

        final List<CalendarSuggestionDto> suggestions = new ArrayList<>();
        final String mailtoEmail = "mailto:" + userEmail.toLowerCase();

        try {
            final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(calendarUrl))
                .GET()
                .build();

            final HttpResponse<InputStream> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                LOG.warn("Unexpected status {} fetching ooo calendar", response.statusCode());
                return List.of();
            }

            final CalendarBuilder builder = new CalendarBuilder();
            final Calendar calendar = builder.build(response.body());

            for (final Object component : calendar.getComponents(Component.VEVENT)) {
                final VEvent event = (VEvent) component;

                // Match by ATTENDEE email — more reliable than name matching
                final boolean isForUser = event.getProperties(Property.ATTENDEE).stream()
                    .map(p -> (Attendee) p)
                    .anyMatch(a -> mailtoEmail.equalsIgnoreCase(a.getValue()));

                if (!isForUser) continue;

                if (event.getStartDate() == null) continue;
                final LocalDate eventStart = toLocalDate(event.getStartDate().getDate());

                // DTEND is exclusive for all-day events
                final LocalDate eventEnd = event.getEndDate() != null
                    ? toLocalDate(event.getEndDate().getDate())
                    : eventStart.plusDays(1);

                if (!date.isBefore(eventStart) && date.isBefore(eventEnd)) {
                    suggestions.add(new CalendarSuggestionDto(
                        "Vacation",
                        date.toString(),
                        ""
                    ));
                }
            }
        } catch (Exception e) {
            LOG.warn("Could not fetch or parse ooo calendar: {}", e.getMessage());
        }

        return suggestions;
    }

    private static LocalDate toLocalDate(java.util.Date date) {
        if (date instanceof net.fortuna.ical4j.model.DateTime) {
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        // All-day DATE values: use string representation to avoid TZ shifts
        return LocalDate.parse(
            new java.text.SimpleDateFormat("yyyy-MM-dd").format(date)
        );
    }
}
