package de.focusshift.zeiterfassung.importer;


import de.focusshift.zeiterfassung.importer.model.TimeClockDTO;
import de.focusshift.zeiterfassung.importer.model.TimeEntryDTO;
import de.focusshift.zeiterfassung.importer.model.UserDTO;
import de.focusshift.zeiterfassung.importer.model.UserExport;
import de.focusshift.zeiterfassung.importer.model.WorkingTimeDTO;
import de.focusshift.zeiterfassung.publicholiday.FederalState;
import de.focusshift.zeiterfassung.security.SecurityRole;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantService;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.tenancy.user.TenantUser;
import de.focusshift.zeiterfassung.tenancy.user.TenantUserService;
import de.focusshift.zeiterfassung.timeclock.TimeClock;
import de.focusshift.zeiterfassung.timeclock.TimeClockService;
import de.focusshift.zeiterfassung.timeentry.TimeEntryService;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.usermanagement.OvertimeAccountService;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;

class TenantImporterComponent {

    private static final Logger LOG = LoggerFactory.getLogger(TenantImporterComponent.class);
    private static final ZoneId EUROPE_BERLIN = ZoneId.of("Europe/Berlin");

    private final TenantContextHolder tenantContextHolder;
    private final TenantService tenantService;
    private final TenantUserService tenantUserService;
    private final OvertimeAccountService overtimeAccountService;
    private final TimeClockService timeClockService;
    private final TimeEntryService timeEntryService;
    private final WorkingTimeService workingTimeService;
    private final ImportInputProvider importInputProvider;

    TenantImporterComponent(TenantContextHolder tenantContextHolder, TenantService tenantService,
                                   TenantUserService tenantUserService, OvertimeAccountService overtimeAccountService,
                                   TimeClockService timeClockService, TimeEntryService timeEntryService,
                                   WorkingTimeService workingTimeService, ImportInputProvider importInputProvider) {
        this.tenantContextHolder = tenantContextHolder;
        this.tenantService = tenantService;
        this.tenantUserService = tenantUserService;
        this.overtimeAccountService = overtimeAccountService;
        this.timeClockService = timeClockService;
        this.timeEntryService = timeEntryService;
        this.workingTimeService = workingTimeService;
        this.importInputProvider = importInputProvider;
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void runImport() {

        importInputProvider.fromExport().ifPresentOrElse(importerData -> {

            final TenantId tenantId = new TenantId(importerData.tenantId());

            if (importerData.tenantId().equalsIgnoreCase("default")) {
                LOG.warn("tenantId={} is default, please adjust tenantId!", tenantId.tenantId());
                return;
            }

            if (tenantService.getTenantByTenantId(tenantId.tenantId()).isEmpty()) {
                LOG.warn("tenantId={} not found, skipping import", tenantId.tenantId());
                return;
            }

            LOG.info("Found existing tenantId={} - going to import users", tenantId.tenantId());

            tenantContextHolder.runInTenantIdContext(tenantId, passedTenantId -> {
                try {
                    if (!tenantUserService.findAllUsers().isEmpty()) {
                        LOG.info("tenantId={} already has users, skipping import", passedTenantId);
                        return;
                    }
                    LOG.info("tenantId={} has no users, starting import {} users!", passedTenantId, importerData.users().size());
                    importerData.users().forEach(userToImport -> importUser(userToImport, new TenantId(passedTenantId)));
                    LOG.info("finished importing {} users of tenantId={}", importerData.users().size(), passedTenantId);
                } catch (Exception e) {
                    LOG.error("Error occurred while importing users", e);
                }
            });


            LOG.info("Finished import for tenant={}", tenantId.tenantId());

        }, () -> LOG.info("No export file found"));
    }

    private void importUser(UserExport userToImport, TenantId tenantId) {
        final UserDTO user = userToImport.user();

        LOG.info("importing user={} of tenantId={}", user.externalId(), tenantId.tenantId());

        final TenantUser createdUser = user(user, tenantId);

        final UserLocalId userLocalId = new UserLocalId(createdUser.localId());
        final UserId externalUserId = new UserId(user.externalId());

        overtime(userToImport, tenantId, userLocalId);
        workingTime(userToImport, tenantId, userLocalId);
        timeClocks(tenantId, externalUserId, userToImport.timeClocks());
        timeEntries(externalUserId, tenantId, userToImport.timeEntries());
        LOG.info("imported user={} of tenantId={}", user.externalId(), tenantId.tenantId());
    }

    private TenantUser user(UserDTO user, TenantId tenantId) {
        LOG.info("creating user={} of tenantId={}", user.externalId(), tenantId.tenantId());
        return tenantUserService.createNewUser(user.externalId(), user.givenName(), user.familyName(), new EMailAddress(user.eMail()), user.authorities().stream().map(SecurityRole::valueOf).collect(Collectors.toSet()));
    }

    private void timeClocks(TenantId tenantId, UserId externalUserId, List<TimeClockDTO> timeClockDTOS) {
        LOG.info("creating {} timeClocks of user={} of tenantId={}", timeClockDTOS.size(), externalUserId.value(), tenantId.tenantId());
        timeClockDTOS.forEach(timeClock -> timeClockService.importTimeClock(new TimeClock(null, externalUserId, adjustWithDefaultTimeZone(timeClock.startedAt()), timeClock.comment(), timeClock.isBreak(), adjustWithDefaultTimeZone(timeClock.stoppedAt()))));
        LOG.info("created timeClocks of user={} of tenantId={}", externalUserId.value(), tenantId.tenantId());
    }

    private void timeEntries(UserId externalUserId, TenantId tenantId, List<TimeEntryDTO> timeEntryDTOS) {
        LOG.info("creating {} timeEntries of user={} of tenantId={}", timeEntryDTOS.size(), externalUserId.value(), tenantId.tenantId());
        timeEntryDTOS.forEach(timeEntry -> timeEntryService.createTimeEntry(externalUserId, timeEntry.comment(), adjustWithDefaultTimeZone(timeEntry.start()), adjustWithDefaultTimeZone(timeEntry.end()), timeEntry.isBreak()));
        LOG.info("created timeEntries of user={} of tenantId={}", externalUserId.value(), tenantId.tenantId());
    }

    private void overtime(UserExport userToImport, TenantId tenantId, UserLocalId userLocalId) {
        LOG.info("updating overtimeAccount for user={} of tenantId={}", userToImport.user().externalId(), tenantId.tenantId());
        overtimeAccountService.updateOvertimeAccount(userLocalId, userToImport.overtimeAccount().allowed(), userToImport.overtimeAccount().maxAllowedOvertime());
    }

    private void workingTime(UserExport userToImport, TenantId tenantId, UserLocalId userLocalId) {
        WorkingTimeDTO workingTimeDTO = userToImport.workingTime();
        final EnumMap<DayOfWeek, Duration> workdays = new EnumMap<>(Map.of(
            MONDAY, workingTimeDTO.monday().duration(),
            TUESDAY, workingTimeDTO.tuesday().duration(),
            WEDNESDAY, workingTimeDTO.wednesday().duration(),
            THURSDAY, workingTimeDTO.thursday().duration(),
            FRIDAY, workingTimeDTO.friday().duration(),
            SATURDAY, workingTimeDTO.saturday().duration(),
            SUNDAY, workingTimeDTO.sunday().duration())
        );

        LOG.info("creating workingTime for user={} of tenantId={}", userToImport.user().externalId(), tenantId.tenantId());
        // initial workingTime validFrom has to be null or it won't work!
        // worksOnPublicHoliday is not set here, to use global settings
        workingTimeService.createWorkingTime(userLocalId, null, FederalState.GLOBAL, null, workdays);
    }

    private static ZonedDateTime adjustWithDefaultTimeZone(ZonedDateTime zonedDateTime) {
        // current export is done with hard coded timezone europe/berlin.
        // this changes as soon as the user can configure a custom timezone (see UserSettingsProvider)
        return zonedDateTime.withZoneSameInstant(EUROPE_BERLIN);
    }

    private static Optional<ZonedDateTime> adjustWithDefaultTimeZone(Optional<ZonedDateTime> zonedDateTime) {
        return zonedDateTime.map(TenantImporterComponent::adjustWithDefaultTimeZone);
    }
}
