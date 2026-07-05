package de.focusshift.zeiterfassung.overtime;

import de.focusshift.zeiterfassung.DateRange;
import de.focusshift.zeiterfassung.absence.AbsenceAddedEvent;
import de.focusshift.zeiterfassung.absence.AbsenceDeletedEvent;
import de.focusshift.zeiterfassung.absence.AbsenceUpdatedEvent;
import de.focusshift.zeiterfassung.overtime.events.UserHasWorkedOvertimeEvent;
import de.focusshift.zeiterfassung.publicholiday.FederalState;
import de.focusshift.zeiterfassung.publicholiday.PublicHolidayCalendar;
import de.focusshift.zeiterfassung.publicholiday.PublicHolidaysService;
import de.focusshift.zeiterfassung.settings.FederalStateSettingsUpdatedEvent;
import de.focusshift.zeiterfassung.settings.LockTimeEntriesSettings;
import de.focusshift.zeiterfassung.timeentry.TimeEntryLockService;
import de.focusshift.zeiterfassung.timeentry.events.DayLockedEvent;
import de.focusshift.zeiterfassung.timeentry.events.TimeEntryCreatedEvent;
import de.focusshift.zeiterfassung.timeentry.events.TimeEntryDeletedEvent;
import de.focusshift.zeiterfassung.timeentry.events.TimeEntryUpdatedEvent;
import de.focusshift.zeiterfassung.timeentry.events.TimeEntryUpdatedEvent.UpdatedValueCandidate;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.OvertimeAccount;
import de.focusshift.zeiterfassung.usermanagement.OvertimeAccountService;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import de.focusshift.zeiterfassung.workduration.WorkDuration;
import de.focusshift.zeiterfassung.workingtime.WorkingTime;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeCalendar;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeCalendarService;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeCreatedEvent;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeDeletedEvent;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeService;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeUpdatedEvent;
import de.focusshift.zeiterfassung.workingtime.WorksOnPublicHoliday;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class OvertimePublisher {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    /**
     * Lower bound for resyncing overtime after a global federal state change. Working time periods with a concrete
     * {@code validFrom} are bounded by that date; the very first (legacy default) working time has no {@code validFrom}
     * and would otherwise reach infinitely into the past, so it is floored here. Since only public holidays are
     * republished this bound can be generous.
     */
    private static final LocalDate FEDERAL_STATE_RESYNC_FLOOR = LocalDate.of(2015, 1, 1);

    private final OvertimeService overtimeService;
    private final OvertimeAccountService overtimeAccountService;
    private final TimeEntryLockService timeEntryLockService;
    private final UserManagementService userManagementService;
    private final WorkingTimeCalendarService workingTimeCalendarService;
    private final WorkingTimeService workingTimeService;
    private final PublicHolidaysService publicHolidaysService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final Clock clock;

    OvertimePublisher(
        OvertimeService overtimeService,
        OvertimeAccountService overtimeAccountService,
        TimeEntryLockService timeEntryLockService,
        UserManagementService userManagementService,
        WorkingTimeCalendarService workingTimeCalendarService,
        WorkingTimeService workingTimeService,
        PublicHolidaysService publicHolidaysService,
        ApplicationEventPublisher applicationEventPublisher,
        Clock clock
    ) {
        this.overtimeService = overtimeService;
        this.overtimeAccountService = overtimeAccountService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.timeEntryLockService = timeEntryLockService;
        this.userManagementService = userManagementService;
        this.workingTimeCalendarService = workingTimeCalendarService;
        this.workingTimeService = workingTimeService;
        this.publicHolidaysService = publicHolidaysService;
        this.clock = clock;
    }

    @EventListener
    public void publishOvertime(DayLockedEvent event) {
        LOG.info("Handling DayLockedEvent for date={} and zoneId={}. Calculating overtime and publish application event.", event.date(), event.zoneId());

        final Map<UserIdComposite, OvertimeAccount> overtimeAccountByUserId = overtimeAccountService.getAllOvertimeAccounts();

        overtimeService.getOvertimeForDate(event.date()).forEach((userIdComposite, overtimeHours) -> {
            // currently we just know whether overtime is allowed or not.
            // this may change in the future to have a date range for this info or more granular stuff.
            final OvertimeAccount overtimeAccount = overtimeAccountByUserId.get(userIdComposite);
            if (overtimeAccount.isAllowed()) {
                publishUpdated(userIdComposite, event.date(), overtimeHours);
            } else {
                LOG.info("Overtime not allowed, ignore DayLockedEvent for User {}", userIdComposite);
            }
        });
    }

    @EventListener
    public void publishOvertimeUpdated(TimeEntryCreatedEvent event) {

        if (!event.locked()) {
            LOG.debug("Ignore not locked TimeEntryCreatedEvent.");
            return;
        }

        final UserIdComposite userIdComposite = event.ownerUserIdComposite();
        final UserLocalId userLocalId = userIdComposite.localId();
        final OvertimeAccount overtimeAccount = overtimeAccountService.getOvertimeAccount(userLocalId);
        if (!overtimeAccount.isAllowed()) {
            LOG.info("Ignore TimeEntryCreatedEvent. User {} overtime not allowed.", userIdComposite);
            return;
        }
        LOG.info("TimeEntry created and locked.");
        publishUpdated(userIdComposite, event.date());
    }

    @EventListener
    public void publishOvertimeUpdated(TimeEntryUpdatedEvent event) {
        // attributes to consider
        // - locked
        //     not locked timeEntries must not be published since they will be handled by the scheduler in the future
        // - date
        //     an updated date results in updated overtime hours for the previous and the new date! check for locked for the date!
        // - workDuration
        //     at the same locked date, overtime hours can only change when workDuration changed
        //     (e.g. the comment could be updated by someone. however, that doesn't change the overtime hours)
        //
        //
        // | prev locked | current locked | lock changed | date changed | workDuration changed | publish previous date | publish current date |
        // | ----------- | -------------- | ------------ | ------------ | -------------------- | --------------------- | -------------------- |
        // | no          | no             | no           | -            | -                    | -                     | -                    |
        // | no          | yes            | yes          | yes          | -                    | -                     | yes                  |
        // | yes         | no             | yes          | yes          | -                    | yes                   | -                    |
        // | yes         | yes            | no           | yes          | -                    | yes                   | yes                  |
        // | yes         | yes            | no           | no           | no                   | -                     | -                    |
        // | yes         | yes            | no           | no           | yes                  | -                     | yes                  |
        //
        final UpdatedValueCandidate<Boolean> lockedCandidate = event.lockedCandidate();
        final UpdatedValueCandidate<LocalDate> dateCandidate = event.dateCandidate();
        final UpdatedValueCandidate<WorkDuration> workDurationCandidate = event.workDurationCandidate();

        final boolean preLocked = lockedCandidate.previous();
        final boolean curLocked = lockedCandidate.current();
        final boolean dateChanged = dateCandidate.hasChanged();
        final boolean workDurationChanged = workDurationCandidate.hasChanged();

        if (!preLocked && !curLocked) {
            LOG.debug("Ignore not locked TimeEntryUpdate.");
            return;
        }

        final UserIdComposite userIdComposite = event.ownerUserIdComposite();
        final UserLocalId userLocalId = userIdComposite.localId();
        final OvertimeAccount overtimeAccount = overtimeAccountService.getOvertimeAccount(userLocalId);
        if (!overtimeAccount.isAllowed()) {
            LOG.info("Ignore TimeEntryUpdatedEvent. User {} overtime not allowed.", userIdComposite);
            return;
        }

        if (preLocked && dateChanged) {
            LOG.info("TimeEntry date changed and was previously locked.");
            publishUpdated(userIdComposite, dateCandidate.previous());
        }

        if (!preLocked && dateChanged) {
            LOG.info("TimeEntry date changed and is now locked.");
            publishUpdated(userIdComposite, dateCandidate.current());
        } else if (curLocked) {
            if (dateChanged) {
                LOG.info("TimeEntry date changed and is still locked.");
                publishUpdated(userIdComposite, dateCandidate.current());
            } else if (workDurationChanged) {
                LOG.info("TimeEntry workDuration changed and it still locked.");
                publishUpdated(userIdComposite, dateCandidate.current());
            }
        }
    }

    @EventListener
    public void publishOvertimeUpdated(TimeEntryDeletedEvent event) {

        final boolean locked = event.locked();
        final LocalDate date = event.date();
        if (!locked) {
            LOG.info("Ignore not locked TimeEntryDeleteEvent.");
            return;
        }

        final UserIdComposite userIdComposite = event.ownerUserIdComposite();
        final UserLocalId userLocalId = userIdComposite.localId();

        final OvertimeAccount overtimeAccount = overtimeAccountService.getOvertimeAccount(userLocalId);
        // currently we do not have time spans for overtime-allowed/not-allowed
        // otherwise we would have to check the date here!
        if (!overtimeAccount.isAllowed()) {
            LOG.info("Ignore TimeEntryDeletedEvent. User {} overtime not allowed.", userIdComposite);
            return;
        }

        publishUpdated(userIdComposite, date);
    }

    @EventListener
    public void publishOvertimeUpdated(AbsenceAddedEvent event) {
        publishForUserAndDateRange(event.userId(), event.dateRange());
    }

    @EventListener
    public void publishOvertimeUpdated(AbsenceDeletedEvent event) {
        publishForUserAndDateRange(event.userId(), event.dateRange());
    }

    @EventListener
    public void publishOvertimeUpdated(AbsenceUpdatedEvent event) {
        publishForUserAndDateRange(event.userId(), event.oldDateRange());
        publishForUserAndDateRange(event.userId(), event.newDateRange());
    }

    @EventListener
    public void publishOvertimeUpdated(WorkingTimeCreatedEvent event) {
        publishOvertimeForWorkingTimeChange(event.userIdComposite(), event.validFrom());
    }

    @EventListener
    public void publishOvertimeUpdated(WorkingTimeUpdatedEvent event) {
        // validFrom may have been moved into the past or into the future. Either way the whole span between the
        // previous and the current validFrom (and everything up to the last locked day) is affected, because the
        // planned working hours of those days changed.
        final LocalDate affectedValidFrom = earliestNonNull(event.previousValidFrom(), event.validFrom());
        publishOvertimeForWorkingTimeChange(event.userIdComposite(), affectedValidFrom);
    }

    @EventListener
    public void publishOvertimeUpdated(WorkingTimeDeletedEvent event) {
        // after deletion the preceding working time extends over the deleted range, so its planned working hours change.
        publishOvertimeForWorkingTimeChange(event.userIdComposite(), event.validFrom());
    }

    /**
     * Recalculate and publish overtime for the locked days that are affected by a change of the global
     * {@link de.focusshift.zeiterfassung.settings.FederalStateSettings federal state settings}.
     *
     * <p>
     * Changing the global federal state (or {@code worksOnPublicHoliday}) changes which days are public holidays for
     * every person whose {@link WorkingTime} inherits the global setting (individual federal state {@code GLOBAL} or
     * {@code worksOnPublicHoliday} {@code GLOBAL}). Only such persons and only the days that are public holidays under
     * the old or the new federal state are affected, so we recompute exactly those (already locked) days. Not-yet-locked
     * days are (re)calculated by the scheduler once they get locked.
     */
    @EventListener
    public void publishOvertimeUpdated(FederalStateSettingsUpdatedEvent event) {

        final Optional<LocalDate> maybeLastLockedDate = getLastLockedDate();
        if (maybeLastLockedDate.isEmpty()) {
            LOG.debug("Locking is disabled, no locked overtime to resync for federal state settings change.");
            return;
        }

        final LocalDate lastLockedDate = maybeLastLockedDate.get();
        final LocalDate from = FEDERAL_STATE_RESYNC_FLOOR;
        if (from.isAfter(lastLockedDate)) {
            LOG.debug("Federal state settings change does not reach into the locked period (lastLockedDate={}). Nothing to resync.", lastLockedDate);
            return;
        }

        final LocalDate toExclusive = lastLockedDate.plusDays(1);

        final Set<LocalDate> affectedHolidays = affectedPublicHolidays(event, from, toExclusive);
        if (affectedHolidays.isEmpty()) {
            LOG.info("Federal state settings changed but no public holidays are affected in [{} - {}]. Nothing to resync.", from, lastLockedDate);
            return;
        }

        final Map<UserIdComposite, List<WorkingTime>> workingTimesByUser = workingTimeService.getAllWorkingTimes(from, toExclusive);
        final Map<UserIdComposite, OvertimeAccount> overtimeAccountByUser = overtimeAccountService.getAllOvertimeAccounts();
        final LockTimeEntriesSettings lockSettings = timeEntryLockService.getLockTimeEntriesSettings();

        LOG.info("Federal state settings changed. Resync overtime for {} affected public holiday(s) in [{} - {}].", affectedHolidays.size(), from, lastLockedDate);

        workingTimesByUser.forEach((userIdComposite, workingTimes) -> {

            final OvertimeAccount overtimeAccount = overtimeAccountByUser.get(userIdComposite);
            if (overtimeAccount == null || !overtimeAccount.isAllowed()) {
                LOG.debug("Skip federal state resync for user={} because overtime is not allowed.", userIdComposite);
                return;
            }

            for (WorkingTime workingTime : workingTimes) {
                if (!inheritsGlobalFederalStateSettings(workingTime)) {
                    // this working time has its own federal state / worksOnPublicHoliday - not affected by the global change
                    continue;
                }

                final LocalDate effStart = maxDate(workingTime.validFrom().orElse(from), from);
                final LocalDate effEnd = minDate(workingTime.validTo().orElse(lastLockedDate), lastLockedDate);
                if (effStart.isAfter(effEnd)) {
                    continue;
                }

                for (LocalDate date : affectedHolidays) {
                    final boolean withinPeriod = !date.isBefore(effStart) && !date.isAfter(effEnd);
                    if (withinPeriod && timeEntryLockService.isLocked(date, lockSettings)) {
                        publishUpdated(userIdComposite, date);
                    }
                }
            }
        });
    }

    private Set<LocalDate> affectedPublicHolidays(FederalStateSettingsUpdatedEvent event, LocalDate from, LocalDate toExclusive) {

        final Set<FederalState> federalStates = new HashSet<>();
        federalStates.add(event.oldFederalState());
        federalStates.add(event.newFederalState());

        final Map<FederalState, PublicHolidayCalendar> calendars =
            publicHolidaysService.getPublicHolidays(from, toExclusive, federalStates);

        final Set<LocalDate> dates = new HashSet<>();
        for (PublicHolidayCalendar calendar : calendars.values()) {
            dates.addAll(calendar.publicHolidays().keySet());
        }
        return dates;
    }

    private static boolean inheritsGlobalFederalStateSettings(WorkingTime workingTime) {
        return FederalState.GLOBAL.equals(workingTime.individualFederalState())
            || WorksOnPublicHoliday.GLOBAL.equals(workingTime.individualWorksOnPublicHoliday());
    }

    private static LocalDate maxDate(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalDate minDate(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }

    /**
     * Recalculate and publish overtime for the locked days that are affected by a working time change.
     *
     * <p>
     * A working time change alters the {@link de.focusshift.zeiterfassung.workingtime.PlannedWorkingHours planned
     * working hours} from {@code validFrom} onwards. Only days that are already locked ("festgeschrieben") need to be
     * synced here, since not-yet-locked days are (re)calculated by the scheduler once they get locked.
     *
     * @param userIdComposite user whose working time changed
     * @param validFrom earliest date affected by the change, or {@code null} for the very first working time (which
     *                  has no {@code validFrom} and therefore cannot be bounded - skipped)
     */
    private void publishOvertimeForWorkingTimeChange(UserIdComposite userIdComposite, @Nullable LocalDate validFrom) {

        if (validFrom == null) {
            LOG.info("Cannot resync overtime for working time change of user={} without validFrom (very first working time). Skipping.", userIdComposite);
            return;
        }

        final Optional<LocalDate> maybeLastLockedDate = getLastLockedDate();
        if (maybeLastLockedDate.isEmpty()) {
            LOG.debug("Locking is disabled, no locked overtime to resync for working time change of user={}.", userIdComposite);
            return;
        }

        final LocalDate lastLockedDate = maybeLastLockedDate.get();
        if (validFrom.isAfter(lastLockedDate)) {
            LOG.debug("Working time change of user={} (validFrom={}) does not reach into the locked period (lastLockedDate={}). Nothing to resync.", userIdComposite, validFrom, lastLockedDate);
            return;
        }

        LOG.info("Working time changed for user={}. Resync overtime for locked days in range [{} - {}].", userIdComposite, validFrom, lastLockedDate);
        publishForUserAndDateRange(userIdComposite, new DateRange(validFrom, lastLockedDate));
    }

    private Optional<LocalDate> getLastLockedDate() {
        return timeEntryLockService.getMinValidTimeEntryDate(clock.getZone())
            .map(minValidDate -> minValidDate.minusDays(1));
    }

    private static LocalDate earliestNonNull(@Nullable LocalDate first, @Nullable LocalDate second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.isBefore(second) ? first : second;
    }

    private void publishForUserAndDateRange(UserId userId, DateRange dateRange) {
        final Optional<UserIdComposite> maybeUserIdComposite = userManagementService.findUserById(userId).map(User::userIdComposite);
        if (maybeUserIdComposite.isEmpty()) {
            LOG.info("Ignore event. Unknown user {}.", userId);
            return;
        }

        publishForUserAndDateRange(maybeUserIdComposite.get(), dateRange);
    }

    private void publishForUserAndDateRange(UserIdComposite userIdComposite, DateRange dateRange) {
        final UserLocalId userLocalId = userIdComposite.localId();

        final OvertimeAccount overtimeAccount = overtimeAccountService.getOvertimeAccount(userLocalId);
        if (!overtimeAccount.isAllowed()) {
            LOG.info("Ignore event. User {} overtime not allowed.", userIdComposite);
            return;
        }

        final WorkingTimeCalendar workingTimeCalendar = workingTimeCalendarService.getWorkingTimeCalender(
            dateRange.startDate(), dateRange.endDate().plusDays(1), userLocalId);

        final LockTimeEntriesSettings lockSettings = timeEntryLockService.getLockTimeEntriesSettings();

        for (LocalDate date : dateRange) {

            final boolean locked = timeEntryLockService.isLocked(date, lockSettings);
            if (!locked) {
                LOG.info("Skip not locked date {} for user {}.", date, userIdComposite);
                break;
            }

            final boolean hasPlannedWorkingHours = workingTimeCalendar.plannedWorkingHours(date)
                .map(plannedWorkingHours -> !plannedWorkingHours.duration().isZero())
                .orElse(false);

            if (hasPlannedWorkingHours) {
                publishUpdated(userIdComposite, date);
            } else {
                LOG.info("Skip day {} for user {} because planned working hours are zero.", date, userIdComposite);
            }
        }
    }

    private void publishUpdated(UserIdComposite userIdComposite, LocalDate date) {
        // optimize maybe: call overtimeService only once with a new method that returns Map<LocalDate, OvertimeHours> -> only one database call
        final OvertimeHours overtimeHours = overtimeService.getOvertimeForDateAndUser(date, userIdComposite.localId());
        publishUpdated(userIdComposite, date, overtimeHours);
    }

    private void publishUpdated(UserIdComposite userIdComposite, LocalDate date, OvertimeHours overtimeHours) {
        final UserHasWorkedOvertimeEvent overtimeUpdatedEvent = new UserHasWorkedOvertimeEvent(userIdComposite, date, overtimeHours);
        LOG.info("publish UserHasWorkedOvertimeEvent date={} user={}", date, userIdComposite);
        applicationEventPublisher.publishEvent(overtimeUpdatedEvent);
    }
}
