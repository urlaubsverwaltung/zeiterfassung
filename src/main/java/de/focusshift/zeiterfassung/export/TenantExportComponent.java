package de.focusshift.zeiterfassung.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.focusshift.zeiterfassung.security.SecurityRole;
import de.focusshift.zeiterfassung.tenancy.configuration.single.SingleTenantConfigurationProperties;
import de.focusshift.zeiterfassung.tenancy.user.TenantUser;
import de.focusshift.zeiterfassung.tenancy.user.TenantUserService;
import de.focusshift.zeiterfassung.timeclock.TimeClock;
import de.focusshift.zeiterfassung.timeclock.TimeClockService;
import de.focusshift.zeiterfassung.timeentry.TimeEntry;
import de.focusshift.zeiterfassung.timeentry.TimeEntryService;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.usermanagement.OvertimeAccount;
import de.focusshift.zeiterfassung.usermanagement.OvertimeAccountService;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.usermanagement.WorkDay;
import de.focusshift.zeiterfassung.usermanagement.WorkingTime;
import de.focusshift.zeiterfassung.usermanagement.WorkingTimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@ConditionalOnProperty(prefix = "zeiterfassung.tenant.export", name = "enabled", havingValue = "true")
@Component
public class TenantExportComponent {

    private static final Logger LOG = LoggerFactory.getLogger(TenantExportComponent.class);

    private final String tenantId;
    private final TenantUserService tenantUserService;
    private final OvertimeAccountService overtimeAccountService;
    private final TimeClockService timeClockService;
    private final TimeEntryService timeEntryService;
    private final WorkingTimeService workingTimeService;
    private final ObjectMapper objectMapper;

    public TenantExportComponent(SingleTenantConfigurationProperties singleTenantConfigurationProperties, TenantUserService tenantUserService, OvertimeAccountService overtimeAccountService, TimeClockService timeClockService, TimeEntryService timeEntryService, WorkingTimeService workingTimeService, ObjectMapper objectMapper) {
        this.tenantId = singleTenantConfigurationProperties.getDefaultTenantId();
        this.tenantUserService = tenantUserService;
        this.overtimeAccountService = overtimeAccountService;
        this.timeClockService = timeClockService;
        this.timeEntryService = timeEntryService;
        this.workingTimeService = workingTimeService;
        this.objectMapper = objectMapper;
    }

    @Async
    @EventListener(ApplicationStartedEvent.class)
    void runExport() {
        LOG.info("Running export for tenant '{}'", tenantId);

        List<UserExport> exports = tenantUserService.findAllUsers()
            .stream()
            .map(user -> {
                LOG.info("Exporting user localId={}, externalId={}", user.localId(), user.id());

                final UserLocalId internalUserId = new UserLocalId(user.localId());
                final UserId externalUserId = new UserId(user.id());

                OvertimeAccount overtimeAccount = overtimeAccountService.getOvertimeAccount(internalUserId);
                WorkingTime workingTime = workingTimeService.getWorkingTimeByUser(internalUserId);
                List<TimeClock> timeClocks = timeClockService.findAllTimeClocks(externalUserId);
                List<TimeEntry> timeEntries = timeEntryService.getEntries(externalUserId);

                return UserExport.from(user, overtimeAccount, workingTime, timeClocks, timeEntries);
            })
            .toList();

        toJson(exports);

        LOG.info("Finished export for tenant '{}'", tenantId);
    }

    void toJson(List<UserExport> userExports) {
        try (FileOutputStream fos = new FileOutputStream(calcFileName())) {
            objectMapper.writeValue(fos, Map.of(
                "tenantId", tenantId,
                "exportedAt", Instant.now(),
                "users", userExports)
            );
        } catch (IOException e) {
            LOG.warn("skipping export - something went wrong exporting as json!", e);
        }
    }

    private File calcFileName() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String dateTimeString = LocalDateTime.now().format(formatter);
        String filename = "export_%s-%s.json".formatted(this.tenantId, dateTimeString);
        Path filePath = Paths.get(System.getProperty("java.io.tmpdir")).resolve(filename);
        LOG.info("Exporting to file '{}'", filePath.toAbsolutePath());
        return filePath.toFile();
    }

    record UserExport(UserDTO user, OvertimeAccountDTO overtimeAccount, WorkingTimeDTO workingTime,
                      List<TimeClockDTO> timeClocks, List<TimeEntryDTO> timeEntries) {
        static UserExport from(TenantUser tenantUser, OvertimeAccount overtimeAccount, WorkingTime workingTime, List<TimeClock> timeClocks, List<TimeEntry> timeEntries) {
            return new UserExport(
                UserDTO.from(tenantUser),
                OvertimeAccountDTO.from(overtimeAccount),
                WorkingTimeDTO.from(workingTime),
                timeClocks.stream().map(TimeClockDTO::from).toList(),
                timeEntries.stream().map(TimeEntryDTO::from).toList()
            );
        }
    }

    record UserDTO(String externalId, String givenName, String familyName, String eMail, Instant firstLoginAt,
                   Set<String> authorities) {
        static UserDTO from(TenantUser tenantUser) {
            return new UserDTO(tenantUser.id(), tenantUser.givenName(), tenantUser.familyName(), tenantUser.eMail().value(), tenantUser.firstLoginAt(), tenantUser.authorities().stream().map(SecurityRole::name).collect(Collectors.toSet()));
        }
    }

    record OvertimeAccountDTO(boolean allowed, Duration maxAllowedOvertime) {
        static OvertimeAccountDTO from(OvertimeAccount overtimeAccount) {
            return new OvertimeAccountDTO(overtimeAccount.isAllowed(), overtimeAccount.getMaxAllowedOvertime().orElse(null));
        }
    }

    record WorkingTimeDTO(WorkDayDTO monday, WorkDayDTO tuesday, WorkDayDTO wednesday, WorkDayDTO thursday,
                          WorkDayDTO friday, WorkDayDTO saturday, WorkDayDTO sunday) {
        static WorkingTimeDTO from(WorkingTime workingTime) {
            return new WorkingTimeDTO(
                WorkDayDTO.from(workingTime.getMonday()),
                WorkDayDTO.from(workingTime.getTuesday()),
                WorkDayDTO.from(workingTime.getWednesday()),
                WorkDayDTO.from(workingTime.getThursday()),
                WorkDayDTO.from(workingTime.getFriday()),
                WorkDayDTO.from(workingTime.getSaturday()),
                WorkDayDTO.from(workingTime.getSunday())
            );
        }
    }

    record WorkDayDTO(String dayOfWeek, Duration duration) {
        static WorkDayDTO from(Optional<WorkDay> workDay) {
            return workDay.map(item -> new WorkDayDTO(item.dayOfWeek().name(), item.duration())).orElseGet(null);
        }
    }

    record TimeClockDTO(ZonedDateTime startedAt, String comment, boolean isBreak,
                        Optional<ZonedDateTime> stoppedAt) {
        static TimeClockDTO from(TimeClock timeClock) {
            return new TimeClockDTO(timeClock.startedAt(), timeClock.comment(), timeClock.isBreak(), timeClock.stoppedAt());
        }
    }

    record TimeEntryDTO(String comment, ZonedDateTime start, ZonedDateTime end, boolean isBreak) {
        static TimeEntryDTO from(TimeEntry timeEntry) {
            return new TimeEntryDTO(timeEntry.comment(), timeEntry.start(), timeEntry.end(), timeEntry.isBreak());
        }
    }

}
