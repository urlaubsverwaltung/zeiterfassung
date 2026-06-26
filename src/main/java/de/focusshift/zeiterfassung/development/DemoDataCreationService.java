package de.focusshift.zeiterfassung.development;

import de.focusshift.zeiterfassung.absence.AbsenceColor;
import de.focusshift.zeiterfassung.absence.AbsenceTypeCategory;
import de.focusshift.zeiterfassung.absence.AbsenceTypeService;
import de.focusshift.zeiterfassung.absence.AbsenceTypeSourceId;
import de.focusshift.zeiterfassung.absence.AbsenceTypeUpdate;
import de.focusshift.zeiterfassung.absence.AbsenceWrite;
import de.focusshift.zeiterfassung.absence.AbsenceWriteService;
import de.focusshift.zeiterfassung.absence.DayLength;
import de.focusshift.zeiterfassung.tenancy.user.TenantUser;
import de.focusshift.zeiterfassung.tenancy.user.TenantUserCreatedEvent;
import de.focusshift.zeiterfassung.timeentry.TimeEntryService;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.invoke.MethodHandles.lookup;
import static java.time.ZoneOffset.UTC;

class DemoDataCreationService {

    private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    private static final LocalTime END_OF_WORK_DAY = LocalTime.of(17, 0);

    // demo "Urlaub" absence type that the created HOLIDAY absences refer to
    private static final long DEMO_HOLIDAY_TYPE_SOURCE_ID = 1L;

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
    private final AbsenceWriteService absenceWriteService;
    private final AbsenceTypeService absenceTypeService;
    private final DemoDataProperties demoDataProperties;

    DemoDataCreationService(TimeEntryService timeEntryService, AbsenceWriteService absenceWriteService,
                            AbsenceTypeService absenceTypeService, DemoDataProperties demoDataProperties) {
        this.timeEntryService = timeEntryService;
        this.absenceWriteService = absenceWriteService;
        this.absenceTypeService = absenceTypeService;
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

        createDemoAbsences(user, now);
    }

    /**
     * Creates a few demo absences for the given user. The set contains absences that overlap a month boundary
     * (both into the previous and into the next month) as well as absences fully contained within a single month.
     * Which of the month-overlapping absences a user gets depends on the user's localId, so that different users
     * have different absences and the effect of one user's absence on a multi-person report can be observed.
     */
    private void createDemoAbsences(TenantUser user, LocalDate now) {

        // make sure the "Urlaub" absence type the HOLIDAY absences refer to exists
        absenceTypeService.updateAbsenceType(new AbsenceTypeUpdate(
            DEMO_HOLIDAY_TYPE_SOURCE_ID,
            AbsenceTypeCategory.HOLIDAY,
            AbsenceColor.BLUE,
            Map.of(Locale.GERMAN, "Urlaub", Locale.ENGLISH, "Holiday")
        ));

        final UserId userId = new UserId(user.id());
        final long localId = user.localId();

        final LocalDate firstOfThisMonth = now.withDayOfMonth(1);
        final LocalDate firstOfNextMonth = firstOfThisMonth.plusMonths(1);

        // unique, deterministic sourceIds per user so absences are not deduplicated across users/restarts
        long sourceId = localId * 1000;

        // 1) month overlap: previous -> current month (only every second user)
        if (localId % 2 == 0) {
            addHolidayAbsence(userId, sourceId++, firstOfThisMonth.minusDays(3), firstOfThisMonth.plusDays(2), DayLength.FULL);
        }

        // 2) month overlap: current -> next month (the other users)
        if (localId % 2 == 1) {
            addHolidayAbsence(userId, sourceId++, firstOfNextMonth.minusDays(2), firstOfNextMonth.plusDays(3), DayLength.FULL);
        }

        // 3) fully within the current month, no overlap
        addHolidayAbsence(userId, sourceId++, firstOfThisMonth.plusDays(9), firstOfThisMonth.plusDays(11), DayLength.FULL);

        // 4) half-day holiday (morning) within the current month, for some day-length variety
        addHolidayAbsence(userId, sourceId++, firstOfThisMonth.plusDays(21), firstOfThisMonth.plusDays(21), DayLength.MORNING);

        // 5) sick leave within the current month, for some absence-type variety.
        // start day and duration vary per user so that the sick leave is spread across the month.
        final int sickStartOffset = (int) (localId * 7 % 24);    // day 0..23 within the month
        final int sickDurationDays = (int) (localId % 4);        // 0..3 additional days -> 1..4 days total
        final LocalDate sickStart = firstOfThisMonth.plusDays(sickStartOffset);
        addSickAbsence(userId, sourceId, sickStart, sickStart.plusDays(sickDurationDays));
    }

    private void addHolidayAbsence(UserId userId, long sourceId, LocalDate start, LocalDate endInclusive, DayLength dayLength) {
        absenceWriteService.addAbsence(new AbsenceWrite(
            sourceId,
            userId,
            toInstant(start),
            toInstant(endInclusive),
            dayLength,
            null,
            AbsenceTypeCategory.HOLIDAY,
            new AbsenceTypeSourceId(DEMO_HOLIDAY_TYPE_SOURCE_ID)
        ));
    }

    private void addSickAbsence(UserId userId, long sourceId, LocalDate start, LocalDate endInclusive) {
        absenceWriteService.addAbsence(new AbsenceWrite(
            sourceId,
            userId,
            toInstant(start),
            toInstant(endInclusive),
            DayLength.FULL,
            null,
            AbsenceTypeCategory.SICK
        ));
    }

    private static Instant toInstant(LocalDate date) {
        return date.atStartOfDay().toInstant(UTC);
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
