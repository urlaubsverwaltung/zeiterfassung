package de.focusshift.zeiterfassung.absence;

import de.focusshift.zeiterfassung.TestContainersBase;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AbsenceTypeIT extends TestContainersBase {

    @Autowired
    private AbsenceTypeService absenceTypeService;

    @Autowired
    private AbsenceTypeRepository absenceTypeRepository;

    @Test
    void ensureAbsenceTypeCreation() {

        absenceTypeService.updateAbsenceType(new AbsenceTypeUpdate(
            new TenantId("tenant"),
            42L,
            AbsenceTypeCategory.OTHER,
            AbsenceColor.VIOLET,
            Map.of(
                Locale.GERMAN, "label-de",
                Locale.ENGLISH, "label-en"
            )
        ));

        final List<AbsenceTypeEntity> all = absenceTypeRepository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0)).satisfies(entity -> {
            assertThat(entity.getId()).isNotNull();
            assertThat(entity.getLabelByLocale()).containsExactlyInAnyOrderEntriesOf(Map.of(
                Locale.GERMAN, "label-de",
                Locale.ENGLISH, "label-en"
            ));
        });
    }
}
