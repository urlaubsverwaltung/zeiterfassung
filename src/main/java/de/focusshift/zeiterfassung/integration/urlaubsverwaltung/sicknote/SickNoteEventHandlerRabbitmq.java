package de.focusshift.zeiterfassung.integration.urlaubsverwaltung.sicknote;

import de.focus_shift.urlaubsverwaltung.extension.api.sicknote.SickNoteCancelledEventDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.sicknote.SickNoteCreatedEventDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.sicknote.SickNoteUpdatedEventDTO;
import org.slf4j.Logger;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import static de.focusshift.zeiterfassung.integration.urlaubsverwaltung.sicknote.SickNoteRabbitmqConfiguration.ZEITERFASSUNG_URLAUBSVERWALTUNG_SICKNOTE_CANCELLED_QUEUE;
import static de.focusshift.zeiterfassung.integration.urlaubsverwaltung.sicknote.SickNoteRabbitmqConfiguration.ZEITERFASSUNG_URLAUBSVERWALTUNG_SICKNOTE_CREATED_QUEUE;
import static de.focusshift.zeiterfassung.integration.urlaubsverwaltung.sicknote.SickNoteRabbitmqConfiguration.ZEITERFASSUNG_URLAUBSVERWALTUNG_SICKNOTE_UPDATED_QUEUE;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

public class SickNoteEventHandlerRabbitmq {
    private static final Logger LOG = getLogger(lookup().lookupClass());

    @RabbitListener(queues = {ZEITERFASSUNG_URLAUBSVERWALTUNG_SICKNOTE_CANCELLED_QUEUE})
    void on(SickNoteCancelledEventDTO event) {
        LOG.info("Received SickNoteCancelledEvent for person={} and tenantId={}", event.getPerson(), event.getTenantId());
    }

    @RabbitListener(queues = {ZEITERFASSUNG_URLAUBSVERWALTUNG_SICKNOTE_CREATED_QUEUE})
    void on(SickNoteCreatedEventDTO event) {
        LOG.info("Received SickNoteCreatedEvent for person={} and tenantId={}", event.getPerson(), event.getTenantId());
    }

    @RabbitListener(queues = {ZEITERFASSUNG_URLAUBSVERWALTUNG_SICKNOTE_UPDATED_QUEUE})
    void on(SickNoteUpdatedEventDTO event) {
        LOG.info("Received SickNoteUpdatedEvent for person={} and tenantId={}", event.getPerson(), event.getTenantId());
    }
}
