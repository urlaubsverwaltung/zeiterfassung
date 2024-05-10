package de.focusshift.zeiterfassung.integration.urlaubsverwaltung.sicknote;

import de.focus_shift.urlaubsverwaltung.extension.api.sicknote.SickNoteCancelledEventDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.sicknote.SickNoteConvertedToApplicationEventDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.sicknote.SickNoteCreatedEventDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.sicknote.SickNotePeriodDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.sicknote.SickNotePersonDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.sicknote.SickNoteUpdatedEventDTO;
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

import static de.focusshift.zeiterfassung.absence.AbsenceTypeCategory.SICK;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SickNoteEventHandlerRabbitmqTest {

    private SickNoteEventHandlerRabbitmq sut;

    @Mock
    private AbsenceWriteService absenceWriteService;

    @BeforeEach
    void setUp() {
        sut = new SickNoteEventHandlerRabbitmq(absenceWriteService);
    }

    @Test
    void onSickNoteCreatedEvent() {
        final SickNoteCreatedEventDTO event = SickNoteCreatedEventDTO.builder()
                .id(UUID.randomUUID())
                .tenantId("tenantId")
                .sourceId(1L)
                .person(SickNotePersonDTO.builder().personId(2L).username("userId").build())
                .period(SickNotePeriodDTO.builder().startDate(Instant.ofEpochMilli(0L)).endDate(Instant.ofEpochMilli(1L)).dayLength(de.focus_shift.urlaubsverwaltung.extension.api.sicknote.DayLength.FULL).build())
                .type("type")
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
            SICK
        );
        verify(absenceWriteService).addAbsence(absence);
    }

    @Test
    void onSickNoteCancelledEvent() {
        final SickNoteCancelledEventDTO event = SickNoteCancelledEventDTO.builder()
                .id(UUID.randomUUID())
                .tenantId("tenantId")
                .sourceId(1L)
                .person(SickNotePersonDTO.builder().personId(2L).username("userId").build())
                .period(SickNotePeriodDTO.builder().startDate(Instant.ofEpochMilli(0L)).endDate(Instant.ofEpochMilli(1L)).dayLength(de.focus_shift.urlaubsverwaltung.extension.api.sicknote.DayLength.FULL).build())
                .type("type")
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
            SICK
        );
        verify(absenceWriteService).deleteAbsence(absence);
    }

    @Test
    void onSickNoteUpdatedEvent() {
        final SickNoteUpdatedEventDTO event = SickNoteUpdatedEventDTO.builder()
                .id(UUID.randomUUID())
                .tenantId("tenantId")
                .sourceId(1L)
                .person(SickNotePersonDTO.builder().personId(2L).username("userId").build())
                .period(SickNotePeriodDTO.builder().startDate(Instant.ofEpochMilli(0L)).endDate(Instant.ofEpochMilli(1L)).dayLength(de.focus_shift.urlaubsverwaltung.extension.api.sicknote.DayLength.FULL).build())
                .type("type")
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
            SICK
        );
        verify(absenceWriteService).updateAbsence(absence);
    }

    @Test
    void onSickNoteConvertedToApplicationEvent() {
        final SickNoteConvertedToApplicationEventDTO event = SickNoteConvertedToApplicationEventDTO.builder()
                .id(UUID.randomUUID())
                .tenantId("tenantId")
                .sourceId(1L)
                .person(SickNotePersonDTO.builder().personId(2L).username("userId").build())
                .period(SickNotePeriodDTO.builder().startDate(Instant.ofEpochMilli(0L)).endDate(Instant.ofEpochMilli(1L)).dayLength(de.focus_shift.urlaubsverwaltung.extension.api.sicknote.DayLength.FULL).build())
                .type("type")
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
            SICK
        );
        verify(absenceWriteService).deleteAbsence(absence);
    }


}
