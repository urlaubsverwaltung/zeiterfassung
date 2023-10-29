package de.focusshift.zeiterfassung.integration.urlaubsverwaltung.vacationtype;

import de.focus_shift.urlaubsverwaltung.extension.api.vacationtype.VacationTypeUpdatedEventDTO;
import de.focusshift.zeiterfassung.absence.AbsenceColor;
import de.focusshift.zeiterfassung.absence.AbsenceTypeCategory;
import de.focusshift.zeiterfassung.absence.AbsenceTypeService;
import de.focusshift.zeiterfassung.absence.AbsenceTypeUpdate;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import org.slf4j.Logger;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static de.focusshift.zeiterfassung.integration.urlaubsverwaltung.vacationtype.VacationTypeRabbitmqConfiguration.ZEITERFASSUNG_URLAUBSVERWALTUNG_VACATIONTYPE_UPDATED_QUEUE;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

class VacationTypeHandlerRabbitmq {

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

    private Optional<AbsenceTypeCategory> toAbsenceTypeCategory(String category) {
        return map(category, AbsenceTypeCategory::valueOf)
            .or(peek(() -> LOG.info("could not map category={} to AbsenceTypeCategory", category)));
    }

    private static Optional<AbsenceColor> toAbsenceColor(String color) {
        return map(color, AbsenceColor::valueOf)
            .or(peek(() -> LOG.info("could not map color={} to AbsenceColor", color)));
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
