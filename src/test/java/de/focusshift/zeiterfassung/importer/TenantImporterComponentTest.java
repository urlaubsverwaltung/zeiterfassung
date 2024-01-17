package de.focusshift.zeiterfassung.importer;

import de.focusshift.zeiterfassung.importer.model.OvertimeAccountDTO;
import de.focusshift.zeiterfassung.importer.model.TenantExport;
import de.focusshift.zeiterfassung.importer.model.TimeClockDTO;
import de.focusshift.zeiterfassung.importer.model.TimeEntryDTO;
import de.focusshift.zeiterfassung.importer.model.UserDTO;
import de.focusshift.zeiterfassung.importer.model.UserExport;
import de.focusshift.zeiterfassung.importer.model.WorkDayDTO;
import de.focusshift.zeiterfassung.importer.model.WorkingTimeDTO;
import de.focusshift.zeiterfassung.publicholiday.FederalState;
import de.focusshift.zeiterfassung.security.SecurityRole;
import de.focusshift.zeiterfassung.tenancy.tenant.Tenant;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantService;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantStatus;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.tenancy.user.TenantUser;
import de.focusshift.zeiterfassung.tenancy.user.TenantUserService;
import de.focusshift.zeiterfassung.tenancy.user.UserStatus;
import de.focusshift.zeiterfassung.timeclock.TimeClock;
import de.focusshift.zeiterfassung.timeclock.TimeClockService;
import de.focusshift.zeiterfassung.timeentry.TimeEntryService;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.usermanagement.OvertimeAccountService;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anySet;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantImporterComponentTest {

    @InjectMocks
    private TenantImporterComponent sut;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private TenantContextHolder tenantContextHolder;
    @Mock
    private TenantService tenantService;
    @Mock
    private TenantUserService tenantUserService;
    @Mock
    private OvertimeAccountService overtimeAccountService;
    @Mock
    private TimeClockService timeClockService;
    @Mock
    private TimeEntryService timeEntryService;
    @Mock
    private WorkingTimeService workingTimeService;
    @Mock
    private ImportInputProvider importInputProvider;

    private static TenantExport exportedData() {
        UserExport userExport = new UserExport(
            new UserDTO("externalId", "marlene", "muster", "my.name@example.org", Instant.now().minus(365, ChronoUnit.DAYS), Set.of(SecurityRole.ZEITERFASSUNG_USER.name())),
            new OvertimeAccountDTO(true, Duration.ofHours(8)),
            new WorkingTimeDTO(
                new WorkDayDTO("MONDAY", Duration.ofHours(8)),
                new WorkDayDTO("TUESDAY", Duration.ofHours(8)),
                new WorkDayDTO("WEDNESDAY", Duration.ofHours(8)),
                new WorkDayDTO("THURSDAY", Duration.ofHours(8)),
                new WorkDayDTO("FRIDAY", Duration.ofHours(8)),
                new WorkDayDTO("SATURDAY", Duration.ofHours(0)),
                new WorkDayDTO("SUNDAY", Duration.ofHours(0))
            ),
            List.of(
                new TimeClockDTO(ZonedDateTime.parse("2024-06-01T06:00:33.123Z"), "my comment", false, Optional.of(ZonedDateTime.parse("2024-06-01T10:00:00.123Z")))
            ),
            List.of(
                new TimeEntryDTO("lala", ZonedDateTime.parse("2024-06-01T06:00:00.123Z"), ZonedDateTime.parse("2024-06-01T10:00:00.123Z"), false)
            )
        );
        return new TenantExport("tenantId", Instant.now(), List.of(userExport));
    }

    @Test
    void whenTenantExistsInDBAndHasNoUsersImporterRuns() {

        TenantExport tenantExport = exportedData();

        when(importInputProvider.fromExport()).thenReturn(Optional.of(tenantExport));
        Instant firstLoginAt = Instant.now().minus(365, ChronoUnit.DAYS);
        when(tenantService.getTenantByTenantId(anyString())).thenReturn(Optional.of(new Tenant("tenantId", firstLoginAt, firstLoginAt, TenantStatus.ACTIVE)));
        when(tenantUserService.findAllUsers()).thenReturn(List.of());

        when(tenantUserService.createNewUser(anyString(), anyString(), anyString(), any(EMailAddress.class), anySet())).thenReturn(
            new TenantUser("externalId", 1L, "marlene", "muster", new EMailAddress("my.name@example.org"), firstLoginAt, Set.of(SecurityRole.ZEITERFASSUNG_USER), firstLoginAt, firstLoginAt, null, null, UserStatus.ACTIVE)
        );

        sut.runImport();

        InOrder inOrder = Mockito.inOrder(tenantContextHolder);
        inOrder.verify(tenantContextHolder).setTenantId(new TenantId("tenantId"));
        inOrder.verify(tenantContextHolder).clear();

        verify(tenantUserService).findAllUsers();

        verify(tenantUserService).createNewUser(
            "externalId",
            "marlene",
            "muster",
            new EMailAddress("my.name@example.org"),
            Set.of(SecurityRole.ZEITERFASSUNG_USER)
        );

        verify(overtimeAccountService).updateOvertimeAccount(new UserLocalId(1L), true, Duration.ofHours(8));

        // verify workintime
        EnumMap<DayOfWeek, Duration> workdays = new EnumMap<>(Map.of(
            MONDAY, Duration.ofHours(8),
            TUESDAY, Duration.ofHours(8),
            WEDNESDAY, Duration.ofHours(8),
            THURSDAY, Duration.ofHours(8),
            FRIDAY, Duration.ofHours(8),
            SATURDAY, Duration.ofHours(0),
            SUNDAY, Duration.ofHours(0))
        );
        verify(workingTimeService).createWorkingTime(new UserLocalId(1L), null, FederalState.GLOBAL, null, workdays);

        verify(timeClockService).importTimeClock(
            new TimeClock(null, new UserId("externalId"), ZonedDateTime.parse("2024-06-01T08:00:33.123+02:00[Europe/Berlin]"), "my comment", false, Optional.of(ZonedDateTime.parse("2024-06-01T12:00:00.123+02:00[Europe/Berlin]")))
        );

        verify(timeEntryService).createTimeEntry(
            new UserId("externalId"), "lala", ZonedDateTime.parse("2024-06-01T08:00:00.123+02:00[Europe/Berlin]"), ZonedDateTime.parse("2024-06-01T12:00:00.123+02:00[Europe/Berlin]"), false
        );

    }

    @Test
    void whenTenantNotFoundInDBNothingIsImported() {

        TenantExport tenantExport = exportedData();

        when(importInputProvider.fromExport()).thenReturn(Optional.of(tenantExport));
        when(tenantService.getTenantByTenantId(anyString())).thenReturn(Optional.empty());

        sut.runImport();

        verifyNoInteractions(tenantContextHolder);
        verifyNoInteractions(tenantUserService);
        verifyNoInteractions(overtimeAccountService);
        verifyNoInteractions(timeClockService);
        verifyNoInteractions(timeEntryService);
        verifyNoInteractions(workingTimeService);
    }

    @Test
    void whenAnyUserExistsOfTenantInDBNothingIsImported() {

        TenantExport tenantExport = exportedData();

        when(importInputProvider.fromExport()).thenReturn(Optional.of(tenantExport));
        Instant firstLoginAt = Instant.now().minus(365, ChronoUnit.DAYS);
        when(tenantService.getTenantByTenantId(anyString())).thenReturn(Optional.of(new Tenant("tenantId", firstLoginAt, firstLoginAt, TenantStatus.ACTIVE)));
        when(tenantUserService.findAllUsers()).thenReturn(List.of(new TenantUser("externalId", 1L, "marlene", "muster", new EMailAddress("my.name@example.org"), firstLoginAt, Set.of(SecurityRole.ZEITERFASSUNG_USER), firstLoginAt, firstLoginAt, null, null, UserStatus.ACTIVE)));

        sut.runImport();

        verify(tenantContextHolder).setTenantId(new TenantId("tenantId"));
        verify(tenantUserService).findAllUsers();
        verify(tenantContextHolder).clear();

        verifyNoInteractions(overtimeAccountService);
        verifyNoInteractions(timeClockService);
        verifyNoInteractions(timeEntryService);
        verifyNoInteractions(workingTimeService);
    }

    @Test
    void whenExportUsesDefaultTenantIdNothingIsImported() {

        TenantExport tenantExport = new TenantExport("default", Instant.now(), List.of());

        when(importInputProvider.fromExport()).thenReturn(Optional.of(tenantExport));

        sut.runImport();

        verifyNoInteractions(tenantService);
        verifyNoInteractions(tenantContextHolder);
        verifyNoInteractions(tenantUserService);
        verifyNoInteractions(overtimeAccountService);
        verifyNoInteractions(timeClockService);
        verifyNoInteractions(timeEntryService);
        verifyNoInteractions(workingTimeService);
    }

}
