package de.focusshift.zeiterfassung.absence;

import de.focus_shift.urlaubsverwaltung.extension.api.vacationtype.VacationTypeUpdatedEventDTO;
import de.focusshift.zeiterfassung.RabbitTestConfiguration;
import de.focusshift.zeiterfassung.TestContainersBase;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(
    properties = {
        "zeiterfassung.integration.urlaubsverwaltung.vacationtype.enabled=true",
        "zeiterfassung.integration.urlaubsverwaltung.vacationtype.manage-topology=true",
        "zeiterfassung.integration.urlaubsverwaltung.vacationtype.topic=vacationtype.topic",
        "zeiterfassung.integration.urlaubsverwaltung.vacationtype.routing-key-updated=updated",
    }
)
@Import(RabbitTestConfiguration.class)
@Transactional
class AbsenceTypeIT extends TestContainersBase {

    @Autowired
    private AbsenceTypeService absenceTypeService;

    @Autowired
    private AbsenceTypeRepository absenceTypeRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    void ensureAbsenceTypeCreation() {

        rabbitTemplate.convertAndSend("vacationtype.topic", "updated", VacationTypeUpdatedEventDTO.builder()
            .id(UUID.randomUUID())
            .tenantId("tenant-id")
            .sourceId(42L)
            .category(AbsenceTypeCategory.OTHER.name())
            .requiresApprovalToApply(false)
            .requiresApprovalToCancel(false)
            .color(AbsenceColor.VIOLET.name())
            .visibleToEveryone(true)
            .label(Map.of(
                Locale.GERMAN, "label-de",
                Locale.ENGLISH, "label-en"
            ))
            .build()
        );

        await().untilAsserted(() -> {
            final List<AbsenceTypeEntity> all = absenceTypeRepository.findAll();
            assertThat(all)
                .hasSize(1)
                .first()
                .satisfies(entity -> {
                    assertThat(entity.getId()).isNotNull();
                    assertThat(entity.getTenantId()).isEqualTo("tenant-id");
                    assertThat(entity.getSourceId()).isEqualTo(42);
                    assertThat(entity.getCategory()).isEqualTo(AbsenceTypeCategory.OTHER);
                    assertThat(entity.getColor()).isEqualTo(AbsenceColor.VIOLET);
                    assertThat(entity.getLabelByLocale()).containsExactlyInAnyOrderEntriesOf(Map.of(
                        Locale.GERMAN, "label-de",
                        Locale.ENGLISH, "label-en"
                    ));
                });
        });
    }
}
