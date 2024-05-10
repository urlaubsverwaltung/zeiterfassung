package de.focusshift.zeiterfassung.absence;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static de.focusshift.zeiterfassung.absence.AbsenceColor.CYAN;
import static de.focusshift.zeiterfassung.absence.AbsenceColor.PINK;
import static de.focusshift.zeiterfassung.absence.AbsenceColor.VIOLET;
import static de.focusshift.zeiterfassung.absence.AbsenceColor.YELLOW;
import static de.focusshift.zeiterfassung.absence.AbsenceType.absenceTypeHoliday;
import static de.focusshift.zeiterfassung.absence.AbsenceType.absenceTypeSpecialLeave;
import static de.focusshift.zeiterfassung.absence.AbsenceTypeCategory.OTHER;
import static de.focusshift.zeiterfassung.absence.DayLength.FULL;
import static de.focusshift.zeiterfassung.absence.DayLength.MORNING;
import static de.focusshift.zeiterfassung.absence.DayLength.NOON;
import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbsenceServiceImplTest {

    private AbsenceServiceImpl sut;

    @Mock
    private AbsenceRepository repository;
    @Mock
    private AbsenceTypeService absenceTypeService;
    @Mock
    private UserSettingsProvider userSettingsProvider;
    @Mock
    private TenantContextHolder tenantContextHolder;
    @Mock
    private UserManagementService userManagementService;
    @Mock
    private MessageSource messageSource;

    @BeforeEach
    void setUp() {
        sut = new AbsenceServiceImpl(repository, absenceTypeService, userSettingsProvider, tenantContextHolder, userManagementService, messageSource);
    }

    @Test
    void ensureFindAllAbsences() {

        final ZonedDateTime today = LocalDate.now().atStartOfDay(UTC);
        final Instant startDate = today.toInstant();
        final Instant endDateExclusive = today.plusWeeks(1).toInstant();

        final ZoneId berlin = ZoneId.of("Europe/Berlin");
        when(userSettingsProvider.zoneId()).thenReturn(berlin);
        when(tenantContextHolder.getCurrentTenantId()).thenReturn(Optional.of(new TenantId("tenant")));

        final AbsenceType holiday = absenceTypeHoliday(Map.of(
            Locale.GERMAN, "holiday-full-de",
            Locale.ENGLISH, "holiday-full-en"
        ), PINK);

        final AbsenceType specialleave = absenceTypeSpecialLeave(Map.of(
            Locale.GERMAN, "special-morning-de",
            Locale.ENGLISH, "special-morning-en"
        ), VIOLET);

        final AbsenceWriteEntity entity_1 = new AbsenceWriteEntity();
        entity_1.setId(1L);
        entity_1.setUserId("user");
        entity_1.setStartDate(today.plusDays(1).toInstant());
        entity_1.setEndDate(today.plusDays(2).toInstant());
        entity_1.setDayLength(FULL);
        entity_1.setType(new AbsenceTypeEntityEmbeddable(holiday.category(), holiday.sourceId()));
        entity_1.setColor(PINK);

        final AbsenceWriteEntity entity_2 = new AbsenceWriteEntity();
        entity_2.setId(2L);
        entity_2.setUserId("user");
        entity_2.setStartDate(today.plusDays(4).toInstant());
        entity_2.setEndDate(today.plusDays(4).toInstant());
        entity_2.setDayLength(MORNING);
        entity_2.setType(new AbsenceTypeEntityEmbeddable(specialleave.category(), specialleave.sourceId()));
        entity_2.setColor(VIOLET);

        when(repository.findAllByTenantIdAndUserIdInAndStartDateLessThanAndEndDateGreaterThanEqual("tenant", List.of("user"), endDateExclusive, startDate))
            .thenReturn(List.of(entity_1, entity_2));

        final Absence expectedAbsence_1 = new Absence(
            new UserId("user"),
            today.plusDays(1).withZoneSameInstant(berlin),
            today.plusDays(2).withZoneSameInstant(berlin),
            FULL,
            locale -> "",
            holiday.color()
        );
        final Absence expectedAbsence_2 = new Absence(
            new UserId("user"),
            today.plusDays(4).withZoneSameInstant(berlin),
            today.plusDays(4).withZoneSameInstant(berlin),
            MORNING,
            locale -> "",
            specialleave.color()
        );

        // SupportedLanguages are GERMAN and ENGLISH right now
        when(messageSource.getMessage("absence.HOLIDAY.1000.FULL", new Object[]{}, Locale.GERMAN)).thenReturn("holiday-full-de");
        when(messageSource.getMessage("absence.HOLIDAY.1000.FULL", new Object[]{}, Locale.ENGLISH)).thenReturn("holiday-full-en");
        when(messageSource.getMessage("absence.SPECIALLEAVE.2000.MORNING", new Object[]{}, Locale.GERMAN)).thenReturn("special-morning-de");
        when(messageSource.getMessage("absence.SPECIALLEAVE.2000.MORNING", new Object[]{}, Locale.ENGLISH)).thenReturn("special-morning-en");

        final Map<LocalDate, List<Absence>> actual = sut.findAllAbsences(new UserId("user"), startDate, endDateExclusive);
        assertThat(actual)
            .hasSize(7)
            .containsEntry(today.toLocalDate(), List.of())
            .containsEntry(today.plusDays(1).toLocalDate(), List.of(expectedAbsence_1))
            .containsEntry(today.plusDays(2).toLocalDate(), List.of(expectedAbsence_1))
            .containsEntry(today.plusDays(3).toLocalDate(), List.of())
            .containsEntry(today.plusDays(4).toLocalDate(), List.of(expectedAbsence_2))
            .containsEntry(today.plusDays(5).toLocalDate(), List.of())
            .containsEntry(today.plusDays(6).toLocalDate(), List.of());
    }

    @Test
    void ensureGetAbsencesByUserIdsReturnsEmptyListForAskedUsersWithoutAbsences() {

        final ZoneId berlin = ZoneId.of("Europe/Berlin");
        when(userSettingsProvider.zoneId()).thenReturn(berlin);
        when(tenantContextHolder.getCurrentTenantId()).thenReturn(Optional.of(new TenantId("tenant")));

        final LocalDate from = LocalDate.of(2023, 11, 16);
        final LocalDate toExclusive = LocalDate.of(2023, 11, 16);
        final Instant fromStartOfDay = Instant.from(from.atStartOfDay().atZone(berlin));
        final Instant toExclusiveStartOfDay = Instant.from(toExclusive.atStartOfDay().atZone(berlin));

        final UserId userId = new UserId(UUID.randomUUID().toString());
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        when(userManagementService.findAllUsersByLocalIds(List.of(userLocalId))).thenReturn(List.of(
            new User(userIdComposite, null, null, null, Set.of()))
        );

        final List<String> userIdsValues = List.of(userId.value());

        when(repository.findAllByTenantIdAndUserIdInAndStartDateLessThanAndEndDateGreaterThanEqual("tenant", userIdsValues, toExclusiveStartOfDay, fromStartOfDay))
            .thenReturn(List.of());

        final Map<UserIdComposite, List<Absence>> actual = sut.getAbsencesByUserIds(List.of(userLocalId), from, toExclusive);
        assertThat(actual).containsEntry(userIdComposite, List.of());
    }

    @Test
    void ensureGetAbsencesByUserIds() {

        final ZoneId berlin = ZoneId.of("Europe/Berlin");
        when(userSettingsProvider.zoneId()).thenReturn(berlin);
        when(tenantContextHolder.getCurrentTenantId()).thenReturn(Optional.of(new TenantId("tenant")));

        final LocalDate from = LocalDate.of(2023, 11, 1);
        final LocalDate toExclusive = LocalDate.of(2023, 11, 30);
        final Instant fromStartOfDay = Instant.from(from.atStartOfDay().atZone(berlin));
        final Instant toExclusiveStartOfDay = Instant.from(toExclusive.atStartOfDay().atZone(berlin));

        final UserId userId_1 = new UserId(UUID.randomUUID().toString());
        final UserLocalId userLocalId_1 = new UserLocalId(1L);
        final UserIdComposite userIdComposite_1 = new UserIdComposite(userId_1, userLocalId_1);
        final UserId userId_2 = new UserId(UUID.randomUUID().toString());
        final UserLocalId userLocalId_2 = new UserLocalId(2L);
        final UserIdComposite userIdComposite_2 = new UserIdComposite(userId_2, userLocalId_2);

        when(userManagementService.findAllUsersByLocalIds(List.of(userLocalId_1, userLocalId_2))).thenReturn(List.of(
            new User(userIdComposite_1, "Bruce", null, null, Set.of()),
            new User(userIdComposite_2, "Alfred", null, null, Set.of())
        ));

        final Instant absence_1_start = Instant.from(from.plusDays(1).atStartOfDay().atZone(berlin));
        final Instant absence_1_end = Instant.from(from.plusDays(1).atStartOfDay().atZone(berlin));
        final AbsenceWriteEntity absenceEntity_1 = new AbsenceWriteEntity();
        absenceEntity_1.setUserId(userId_1.value());
        absenceEntity_1.setStartDate(absence_1_start);
        absenceEntity_1.setEndDate(absence_1_end);
        absenceEntity_1.setDayLength(FULL);
        absenceEntity_1.setType(new AbsenceTypeEntityEmbeddable(OTHER, 1000L));
        absenceEntity_1.setColor(YELLOW);

        final Instant absence_2_1_start = Instant.from(from.plusDays(1).atStartOfDay().atZone(berlin));
        final Instant absence_2_1_end = Instant.from(from.plusDays(1).atStartOfDay().atZone(berlin));
        final AbsenceWriteEntity absenceEntity_2_1 = new AbsenceWriteEntity();
        absenceEntity_2_1.setUserId(userId_2.value());
        absenceEntity_2_1.setStartDate(absence_2_1_start);
        absenceEntity_2_1.setEndDate(absence_2_1_end);
        absenceEntity_2_1.setDayLength(MORNING);
        absenceEntity_2_1.setType(new AbsenceTypeEntityEmbeddable(OTHER, 2000L));
        absenceEntity_2_1.setColor(VIOLET);

        final Instant absence_2_2_start = Instant.from(from.plusDays(2).atStartOfDay().atZone(berlin));
        final Instant absence_2_2_end = Instant.from(from.plusDays(2).atStartOfDay().atZone(berlin));
        final AbsenceWriteEntity absenceEntity_2_2 = new AbsenceWriteEntity();
        absenceEntity_2_2.setUserId(userId_2.value());
        absenceEntity_2_2.setStartDate(absence_2_2_start);
        absenceEntity_2_2.setEndDate(absence_2_2_end);
        absenceEntity_2_2.setDayLength(NOON);
        absenceEntity_2_2.setType(new AbsenceTypeEntityEmbeddable(OTHER, 3000L));
        absenceEntity_2_2.setColor(CYAN);

        when(repository.findAllByTenantIdAndUserIdInAndStartDateLessThanAndEndDateGreaterThanEqual("tenant", List.of(userId_1.value(), userId_2.value()), toExclusiveStartOfDay, fromStartOfDay))
            .thenReturn(List.of(absenceEntity_1, absenceEntity_2_1, absenceEntity_2_2));

        // SupportedLanguages are GERMAN and ENGLISH right now
        when(messageSource.getMessage("absence.OTHER.1000.FULL", new Object[]{}, Locale.GERMAN)).thenReturn("full-de");
        when(messageSource.getMessage("absence.OTHER.2000.MORNING", new Object[]{}, Locale.GERMAN)).thenReturn("morning-de");
        when(messageSource.getMessage("absence.OTHER.3000.NOON", new Object[]{}, Locale.GERMAN)).thenReturn("noon-de");
        when(messageSource.getMessage("absence.OTHER.1000.FULL", new Object[]{}, Locale.ENGLISH)).thenReturn("full-en");
        when(messageSource.getMessage("absence.OTHER.2000.MORNING", new Object[]{}, Locale.ENGLISH)).thenReturn("morning-en");
        when(messageSource.getMessage("absence.OTHER.3000.NOON", new Object[]{}, Locale.ENGLISH)).thenReturn("noon-en");

        final Map<UserIdComposite, List<Absence>> actual = sut.getAbsencesByUserIds(List.of(userLocalId_1, userLocalId_2), from, toExclusive);

        assertThat(actual).containsExactlyInAnyOrderEntriesOf(Map.of(
            userIdComposite_1, List.of(
                new Absence(userId_1, absence_1_start.atZone(berlin), absence_1_end.atZone(berlin), FULL, locale -> "", YELLOW)
            ),
            userIdComposite_2, List.of(
                new Absence(userId_2, absence_2_1_start.atZone(berlin), absence_2_1_end.atZone(berlin), MORNING, locale -> "", VIOLET),
                new Absence(userId_2, absence_2_2_start.atZone(berlin), absence_2_2_end.atZone(berlin), NOON, locale -> "", CYAN)
            )
        ));
    }

    @Test
    void ensureGetAbsencesForAllUsersReturnsEmptyListForUsersWithoutAbsences() {

        final ZoneId berlin = ZoneId.of("Europe/Berlin");
        when(userSettingsProvider.zoneId()).thenReturn(berlin);
        when(tenantContextHolder.getCurrentTenantId()).thenReturn(Optional.of(new TenantId("tenant")));

        final LocalDate from = LocalDate.of(2023, 11, 1);
        final LocalDate toExclusive = LocalDate.of(2023, 11, 30);
        final Instant fromStartOfDay = Instant.from(from.atStartOfDay().atZone(berlin));
        final Instant toExclusiveStartOfDay = Instant.from(toExclusive.atStartOfDay().atZone(berlin));

        final UserId userId = new UserId(UUID.randomUUID().toString());
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "", "", new EMailAddress(""), Set.of());
        when(userManagementService.findAllUsers()).thenReturn(List.of(user));

        when(repository.findAllByTenantIdAndStartDateLessThanAndEndDateGreaterThanEqual("tenant", toExclusiveStartOfDay, fromStartOfDay))
            .thenReturn(List.of());

        final Map<UserIdComposite, List<Absence>> actual = sut.getAbsencesForAllUsers(from, toExclusive);
        assertThat(actual).containsExactlyInAnyOrderEntriesOf(Map.of(userIdComposite, List.of()));
    }

    @Test
    void ensureGetAbsencesForAllUsers() {

        final ZoneId berlin = ZoneId.of("Europe/Berlin");
        when(userSettingsProvider.zoneId()).thenReturn(berlin);
        when(tenantContextHolder.getCurrentTenantId()).thenReturn(Optional.of(new TenantId("tenant")));

        final LocalDate from = LocalDate.of(2023, 11, 1);
        final LocalDate toExclusive = LocalDate.of(2023, 11, 30);
        final Instant fromStartOfDay = Instant.from(from.atStartOfDay().atZone(berlin));
        final Instant toExclusiveStartOfDay = Instant.from(toExclusive.atStartOfDay().atZone(berlin));

        final UserId userId_1 = new UserId(UUID.randomUUID().toString());
        final UserLocalId userLocalId_1 = new UserLocalId(1L);
        final UserIdComposite userIdComposite_1 = new UserIdComposite(userId_1, userLocalId_1);
        final User user_1 = new User(userIdComposite_1, "", "", new EMailAddress(""), Set.of());

        final UserId userId_2 = new UserId(UUID.randomUUID().toString());
        final UserLocalId userLocalId_2 = new UserLocalId(2L);
        final UserIdComposite userIdComposite_2 = new UserIdComposite(userId_2, userLocalId_2);
        final User user_2 = new User(userIdComposite_2, "", "", new EMailAddress(""), Set.of());

        when(userManagementService.findAllUsers()).thenReturn(List.of(user_1, user_2));

        final Instant absence_1_start = Instant.from(from.plusDays(1).atStartOfDay().atZone(berlin));
        final Instant absence_1_end = Instant.from(from.plusDays(1).atStartOfDay().atZone(berlin));
        final AbsenceWriteEntity absenceEntity_1 = new AbsenceWriteEntity();
        absenceEntity_1.setUserId(userId_1.value());
        absenceEntity_1.setStartDate(absence_1_start);
        absenceEntity_1.setEndDate(absence_1_end);
        absenceEntity_1.setDayLength(FULL);
        absenceEntity_1.setType(new AbsenceTypeEntityEmbeddable(OTHER, 1000L));
        absenceEntity_1.setColor(YELLOW);

        final Instant absence_2_1_start = Instant.from(from.plusDays(1).atStartOfDay().atZone(berlin));
        final Instant absence_2_1_end = Instant.from(from.plusDays(1).atStartOfDay().atZone(berlin));
        final AbsenceWriteEntity absenceEntity_2_1 = new AbsenceWriteEntity();
        absenceEntity_2_1.setUserId(userId_2.value());
        absenceEntity_2_1.setStartDate(absence_2_1_start);
        absenceEntity_2_1.setEndDate(absence_2_1_end);
        absenceEntity_2_1.setDayLength(MORNING);
        absenceEntity_2_1.setType(new AbsenceTypeEntityEmbeddable(OTHER, 2000L));
        absenceEntity_2_1.setColor(VIOLET);

        final Instant absence_2_2_start = Instant.from(from.plusDays(2).atStartOfDay().atZone(berlin));
        final Instant absence_2_2_end = Instant.from(from.plusDays(2).atStartOfDay().atZone(berlin));
        final AbsenceWriteEntity absenceEntity_2_2 = new AbsenceWriteEntity();
        absenceEntity_2_2.setUserId(userId_2.value());
        absenceEntity_2_2.setStartDate(absence_2_2_start);
        absenceEntity_2_2.setEndDate(absence_2_2_end);
        absenceEntity_2_2.setDayLength(NOON);
        absenceEntity_2_2.setType(new AbsenceTypeEntityEmbeddable(OTHER, 3000L));
        absenceEntity_2_2.setColor(CYAN);

        when(repository.findAllByTenantIdAndStartDateLessThanAndEndDateGreaterThanEqual("tenant", toExclusiveStartOfDay, fromStartOfDay))
            .thenReturn(List.of(absenceEntity_1, absenceEntity_2_1, absenceEntity_2_2));

        // SupportedLanguages are GERMAN and ENGLISH right now
        when(messageSource.getMessage("absence.OTHER.1000.FULL", new Object[]{}, Locale.GERMAN)).thenReturn("full-de");
        when(messageSource.getMessage("absence.OTHER.2000.MORNING", new Object[]{}, Locale.GERMAN)).thenReturn("morning-de");
        when(messageSource.getMessage("absence.OTHER.3000.NOON", new Object[]{}, Locale.GERMAN)).thenReturn("noon-de");
        when(messageSource.getMessage("absence.OTHER.1000.FULL", new Object[]{}, Locale.ENGLISH)).thenReturn("full-en");
        when(messageSource.getMessage("absence.OTHER.2000.MORNING", new Object[]{}, Locale.ENGLISH)).thenReturn("morning-en");
        when(messageSource.getMessage("absence.OTHER.3000.NOON", new Object[]{}, Locale.ENGLISH)).thenReturn("noon-en");

        final Map<UserIdComposite, List<Absence>> actual = sut.getAbsencesForAllUsers(from, toExclusive);
        assertThat(actual).containsExactlyInAnyOrderEntriesOf(Map.of(
            userIdComposite_1, List.of(
                new Absence(userId_1, absence_1_start.atZone(berlin), absence_1_end.atZone(berlin), FULL, locale -> "", YELLOW)
            ),
            userIdComposite_2, List.of(
                new Absence(userId_2, absence_2_1_start.atZone(berlin), absence_2_1_end.atZone(berlin), MORNING, locale -> "", VIOLET),
                new Absence(userId_2, absence_2_2_start.atZone(berlin), absence_2_2_end.atZone(berlin), NOON, locale -> "", CYAN)
            )
        ));
    }
}
