package de.focusshift.zeiterfassung.integration.urlaubsverwaltung.vacationtype;

import de.focus_shift.urlaubsverwaltung.extension.api.vacationtype.VacationTypeUpdatedEventDTO;
import de.focusshift.zeiterfassung.absence.AbsenceColor;
import de.focusshift.zeiterfassung.absence.AbsenceTypeCategory;
import de.focusshift.zeiterfassung.absence.AbsenceTypeService;
import de.focusshift.zeiterfassung.absence.AbsenceTypeUpdate;
import de.focusshift.zeiterfassung.integration.urlaubsverwaltung.RabbitMessageConsumer;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import org.slf4j.Logger;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import java.util.Optional;

import static de.focusshift.zeiterfassung.integration.urlaubsverwaltung.vacationtype.VacationTypeRabbitmqConfiguration.ZEITERFASSUNG_URLAUBSVERWALTUNG_VACATIONTYPE_UPDATED_QUEUE;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

class VacationTypeHandlerRabbitmq extends RabbitMessageConsumer {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final AbsenceTypeService absenceTypeService;
    private final TenantContextHolder tenantContextHolder;

    VacationTypeHandlerRabbitmq(AbsenceTypeService absenceTypeService, TenantContextHolder tenantContextHolder) {
        this.absenceTypeService = absenceTypeService;
        this.tenantContextHolder = tenantContextHolder;
    }

    @RabbitListener(queues = ZEITERFASSUNG_URLAUBSVERWALTUNG_VACATIONTYPE_UPDATED_QUEUE)
    void on(VacationTypeUpdatedEventDTO event) {
        tenantContextHolder.runInTenantIdContext(event.tenantId(), tenantId -> {
            LOG.info("Received VacationTypeUpdatedEvent id={} for tenantId={}", event.id(), event.tenantId());
            toAbsenceTypeUpdate(event)
                .ifPresentOrElse(
                    absenceTypeService::updateAbsenceType,
                    () -> LOG.info("could not map VacationTypeUpdatedEvent -> could not update AbsenceType")
                );
        });
    }

    private Optional<AbsenceTypeUpdate> toAbsenceTypeUpdate(VacationTypeUpdatedEventDTO eventDTO) {
        return toAbsenceColor(eventDTO.color())
            .flatMap(color ->
                toAbsenceTypeCategory(eventDTO.category())
                    .map(category ->
                        new AbsenceTypeUpdate(
                            eventDTO.sourceId(),
                            category,
                            color,
                            eventDTO.label()
                        )
                    )
            );
    }

    private static Optional<AbsenceTypeCategory> toAbsenceTypeCategory(String category) {
        return mapToEnum(category, AbsenceTypeCategory.class);
    }

    private static Optional<AbsenceColor> toAbsenceColor(String color) {
        return mapToEnum(color, AbsenceColor.class);
    }
}
