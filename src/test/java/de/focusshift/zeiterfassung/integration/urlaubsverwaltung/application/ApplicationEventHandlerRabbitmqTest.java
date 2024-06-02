package de.focusshift.zeiterfassung.integration.urlaubsverwaltung.application;


import de.focus_shift.urlaubsverwaltung.extension.api.application.ApplicationAllowedEventDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.application.ApplicationCancelledEventDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.application.ApplicationCreatedFromSickNoteEventDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.application.ApplicationPeriodDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.application.ApplicationPersonDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.application.VacationTypeDTO;
import de.focusshift.zeiterfassung.absence.AbsenceTypeSourceId;
import de.focusshift.zeiterfassung.absence.AbsenceWrite;
import de.focusshift.zeiterfassung.absence.AbsenceWriteService;
import de.focusshift.zeiterfassung.absence.DayLength;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import de.focusshift.zeiterfassung.user.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static de.focusshift.zeiterfassung.absence.AbsenceTypeCategory.HOLIDAY;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ApplicationEventHandlerRabbitmqTest {

    private ApplicationEventHandlerRabbitmq sut;

    @Mock
    private AbsenceWriteService absenceWriteService;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private TenantContextHolder tenantContextHolder;

    @BeforeEach
    void setUp() {
        sut = new ApplicationEventHandlerRabbitmq(absenceWriteService, tenantContextHolder);
    }

    @Test
    void onApplicationAllowedEvent() {
        TenantId tenantId = new TenantId("tenantId");

        final ApplicationAllowedEventDTO event = ApplicationAllowedEventDTO.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId.tenantId())
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
            1L,
            new UserId("userId"),
            Instant.ofEpochMilli(0L),
            Instant.ofEpochMilli(1L),
            DayLength.FULL,
            HOLIDAY,
            new AbsenceTypeSourceId(1000L)
        );
        verify(absenceWriteService).addAbsence(absence);

        InOrder inOrder = Mockito.inOrder(tenantContextHolder);
        inOrder.verify(tenantContextHolder).setTenantId(tenantId);
        inOrder.verify(tenantContextHolder).clear();
    }

    @Test
    void onApplicationCreatedFromSickNoteEvent() {
        TenantId tenantId = new TenantId("tenantId");

        final ApplicationCreatedFromSickNoteEventDTO event = ApplicationCreatedFromSickNoteEventDTO.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId.tenantId())
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
            1L,
            new UserId("userId"),
            Instant.ofEpochMilli(0L),
            Instant.ofEpochMilli(1L),
            DayLength.FULL,
            HOLIDAY,
            new AbsenceTypeSourceId(1000L)
        );
        verify(absenceWriteService).addAbsence(absence);

        InOrder inOrder = Mockito.inOrder(tenantContextHolder);
        inOrder.verify(tenantContextHolder).setTenantId(tenantId);
        inOrder.verify(tenantContextHolder).clear();
    }

    @Test
    void onApplicationCancelledEvent() {
        TenantId tenantId = new TenantId("tenantId");

        final ApplicationCancelledEventDTO event = ApplicationCancelledEventDTO.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId.tenantId())
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
            1L,
            new UserId("userId"),
            Instant.ofEpochMilli(0L),
            Instant.ofEpochMilli(1L),
            DayLength.FULL,
            HOLIDAY,
            new AbsenceTypeSourceId(1000L)
        );
        verify(absenceWriteService).deleteAbsence(absence);

        InOrder inOrder = Mockito.inOrder(tenantContextHolder);
        inOrder.verify(tenantContextHolder).setTenantId(tenantId);
        inOrder.verify(tenantContextHolder).clear();
    }

    @Test
    void onApplicationWithAbsentWorkingDays() {
        TenantId tenantId = new TenantId("tenantId");

        final ApplicationAllowedEventDTO event = ApplicationAllowedEventDTO.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId.tenantId())
                .sourceId(1L)
                .person(ApplicationPersonDTO.builder().personId(2L).username("userId").build())
                .period(ApplicationPeriodDTO.builder().startDate(Instant.ofEpochMilli(0L)).endDate(Instant.ofEpochMilli(1L)).dayLength(de.focus_shift.urlaubsverwaltung.extension.api.application.DayLength.FULL).build())
                .vacationType(VacationTypeDTO.builder().category("HOLIDAY").sourceId(1000L).color("CYAN").build())
                .createdAt(Instant.ofEpochMilli(0L))
                .status("status")
                .absentWorkingDays(Set.of())
                .build();
        sut.on(event);

        verifyNoInteractions(absenceWriteService);

        InOrder inOrder = Mockito.inOrder(tenantContextHolder);
        inOrder.verify(tenantContextHolder).setTenantId(tenantId);
        inOrder.verify(tenantContextHolder).clear();
    }
}
