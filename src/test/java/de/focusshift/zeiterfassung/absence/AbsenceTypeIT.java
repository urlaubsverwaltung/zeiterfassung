package de.focusshift.zeiterfassung.absence;

import de.focus_shift.urlaubsverwaltung.extension.api.vacationtype.VacationTypeUpdatedEventDTO;
import de.focusshift.zeiterfassung.RabbitTestConfiguration;
import de.focusshift.zeiterfassung.SingleTenantTestContainersBase;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
class AbsenceTypeIT extends SingleTenantTestContainersBase {

    @Autowired
    private AbsenceTypeService absenceTypeService;

    @Autowired
    private AbsenceTypeRepository absenceTypeRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @MockBean(answer = Answers.CALLS_REAL_METHODS)
    private TenantContextHolder tenantContextHolder;

    @Test
    void ensureAbsenceTypeCreation() {

        final TenantId tenantId = new TenantId("default");
        when(tenantContextHolder.getCurrentTenantId()).thenReturn(Optional.of(tenantId));

        rabbitTemplate.convertAndSend("vacationtype.topic", "updated", VacationTypeUpdatedEventDTO.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId.tenantId())
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
            final Iterable<AbsenceTypeEntity> all = absenceTypeRepository.findAll();
            assertThat(all)
                .hasSize(1)
                .first()
                .satisfies(entity -> {
                    assertThat(entity.getId()).isNotNull();
                    assertThat(entity.getTenantId()).isEqualTo(tenantId.tenantId());
                    assertThat(entity.getSourceId()).isEqualTo(42);
                    assertThat(entity.getCategory()).isEqualTo(AbsenceTypeCategory.OTHER);
                    assertThat(entity.getColor()).isEqualTo(AbsenceColor.VIOLET);
                    assertThat(entity.getLabelByLocale()).containsExactlyInAnyOrderEntriesOf(Map.of(
                        Locale.GERMAN, "label-de",
                        Locale.ENGLISH, "label-en"
                    ));
                });
        });

        final InOrder inOrder = Mockito.inOrder(tenantContextHolder);
        inOrder.verify(tenantContextHolder).setTenantId(tenantId);
        inOrder.verify(tenantContextHolder).clear();

        verify(tenantContextHolder).getCurrentTenantId();
    }
}
