package de.focusshift.zeiterfassung.integration.urlaubsverwaltung.companyvacation;

import de.focus_shift.urlaubsverwaltung.extension.api.companyvacation.CompanyVacationDeletedEventDto;
import de.focus_shift.urlaubsverwaltung.extension.api.companyvacation.CompanyVacationPeriodDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.companyvacation.CompanyVacationPublishedEventDto;
import de.focus_shift.urlaubsverwaltung.extension.api.companyvacation.DayLength;
import de.focusshift.zeiterfassung.RabbitTestConfiguration;
import de.focusshift.zeiterfassung.SingleTenantTestContainersBase;
import de.focusshift.zeiterfassung.absence.Absence;
import de.focusshift.zeiterfassung.absence.AbsenceService;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import static de.focusshift.zeiterfassung.absence.AbsenceColor.YELLOW;
import static de.focusshift.zeiterfassung.absence.AbsenceTypeCategory.HOLIDAY;
import static de.focusshift.zeiterfassung.absence.DayLength.FULL;
import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;

@SpringBootTest(
    properties = {
        "zeiterfassung.tenant.mode=single",
        "zeiterfassung.tenant.single.default-tenant-id=" + CompanyVacationEventHandlerRabbitmqIT.TENANT_ID,
        "zeiterfassung.integration.urlaubsverwaltung.companyvacation.enabled=true",
        "zeiterfassung.integration.urlaubsverwaltung.companyvacation.manage-topology=true",
        "zeiterfassung.integration.urlaubsverwaltung.companyvacation.topic=" + CompanyVacationEventHandlerRabbitmqIT.TOPIC,
        "zeiterfassung.integration.urlaubsverwaltung.companyvacation.routing-key-published=" + CompanyVacationEventHandlerRabbitmqIT.PUBLISHED_ROUTING_KEY,
        "zeiterfassung.integration.urlaubsverwaltung.companyvacation.routing-key-deleted=" + CompanyVacationEventHandlerRabbitmqIT.DELETED_ROUTING_KEY,
    }
)
@Import(RabbitTestConfiguration.class)
class CompanyVacationEventHandlerRabbitmqIT extends SingleTenantTestContainersBase {

    protected static final String TENANT_ID = "tenant";
    protected static final String TOPIC = "topic";
    protected static final String PUBLISHED_ROUTING_KEY = "published";
    protected static final String DELETED_ROUTING_KEY = "deleted";

    @MockitoBean(answers = Answers.CALLS_REAL_METHODS)
    private TenantContextHolder tenantContextHolder;
    @MockitoBean
    private UserManagementService userManagementService;

    @Autowired
    private AbsenceService absenceService;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    void ensureCompanyVacationPublishedAndDeleted() {

        final LocalDate now = LocalDate.now();
        final Instant startDate = now.atStartOfDay().toInstant(UTC);
        final Instant endDate = now.plusDays(1).atStartOfDay().toInstant(UTC);

        final TenantId tenantId = new TenantId(TENANT_ID);
        when(tenantContextHolder.getCurrentTenantId()).thenReturn(Optional.of(tenantId));

        final UserId userId = new UserId("boss");
        final UserIdComposite userIdComposite = new UserIdComposite(userId, new UserLocalId(1L));
        final User user = new User(userIdComposite, "Hans", "Dampf", new EMailAddress("hans.dampf@example.org"), Set.of());
        when(userManagementService.findAllUsers()).thenReturn(List.of(user));

        // PUBLISH company vacation absence
        rabbitTemplate.convertAndSend(TOPIC, PUBLISHED_ROUTING_KEY, CompanyVacationPublishedEventDto.builder()
            .createdAt(Instant.now())
            .id(UUID.randomUUID())
            .sourceId("1L")
            .tenantId(TENANT_ID)
            .period(CompanyVacationPeriodDTO.builder().startDate(startDate).endDate(endDate).dayLength(DayLength.FULL).build())
            .build());

        final Function<Locale, String> anyLabel = locale -> "";

        await().untilAsserted(() -> {
            final Absence expected = new Absence(userId, startDate, endDate, FULL, anyLabel, YELLOW, HOLIDAY);
            final Map<UserIdComposite, List<Absence>> absences = absenceService.getAbsencesForAllUsers(LocalDate.ofInstant(startDate, UTC), LocalDate.ofInstant(endDate, UTC));
            assertThat(absences)
                .hasSize(1)
                .containsEntry(userIdComposite, List.of(expected));

            final InOrder inOrder = Mockito.inOrder(tenantContextHolder);
            inOrder.verify(tenantContextHolder).setTenantId(tenantId);
            inOrder.verify(tenantContextHolder).clear();
        });

        // DELETE company vacation absence
        rabbitTemplate.convertAndSend(TOPIC, DELETED_ROUTING_KEY, CompanyVacationDeletedEventDto.builder()
            .deletedAt(Instant.now())
            .id(UUID.randomUUID())
            .sourceId("1L")
            .tenantId(TENANT_ID)
            .build());

        await().untilAsserted(() -> {
            final Map<UserIdComposite, List<Absence>> absences = absenceService.getAbsencesForAllUsers(LocalDate.ofInstant(startDate, UTC), LocalDate.ofInstant(endDate, UTC));
            assertThat(absences)
                .hasSize(1)
                .containsEntry(userIdComposite, List.of());

            final InOrder inOrder = Mockito.inOrder(tenantContextHolder);
            inOrder.verify(tenantContextHolder).setTenantId(tenantId);
            inOrder.verify(tenantContextHolder).clear();
        });
    }
}
