package de.focusshift.zeiterfassung.absence;

import de.focus_shift.urlaubsverwaltung.extension.api.application.ApplicationAllowedEventDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.application.ApplicationPeriodDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.application.ApplicationPersonDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.application.VacationTypeDTO;
import de.focusshift.zeiterfassung.RabbitTestConfiguration;
import de.focusshift.zeiterfassung.SingleTenantTestContainersBase;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import static de.focus_shift.urlaubsverwaltung.extension.api.application.DayLength.FULL;
import static de.focusshift.zeiterfassung.absence.AbsenceTypeCategory.HOLIDAY;
import static java.time.ZoneOffset.UTC;
import static java.util.Locale.ENGLISH;
import static java.util.Locale.GERMAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(
    properties = {
        "zeiterfassung.tenant.mode=single",
        "zeiterfassung.tenant.single.default-tenant-id=" + AbsenceIT.TENANT_ID,
        "zeiterfassung.integration.urlaubsverwaltung.application.enabled=true",
        "zeiterfassung.integration.urlaubsverwaltung.application.manage-topology=true",
        "zeiterfassung.integration.urlaubsverwaltung.application.topic=" + AbsenceIT.TOPIC,
        "zeiterfassung.integration.urlaubsverwaltung.application.routing-key-allowed=" + AbsenceIT.ALLOWED_ROUTING_KEY,
    }
)
@Import(RabbitTestConfiguration.class)
class AbsenceIT extends SingleTenantTestContainersBase {

    protected static final String TENANT_ID = "default";
    protected static final String TOPIC = "topic";
    protected static final String ALLOWED_ROUTING_KEY = "allowed";

    private static final ZoneOffset ZONE_ID = UTC;

    @MockitoBean
    private UserSettingsProvider userSettingsProvider;
    @MockitoBean(answers = Answers.CALLS_REAL_METHODS)
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private AbsenceService absenceService;
    @Autowired
    private AbsenceTypeService absenceTypeService;
    @Autowired
    private AbsenceRepository absenceRepository;
    @Autowired
    private AbsenceTypeRepository absenceTypeRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void setUp() {
        when(userSettingsProvider.zoneId()).thenReturn(ZONE_ID);
        when(tenantContextHolder.getCurrentTenantId()).thenReturn(Optional.of(new TenantId(TENANT_ID)));
    }

    @AfterEach
    void tearDown() {
        absenceRepository.deleteAll();
        absenceTypeRepository.deleteAll();
    }

    @Test
    void ensureAbsenceWithAbsenceTypeInformation() {

        final LocalDate now = LocalDate.now(ZONE_ID);
        final Instant startOfDay = now.atStartOfDay().toInstant(ZONE_ID);
        final Instant startOfNextDay = now.plusDays(1).atStartOfDay().toInstant(ZONE_ID);

        final long erholungsUrlaubSourceId = 1000L;
        final AbsenceColor erholungsUrlaubColor = AbsenceColor.CYAN;
        final Map<Locale, String> erholungsurlaubLabels = Map.of(GERMAN, "label-de", ENGLISH, "label-en");
        final AbsenceTypeCategory erholungsurlaubCategory = HOLIDAY;

        absenceTypeService.updateAbsenceType(new AbsenceTypeUpdate(
            erholungsUrlaubSourceId,
            erholungsurlaubCategory,
            erholungsUrlaubColor,
            erholungsurlaubLabels
        ));

        rabbitTemplate.convertAndSend(TOPIC, ALLOWED_ROUTING_KEY, ApplicationAllowedEventDTO.builder()
            .createdAt(Instant.now())
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .sourceId(42L)
            .status("ALLOWED")
            // color is different on purpose
            // the expected absence should have the color of the AbsenceType persisted in database
            .vacationType(VacationTypeDTO.builder().sourceId(erholungsUrlaubSourceId).category("HOLIDAY").color("ORANGE").build())
            .allowedBy(ApplicationPersonDTO.builder().username("boss").personId(1L).build())
            .person(ApplicationPersonDTO.builder().username("boss").personId(1L).build())
            .period(ApplicationPeriodDTO.builder().startDate(startOfDay).endDate(startOfDay).dayLength(FULL).build())
            .absentWorkingDays(Set.of(now))
            .build()
        );

        final UserId userId = new UserId("boss");
        final Function<Locale, String> anyLabel = locale -> "";
        final Absence expectedAbsence = new Absence(userId, ZonedDateTime.ofInstant(startOfDay, UTC), ZonedDateTime.ofInstant(startOfDay, ZONE_ID), DayLength.FULL, anyLabel, erholungsUrlaubColor, erholungsurlaubCategory);

        await().untilAsserted(() -> {
            final Map<LocalDate, List<Absence>> absences = absenceService.findAllAbsences(userId, startOfDay, startOfNextDay);
            assertThat(absences)
                .hasSize(1)
                .contains(Map.entry(now, List.of(expectedAbsence)));
        });

        InOrder inOrder = Mockito.inOrder(tenantContextHolder);
        inOrder.verify(tenantContextHolder).setTenantId(new TenantId(TENANT_ID));
        inOrder.verify(tenantContextHolder).clear();

        verify(tenantContextHolder, times(2)).getCurrentTenantId();
    }
}
