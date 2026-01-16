package de.focusshift.zeiterfassung.overtime;

import de.focusshift.zeiterfassung.DateRange;
import de.focusshift.zeiterfassung.absence.AbsenceAddedEvent;
import de.focusshift.zeiterfassung.absence.AbsenceDeletedEvent;
import de.focusshift.zeiterfassung.absence.AbsenceUpdatedEvent;
import de.focusshift.zeiterfassung.overtime.events.UserHasWorkedOvertimeEvent;
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
import de.focusshift.zeiterfassung.workingtime.WorkingTimeCalendar;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeCalendarService;
import org.slf4j.Logger;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

@Component
class OvertimePublisher {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final OvertimeService overtimeService;
    private final OvertimeAccountService overtimeAccountService;
    private final TimeEntryLockService timeEntryLockService;
    private final UserManagementService userManagementService;
    private final WorkingTimeCalendarService workingTimeCalendarService;
    private final ApplicationEventPublisher applicationEventPublisher;

    OvertimePublisher(
        OvertimeService overtimeService,
        OvertimeAccountService overtimeAccountService,
        TimeEntryLockService timeEntryLockService,
        UserManagementService userManagementService,
        WorkingTimeCalendarService workingTimeCalendarService,
        ApplicationEventPublisher applicationEventPublisher
    ) {
        this.overtimeService = overtimeService;
        this.overtimeAccountService = overtimeAccountService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.timeEntryLockService = timeEntryLockService;
        this.userManagementService = userManagementService;
        this.workingTimeCalendarService = workingTimeCalendarService;
    }

    @EventListener
    public void publishOvertime(DayLockedEvent event) {
        LOG.info("Handling DayLockedEvent for date={} and zoneId={}. Calculating overtime and publish application event.", event.date(), event.zoneId());

        final Map<UserIdComposite, OvertimeAccount> overtimeAccountByUserId = overtimeAccountService.getAllOvertimeAccounts();

        overtimeService.getOvertimeForDate(event.date()).forEach((userIdComposite, overtimeHours) -> {
            // currently we just know whether overtime is allowed or not.
            // this may change in the future to have a date range for this info or more granular stuff.
            final OvertimeAccount overtimeAccount = overtimeAccountByUserId.get(userIdComposite);
            if (overtimeAccount.isAllowed() && !overtimeHours.durationInMinutes().isZero()) {
                publishUpdated(userIdComposite, event.date(), overtimeHours);
            }
        });
    }

    @EventListener
    public void publishOvertimeUpdated(TimeEntryCreatedEvent event) {

        if (!event.locked()) {
            LOG.debug("Ignore not locked TimeEntryCreatedEvent.");
            return;
        }

        if (event.workDuration().durationInMinutes().isZero()) {
            LOG.info("Ignore TimeEntryCreatedEvent. WorkDuration is zero.");
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
        final WorkDuration workDuration = event.workDuration();

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

        if (workDuration.durationInMinutes().isZero()) {
            LOG.info("Ignore TimeEntryDeleted Event. WorkDuration is zero.");
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

    private void publishForUserAndDateRange(UserId userId, DateRange dateRange) {
        final Optional<UserIdComposite> maybeUserIdComposite = userManagementService.findUserById(userId).map(User::userIdComposite);
        if (maybeUserIdComposite.isEmpty()) {
            LOG.info("Ignore absence. Unknown user {}.", userId);
            return;
        }

        final UserIdComposite userIdComposite = maybeUserIdComposite.get();
        final UserLocalId userLocalId = userIdComposite.localId();

        final OvertimeAccount overtimeAccount = overtimeAccountService.getOvertimeAccount(userLocalId);
        if (!overtimeAccount.isAllowed()) {
            LOG.info("Ignore absence. User {} overtime not allowed.", userIdComposite);
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
                LOG.info("Skip absence day {} for user {} because planned working hours are zero.", date, userIdComposite);
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
