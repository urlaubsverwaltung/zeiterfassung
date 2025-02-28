package de.focusshift.zeiterfassung.development;

import de.focusshift.zeiterfassung.tenancy.user.TenantUser;
import de.focusshift.zeiterfassung.tenancy.user.TenantUserCreatedEvent;
import de.focusshift.zeiterfassung.timeentry.TimeEntryService;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.invoke.MethodHandles.lookup;

class DemoDataCreationService {

    private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    private static final LocalTime END_OF_WORK_DAY = LocalTime.of(17, 0);

    private static final String[] comments = {
        "Telefonat mit Kunden zur Livegang \uD83D\uDE80",
        "Meeting zur Digitalisierung mit der Geschäftsführung",
        "Austausch zur Strategie für kommendes Jahr",
        "Meetingvorbereitung",
        "Rechnungsausgang",
        "Buchhaltung",
        "Rechnungsabschluss",
        "Kalkulation zum Bauvertrag",
        "Gassi gehen \uD83D\uDC15",
        "Blumengießen \uD83C\uDF95",
        "Mittagessen mit Freunden \uD83C\uDF5C",
        "Kunden-Workshop",
        "Jourfix im Team",
        "Ablösung der Zeiterfassung mit urlaubsverwaltung.cloud",
        "Bericht vorbereiten",
        "Präsentation vorbereiten",
        "Austausch mit Marketing-Team",
        "Finanzplanung",
        "\u2615\uD83D\uDC96\uD83D\uDE0D"
    };
    private static final Random RANDOM = new Random();

    private final TimeEntryService timeEntryService;
    private final DemoDataProperties demoDataProperties;

    DemoDataCreationService(TimeEntryService timeEntryService, DemoDataProperties demoDataProperties) {
        this.timeEntryService = timeEntryService;
        this.demoDataProperties = demoDataProperties;
    }

    @EventListener
    public void on(TenantUserCreatedEvent event) {
        final TenantUser user = event.tenantUser();
        LOG.info("Creating time entries for new user {}", user);
        final LocalDate now = LocalDate.now();

        final LocalDate startDate = now.minus(demoDataProperties.getPast());
        final LocalDate endDate = now.plus(demoDataProperties.getFuture().plusDays(1));

        startDate.datesUntil(endDate).forEach(actualDate -> {
            LocalTime startTime = LocalTime.of(8, 0);
            LocalTime endTime = randomTimeBetween(startTime, END_OF_WORK_DAY);

            while (endTime.isBefore(END_OF_WORK_DAY)) {

                final UserLocalId userLocalId = new UserLocalId(user.localId());
                final ZoneId zoneId = ZoneId.of("Europe/Berlin");
                final ZonedDateTime start = ZonedDateTime.of(LocalDateTime.of(actualDate, startTime), zoneId);
                final ZonedDateTime end = ZonedDateTime.of(LocalDateTime.of(actualDate, endTime), zoneId);

                timeEntryService.createTimeEntry(userLocalId, getRandomComment(), start, end, false);

                startTime = endTime;
                endTime = randomTimeBetween(startTime, END_OF_WORK_DAY).plusMinutes(30);
            }
        });
    }

    private static String getRandomComment() {
        return comments[RANDOM.nextInt(comments.length)];
    }

    private static LocalTime randomTimeBetween(LocalTime startTime, LocalTime endTime) {
        int startSeconds = startTime.toSecondOfDay();
        int endSeconds = endTime.toSecondOfDay();
        int randomTime = ThreadLocalRandom
            .current()
            .nextInt(startSeconds, endSeconds);

        return LocalTime.ofSecondOfDay(randomTime);
    }
}
