package de.focusshift.zeiterfassung.integration.urlaubsverwaltung.application;

import de.focus_shift.urlaubsverwaltung.extension.api.application.ApplicationAllowedEventDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.application.ApplicationCancelledEventDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.application.ApplicationCreatedFromSickNoteEventDTO;
import de.focusshift.zeiterfassung.absence.AbsenceColor;
import de.focusshift.zeiterfassung.absence.AbsenceType;
import de.focusshift.zeiterfassung.absence.AbsenceTypeCategory;
import de.focusshift.zeiterfassung.absence.AbsenceWrite;
import de.focusshift.zeiterfassung.absence.AbsenceWriteService;
import de.focusshift.zeiterfassung.absence.DayLength;
import de.focusshift.zeiterfassung.integration.urlaubsverwaltung.RabbitMessageConsumer;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
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

    ApplicationEventHandlerRabbitmq(AbsenceWriteService absenceWriteService) {
        this.absenceWriteService = absenceWriteService;
    }

    @RabbitListener(queues = {ZEITERFASSUNG_URLAUBSVERWALTUNG_APPLICATION_ALLOWED_QUEUE})
    void on(ApplicationAllowedEventDTO event) {

        LOG.info("Received ApplicationAllowedEvent id={} for person={} and tenantId={}",
            event.getId(), event.getPerson(), event.getTenantId());

        toAbsence(new ApplicationEventDtoAdapter(event))
            .ifPresentOrElse(
                absenceWriteService::addAbsence,
                () -> LOG.info("could not map ApplicationAllowedEvent with id={} to Absence for person={} and tenantId={} -> skip adding Absence", event.getId(), event.getPerson(), event.getTenantId()));
    }

    @RabbitListener(queues = {ZEITERFASSUNG_URLAUBSVERWALTUNG_APPLICATION_CREATED_FROM_SICKNOTE_QUEUE})
    void on(ApplicationCreatedFromSickNoteEventDTO event) {
        LOG.info("Received ApplicationCreatedFromSicknoteEvent for person={} and tenantId={}", event.getPerson(), event.getTenantId());
        toAbsence(new ApplicationEventDtoAdapter(event))
            .ifPresentOrElse(
                absenceWriteService::addAbsence,
                () -> LOG.info("could not map ApplicationCreatedFromSicknoteEvent with id={} to Absence for person={} and tenantId={} -> skip adding Absence", event.getId(), event.getPerson(), event.getTenantId()));
    }

    @RabbitListener(queues = {ZEITERFASSUNG_URLAUBSVERWALTUNG_APPLICATION_CANCELLED_QUEUE})
    void on(ApplicationCancelledEventDTO event) {
        LOG.info("Received ApplicationCancelledEvent for person={} and tenantId={}", event.getPerson(), event.getTenantId());
        toAbsence(new ApplicationEventDtoAdapter(event))
            .ifPresentOrElse(
                absenceWriteService::deleteAbsence,
                () -> LOG.info("could not map ApplicationCancelledEvent with id={} to Absence for person={} and tenantId={} -> skip adding Absence", event.getId(), event.getPerson(), event.getTenantId()));
    }

    private static Optional<AbsenceWrite> toAbsence(ApplicationEventDtoAdapter event) {

        final List<LocalDate> absentWorkingDays = event.getAbsentWorkingDays().stream().sorted().toList();
        final Optional<DayLength> maybeDayLength = toDayLength(event.getPeriod().getDayLength());
        final Optional<AbsenceType> maybeAbsenceType = toAbsenceType(event.getVacationType().getCategory(), event.getVacationType().getSourceId());
        final Optional<AbsenceColor> maybeAbsenceColor = toAbsenceColor(event.getVacationType().getColor());

        if (absentWorkingDays.isEmpty() || maybeDayLength.isEmpty() || maybeAbsenceType.isEmpty() || maybeAbsenceColor.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new AbsenceWrite(
            new TenantId(event.getTenantId()),
            event.getSourceId(),
            new UserId(event.getPerson().getUsername()),
            event.getPeriod().getStartDate(),
            event.getPeriod().getEndDate(),
            maybeDayLength.get(),
            maybeAbsenceType.get(),
            maybeAbsenceColor.get()
        ));
    }

    private static Optional<DayLength> toDayLength(de.focus_shift.urlaubsverwaltung.extension.api.application.DayLength dayLength) {
        return mapToEnum(dayLength.name(), DayLength::valueOf, () -> "could not map dayLength");
    }

    private static Optional<AbsenceType> toAbsenceType(String absenceTypeCategoryName, Long sourceId) {
        return mapToEnum(absenceTypeCategoryName, AbsenceTypeCategory::valueOf, () -> "could not map vacationTypeCategory to AbsenceType")
            .map(category -> new AbsenceType(category, sourceId));
    }

    private static Optional<AbsenceColor> toAbsenceColor(String vacationTypeColor) {
        return mapToEnum(vacationTypeColor, AbsenceColor::valueOf, () -> "could not map vacationTypeColor to AbsenceColor");
    }
}
