package de.focusshift.zeiterfassung.integration.urlaubsverwaltung.vacationtype;

import de.focus_shift.urlaubsverwaltung.extension.api.vacationtype.VacationTypeUpdatedEventDTO;
import de.focusshift.zeiterfassung.ArgumentsPermutation;
import de.focusshift.zeiterfassung.absence.AbsenceColor;
import de.focusshift.zeiterfassung.absence.AbsenceTypeCategory;
import de.focusshift.zeiterfassung.absence.AbsenceTypeService;
import de.focusshift.zeiterfassung.absence.AbsenceTypeUpdate;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class VacationTypeHandlerRabbitmqTest {

    @InjectMocks
    private VacationTypeHandlerRabbitmq sut;

    @Mock
    private AbsenceTypeService absenceTypeService;

    @Test
    void ensureAbsenceTypeUpdateIsIgnoredWhenCategoryCannotBeMapped() {

        final VacationTypeUpdatedEventDTO eventDto = VacationTypeUpdatedEventDTO.builder()
            .id(UUID.randomUUID())
            .tenantId("tenant")
            .sourceId(42L)
            .category("unknown")
            .color("VIOLET")
            .label(Map.of())
            .build();

        sut.on(eventDto);
        verifyNoInteractions(absenceTypeService);
    }

    @Test
    void ensureAbsenceTypeUpdateIsIgnoredWhenColorCannotBeMapped() {

        final VacationTypeUpdatedEventDTO eventDto = VacationTypeUpdatedEventDTO.builder()
            .id(UUID.randomUUID())
            .tenantId("tenant")
            .sourceId(42L)
            .category("OTHER")
            .color("unknown")
            .label(Map.of())
            .build();

        sut.on(eventDto);
        verifyNoInteractions(absenceTypeService);
    }

    static Stream<Arguments> categoryColorPermutation() {
        return ArgumentsPermutation.of(AbsenceTypeCategory.values(), AbsenceColor.values());
    }

    @ParameterizedTest
    @MethodSource("categoryColorPermutation")
    void ensureAbsenceTypeUpdateImpl(AbsenceTypeCategory category, AbsenceColor color) {

        final Map<Locale, String> labels = Map.of(
            Locale.GERMAN, "label-de",
            Locale.ENGLISH, "label-en"
        );

        final VacationTypeUpdatedEventDTO eventDto = VacationTypeUpdatedEventDTO.builder()
            .id(UUID.randomUUID())
            .tenantId("tenant")
            .sourceId(42L)
            .category(category.name())
            .color(color.name())
            .label(labels)
            .build();

        sut.on(eventDto);

        verify(absenceTypeService).updateAbsenceType(new AbsenceTypeUpdate(
            new TenantId("tenant"),
            42L,
            category,
            color,
            labels
        ));
    }
}
