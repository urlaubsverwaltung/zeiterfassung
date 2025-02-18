package de.focusshift.zeiterfassung.integration.urlaubsverwaltung.sicknote;

import de.focus_shift.urlaubsverwaltung.extension.api.sicknote.SickNoteAcceptedEventDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.sicknote.SickNoteCancelledEventDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.sicknote.SickNoteConvertedToApplicationEventDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.sicknote.SickNoteCreatedEventDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.sicknote.SickNoteUpdatedEventDTO;
import de.focusshift.zeiterfassung.absence.AbsenceTypeCategory;
import de.focusshift.zeiterfassung.absence.AbsenceWrite;
import de.focusshift.zeiterfassung.absence.AbsenceWriteService;
import de.focusshift.zeiterfassung.absence.DayLength;
import de.focusshift.zeiterfassung.integration.urlaubsverwaltung.RabbitMessageConsumer;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import de.focusshift.zeiterfassung.user.UserId;
import org.slf4j.Logger;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import java.util.Optional;

import static de.focusshift.zeiterfassung.integration.urlaubsverwaltung.sicknote.SickNoteRabbitmqConfiguration.ZEITERFASSUNG_URLAUBSVERWALTUNG_SICKNOTE_ACCEPTED_QUEUE;
import static de.focusshift.zeiterfassung.integration.urlaubsverwaltung.sicknote.SickNoteRabbitmqConfiguration.ZEITERFASSUNG_URLAUBSVERWALTUNG_SICKNOTE_CANCELLED_QUEUE;
import static de.focusshift.zeiterfassung.integration.urlaubsverwaltung.sicknote.SickNoteRabbitmqConfiguration.ZEITERFASSUNG_URLAUBSVERWALTUNG_SICKNOTE_CONVERTED_TO_APPLICATION_QUEUE;
import static de.focusshift.zeiterfassung.integration.urlaubsverwaltung.sicknote.SickNoteRabbitmqConfiguration.ZEITERFASSUNG_URLAUBSVERWALTUNG_SICKNOTE_CREATED_QUEUE;
import static de.focusshift.zeiterfassung.integration.urlaubsverwaltung.sicknote.SickNoteRabbitmqConfiguration.ZEITERFASSUNG_URLAUBSVERWALTUNG_SICKNOTE_UPDATED_QUEUE;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

public class SickNoteEventHandlerRabbitmq extends RabbitMessageConsumer {
    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final AbsenceWriteService absenceWriteService;
    private final TenantContextHolder tenantContextHolder;

    SickNoteEventHandlerRabbitmq(AbsenceWriteService absenceWriteService, TenantContextHolder tenantContextHolder) {
        this.absenceWriteService = absenceWriteService;
        this.tenantContextHolder = tenantContextHolder;
    }

    @RabbitListener(queues = {ZEITERFASSUNG_URLAUBSVERWALTUNG_SICKNOTE_CANCELLED_QUEUE})
    void on(SickNoteCancelledEventDTO event) {
        tenantContextHolder.runInTenantIdContext(new TenantId(event.getTenantId()), tenantId -> {
            LOG.info("Received SickNoteCancelledEvent for person={} and tenantId={}", event.getPerson(), tenantId);
            toAbsence(new SickNoteEventDtoAdapter(event))
                .ifPresentOrElse(
                    absenceWriteService::deleteAbsence,
                    () -> LOG.info("could not map SickNoteCancelledEvent to Absence -> could not delete Absence")
                );
        });
    }

    @RabbitListener(queues = {ZEITERFASSUNG_URLAUBSVERWALTUNG_SICKNOTE_CREATED_QUEUE})
    void on(SickNoteCreatedEventDTO event) {
        tenantContextHolder.runInTenantIdContext(new TenantId(event.getTenantId()), tenantId -> {
            LOG.info("Received SickNoteCreatedEvent for person={} and tenantId={}", event.getPerson(), tenantId);
            toAbsence(new SickNoteEventDtoAdapter(event))
                .ifPresentOrElse(
                    absenceWriteService::addAbsence,
                    () -> LOG.info("could not map SickNoteCreatedEventDTO to Absence -> could not add Absence")
                );
        });
    }

    @RabbitListener(queues = {ZEITERFASSUNG_URLAUBSVERWALTUNG_SICKNOTE_UPDATED_QUEUE})
    void on(SickNoteUpdatedEventDTO event) {
        tenantContextHolder.runInTenantIdContext(event.getTenantId(), tenantId -> {
            LOG.info("Received SickNoteUpdatedEvent for person={} and tenantId={}", event.getPerson(), tenantId);
            toAbsence(new SickNoteEventDtoAdapter(event))
                .ifPresentOrElse(
                    absenceWriteService::updateAbsence,
                    () -> LOG.info("could not map SickNoteUpdatedEventDTO to Absence -> could not update Absence")
                );
        });
    }

    @RabbitListener(queues = {ZEITERFASSUNG_URLAUBSVERWALTUNG_SICKNOTE_CONVERTED_TO_APPLICATION_QUEUE})
    void on(SickNoteConvertedToApplicationEventDTO event) {
        tenantContextHolder.runInTenantIdContext(new TenantId(event.getTenantId()), tenantId -> {
            LOG.info("Received SickNoteConvertedToApplicationEvent for person={} and tenantId={}", event.getPerson(), tenantId);
            toAbsence(new SickNoteEventDtoAdapter(event))
                .ifPresentOrElse(
                    absenceWriteService::deleteAbsence,
                    () -> LOG.info("could not map SickNoteConvertedToApplicationEventDTO to Absence -> could not convert SickNote to Absence")
                );

        });
    }

    @RabbitListener(queues = {ZEITERFASSUNG_URLAUBSVERWALTUNG_SICKNOTE_ACCEPTED_QUEUE})
    void on(SickNoteAcceptedEventDTO event) {
        tenantContextHolder.runInTenantIdContext(new TenantId(event.getTenantId()), tenantId -> {
            LOG.info("Received SickNoteAcceptedEvent for person={} and tenantId={}", event.getPerson(), tenantId);
            toAbsence(new SickNoteEventDtoAdapter(event))
                .ifPresentOrElse(
                    absenceWriteService::addAbsence,
                    () -> LOG.info("could not map SickNoteAcceptedEventDTO to Absence -> could not convert SickNote to Absence")
                );

        });
    }

    private static Optional<AbsenceWrite> toAbsence(SickNoteEventDtoAdapter event) {
        return toDayLength(event.getPeriod().getDayLength())
            .map(dayLength -> new AbsenceWrite(
                event.getSourceId(),
                new UserId(event.getPerson().getUsername()),
                event.getPeriod().getStartDate(),
                event.getPeriod().getEndDate(),
                dayLength,
                AbsenceTypeCategory.SICK
            ));
    }

    private static Optional<DayLength> toDayLength(de.focus_shift.urlaubsverwaltung.extension.api.sicknote.DayLength dayLength) {
        return mapToEnum(dayLength.name(), DayLength.class);
    }
}
