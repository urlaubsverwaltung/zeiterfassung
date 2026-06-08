package de.focusshift.zeiterfassung.notification;

import de.focusshift.zeiterfassung.email.EMailConstants;
import de.focusshift.zeiterfassung.email.EMailService;
import de.focusshift.zeiterfassung.settings.LockTimeEntriesSettings;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextRunner;
import de.focusshift.zeiterfassung.timeentry.TimeEntryDay;
import de.focusshift.zeiterfassung.timeentry.TimeEntryDayService;
import de.focusshift.zeiterfassung.timeentry.TimeEntryLockService;
import de.focusshift.zeiterfassung.timeentry.TimeEntryWeek;
import de.focusshift.zeiterfassung.user.UserSettings;
import de.focusshift.zeiterfassung.user.UserSettingsService;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.threeten.extra.YearWeek;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

@Service
class NotificationServiceImpl implements NotificationService {

    private static final Logger LOG = getLogger(lookup().lookupClass());
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final TenantContextRunner tenantContextRunner;
    private final UserManagementService userManagementService;
    private final UserSettingsService userSettingsService;
    private final TimeEntryDayService timeEntryDayService;
    private final TimeEntryLockService timeEntryLockService;
    private final EMailService eMailService;
    private final ITemplateEngine mailTemplateEngine;
    private final Clock clock;

    NotificationServiceImpl(
        TenantContextRunner tenantContextRunner,
        UserManagementService userManagementService,
        UserSettingsService userSettingsService,
        TimeEntryDayService timeEntryDayService,
        TimeEntryLockService timeEntryLockService,
        EMailService eMailService,
        ITemplateEngine emailTemplateEngine,
        Clock clock
    ) {
        this.tenantContextRunner = tenantContextRunner;
        this.userManagementService = userManagementService;
        this.userSettingsService = userSettingsService;
        this.timeEntryDayService = timeEntryDayService;
        this.timeEntryLockService = timeEntryLockService;
        this.eMailService = eMailService;
        this.mailTemplateEngine = emailTemplateEngine;
        this.clock = clock;
    }

    @Override
    public void sendLockWarnings() {
        tenantContextRunner.runForEachActiveTenant(this::sendLockWarningsForTenant).run();
    }

    @Override
    public void sendWeeklySummary() {
        tenantContextRunner.runForEachActiveTenant(this::sendWeeklySummaryForTenant).run();
    }

    // ── Lock warnings ──────────────────────────────────────────────────────────

    private void sendLockWarningsForTenant() {
        final LockTimeEntriesSettings lockSettings = timeEntryLockService.getLockTimeEntriesSettings();

        if (!lockSettings.lockingIsActive() || lockSettings.lockTimeEntriesDaysInPast() < 3) {
            LOG.debug("Lock warning skipped: locking inactive or threshold too small ({})", lockSettings.lockTimeEntriesDaysInPast());
            return;
        }

        // The day that is (threshold - 2) days ago will be locked in exactly 2 nights
        final LocalDate today = LocalDate.now(clock);
        final LocalDate warningDate = today.minusDays(lockSettings.lockTimeEntriesDaysInPast() - 2);

        LOG.info("Checking lock warnings for date={}", warningDate);

        for (final User user : userManagementService.findAllUsers()) {
            if (user.email() == null || user.email().value().isBlank()) continue;

            final UserSettings settings = userSettingsService.getUserSettings(user.userIdComposite());
            if (!settings.notificationsEnabled()) continue;

            final List<TimeEntryDay> days = timeEntryDayService.getTimeEntryDays(warningDate, warningDate.plusDays(1), user.userLocalId());
            if (days.isEmpty() || !days.getFirst().overtime().isNegative()) continue;

            LOG.info("Sending lock warning to user={} for date={}", user.userLocalId().value(), warningDate);
            sendLockWarningEmail(user, warningDate, lockSettings.lockTimeEntriesDaysInPast());
        }
    }

    private void sendLockWarningEmail(User user, LocalDate warningDate, int threshold) {
        final String dayName = warningDate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);

        final Context ctx = new Context(EMailConstants.DEFAULT_LOCALE);
        ctx.setVariable("name", user.givenName());
        ctx.setVariable("date", DATE_FORMAT.format(warningDate));
        ctx.setVariable("dayName", dayName);
        ctx.setVariable("daysUntilLock", 2);

        final String body = mailTemplateEngine.process("text/notification-lock-warning", ctx);
        final String subject = "Reminder: No time entries for " + dayName + ", " + DATE_FORMAT.format(warningDate);

        try {
            eMailService.sendMail(user.email().value(), subject, body, "");
        } catch (MessagingException e) {
            LOG.error("Failed to send lock warning to user={}", user.userLocalId().value(), e);
        }
    }

    // ── Weekly summary ─────────────────────────────────────────────────────────

    private void sendWeeklySummaryForTenant() {
        final YearWeek lastWeek = YearWeek.now(clock).minusWeeks(1);
        LOG.info("Sending weekly summary for week={}", lastWeek);

        for (final User user : userManagementService.findAllUsers()) {
            if (user.email() == null || user.email().value().isBlank()) continue;

            final UserSettings settings = userSettingsService.getUserSettings(user.userIdComposite());
            if (!settings.notificationsEnabled()) continue;

            try {
                final TimeEntryWeek week = timeEntryDayService
                    .getEntryWeekPage(user.userLocalId(), lastWeek.getYear(), lastWeek.getWeek())
                    .timeEntryWeek();

                sendWeeklySummaryEmail(user, week, lastWeek);
            } catch (Exception e) {
                LOG.error("Failed to build weekly summary for user={}", user.userLocalId().value(), e);
            }
        }
    }

    private void sendWeeklySummaryEmail(User user, TimeEntryWeek week, YearWeek yearWeek) {
        final LocalDate monday = week.firstDateOfWeek();
        final LocalDate sunday = week.lastDateOfWeek();

        final String worked = formatDuration(week.workDuration().durationInMinutes().toMinutes());
        final String target = formatDuration(week.shouldWorkingHours().durationInMinutes().toMinutes());
        final long overtimeMinutes = week.overtime().toMinutes();
        final boolean negative = overtimeMinutes < 0;
        final String delta = (negative ? "−" : "+") + formatDuration(Math.abs(overtimeMinutes));

        // Days Mon-Fri with entries vs without
        final List<String> missingDays = week.days().stream()
            .filter(d -> d.date().getDayOfWeek() != DayOfWeek.SATURDAY && d.date().getDayOfWeek() != DayOfWeek.SUNDAY)
            .filter(d -> d.timeEntries().stream().noneMatch(e -> !e.isBreak()))
            .filter(d -> !d.shouldWorkingHours().durationInMinutes().isZero()) // skip days with no target
            .map(d -> d.date().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + DATE_FORMAT.format(d.date()))
            .toList();

        final Context ctx = new Context(EMailConstants.DEFAULT_LOCALE);
        ctx.setVariable("name", user.givenName());
        ctx.setVariable("weekNumber", yearWeek.getWeek());
        ctx.setVariable("from", DATE_FORMAT.format(monday));
        ctx.setVariable("to", DATE_FORMAT.format(sunday));
        ctx.setVariable("worked", worked);
        ctx.setVariable("target", target);
        ctx.setVariable("delta", delta);
        ctx.setVariable("deltaNegative", negative);
        ctx.setVariable("missingDays", missingDays);
        ctx.setVariable("allDaysCovered", missingDays.isEmpty());

        final String body = mailTemplateEngine.process("text/notification-weekly-summary", ctx);
        final String subject = "Time tracking: Week " + yearWeek.getWeek() + " summary (" + DATE_FORMAT.format(monday) + " – " + DATE_FORMAT.format(sunday) + ")";

        try {
            eMailService.sendMail(user.email().value(), subject, body, "");
        } catch (MessagingException e) {
            LOG.error("Failed to send weekly summary to user={}", user.userLocalId().value(), e);
        }
    }

    private static String formatDuration(long totalMinutes) {
        final long hours = Math.abs(totalMinutes) / 60;
        final long minutes = Math.abs(totalMinutes) % 60;
        return "%02d:%02d".formatted(hours, minutes);
    }
}
