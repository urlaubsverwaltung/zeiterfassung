package de.focusshift.zeiterfassung.integration.urlaubsverwaltung.vacationtype;

import de.focus_shift.urlaubsverwaltung.extension.api.vacationtype.VacationTypeUpdatedEventDTO;
import de.focusshift.zeiterfassung.absence.AbsenceColor;
import de.focusshift.zeiterfassung.absence.AbsenceTypeCategory;
import de.focusshift.zeiterfassung.absence.AbsenceTypeService;
import de.focusshift.zeiterfassung.absence.AbsenceTypeUpdate;
import de.focusshift.zeiterfassung.integration.urlaubsverwaltung.RabbitMessageConsumer;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import org.slf4j.Logger;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import java.util.Optional;

import static de.focusshift.zeiterfassung.integration.urlaubsverwaltung.vacationtype.VacationTypeRabbitmqConfiguration.ZEITERFASSUNG_URLAUBSVERWALTUNG_VACATIONTYPE_UPDATED_QUEUE;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

class VacationTypeHandlerRabbitmq extends RabbitMessageConsumer {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final AbsenceTypeService absenceTypeService;

    VacationTypeHandlerRabbitmq(AbsenceTypeService absenceTypeService) {
        this.absenceTypeService = absenceTypeService;
    }

    @RabbitListener(queues = ZEITERFASSUNG_URLAUBSVERWALTUNG_VACATIONTYPE_UPDATED_QUEUE)
    void on(VacationTypeUpdatedEventDTO event) {

        LOG.info("Received VacationTypeUpdatedEvent id={} for tenantId={}", event.getId(), event.getTenantId());

        toAbsenceTypeUpdate(event)
            .ifPresentOrElse(
                absenceTypeService::updateAbsenceType,
                () -> LOG.info("could not map VacationTypeUpdatedEvent -> could not update AbsenceType")
            );
    }

    private Optional<AbsenceTypeUpdate> toAbsenceTypeUpdate(VacationTypeUpdatedEventDTO eventDTO) {
        return toAbsenceColor(eventDTO.getColor())
            .flatMap(color ->
                toAbsenceTypeCategory(eventDTO.getCategory())
                    .map(category ->
                        new AbsenceTypeUpdate(
                            new TenantId(eventDTO.getTenantId()),
                            eventDTO.getSourceId(),
                            category,
                            color,
                            eventDTO.getLabel()
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
