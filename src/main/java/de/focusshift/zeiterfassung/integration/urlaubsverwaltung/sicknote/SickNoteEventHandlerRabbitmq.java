package de.focusshift.zeiterfassung.integration.urlaubsverwaltung.sicknote;

import de.focus_shift.urlaubsverwaltung.extension.api.sicknote.SickNoteCancelledEventDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.sicknote.SickNoteConvertedToApplicationEventDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.sicknote.SickNoteCreatedEventDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.sicknote.SickNoteUpdatedEventDTO;
import de.focusshift.zeiterfassung.absence.AbsenceColor;
import de.focusshift.zeiterfassung.absence.AbsenceType;
import de.focusshift.zeiterfassung.absence.AbsenceWrite;
import de.focusshift.zeiterfassung.absence.AbsenceWriteService;
import de.focusshift.zeiterfassung.absence.DayLength;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import de.focusshift.zeiterfassung.user.UserId;
import org.slf4j.Logger;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static de.focusshift.zeiterfassung.integration.urlaubsverwaltung.sicknote.SickNoteRabbitmqConfiguration.ZEITERFASSUNG_URLAUBSVERWALTUNG_SICKNOTE_CANCELLED_QUEUE;
import static de.focusshift.zeiterfassung.integration.urlaubsverwaltung.sicknote.SickNoteRabbitmqConfiguration.ZEITERFASSUNG_URLAUBSVERWALTUNG_SICKNOTE_CONVERTED_TO_APPLICATION_QUEUE;
import static de.focusshift.zeiterfassung.integration.urlaubsverwaltung.sicknote.SickNoteRabbitmqConfiguration.ZEITERFASSUNG_URLAUBSVERWALTUNG_SICKNOTE_CREATED_QUEUE;
import static de.focusshift.zeiterfassung.integration.urlaubsverwaltung.sicknote.SickNoteRabbitmqConfiguration.ZEITERFASSUNG_URLAUBSVERWALTUNG_SICKNOTE_UPDATED_QUEUE;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

public class SickNoteEventHandlerRabbitmq {
    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final AbsenceWriteService absenceWriteService;

    SickNoteEventHandlerRabbitmq(AbsenceWriteService absenceWriteService) {
        this.absenceWriteService = absenceWriteService;
    }

    @RabbitListener(queues = {ZEITERFASSUNG_URLAUBSVERWALTUNG_SICKNOTE_CANCELLED_QUEUE})
    void on(SickNoteCancelledEventDTO event) {
        LOG.info("Received SickNoteCancelledEvent for person={} and tenantId={}", event.getPerson(), event.getTenantId());
        toAbsence(new SickNoteEventDtoAdapter(event))
            .ifPresentOrElse(
                absenceWriteService::deleteAbsence,
                () -> LOG.info("could not map SickNoteCancelledEvent to Absence -> could not delete Absence")
            );
    }

    @RabbitListener(queues = {ZEITERFASSUNG_URLAUBSVERWALTUNG_SICKNOTE_CREATED_QUEUE})
    void on(SickNoteCreatedEventDTO event) {
        LOG.info("Received SickNoteCreatedEvent for person={} and tenantId={}", event.getPerson(), event.getTenantId());
        toAbsence(new SickNoteEventDtoAdapter(event))
            .ifPresentOrElse(
                absenceWriteService::addAbsence,
                () -> LOG.info("could not map SickNoteCreatedEventDTO to Absence -> could not add Absence")
            );
    }

    @RabbitListener(queues = {ZEITERFASSUNG_URLAUBSVERWALTUNG_SICKNOTE_UPDATED_QUEUE})
    void on(SickNoteUpdatedEventDTO event) {
        LOG.info("Received SickNoteUpdatedEvent for person={} and tenantId={}", event.getPerson(), event.getTenantId());
        toAbsence(new SickNoteEventDtoAdapter(event))
            .ifPresentOrElse(
                absenceWriteService::updateAbsence,
                () -> LOG.info("could not map SickNoteUpdatedEventDTO to Absence -> could not update Absence")
            );
    }

    @RabbitListener(queues = {ZEITERFASSUNG_URLAUBSVERWALTUNG_SICKNOTE_CONVERTED_TO_APPLICATION_QUEUE})
    void on(SickNoteConvertedToApplicationEventDTO event) {
        LOG.info("Received SickNoteConvertedToApplicationEvent for person={} and tenantId={}", event.getPerson(), event.getTenantId());
        toAbsence(new SickNoteEventDtoAdapter(event))
            .ifPresentOrElse(
                absenceWriteService::deleteAbsence,
                () -> LOG.info("could not map SickNoteConvertedToApplicationEventDTO to Absence -> could not convert SickNote to Absence")
            );
    }

    private static Optional<AbsenceWrite> toAbsence(SickNoteEventDtoAdapter event) {

        final Optional<DayLength> maybeDayLength = toDayLength(event.getPeriod().getDayLength());

        if (maybeDayLength.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new AbsenceWrite(
            new TenantId(event.getTenantId()),
            event.getSourceId().longValue(),
            new UserId(event.getPerson().getUsername()),
            event.getPeriod().getStartDate(),
            event.getPeriod().getEndDate(),
            maybeDayLength.get(),
            AbsenceType.SICK,
            AbsenceColor.RED
        ));
    }

    private static Optional<DayLength> toDayLength(de.focus_shift.urlaubsverwaltung.extension.api.sicknote.DayLength dayLength) {
        return map(dayLength.name(), DayLength::valueOf)
            .or(peek(() -> LOG.info("could not map dayLength")));
    }

    private static <R, T> Optional<R> map(T t, Function<T, R> mapper) {
        try {
            return Optional.of(mapper.apply(t));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static <T> Supplier<Optional<T>> peek(Runnable runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                //
            }
            return Optional.empty();
        };
    }
}
