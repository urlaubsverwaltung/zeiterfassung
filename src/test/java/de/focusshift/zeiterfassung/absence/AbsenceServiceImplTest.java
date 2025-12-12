package de.focusshift.zeiterfassung.absence;

import de.focusshift.zeiterfassung.companyvacation.CompanyVacation;
import de.focusshift.zeiterfassung.companyvacation.CompanyVacationService;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import static de.focusshift.zeiterfassung.absence.AbsenceColor.CYAN;
import static de.focusshift.zeiterfassung.absence.AbsenceColor.PINK;
import static de.focusshift.zeiterfassung.absence.AbsenceColor.VIOLET;
import static de.focusshift.zeiterfassung.absence.AbsenceColor.YELLOW;
import static de.focusshift.zeiterfassung.absence.AbsenceTypeCategory.HOLIDAY;
import static de.focusshift.zeiterfassung.absence.AbsenceTypeCategory.OTHER;
import static de.focusshift.zeiterfassung.absence.AbsenceTypeCategory.OVERTIME;
import static de.focusshift.zeiterfassung.absence.AbsenceTypeCategory.SICK;
import static de.focusshift.zeiterfassung.absence.AbsenceTypeCategory.SPECIALLEAVE;
import static de.focusshift.zeiterfassung.absence.DayLength.FULL;
import static de.focusshift.zeiterfassung.absence.DayLength.MORNING;
import static de.focusshift.zeiterfassung.absence.DayLength.NOON;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Locale.ENGLISH;
import static java.util.Locale.GERMAN;
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
    private UserManagementService userManagementService;
    @Mock
    private CompanyVacationService companyVacationService;
    @Mock
    private MessageSource messageSource;

    @BeforeEach
    void setUp() {
        sut = new AbsenceServiceImpl(repository, absenceTypeService, userSettingsProvider, userManagementService,
            companyVacationService, messageSource);
    }

    @Nested
    class FindAllAbsences {

        @Test
        void ensureFindAllAbsences() {

            final Instant today = Instant.now();
            final Instant startDate = today;
            final Instant endDateExclusive = today.plus(7, DAYS);

            final ZoneId berlin = ZoneId.of("Europe/Berlin");
            when(userSettingsProvider.zoneId()).thenReturn(berlin);

            final AbsenceWriteEntity entity_1 = new AbsenceWriteEntity();
            entity_1.setId(1L);
            entity_1.setUserId("user");
            entity_1.setStartDate(today.plus(1, DAYS));
            entity_1.setEndDate(today.plus(2, DAYS));
            entity_1.setDayLength(FULL);
            entity_1.setType(new AbsenceTypeEntityEmbeddable(HOLIDAY, 1000L));

            final AbsenceWriteEntity entity_2 = new AbsenceWriteEntity();
            entity_2.setId(2L);
            entity_2.setUserId("user");
            entity_2.setStartDate(today.plus(4, DAYS));
            entity_2.setEndDate(today.plus(4, DAYS));
            entity_2.setDayLength(MORNING);
            entity_2.setType(new AbsenceTypeEntityEmbeddable(SPECIALLEAVE, 2000L));

            when(repository.findAllByUserIdInAndStartDateLessThanAndEndDateGreaterThanEqual(List.of("user"), endDateExclusive, startDate))
                .thenReturn(List.of(entity_1, entity_2));

            final AbsenceType absenceType1 = new AbsenceType(HOLIDAY, 1000L, label(GERMAN, "1000-de", ENGLISH, "1000-en"), PINK);
            final AbsenceType absenceType2 = new AbsenceType(SPECIALLEAVE, 2000L, label(GERMAN, "2000-de", ENGLISH, "2000-en"), VIOLET);

            when(absenceTypeService.findAllByAbsenceTypeSourceIds(List.of(1000L, 2000L)))
                .thenReturn(List.of(absenceType1, absenceType2));

            final Function<Locale, String> anyLabel = locale -> "";
            final Absence expectedAbsence_1 = new Absence(
                new UserId("user"),
                today.plus(1, DAYS),
                today.plus(2, DAYS),
                FULL,
                anyLabel,
                PINK,
                HOLIDAY
            );
            final Absence expectedAbsence_2 = new Absence(
                new UserId("user"),
                today.plus(4, DAYS),
                today.plus(4, DAYS),
                MORNING,
                anyLabel,
                VIOLET,
                SPECIALLEAVE
            );

            final Map<LocalDate, List<Absence>> actual = sut.findAllAbsences(new UserId("user"), startDate, endDateExclusive);
            assertThat(actual)
                .hasSize(7)
                .containsEntry(LocalDate.ofInstant(today, UTC), List.of())
                .containsEntry(LocalDate.ofInstant(today.plus(1, DAYS), UTC), List.of(expectedAbsence_1))
                .containsEntry(LocalDate.ofInstant(today.plus(2, DAYS), UTC), List.of(expectedAbsence_1))
                .containsEntry(LocalDate.ofInstant(today.plus(3, DAYS), UTC), List.of())
                .containsEntry(LocalDate.ofInstant(today.plus(4, DAYS), UTC), List.of(expectedAbsence_2))
                .containsEntry(LocalDate.ofInstant(today.plus(5, DAYS), UTC), List.of())
                .containsEntry(LocalDate.ofInstant(today.plus(6, DAYS), UTC), List.of());
        }

        @ParameterizedTest
        @EnumSource(DayLength.class)
        void ensureFindAllAbsencesWithLabelForDayLength(DayLength givenDayLength) {

            final ZonedDateTime today = LocalDate.now().atStartOfDay(UTC);
            final Instant startDate = today.toInstant();
            final Instant endDateExclusive = today.plusWeeks(1).toInstant();

            final ZoneId berlin = ZoneId.of("Europe/Berlin");
            when(userSettingsProvider.zoneId()).thenReturn(berlin);

            final AbsenceWriteEntity entity = new AbsenceWriteEntity();
            entity.setId(1L);
            entity.setUserId("user");
            entity.setStartDate(today.toInstant());
            entity.setEndDate(today.toInstant());
            entity.setDayLength(givenDayLength);
            entity.setType(new AbsenceTypeEntityEmbeddable(HOLIDAY, 1000L));

            when(repository.findAllByUserIdInAndStartDateLessThanAndEndDateGreaterThanEqual(List.of("user"), endDateExclusive, startDate))
                .thenReturn(List.of(entity));

            final AbsenceType absenceType = new AbsenceType(HOLIDAY, 1000L, label(GERMAN, "de", ENGLISH, "en"), PINK);

            when(absenceTypeService.findAllByAbsenceTypeSourceIds(List.of(1000L)))
                .thenReturn(List.of(absenceType));

            // SupportedLanguages are GERMAN and ENGLISH right now
            when(messageSource.getMessage("absence.label." + givenDayLength, new Object[]{"de"}, GERMAN)).thenReturn("message-de");
            when(messageSource.getMessage("absence.label." + givenDayLength, new Object[]{"en"}, ENGLISH)).thenReturn("message-en");

            final Map<LocalDate, List<Absence>> actual = sut.findAllAbsences(new UserId("user"), startDate, endDateExclusive);
            assertThat(actual.get(today.toLocalDate())).satisfies(absences -> {
                assertThat(absences.getFirst().label(GERMAN)).isEqualTo("message-de");
                assertThat(absences.getFirst().label(ENGLISH)).isEqualTo("message-en");
            });
        }

        @ParameterizedTest
        @EnumSource(DayLength.class)
        void ensureFindAllAbsencesWithLabelForDayLengthAndCategorySickness(DayLength givenDayLength) {

            final ZonedDateTime today = LocalDate.now().atStartOfDay(UTC);
            final Instant startDate = today.toInstant();
            final Instant endDateExclusive = today.plusWeeks(1).toInstant();

            final ZoneId berlin = ZoneId.of("Europe/Berlin");
            when(userSettingsProvider.zoneId()).thenReturn(berlin);

            final AbsenceWriteEntity entitySickness = new AbsenceWriteEntity();
            entitySickness.setId(2L);
            entitySickness.setUserId("user");
            entitySickness.setStartDate(today.toInstant());
            entitySickness.setEndDate(today.toInstant());
            entitySickness.setDayLength(givenDayLength);
            entitySickness.setType(new AbsenceTypeEntityEmbeddable(SICK, null));

            when(repository.findAllByUserIdInAndStartDateLessThanAndEndDateGreaterThanEqual(List.of("user"), endDateExclusive, startDate))
                .thenReturn(List.of(entitySickness));

            // SupportedLanguages are GERMAN and ENGLISH right now
            when(messageSource.getMessage("absence.type.category.SICK", null, GERMAN)).thenReturn("Krank");
            when(messageSource.getMessage("absence.type.category.SICK", null, ENGLISH)).thenReturn("Sickness");
            when(messageSource.getMessage("absence.label." + givenDayLength, new Object[]{"Krank"}, GERMAN)).thenReturn("sick-message-de");
            when(messageSource.getMessage("absence.label." + givenDayLength, new Object[]{"Sickness"}, ENGLISH)).thenReturn("sick-message-en");

            final Map<LocalDate, List<Absence>> actual = sut.findAllAbsences(new UserId("user"), startDate, endDateExclusive);
            assertThat(actual.get(today.toLocalDate())).satisfies(absences -> {
                assertThat(absences.getFirst().label(GERMAN)).isEqualTo("sick-message-de");
                assertThat(absences.getFirst().label(ENGLISH)).isEqualTo("sick-message-en");
            });
        }

        @Test
        void ensureCompanyVacationIsIncluded() {

            final Instant today = Instant.now();
            final Instant startDate = today;
            final Instant endDateExclusive = today.plus(7, DAYS);

            final ZoneId berlin = ZoneId.of("Europe/Berlin");
            when(userSettingsProvider.zoneId()).thenReturn(berlin);

            final Instant companyVacationStart = startDate.plus(1, DAYS);
            final Instant companyVacationEnd = startDate.plus(2, DAYS);
            final CompanyVacation companyVacation = new CompanyVacation(companyVacationStart, companyVacationEnd, de.focusshift.zeiterfassung.companyvacation.DayLength.FULL);
            when(companyVacationService.getCompanyVacations(startDate, endDateExclusive)).thenReturn(List.of(companyVacation));

            final UserId userId = new UserId("user");
            final Map<LocalDate, List<Absence>> actual = sut.findAllAbsences(userId, startDate, endDateExclusive);

            final Function<Locale, String> anyLabel = locale -> "";
            final Absence expectedAbsence = new Absence(userId, companyVacationStart, companyVacationEnd, FULL, anyLabel, YELLOW, HOLIDAY);

            assertThat(actual)
                .hasSize(7)
                .containsEntry(LocalDate.ofInstant(today, UTC), List.of())
                .containsEntry(LocalDate.ofInstant(today.plus(1, DAYS), UTC), List.of(expectedAbsence))
                .containsEntry(LocalDate.ofInstant(today.plus(2, DAYS), UTC), List.of(expectedAbsence))
                .containsEntry(LocalDate.ofInstant(today.plus(3, DAYS), UTC), List.of())
                .containsEntry(LocalDate.ofInstant(today.plus(4, DAYS), UTC), List.of())
                .containsEntry(LocalDate.ofInstant(today.plus(5, DAYS), UTC), List.of())
                .containsEntry(LocalDate.ofInstant(today.plus(6, DAYS), UTC), List.of());
        }
    }

    @Nested
    class GetAbsencesByUserIds {

        @Test
        void ensureGetAbsencesByUserIdsReturnsEmptyListForAskedUsersWithoutAbsences() {

            final ZoneId berlin = ZoneId.of("Europe/Berlin");
            when(userSettingsProvider.zoneId()).thenReturn(berlin);

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

            when(repository.findAllByUserIdInAndStartDateLessThanAndEndDateGreaterThanEqual(userIdsValues, toExclusiveStartOfDay, fromStartOfDay))
                .thenReturn(List.of());

            final Map<UserIdComposite, List<Absence>> actual = sut.getAbsencesByUserIds(List.of(userLocalId), from, toExclusive);
            assertThat(actual).containsEntry(userIdComposite, List.of());
        }

        @Test
        void ensureGetAbsencesByUserIds() {

            final ZoneId berlin = ZoneId.of("Europe/Berlin");
            when(userSettingsProvider.zoneId()).thenReturn(berlin);

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

            final Instant absence_2_1_start = Instant.from(from.plusDays(1).atStartOfDay().atZone(berlin));
            final Instant absence_2_1_end = Instant.from(from.plusDays(1).atStartOfDay().atZone(berlin));
            final AbsenceWriteEntity absenceEntity_2_1 = new AbsenceWriteEntity();
            absenceEntity_2_1.setUserId(userId_2.value());
            absenceEntity_2_1.setStartDate(absence_2_1_start);
            absenceEntity_2_1.setEndDate(absence_2_1_end);
            absenceEntity_2_1.setDayLength(MORNING);
            absenceEntity_2_1.setType(new AbsenceTypeEntityEmbeddable(OTHER, 2000L));

            final Instant absence_2_2_start = Instant.from(from.plusDays(2).atStartOfDay().atZone(berlin));
            final Instant absence_2_2_end = Instant.from(from.plusDays(2).atStartOfDay().atZone(berlin));
            final AbsenceWriteEntity absenceEntity_2_2 = new AbsenceWriteEntity();
            absenceEntity_2_2.setUserId(userId_2.value());
            absenceEntity_2_2.setStartDate(absence_2_2_start);
            absenceEntity_2_2.setEndDate(absence_2_2_end);
            absenceEntity_2_2.setDayLength(NOON);
            absenceEntity_2_2.setType(new AbsenceTypeEntityEmbeddable(OTHER, 3000L));

            when(repository.findAllByUserIdInAndStartDateLessThanAndEndDateGreaterThanEqual(List.of(userId_1.value(), userId_2.value()), toExclusiveStartOfDay, fromStartOfDay))
                .thenReturn(List.of(absenceEntity_1, absenceEntity_2_1, absenceEntity_2_2));

            final AbsenceType absenceType1 = new AbsenceType(OTHER, 1000L, label(GERMAN, "1000-de", ENGLISH, "1000-en"), YELLOW);
            final AbsenceType absenceType2 = new AbsenceType(OTHER, 2000L, label(GERMAN, "2000-de", ENGLISH, "2000-en"), VIOLET);
            final AbsenceType absenceType3 = new AbsenceType(OTHER, 3000L, label(GERMAN, "3000-de", ENGLISH, "3000-en"), CYAN);

            when(absenceTypeService.findAllByAbsenceTypeSourceIds(List.of(1000L, 2000L, 3000L)))
                .thenReturn(List.of(absenceType1, absenceType2, absenceType3));

            final Map<UserIdComposite, List<Absence>> actual = sut.getAbsencesByUserIds(List.of(userLocalId_1, userLocalId_2), from, toExclusive);

            final Function<Locale, String> anyLabel = locale -> "";
            assertThat(actual).containsExactlyInAnyOrderEntriesOf(Map.of(
                userIdComposite_1, List.of(
                    new Absence(userId_1, absence_1_start.atZone(berlin).toInstant(), absence_1_end.atZone(berlin).toInstant(), FULL, anyLabel, YELLOW, OTHER)
                ),
                userIdComposite_2, List.of(
                    new Absence(userId_2, absence_2_1_start.atZone(berlin).toInstant(), absence_2_1_end.atZone(berlin).toInstant(), MORNING, anyLabel, VIOLET, OTHER),
                    new Absence(userId_2, absence_2_2_start.atZone(berlin).toInstant(), absence_2_2_end.atZone(berlin).toInstant(), NOON, anyLabel, CYAN, OTHER)
                )
            ));
        }

        @Test
        void ensureGetAbsencesWithHours() {

            final ZoneId berlin = ZoneId.of("Europe/Berlin");
            when(userSettingsProvider.zoneId()).thenReturn(berlin);

            final LocalDate from = LocalDate.of(2023, 11, 1);
            final LocalDate toExclusive = LocalDate.of(2023, 11, 30);
            final Instant fromStartOfDay = Instant.from(from.atStartOfDay().atZone(berlin));
            final Instant toExclusiveStartOfDay = Instant.from(toExclusive.atStartOfDay().atZone(berlin));

            final UserId userId = new UserId(UUID.randomUUID().toString());
            final UserLocalId userLocalId = new UserLocalId(1L);
            final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

            when(userManagementService.findAllUsersByLocalIds(List.of(userLocalId))).thenReturn(List.of(
                new User(userIdComposite, "Bruce", null, null, Set.of())
            ));

            final Instant absenceStart = Instant.from(from.plusDays(1).atStartOfDay().atZone(berlin));
            final Instant absenceEnd = Instant.from(from.plusDays(1).atStartOfDay().atZone(berlin));
            final AbsenceWriteEntity absenceEntity = new AbsenceWriteEntity();
            absenceEntity.setUserId(userId.value());
            absenceEntity.setStartDate(absenceStart);
            absenceEntity.setEndDate(absenceEnd);
            absenceEntity.setDayLength(FULL);
            absenceEntity.setType(new AbsenceTypeEntityEmbeddable(OVERTIME, 4242L));
            absenceEntity.setOvertimeHours(Duration.ofHours(8));

            when(repository.findAllByUserIdInAndStartDateLessThanAndEndDateGreaterThanEqual(List.of(userId.value()), toExclusiveStartOfDay, fromStartOfDay))
                .thenReturn(List.of(absenceEntity));

            final AbsenceType absenceType1 = new AbsenceType(OVERTIME, 4242L, label(GERMAN, "4242-de", ENGLISH, "4242-en"), YELLOW);

            when(absenceTypeService.findAllByAbsenceTypeSourceIds(List.of(4242L)))
                .thenReturn(List.of(absenceType1));

            final Map<UserIdComposite, List<Absence>> actual = sut.getAbsencesByUserIds(List.of(userLocalId), from, toExclusive);

            final Function<Locale, String> anyLabel = locale -> "";
            assertThat(actual).containsExactlyInAnyOrderEntriesOf(Map.of(
                userIdComposite, List.of(
                    new Absence(userId, absenceStart.atZone(berlin).toInstant(), absenceEnd.atZone(berlin).toInstant(), FULL, anyLabel, YELLOW, OVERTIME, Duration.ofHours(8))
                )
            ));
        }

        @ParameterizedTest
        @EnumSource(DayLength.class)
        void ensureGetAbsencesByUserIdsLabelForDayLength(DayLength givenDayLength) {

            final ZoneId berlin = ZoneId.of("Europe/Berlin");
            when(userSettingsProvider.zoneId()).thenReturn(berlin);

            final LocalDate from = LocalDate.of(2023, 11, 1);
            final LocalDate toExclusive = LocalDate.of(2023, 11, 30);
            final Instant fromStartOfDay = Instant.from(from.atStartOfDay().atZone(berlin));
            final Instant toExclusiveStartOfDay = Instant.from(toExclusive.atStartOfDay().atZone(berlin));

            final UserId userId = new UserId(UUID.randomUUID().toString());
            final UserLocalId userLocalId = new UserLocalId(1L);
            final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

            when(userManagementService.findAllUsersByLocalIds(List.of(userLocalId))).thenReturn(List.of(
                new User(userIdComposite, "Bruce", null, null, Set.of())
            ));

            final Instant absence_start = Instant.from(from.plusDays(1).atStartOfDay().atZone(berlin));
            final Instant absence_end = Instant.from(from.plusDays(1).atStartOfDay().atZone(berlin));
            final AbsenceWriteEntity absenceEntity = new AbsenceWriteEntity();
            absenceEntity.setUserId(userId.value());
            absenceEntity.setStartDate(absence_start);
            absenceEntity.setEndDate(absence_end);
            absenceEntity.setDayLength(givenDayLength);
            absenceEntity.setType(new AbsenceTypeEntityEmbeddable(OTHER, 1000L));

            when(repository.findAllByUserIdInAndStartDateLessThanAndEndDateGreaterThanEqual(List.of(userId.value()), toExclusiveStartOfDay, fromStartOfDay))
                .thenReturn(List.of(absenceEntity));

            final AbsenceType absenceType = new AbsenceType(OTHER, 1000L, label(GERMAN, "de", ENGLISH, "en"), YELLOW);

            when(absenceTypeService.findAllByAbsenceTypeSourceIds(List.of(1000L)))
                .thenReturn(List.of(absenceType));

            // SupportedLanguages are GERMAN and ENGLISH right now
            when(messageSource.getMessage("absence.label." + givenDayLength, new Object[]{"de"}, GERMAN)).thenReturn("message-de");
            when(messageSource.getMessage("absence.label." + givenDayLength, new Object[]{"en"}, ENGLISH)).thenReturn("message-en");

            final Map<UserIdComposite, List<Absence>> actual = sut.getAbsencesByUserIds(List.of(userLocalId), from, toExclusive);
            final Absence actualAbsence = actual.get(userIdComposite).getFirst();
            assertThat(actualAbsence.label(GERMAN)).isEqualTo("message-de");
            assertThat(actualAbsence.label(ENGLISH)).isEqualTo("message-en");
        }

        @Test
        void ensureCompanyVacationIsIncluded() {

            final ZoneId berlin = ZoneId.of("Europe/Berlin");
            when(userSettingsProvider.zoneId()).thenReturn(berlin);

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

            final Instant vacationStart = Instant.from(from.plusDays(1).atStartOfDay().atZone(berlin));
            final Instant vacationEnd = Instant.from(from.plusDays(1).atStartOfDay().atZone(berlin));

            when(companyVacationService.getCompanyVacations(fromStartOfDay, toExclusiveStartOfDay))
                .thenReturn(List.of(new CompanyVacation(vacationStart, vacationEnd, de.focusshift.zeiterfassung.companyvacation.DayLength.NOON)));

            final Map<UserIdComposite, List<Absence>> actual = sut.getAbsencesByUserIds(List.of(userLocalId_1, userLocalId_2), from, toExclusive);

            final Function<Locale, String> anyLabel = locale -> "";
            assertThat(actual).containsExactlyInAnyOrderEntriesOf(Map.of(
                userIdComposite_1, List.of(
                    new Absence(userId_1, vacationStart, vacationEnd, NOON, anyLabel, YELLOW, HOLIDAY)
                ),
                userIdComposite_2, List.of(
                    new Absence(userId_2, vacationStart, vacationEnd, NOON, anyLabel, YELLOW, HOLIDAY)
                )
            ));
        }
    }

    @Nested
    class GetAbsencesForAllUser {

        @Test
        void ensureGetAbsencesForAllUsersReturnsEmptyListForUsersWithoutAbsences() {

            final ZoneId berlin = ZoneId.of("Europe/Berlin");
            when(userSettingsProvider.zoneId()).thenReturn(berlin);

            final LocalDate from = LocalDate.of(2023, 11, 1);
            final LocalDate toExclusive = LocalDate.of(2023, 11, 30);
            final Instant fromStartOfDay = Instant.from(from.atStartOfDay().atZone(berlin));
            final Instant toExclusiveStartOfDay = Instant.from(toExclusive.atStartOfDay().atZone(berlin));

            final UserId userId = new UserId(UUID.randomUUID().toString());
            final UserLocalId userLocalId = new UserLocalId(1L);
            final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
            final User user = new User(userIdComposite, "", "", new EMailAddress(""), Set.of());
            when(userManagementService.findAllUsers()).thenReturn(List.of(user));

            when(repository.findAllByStartDateLessThanAndEndDateGreaterThanEqual(toExclusiveStartOfDay, fromStartOfDay))
                .thenReturn(List.of());

            final Map<UserIdComposite, List<Absence>> actual = sut.getAbsencesForAllUsers(from, toExclusive);
            assertThat(actual).containsExactlyInAnyOrderEntriesOf(Map.of(userIdComposite, List.of()));
        }

        @Test
        void ensureGetAbsencesForAllUsers() {

            final ZoneId berlin = ZoneId.of("Europe/Berlin");
            when(userSettingsProvider.zoneId()).thenReturn(berlin);

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

            final Instant absence_2_1_start = Instant.from(from.plusDays(1).atStartOfDay().atZone(berlin));
            final Instant absence_2_1_end = Instant.from(from.plusDays(1).atStartOfDay().atZone(berlin));
            final AbsenceWriteEntity absenceEntity_2_1 = new AbsenceWriteEntity();
            absenceEntity_2_1.setUserId(userId_2.value());
            absenceEntity_2_1.setStartDate(absence_2_1_start);
            absenceEntity_2_1.setEndDate(absence_2_1_end);
            absenceEntity_2_1.setDayLength(MORNING);
            absenceEntity_2_1.setType(new AbsenceTypeEntityEmbeddable(OTHER, 2000L));

            final Instant absence_2_2_start = Instant.from(from.plusDays(2).atStartOfDay().atZone(berlin));
            final Instant absence_2_2_end = Instant.from(from.plusDays(2).atStartOfDay().atZone(berlin));
            final AbsenceWriteEntity absenceEntity_2_2 = new AbsenceWriteEntity();
            absenceEntity_2_2.setUserId(userId_2.value());
            absenceEntity_2_2.setStartDate(absence_2_2_start);
            absenceEntity_2_2.setEndDate(absence_2_2_end);
            absenceEntity_2_2.setDayLength(NOON);
            absenceEntity_2_2.setType(new AbsenceTypeEntityEmbeddable(OTHER, 3000L));

            when(repository.findAllByStartDateLessThanAndEndDateGreaterThanEqual(toExclusiveStartOfDay, fromStartOfDay))
                .thenReturn(List.of(absenceEntity_1, absenceEntity_2_1, absenceEntity_2_2));

            final AbsenceType absenceType1 = new AbsenceType(OTHER, 1000L, label(GERMAN, "1000-de", ENGLISH, "1000-en"), YELLOW);
            final AbsenceType absenceType2 = new AbsenceType(OTHER, 2000L, label(GERMAN, "2000-de", ENGLISH, "2000-en"), VIOLET);
            final AbsenceType absenceType3 = new AbsenceType(OTHER, 3000L, label(GERMAN, "3000-de", ENGLISH, "3000-en"), CYAN);

            when(absenceTypeService.findAllByAbsenceTypeSourceIds(List.of(1000L, 2000L, 3000L)))
                .thenReturn(List.of(absenceType1, absenceType2, absenceType3));

            final Map<UserIdComposite, List<Absence>> actual = sut.getAbsencesForAllUsers(from, toExclusive);

            final Function<Locale, String> anyLabel = locale -> "";
            assertThat(actual).containsExactlyInAnyOrderEntriesOf(Map.of(
                userIdComposite_1, List.of(
                    new Absence(userId_1, absence_1_start.atZone(berlin).toInstant(), absence_1_end.atZone(berlin).toInstant(), FULL, anyLabel, YELLOW, OTHER)
                ),
                userIdComposite_2, List.of(
                    new Absence(userId_2, absence_2_1_start.atZone(berlin).toInstant(), absence_2_1_end.atZone(berlin).toInstant(), MORNING, anyLabel, VIOLET, OTHER),
                    new Absence(userId_2, absence_2_2_start.atZone(berlin).toInstant(), absence_2_2_end.atZone(berlin).toInstant(), NOON, anyLabel, CYAN, OTHER)
                )
            ));
        }

        @ParameterizedTest
        @EnumSource(DayLength.class)
        void ensureGetAbsencesForAllUsersLabelForDayLength(DayLength givenDayLength) {

            final ZoneId berlin = ZoneId.of("Europe/Berlin");
            when(userSettingsProvider.zoneId()).thenReturn(berlin);

            final LocalDate from = LocalDate.of(2023, 11, 1);
            final LocalDate toExclusive = LocalDate.of(2023, 11, 30);
            final Instant fromStartOfDay = Instant.from(from.atStartOfDay().atZone(berlin));
            final Instant toExclusiveStartOfDay = Instant.from(toExclusive.atStartOfDay().atZone(berlin));

            final UserId userId = new UserId(UUID.randomUUID().toString());
            final UserLocalId userLocalId = new UserLocalId(1L);
            final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
            final User user = new User(userIdComposite, "", "", new EMailAddress(""), Set.of());

            when(userManagementService.findAllUsers()).thenReturn(List.of(user));

            final Instant absence_start = Instant.from(from.plusDays(1).atStartOfDay().atZone(berlin));
            final Instant absence_end = Instant.from(from.plusDays(1).atStartOfDay().atZone(berlin));
            final AbsenceWriteEntity absenceEntity = new AbsenceWriteEntity();
            absenceEntity.setUserId(userId.value());
            absenceEntity.setStartDate(absence_start);
            absenceEntity.setEndDate(absence_end);
            absenceEntity.setDayLength(givenDayLength);
            absenceEntity.setType(new AbsenceTypeEntityEmbeddable(OTHER, 1000L));

            when(repository.findAllByStartDateLessThanAndEndDateGreaterThanEqual(toExclusiveStartOfDay, fromStartOfDay))
                .thenReturn(List.of(absenceEntity));

            final AbsenceType absenceType = new AbsenceType(OTHER, 1000L, label(GERMAN, "de", ENGLISH, "en"), YELLOW);

            when(absenceTypeService.findAllByAbsenceTypeSourceIds(List.of(1000L)))
                .thenReturn(List.of(absenceType));

            // SupportedLanguages are GERMAN and ENGLISH right now
            when(messageSource.getMessage("absence.label." + givenDayLength, new Object[]{"de"}, GERMAN)).thenReturn("message-de");
            when(messageSource.getMessage("absence.label." + givenDayLength, new Object[]{"en"}, ENGLISH)).thenReturn("message-en");

            final Map<UserIdComposite, List<Absence>> actual = sut.getAbsencesForAllUsers(from, toExclusive);
            final Absence actualAbsence = actual.get(userIdComposite).getFirst();
            assertThat(actualAbsence.label(GERMAN)).isEqualTo("message-de");
            assertThat(actualAbsence.label(ENGLISH)).isEqualTo("message-en");
        }

        @Test
        void ensureCompanyVacationIsIncluded() {

            final ZoneId berlin = ZoneId.of("Europe/Berlin");
            when(userSettingsProvider.zoneId()).thenReturn(berlin);

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

            when(userManagementService.findAllUsers()).thenReturn(List.of(
                new User(userIdComposite_1, "Bruce", null, null, Set.of()),
                new User(userIdComposite_2, "John", null, null, Set.of())
            ));

            final Instant vacationStart = Instant.from(from.plusDays(1).atStartOfDay().atZone(berlin));
            final Instant vacationEnd = Instant.from(from.plusDays(1).atStartOfDay().atZone(berlin));

            when(companyVacationService.getCompanyVacations(fromStartOfDay, toExclusiveStartOfDay))
                .thenReturn(List.of(new CompanyVacation(vacationStart, vacationEnd, de.focusshift.zeiterfassung.companyvacation.DayLength.NOON)));

            final Map<UserIdComposite, List<Absence>> actual = sut.getAbsencesForAllUsers(from, toExclusive);

            final Function<Locale, String> anyLabel = locale -> "";
            assertThat(actual).containsExactlyInAnyOrderEntriesOf(Map.of(
                userIdComposite_1, List.of(
                    new Absence(userId_1, vacationStart, vacationEnd, NOON, anyLabel, YELLOW, HOLIDAY)
                ),
                userIdComposite_2, List.of(
                    new Absence(userId_2, vacationStart, vacationEnd, NOON, anyLabel, YELLOW, HOLIDAY)
                )
            ));
        }
    }

    private static Function<Locale, String> label(Locale locale, String label, Locale locale2, String label2) {
        final Map<Locale, String> labelByLocale = Map.of(locale, label, locale2, label2);
        return labelByLocale::get;
    }
}
