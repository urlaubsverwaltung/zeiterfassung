package de.focusshift.zeiterfassung.integration.urlaubsverwaltung.sicknote;

import de.focus_shift.urlaubsverwaltung.extension.api.sicknote.DayLength;
import de.focus_shift.urlaubsverwaltung.extension.api.sicknote.SickNoteCancelledEventDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.sicknote.SickNoteCreatedEventDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.sicknote.SickNotePeriodDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.sicknote.SickNotePersonDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.sicknote.SickNoteUpdatedEventDTO;
import de.focusshift.zeiterfassung.RabbitTestConfiguration;
import de.focusshift.zeiterfassung.TestContainersBase;
import de.focusshift.zeiterfassung.absence.Absence;
import de.focusshift.zeiterfassung.absence.AbsenceService;
import de.focusshift.zeiterfassung.absence.AbsenceWrite;
import de.focusshift.zeiterfassung.absence.AbsenceWriteService;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

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

import static de.focusshift.zeiterfassung.absence.AbsenceColor.RED;
import static de.focusshift.zeiterfassung.absence.AbsenceTypeCategory.SICK;
import static de.focusshift.zeiterfassung.absence.DayLength.FULL;
import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;

@SpringBootTest(
    properties = {
        "zeiterfassung.tenant.mode=single",
        "zeiterfassung.tenant.single.default-tenant-id=" + SickNoteEventHandlerRabbitmqIT.TENANT_ID,
        "zeiterfassung.integration.urlaubsverwaltung.sicknote.enabled=true",
        "zeiterfassung.integration.urlaubsverwaltung.sicknote.manage-topology=true",
        "zeiterfassung.integration.urlaubsverwaltung.sicknote.topic=" + SickNoteEventHandlerRabbitmqIT.TOPIC,
        "zeiterfassung.integration.urlaubsverwaltung.sicknote.routing-key-created=" + SickNoteEventHandlerRabbitmqIT.CREATED_ROUTING_KEY,
        "zeiterfassung.integration.urlaubsverwaltung.sicknote.routing-key-converted-to-application=" + SickNoteEventHandlerRabbitmqIT.CONVERTED_TO_APPLICATION_ROUTING_KEY,
        "zeiterfassung.integration.urlaubsverwaltung.sicknote.routing-key-cancelled=" + SickNoteEventHandlerRabbitmqIT.CANCELLED_ROUTING_KEY,
        "zeiterfassung.integration.urlaubsverwaltung.sicknote.routing-key-updated=" + SickNoteEventHandlerRabbitmqIT.UPDATED_ROUTING_KEY,
    }
)
@Import(RabbitTestConfiguration.class)
class SickNoteEventHandlerRabbitmqIT extends TestContainersBase {

    protected static final String TENANT_ID = "tenant";
    protected static final String TOPIC = "topic";
    protected static final String CREATED_ROUTING_KEY = "created";
    protected static final String CONVERTED_TO_APPLICATION_ROUTING_KEY = "converted-to-application";
    protected static final String CANCELLED_ROUTING_KEY = "cancelled";
    protected static final String UPDATED_ROUTING_KEY = "updated";

    private static final ZoneOffset ZONE_ID = UTC;

    @MockBean
    private UserSettingsProvider userSettingsProvider;
    @MockBean(answer = Answers.CALLS_REAL_METHODS)
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private AbsenceService absenceService;
    @Autowired
    private AbsenceWriteService absenceWriteService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void setUp() {
        when(userSettingsProvider.zoneId()).thenReturn(ZONE_ID);
    }

    @Test
    void ensureSickNoteCreatedUpdatedDeleted() {

        final LocalDate now = LocalDate.now();
        final Instant startOfDay = now.atStartOfDay().toInstant(ZONE_ID);
        final Instant startOfNextDay = now.plusDays(1).atStartOfDay().toInstant(ZONE_ID);

        final SickNotePersonDTO boss = SickNotePersonDTO.builder().personId(1L).username("boss").build();

        TenantId tenantId = new TenantId(TENANT_ID);
        when(tenantContextHolder.getCurrentTenantId()).thenReturn(Optional.of(tenantId));

        // CREATE sick note absence
        rabbitTemplate.convertAndSend(TOPIC, CREATED_ROUTING_KEY, SickNoteCreatedEventDTO.builder()
            .createdAt(Instant.now())
            .id(UUID.randomUUID())
            .sourceId(1L)
            .tenantId(TENANT_ID)
            .person(boss)
            .applier(boss)
            .type("SICK_NOTE")
            .status("ACTIVE")
            .period(SickNotePeriodDTO.builder().dayLength(DayLength.FULL).startDate(startOfDay).endDate(startOfDay).build())
            .absentWorkingDays(Set.of(now))
            .build());

        final UserId userId = new UserId("boss");
        final Function<Locale, String> anyLabel = locale -> "";

        await().untilAsserted(() -> {
            final ZonedDateTime start = ZonedDateTime.ofInstant(startOfDay, ZONE_ID);
            final Absence expected = new Absence(userId, start, start, FULL, anyLabel, RED, SICK);
            final Map<LocalDate, List<Absence>> absences = absenceService.findAllAbsences(userId, startOfDay, startOfNextDay);
            assertThat(absences)
                .hasSize(1)
                .containsOnly(Map.entry(now, List.of(expected)));

            InOrder inOrder = Mockito.inOrder(tenantContextHolder);
            inOrder.verify(tenantContextHolder).setTenantId(tenantId);
            inOrder.verify(tenantContextHolder).clear();
        });

        // UPDATE to period of two days
        rabbitTemplate.convertAndSend(TOPIC, UPDATED_ROUTING_KEY, SickNoteUpdatedEventDTO.builder()
            .createdAt(Instant.now())
            .id(UUID.randomUUID())
            .sourceId(1L)
            .tenantId(TENANT_ID)
            .person(boss)
            .applier(boss)
            .type("SICK_NOTE")
            .status("ACTIVE")
            .period(SickNotePeriodDTO.builder().dayLength(DayLength.FULL).startDate(startOfDay).endDate(startOfNextDay).build())
            .absentWorkingDays(Set.of(now, now.plusDays(1)))
            .build());

        await().untilAsserted(() -> {
            final ZonedDateTime start = ZonedDateTime.ofInstant(startOfDay, ZONE_ID);
            final Absence expected = new Absence(userId, start, start.plusDays(1), FULL, anyLabel, RED, SICK);
            final Map<LocalDate, List<Absence>> absences = absenceService.findAllAbsences(userId, startOfDay, startOfNextDay);
            assertThat(absences)
                .hasSize(2)
                .containsOnly(Map.entry(now, List.of(expected)), Map.entry(now.plusDays(1), List.of(expected)));
            InOrder inOrder = Mockito.inOrder(tenantContextHolder);
            inOrder.verify(tenantContextHolder).setTenantId(tenantId);
            inOrder.verify(tenantContextHolder).clear();
        });

        // CANCEL sick note absence
        rabbitTemplate.convertAndSend(TOPIC, CANCELLED_ROUTING_KEY, SickNoteCancelledEventDTO.builder()
            .createdAt(Instant.now())
            .id(UUID.randomUUID())
            .sourceId(1L)
            .tenantId(TENANT_ID)
            .person(boss)
            .applier(boss)
            .type("SICK_NOTE")
            .status("CANCELLED")
            .period(SickNotePeriodDTO.builder().dayLength(DayLength.FULL).startDate(startOfDay).endDate(startOfNextDay).build())
            .absentWorkingDays(Set.of(now, now.plusDays(1)))
            .build());

        await().untilAsserted(() -> {
            final Map<LocalDate, List<Absence>> absences = absenceService.findAllAbsences(userId, startOfDay, startOfNextDay);
            assertThat(absences)
                .hasSize(1)
                .containsOnly(Map.entry(now, List.of()));

            InOrder inOrder = Mockito.inOrder(tenantContextHolder);
            inOrder.verify(tenantContextHolder).setTenantId(tenantId);
            inOrder.verify(tenantContextHolder).clear();
        });

    }

    @Test
    void ensureConvertedToApplicationDeletesSickNote() {

        final LocalDate now = LocalDate.now();
        final Instant startOfDay = now.atStartOfDay().toInstant(ZONE_ID);
        final Instant startOfNextDay = now.plusDays(1).atStartOfDay().toInstant(ZONE_ID);

        final SickNotePersonDTO boss = SickNotePersonDTO.builder().personId(1L).username("boss").build();
        final UserId userId = new UserId("boss");

        TenantId tenantId = new TenantId(TENANT_ID);
        when(tenantContextHolder.getCurrentTenantId()).thenReturn(Optional.of(tenantId));

        final AbsenceWrite absence = new AbsenceWrite(1L, userId, startOfDay, startOfDay, FULL, SICK);
        absenceWriteService.addAbsence(absence);

        // CANCEL sick note absence
        rabbitTemplate.convertAndSend(TOPIC, CONVERTED_TO_APPLICATION_ROUTING_KEY, SickNoteCancelledEventDTO.builder()
            .createdAt(Instant.now())
            .id(UUID.randomUUID())
            .sourceId(1L)
            .tenantId(TENANT_ID)
            .person(boss)
            .applier(boss)
            .type("SICK_NOTE")
            .status("CONVERTED_TO_VACATION")
            .period(SickNotePeriodDTO.builder().dayLength(DayLength.FULL).startDate(startOfDay).endDate(startOfNextDay).build())
            .absentWorkingDays(Set.of(now))
            .build());

        await().untilAsserted(() -> {
            final Map<LocalDate, List<Absence>> absences = absenceService.findAllAbsences(userId, startOfDay, startOfNextDay);
            assertThat(absences)
                .hasSize(1)
                .containsOnly(Map.entry(now, List.of()));
        });

        InOrder inOrder = Mockito.inOrder(tenantContextHolder);
        inOrder.verify(tenantContextHolder).setTenantId(tenantId);
        inOrder.verify(tenantContextHolder).clear();
    }
}
