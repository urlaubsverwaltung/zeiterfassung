package de.focusshift.zeiterfassung.integration.urlaubsverwaltung.application;


import de.focus_shift.urlaubsverwaltung.extension.api.application.ApplicationAllowedEventDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.application.ApplicationCancelledEventDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.application.ApplicationCreatedFromSickNoteEventDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.application.ApplicationPeriodDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.application.ApplicationPersonDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.application.VacationTypeDTO;
import de.focusshift.zeiterfassung.absence.AbsenceWrite;
import de.focusshift.zeiterfassung.absence.AbsenceWriteService;
import de.focusshift.zeiterfassung.absence.DayLength;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import de.focusshift.zeiterfassung.user.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static de.focusshift.zeiterfassung.absence.AbsenceColor.CYAN;
import static de.focusshift.zeiterfassung.absence.AbsenceType.absenceTypeHoliday;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ApplicationEventHandlerRabbitmqTest {

    private ApplicationEventHandlerRabbitmq sut;

    @Mock
    private AbsenceWriteService absenceWriteService;

    @BeforeEach
    void setUp() {
        sut = new ApplicationEventHandlerRabbitmq(absenceWriteService);
    }

    @Test
    void onApplicationAllowedEvent() {
        final ApplicationAllowedEventDTO event = ApplicationAllowedEventDTO.builder()
                .id(UUID.randomUUID())
                .tenantId("tenantId")
                .sourceId(1L)
                .person(ApplicationPersonDTO.builder().personId(2L).username("userId").build())
                .period(ApplicationPeriodDTO.builder().startDate(Instant.ofEpochMilli(0L)).endDate(Instant.ofEpochMilli(1L)).dayLength(de.focus_shift.urlaubsverwaltung.extension.api.application.DayLength.FULL).build())
                .vacationType(VacationTypeDTO.builder().category("HOLIDAY").sourceId(1000L).color("CYAN").build())
                .createdAt(Instant.ofEpochMilli(0L))
                .status("status")
                .absentWorkingDays(Set.of(LocalDate.now()))
                .build();
        sut.on(event);

        final AbsenceWrite absence = new AbsenceWrite(
                new TenantId("tenantId"),
                1L,
                new UserId("userId"),
                Instant.ofEpochMilli(0L),
                Instant.ofEpochMilli(1L),
                DayLength.FULL,
                absenceTypeHoliday(CYAN),
                CYAN
        );
        verify(absenceWriteService).addAbsence(absence);
    }

    @Test
    void onApplicationCreatedFromSickNoteEvent() {
        final ApplicationCreatedFromSickNoteEventDTO event = ApplicationCreatedFromSickNoteEventDTO.builder()
                .id(UUID.randomUUID())
                .tenantId("tenantId")
                .sourceId(1L)
                .person(ApplicationPersonDTO.builder().personId(2L).username("userId").build())
                .period(ApplicationPeriodDTO.builder().startDate(Instant.ofEpochMilli(0L)).endDate(Instant.ofEpochMilli(1L)).dayLength(de.focus_shift.urlaubsverwaltung.extension.api.application.DayLength.FULL).build())
                .vacationType(VacationTypeDTO.builder().category("HOLIDAY").sourceId(1000L).color("CYAN").build())
                .createdAt(Instant.ofEpochMilli(0L))
                .status("status")
                .absentWorkingDays(Set.of(LocalDate.now()))
                .build();
        sut.on(event);

        final AbsenceWrite absence = new AbsenceWrite(
                new TenantId("tenantId"),
                1L,
                new UserId("userId"),
                Instant.ofEpochMilli(0L),
                Instant.ofEpochMilli(1L),
                DayLength.FULL,
                absenceTypeHoliday(CYAN),
                CYAN
        );
        verify(absenceWriteService).addAbsence(absence);
    }

    @Test
    void onApplicationCancelledEvent() {
        final ApplicationCancelledEventDTO event = ApplicationCancelledEventDTO.builder()
                .id(UUID.randomUUID())
                .tenantId("tenantId")
                .sourceId(1L)
                .person(ApplicationPersonDTO.builder().personId(2L).username("userId").build())
                .period(ApplicationPeriodDTO.builder().startDate(Instant.ofEpochMilli(0L)).endDate(Instant.ofEpochMilli(1L)).dayLength(de.focus_shift.urlaubsverwaltung.extension.api.application.DayLength.FULL).build())
                .vacationType(VacationTypeDTO.builder().category("HOLIDAY").sourceId(1000L).color("CYAN").build())
                .createdAt(Instant.ofEpochMilli(0L))
                .status("status")
                .absentWorkingDays(Set.of(LocalDate.now()))
                .build();
        sut.on(event);

        final AbsenceWrite absence = new AbsenceWrite(
                new TenantId("tenantId"),
                1L,
                new UserId("userId"),
                Instant.ofEpochMilli(0L),
                Instant.ofEpochMilli(1L),
                DayLength.FULL,
                absenceTypeHoliday(CYAN),
                CYAN
        );
        verify(absenceWriteService).deleteAbsence(absence);
    }
}
