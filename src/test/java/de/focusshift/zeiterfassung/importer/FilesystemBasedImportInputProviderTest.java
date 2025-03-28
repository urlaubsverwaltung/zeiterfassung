package de.focusshift.zeiterfassung.importer;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.focusshift.zeiterfassung.importer.model.OvertimeAccountDTO;
import de.focusshift.zeiterfassung.importer.model.TenantExport;
import de.focusshift.zeiterfassung.importer.model.TimeClockDTO;
import de.focusshift.zeiterfassung.importer.model.TimeEntryDTO;
import de.focusshift.zeiterfassung.importer.model.UserDTO;
import de.focusshift.zeiterfassung.importer.model.UserExport;
import de.focusshift.zeiterfassung.importer.model.WorkDayDTO;
import de.focusshift.zeiterfassung.importer.model.WorkingTimeDTO;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_USER;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_VIEW_REPORT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_WORKING_TIME_EDIT_ALL;
import static java.time.ZonedDateTime.parse;
import static org.assertj.core.api.Assertions.assertThat;

class FilesystemBasedImportInputProviderTest {

    @Test
    void handlesImportFileNotFound() {
        FilesystemBasedImportInputProvider sut = new FilesystemBasedImportInputProvider(objectMapper(), "src/test/resources/doesnt_exists.json");

        assertThat(sut.fromExport()).isEmpty();
    }

    @Test
    void happyPath() {

        FilesystemBasedImportInputProvider sut = new FilesystemBasedImportInputProvider(objectMapper(), "src/test/resources/export_file.json");

        Optional<TenantExport> optionalTenantExport = sut.fromExport();

        assertThat(optionalTenantExport).isPresent();

        TenantExport tenantExport = optionalTenantExport.get();
        assertThat(tenantExport.tenantId()).isEqualTo("bac98fef");

        assertThat(tenantExport.users()).hasSize(1);

        UserExport userExport = tenantExport.users().getFirst();

        assertThat(userExport.user()).isEqualTo(new UserDTO("58400ef7-1cc9-48cb-93a8-f45c7af186ad", "Marlene", "Muster", "office@example.org", Instant.parse("2023-01-01T12:00:00Z"), Set.of(ZEITERFASSUNG_VIEW_REPORT_ALL.name(), ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL.name(), ZEITERFASSUNG_WORKING_TIME_EDIT_ALL.name(), ZEITERFASSUNG_USER.name())));

        assertThat(userExport.overtimeAccount()).isEqualTo(new OvertimeAccountDTO(true, Duration.ofHours(100)));

        assertThat(userExport.workingTime()).isEqualTo(new WorkingTimeDTO(
            new WorkDayDTO("MONDAY", Duration.ofHours(8)),
            new WorkDayDTO("TUESDAY", Duration.ofHours(8)),
            new WorkDayDTO("WEDNESDAY", Duration.ofHours(8)),
            new WorkDayDTO("THURSDAY", Duration.ofHours(8)),
            new WorkDayDTO("FRIDAY", Duration.ofHours(8)),
            new WorkDayDTO("SATURDAY", Duration.ofHours(0)),
            new WorkDayDTO("SUNDAY", Duration.ofHours(0))
        ));

        assertThat(userExport.timeClocks()).containsOnly(
            new TimeClockDTO(parse("2024-06-01T06:00:33.213Z"), "my comment", false, Optional.of(parse("2024-06-01T10:00:00.213Z"))),
            new TimeClockDTO(parse("2024-06-02T10:00:00.123Z"), "my comment", true, Optional.of(parse("2024-06-02T11:00:00.213Z"))),
            new TimeClockDTO(parse("2024-06-03T06:00:00.123Z"), "my comment", false, Optional.empty())
        );

        assertThat(userExport.timeEntries()).containsOnly(
            new TimeEntryDTO("dies", parse("2024-06-01T06:00:00.123Z"), parse("2024-06-01T10:00:00.123Z"), false, false),
            new TimeEntryDTO("das", parse("2024-06-01T10:00:00.124Z"), parse("2024-06-01T11:00:00.125Z"), false, false),
            new TimeEntryDTO("ananas", parse("2024-06-01T11:00:00.126Z"), parse("2024-06-01T15:00:00.127Z"), false, true)
        );
    }

    private static ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.registerModule(new Jdk8Module());
        return objectMapper;
    }
}
