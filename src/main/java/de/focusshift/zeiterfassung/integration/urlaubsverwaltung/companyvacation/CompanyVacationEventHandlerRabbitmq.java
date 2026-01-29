package de.focusshift.zeiterfassung.integration.urlaubsverwaltung.companyvacation;

import de.focus_shift.urlaubsverwaltung.extension.api.companyvacation.CompanyVacationDeletedEventDto;
import de.focus_shift.urlaubsverwaltung.extension.api.companyvacation.CompanyVacationPublishedEventDto;
import de.focusshift.zeiterfassung.companyvacation.CompanyVacationWrite;
import de.focusshift.zeiterfassung.companyvacation.CompanyVacationWriteService;
import de.focusshift.zeiterfassung.companyvacation.DayLength;
import de.focusshift.zeiterfassung.integration.urlaubsverwaltung.RabbitMessageConsumer;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import org.slf4j.Logger;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import java.util.Optional;

import static de.focusshift.zeiterfassung.integration.urlaubsverwaltung.companyvacation.CompanyVacationRabbitmqConfiguration.ZEITERFASSUNG_URLAUBSVERWALTUNG_COMPANY_VACATION_DELETED_QUEUE;
import static de.focusshift.zeiterfassung.integration.urlaubsverwaltung.companyvacation.CompanyVacationRabbitmqConfiguration.ZEITERFASSUNG_URLAUBSVERWALTUNG_COMPANY_VACATION_PUBLISHED_QUEUE;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

public class CompanyVacationEventHandlerRabbitmq extends RabbitMessageConsumer {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final CompanyVacationWriteService companyVacationWriteService;
    private final TenantContextHolder tenantContextHolder;

    CompanyVacationEventHandlerRabbitmq(
        CompanyVacationWriteService companyVacationWriteService,
        TenantContextHolder tenantContextHolder
    ) {
        this.companyVacationWriteService = companyVacationWriteService;
        this.tenantContextHolder = tenantContextHolder;
    }

    /**
     * Creates or updates the company vacation by {@code sourceId} within the event's tenant context.
     * <p>
     * Example event:
     * {
     *   "id": "8f1b2e2b-3c7a-4d90-b1d7-4b8a9f6e2c3a",
     *   "sourceId": "settings-new-years-eve",
     *   "createdAt": "2025-12-05T10:15:30Z",
     *   "tenantId": "foobar",
     *   "period": {
     *     "startDate": "2025-12-30T23:00:00Z",
     *     "endDate": "2025-12-30T23:00:00Z",
     *     "dayLength": "NOON"
     *   }
     * }
     */
    @RabbitListener(queues = {ZEITERFASSUNG_URLAUBSVERWALTUNG_COMPANY_VACATION_PUBLISHED_QUEUE})
    void on(CompanyVacationPublishedEventDto event) {
        tenantContextHolder.runInTenantIdContext(event.tenantId(), tenantId -> {
            LOG.info("Received CompanyVacationPublishedEvent id={} for tenantId={} and sourceId={}",
                event.id(), tenantId, event.sourceId());
            toCompanyVacation(event)
                .ifPresentOrElse(
                    companyVacationWriteService::addOrUpdateCompanyVacation,
                    () -> LOG.info("could not map CompanyVacationPublishedEvent with id={} to company vacation for tenantId={} -> skip adding company vacation", event.id(), tenantId));
        });
    }

    /**
     * Deletes the company vacation by {@code sourceId} within the event's tenant context.
     * <p>
     * Example event:
     * {
     *   "id": "8f1b2e2b-3c7a-4d90-b1d7-4b8a9f6e2c3a",
     *   "sourceId": "settings-new-years-eve",
     *   "deletedAt": "2025-12-05T10:15:30Z",
     *   "tenantId": "foobar"
     * }
     * <p>
     * @param event deletion event of the company vacation
     */
    @RabbitListener(queues = {ZEITERFASSUNG_URLAUBSVERWALTUNG_COMPANY_VACATION_DELETED_QUEUE})
    void on(CompanyVacationDeletedEventDto event) {
        tenantContextHolder.runInTenantIdContext(event.tenantId(), tenantId -> {
            LOG.info("Received CompanyVacationDeletedEventDto id={} for tenantId={} and sourceId={}",
                event.id(), tenantId, event.sourceId());
            companyVacationWriteService.deleteCompanyVacation(event.createdAt(), event.sourceId());
        });
    }

    private static Optional<CompanyVacationWrite> toCompanyVacation(CompanyVacationPublishedEventDto event) {

        final Optional<DayLength> maybeDayLength = toDayLength(event.period().dayLength());

        return maybeDayLength.map(dayLength -> new CompanyVacationWrite(
            event.sourceId(),
            event.period().startDate(),
            event.period().endDate(),
            dayLength
        ));

    }

    private static Optional<DayLength> toDayLength(de.focus_shift.urlaubsverwaltung.extension.api.companyvacation.DayLength dayLength) {
        return mapToEnum(dayLength.name(), DayLength.class);
    }
}
