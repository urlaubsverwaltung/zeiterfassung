package de.focusshift.zeiterfassung.integration.urlaubsverwaltung.application;

import de.focus_shift.urlaubsverwaltung.extension.api.application.ApplicationAllowedEventDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.application.ApplicationCancelledEventDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.application.ApplicationCreatedFromSickNoteEventDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.application.VacationTypeDTO;
import de.focusshift.zeiterfassung.absence.AbsenceTypeCategory;
import de.focusshift.zeiterfassung.absence.AbsenceTypeSourceId;
import de.focusshift.zeiterfassung.absence.AbsenceWrite;
import de.focusshift.zeiterfassung.absence.AbsenceWriteService;
import de.focusshift.zeiterfassung.absence.DayLength;
import de.focusshift.zeiterfassung.integration.urlaubsverwaltung.RabbitMessageConsumer;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.user.UserId;
import org.slf4j.Logger;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static de.focusshift.zeiterfassung.integration.urlaubsverwaltung.application.ApplicationRabbitmqConfiguration.ZEITERFASSUNG_URLAUBSVERWALTUNG_APPLICATION_ALLOWED_QUEUE;
import static de.focusshift.zeiterfassung.integration.urlaubsverwaltung.application.ApplicationRabbitmqConfiguration.ZEITERFASSUNG_URLAUBSVERWALTUNG_APPLICATION_CANCELLED_QUEUE;
import static de.focusshift.zeiterfassung.integration.urlaubsverwaltung.application.ApplicationRabbitmqConfiguration.ZEITERFASSUNG_URLAUBSVERWALTUNG_APPLICATION_CREATED_FROM_SICKNOTE_QUEUE;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

public class ApplicationEventHandlerRabbitmq extends RabbitMessageConsumer {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final AbsenceWriteService absenceWriteService;
    private final TenantContextHolder tenantContextHolder;

    ApplicationEventHandlerRabbitmq(AbsenceWriteService absenceWriteService, TenantContextHolder tenantContextHolder) {
        this.absenceWriteService = absenceWriteService;
        this.tenantContextHolder = tenantContextHolder;
    }

    private static Optional<AbsenceWrite> toAbsence(ApplicationEventDtoAdapter event) {

        final List<LocalDate> absentWorkingDays = event.getAbsentWorkingDays().stream().sorted().toList();
        final Optional<DayLength> maybeDayLength = toDayLength(event.getPeriod().getDayLength());
        final Optional<AbsenceTypeCategory> maybeAbsenceTypeCategory = toAbsenceType(event.getVacationType());
        final AbsenceTypeSourceId absenceTypeSourceId = new AbsenceTypeSourceId(event.getVacationType().getSourceId());

        if (absentWorkingDays.isEmpty() || maybeDayLength.isEmpty() || maybeAbsenceTypeCategory.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new AbsenceWrite(
            event.getSourceId(),
            new UserId(event.getPerson().getUsername()),
            event.getPeriod().getStartDate(),
            event.getPeriod().getEndDate(),
            maybeDayLength.get(),
            maybeAbsenceTypeCategory.get(),
            absenceTypeSourceId
        ));
    }

    private static Optional<DayLength> toDayLength(de.focus_shift.urlaubsverwaltung.extension.api.application.DayLength dayLength) {
        return mapToEnum(dayLength.name(), DayLength.class);
    }

    private static Optional<AbsenceTypeCategory> toAbsenceType(VacationTypeDTO vacationType) {
        return mapToEnum(vacationType.getCategory(), AbsenceTypeCategory.class);
    }

    @RabbitListener(queues = {ZEITERFASSUNG_URLAUBSVERWALTUNG_APPLICATION_ALLOWED_QUEUE})
    void on(ApplicationAllowedEventDTO event) {
        tenantContextHolder.runInTenantIdContext(event.getTenantId(), tenantId -> {
            LOG.info("Received ApplicationAllowedEvent id={} for person={} and tenantId={}",
                event.getId(), event.getPerson(), tenantId);
            toAbsence(new ApplicationEventDtoAdapter(event))
                .ifPresentOrElse(
                    absenceWriteService::addAbsence,
                    () -> LOG.info("could not map ApplicationAllowedEvent with id={} to Absence for person={} and tenantId={} -> skip adding Absence", event.getId(), event.getPerson(), tenantId));
        });
    }

    @RabbitListener(queues = {ZEITERFASSUNG_URLAUBSVERWALTUNG_APPLICATION_CREATED_FROM_SICKNOTE_QUEUE})
    void on(ApplicationCreatedFromSickNoteEventDTO event) {
        tenantContextHolder.runInTenantIdContext(event.getTenantId(), tenantId -> {
            LOG.info("Received ApplicationCreatedFromSicknoteEvent for person={} and tenantId={}", event.getPerson(), tenantId);
            toAbsence(new ApplicationEventDtoAdapter(event))
                .ifPresentOrElse(
                    absenceWriteService::addAbsence,
                    () -> LOG.info("could not map ApplicationCreatedFromSicknoteEvent with id={} to Absence for person={} and tenantId={} -> skip adding Absence", event.getId(), event.getPerson(), tenantId));
        });
    }

    @RabbitListener(queues = {ZEITERFASSUNG_URLAUBSVERWALTUNG_APPLICATION_CANCELLED_QUEUE})
    void on(ApplicationCancelledEventDTO event) {
        tenantContextHolder.runInTenantIdContext(event.getTenantId(), tenantId -> {
            LOG.info("Received ApplicationCancelledEvent for person={} and tenantId={}", event.getPerson(), tenantId);
            toAbsence(new ApplicationEventDtoAdapter(event))
                .ifPresentOrElse(
                    absenceWriteService::deleteAbsence,
                    () -> LOG.info("could not map ApplicationCancelledEvent with id={} to Absence for person={} and tenantId={} -> skip adding Absence", event.getId(), event.getPerson(), tenantId));
        });
    }
}
