package de.focusshift.zeiterfassung.integration.urlaubsverwaltung.application;

import de.focus_shift.urlaubsverwaltung.extension.api.application.ApplicationAllowedEventDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.application.ApplicationCancelledEventDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.application.ApplicationDeletedEventDTO;
import org.slf4j.Logger;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import static de.focusshift.zeiterfassung.integration.urlaubsverwaltung.application.ApplicationRabbitmqConfiguration.ZEITERFASSUNG_URLAUBSVERWALTUNG_APPLICATION_ALLOWED_QUEUE;
import static de.focusshift.zeiterfassung.integration.urlaubsverwaltung.application.ApplicationRabbitmqConfiguration.ZEITERFASSUNG_URLAUBSVERWALTUNG_APPLICATION_CANCELLED_QUEUE;
import static de.focusshift.zeiterfassung.integration.urlaubsverwaltung.application.ApplicationRabbitmqConfiguration.ZEITERFASSUNG_URLAUBSVERWALTUNG_APPLICATION_DELETED_QUEUE;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

public class ApplicationEventHandlerRabbitmq {
    private static final Logger LOG = getLogger(lookup().lookupClass());

    @RabbitListener(queues = {ZEITERFASSUNG_URLAUBSVERWALTUNG_APPLICATION_ALLOWED_QUEUE})
    void on(ApplicationAllowedEventDTO event) {
        LOG.info("Received ApplicationAllowedEvent for person={} and tenantId={}", event.getPerson(), event.getTenantId());
    }

    @RabbitListener(queues = {ZEITERFASSUNG_URLAUBSVERWALTUNG_APPLICATION_CANCELLED_QUEUE})
    void on(ApplicationCancelledEventDTO event) {
        LOG.info("Received ApplicationCancelledEvent for person={} and tenantId={}", event.getPerson(), event.getTenantId());
    }

    @RabbitListener(queues = {ZEITERFASSUNG_URLAUBSVERWALTUNG_APPLICATION_DELETED_QUEUE})
    void on(ApplicationDeletedEventDTO event) {
        LOG.info("Received ApplicationDeletedEvent for person={} and tenantId={}", event.getPerson(), event.getTenantId());
    }
}
