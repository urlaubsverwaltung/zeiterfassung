package de.focusshift.zeiterfassung.workingtime;

import de.focusshift.zeiterfassung.publicholiday.FederalState;
import de.focusshift.zeiterfassung.settings.FederalStateSettings;
import de.focusshift.zeiterfassung.settings.FederalStateSettingsService;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static de.focusshift.zeiterfassung.publicholiday.FederalState.GERMANY_BADEN_WUERTTEMBERG;
import static de.focusshift.zeiterfassung.publicholiday.FederalState.GERMANY_BERLIN;
import static de.focusshift.zeiterfassung.publicholiday.FederalState.GLOBAL;
import static de.focusshift.zeiterfassung.publicholiday.FederalState.NONE;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkTimeServiceImplTest {

    private WorkTimeServiceImpl sut;

    @Mock
    private WorkingTimeRepository workingTimeRepository;
    @Mock
    private UserManagementService userManagementService;
    @Mock
    private FederalStateSettingsService federalStateSettingsService;

    private static final Clock clockFixed = Clock.fixed(Clock.systemUTC().instant(), UTC);

    @BeforeEach
    void setUp() {
        sut = new WorkTimeServiceImpl(workingTimeRepository, userManagementService, federalStateSettingsService, clockFixed);
    }

    @Nested
    class GetWorkingTimeById {

        @Test
        void returnsEmptyOptionalWhenItDoesNotExist() {

            final UUID uuid = UUID.randomUUID();
            when(workingTimeRepository.findById(uuid)).thenReturn(Optional.empty());

            final Optional<WorkingTime> actual = sut.getWorkingTimeById(new WorkingTimeId(uuid));
            assertThat(actual).isEmpty();
        }

        @Test
        void returnsWorkingTime() {

            final UserId userId = new UserId("userid");
            final UserLocalId userLocalId = new UserLocalId(42L);
            final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
            final User user = new User(userIdComposite, "", "", new EMailAddress(""), Set.of());
            when(userManagementService.findUserByLocalId(userLocalId)).thenReturn(Optional.of(user));

            when(federalStateSettingsService.getFederalStateSettings()).thenReturn(federalStateSettings(GERMANY_BERLIN));

            final UUID workingTimeUuid = UUID.randomUUID();
            final WorkingTimeEntity entity = anyWorkingTimeEntity(workingTimeUuid, 42L, null);
            entity.setId(workingTimeUuid);
            entity.setUserId(userLocalId.value());
            entity.setFederalState(GERMANY_BADEN_WUERTTEMBERG);
            entity.setWorksOnPublicHoliday(true);

            when(workingTimeRepository.findById(workingTimeUuid)).thenReturn(Optional.of(entity));
            when(workingTimeRepository.findAllByUserId(userLocalId.value())).thenReturn(List.of(entity));

            final Optional<WorkingTime> actual = sut.getWorkingTimeById(new WorkingTimeId(workingTimeUuid));
            assertThat(actual).hasValueSatisfying(workingTime -> {
                assertThat(workingTime.id()).isEqualTo(new WorkingTimeId(workingTimeUuid));
                assertThat(workingTime.userIdComposite()).isEqualTo(userIdComposite);
                assertThat(workingTime.isCurrent()).isTrue();
                assertThat(workingTime.validFrom()).isEmpty();
                assertThat(workingTime.validTo()).isEmpty();
                assertThat(workingTime.minValidFrom()).isEmpty();
                assertThat(workingTime.federalState()).isEqualTo(GERMANY_BADEN_WUERTTEMBERG);
                assertThat(workingTime.worksOnPublicHoliday()).isTrue();
            });
        }

        @Test
        void returnsWorkingTimeWhichIsNotCurrent() {

            final UserId userId = new UserId("userid");
            final UserLocalId userLocalId = new UserLocalId(42L);
            final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
            final User user = new User(userIdComposite, "", "", new EMailAddress(""), Set.of());
            when(userManagementService.findUserByLocalId(userLocalId)).thenReturn(Optional.of(user));

            when(federalStateSettingsService.getFederalStateSettings()).thenReturn(federalStateSettings(GERMANY_BERLIN));

            final UUID workingTimeUuid = UUID.randomUUID();
            final WorkingTimeEntity entity = anyWorkingTimeEntity(workingTimeUuid, 42L, null);
            entity.setId(workingTimeUuid);
            entity.setUserId(userLocalId.value());

            final LocalDate yesterday = LocalDate.now(clockFixed).minusDays(1);
            final WorkingTimeEntity entity_2 = anyWorkingTimeEntity(workingTimeUuid, 42L, yesterday);
            entity_2.setId(UUID.randomUUID());
            entity_2.setUserId(userLocalId.value());

            when(workingTimeRepository.findById(workingTimeUuid)).thenReturn(Optional.of(entity));
            when(workingTimeRepository.findAllByUserId(userLocalId.value())).thenReturn(List.of(entity, entity_2));

            final Optional<WorkingTime> actual = sut.getWorkingTimeById(new WorkingTimeId(workingTimeUuid));
            assertThat(actual).hasValueSatisfying(workingTime -> {
                assertThat(workingTime.isCurrent()).isFalse();
                assertThat(workingTime.validTo()).hasValue(yesterday.minusDays(1));
            });
        }
    }

    @Nested
    class GetAllWorkingTimesByUser {

        @Test
        void returnsDefaultAndPersistsIt() {

            final User user = anyUser();
            when(userManagementService.findUserByLocalId(user.userLocalId())).thenReturn(Optional.of(user));

            when(workingTimeRepository.findAllByUserId(user.userLocalId().value())).thenReturn(List.of());

            when(federalStateSettingsService.getFederalStateSettings()).thenReturn(federalStateSettings(GERMANY_BERLIN));

            final UUID workingTimeId = UUID.randomUUID();
            when(workingTimeRepository.save(any())).thenAnswer(invocation -> {
                final WorkingTimeEntity entity = cloneEntity(invocation.getArgument(0));
                entity.setId(workingTimeId);
                return entity;
            });

            final List<WorkingTime> actual = sut.getAllWorkingTimesByUser(user.userLocalId());
            assertThat(actual).hasSize(1);
            assertThat(actual.getFirst()).satisfies(workingTime -> {
                assertThat(workingTime.id()).isEqualTo(new WorkingTimeId(workingTimeId));
                assertThat(workingTime.userIdComposite()).isEqualTo(user.userIdComposite());
                assertThat(workingTime.validFrom()).isEmpty();
                assertThat(workingTime.federalState()).isEqualTo(GLOBAL);
                assertThat(workingTime.worksOnPublicHoliday()).isFalse();
                assertThat(workingTime.isWorksOnPublicHolidayGlobal()).isTrue();
                assertThat(workingTime.getMonday()).isEqualTo(PlannedWorkingHours.EIGHT);
                assertThat(workingTime.getTuesday()).isEqualTo(PlannedWorkingHours.EIGHT);
                assertThat(workingTime.getWednesday()).isEqualTo(PlannedWorkingHours.EIGHT);
                assertThat(workingTime.getThursday()).isEqualTo(PlannedWorkingHours.EIGHT);
                assertThat(workingTime.getFriday()).isEqualTo(PlannedWorkingHours.EIGHT);
                assertThat(workingTime.getSaturday()).isEqualTo(PlannedWorkingHours.ZERO);
                assertThat(workingTime.getSunday()).isEqualTo(PlannedWorkingHours.ZERO);
            });

            final ArgumentCaptor<WorkingTimeEntity> captor = ArgumentCaptor.forClass(WorkingTimeEntity.class);
            verify(workingTimeRepository).save(captor.capture());

            final WorkingTimeEntity persistedEntity = captor.getValue();
            assertThat(persistedEntity.getId()).isNull();
            assertThat(persistedEntity.getUserId()).isEqualTo(user.userLocalId().value());
            assertThat(persistedEntity.getValidFrom()).isNull();
            assertThat(persistedEntity.getFederalState()).isEqualTo(GLOBAL);
            assertThat(persistedEntity.isWorksOnPublicHoliday()).isNull();
            assertThat(persistedEntity.getMonday()).isEqualTo("PT8H");
            assertThat(persistedEntity.getTuesday()).isEqualTo("PT8H");
            assertThat(persistedEntity.getWednesday()).isEqualTo("PT8H");
            assertThat(persistedEntity.getThursday()).isEqualTo("PT8H");
            assertThat(persistedEntity.getFriday()).isEqualTo("PT8H");
            assertThat(persistedEntity.getSaturday()).isEqualTo("PT0S");
            assertThat(persistedEntity.getSunday()).isEqualTo("PT0S");
        }

        @Test
        void isSortedByValidFrom() {

            final User user = anyUser();
            when(userManagementService.findUserByLocalId(user.userLocalId())).thenReturn(Optional.of(user));

            when(federalStateSettingsService.getFederalStateSettings()).thenReturn(federalStateSettings(GERMANY_BERLIN));

            final UUID workingTimeId_1 = UUID.randomUUID();
            final UUID workingTimeId_2 = UUID.randomUUID();
            final UUID workingTimeId_3 = UUID.randomUUID();
            final WorkingTimeEntity entity_1 = anyWorkingTimeEntity(workingTimeId_1, user.userLocalId().value(), null);
            final WorkingTimeEntity entity_2 = anyWorkingTimeEntity(workingTimeId_2, user.userLocalId().value(), LocalDate.of(2024, 1, 1));
            final WorkingTimeEntity entity_3 = anyWorkingTimeEntity(workingTimeId_3, user.userLocalId().value(), LocalDate.of(2024, 7, 1));
            when(workingTimeRepository.findAllByUserId(user.userLocalId().value())).thenReturn(List.of(entity_1, entity_2, entity_3));

            final List<WorkingTime> actual = sut.getAllWorkingTimesByUser(user.userLocalId());

            assertThat(actual).hasSize(3);
            assertThat(actual.get(0).id()).isEqualTo(new WorkingTimeId(workingTimeId_3));
            assertThat(actual.get(1).id()).isEqualTo(new WorkingTimeId(workingTimeId_2));
            assertThat(actual.get(2).id()).isEqualTo(new WorkingTimeId(workingTimeId_1));
        }

        @Test
        void setsCurrentFlagCorrectlyWhenThereIsOnlyOneWorkingTime() {

            final User user = anyUser();
            when(userManagementService.findUserByLocalId(user.userLocalId())).thenReturn(Optional.of(user));

            when(federalStateSettingsService.getFederalStateSettings()).thenReturn(federalStateSettings(GERMANY_BERLIN));

            final UUID workingTimeId = UUID.randomUUID();
            final WorkingTimeEntity entity = anyWorkingTimeEntity(workingTimeId, user.userLocalId().value(), null);
            when(workingTimeRepository.findAllByUserId(user.userLocalId().value())).thenReturn(List.of(entity));

            final List<WorkingTime> actual = sut.getAllWorkingTimesByUser(user.userLocalId());

            assertThat(actual).hasSize(1);
            assertThat(actual.getFirst().isCurrent()).isTrue();
        }

        @Test
        void setsCurrentFlagCorrectlyWhenSecondOneIsInTheFuture() {

            final LocalDate today = LocalDate.now(clockFixed);

            final User user = anyUser();
            when(userManagementService.findUserByLocalId(user.userLocalId())).thenReturn(Optional.of(user));

            when(federalStateSettingsService.getFederalStateSettings()).thenReturn(federalStateSettings(GERMANY_BERLIN));

            final UUID workingTimeId_1 = UUID.randomUUID();
            final UUID workingTimeId_2 = UUID.randomUUID();
            final UUID workingTimeId_3 = UUID.randomUUID();
            final WorkingTimeEntity entity_1 = anyWorkingTimeEntity(workingTimeId_1, user.userLocalId().value(), null);
            final WorkingTimeEntity entity_2 = anyWorkingTimeEntity(workingTimeId_2, user.userLocalId().value(), today.plusDays(1));
            final WorkingTimeEntity entity_3 = anyWorkingTimeEntity(workingTimeId_3, user.userLocalId().value(), today.plusDays(2));
            when(workingTimeRepository.findAllByUserId(user.userLocalId().value())).thenReturn(List.of(entity_1, entity_2, entity_3));

            final List<WorkingTime> actual = sut.getAllWorkingTimesByUser(user.userLocalId());

            assertThat(actual).hasSize(3);
            assertThat(actual.get(0).isCurrent()).isFalse();
            assertThat(actual.get(1).isCurrent()).isFalse();
            assertThat(actual.get(2).isCurrent()).isTrue();
        }

        @Test
        void setsCurrentFlagCorrectlyWhenSecondOneIsValidFromToday() {

            final LocalDate today = LocalDate.now(clockFixed);

            final User user = anyUser();
            when(userManagementService.findUserByLocalId(user.userLocalId())).thenReturn(Optional.of(user));

            when(federalStateSettingsService.getFederalStateSettings()).thenReturn(federalStateSettings(GERMANY_BERLIN));

            final UUID workingTimeId_1 = UUID.randomUUID();
            final UUID workingTimeId_2 = UUID.randomUUID();
            final UUID workingTimeId_3 = UUID.randomUUID();
            final WorkingTimeEntity entity_1 = anyWorkingTimeEntity(workingTimeId_1, user.userLocalId().value(), null);
            final WorkingTimeEntity entity_2 = anyWorkingTimeEntity(workingTimeId_2, user.userLocalId().value(), today);
            final WorkingTimeEntity entity_3 = anyWorkingTimeEntity(workingTimeId_3, user.userLocalId().value(), today.plusDays(1));
            when(workingTimeRepository.findAllByUserId(user.userLocalId().value())).thenReturn(List.of(entity_1, entity_2, entity_3));

            final List<WorkingTime> actual = sut.getAllWorkingTimesByUser(user.userLocalId());

            assertThat(actual).hasSize(3);
            assertThat(actual.get(0).isCurrent()).isFalse();
            assertThat(actual.get(1).isCurrent()).isTrue();
            assertThat(actual.get(2).isCurrent()).isFalse();
        }

        @Test
        void setsCurrentFlagCorrectlyWhenSecondOneIsValidAndThirdOneInTheFuture() {

            final LocalDate today = LocalDate.now(clockFixed);

            final User user = anyUser();
            when(userManagementService.findUserByLocalId(user.userLocalId())).thenReturn(Optional.of(user));

            when(federalStateSettingsService.getFederalStateSettings()).thenReturn(federalStateSettings(GERMANY_BERLIN));

            final UUID workingTimeId_1 = UUID.randomUUID();
            final UUID workingTimeId_2 = UUID.randomUUID();
            final UUID workingTimeId_3 = UUID.randomUUID();
            final WorkingTimeEntity entity_1 = anyWorkingTimeEntity(workingTimeId_1, user.userLocalId().value(), null);
            final WorkingTimeEntity entity_2 = anyWorkingTimeEntity(workingTimeId_2, user.userLocalId().value(), today.minusDays(1));
            final WorkingTimeEntity entity_3 = anyWorkingTimeEntity(workingTimeId_3, user.userLocalId().value(), today.plusDays(1));
            when(workingTimeRepository.findAllByUserId(user.userLocalId().value())).thenReturn(List.of(entity_1, entity_2, entity_3));

            final List<WorkingTime> actual = sut.getAllWorkingTimesByUser(user.userLocalId());

            assertThat(actual).hasSize(3);
            assertThat(actual.get(0).isCurrent()).isFalse();
            assertThat(actual.get(1).isCurrent()).isTrue();
            assertThat(actual.get(2).isCurrent()).isFalse();
        }

        @Test
        void setsCurrentFlagCorrectlyWhenThirdOneIsValidFromToday() {

            final LocalDate today = LocalDate.now(clockFixed);

            final User user = anyUser();
            when(userManagementService.findUserByLocalId(user.userLocalId())).thenReturn(Optional.of(user));

            when(federalStateSettingsService.getFederalStateSettings()).thenReturn(federalStateSettings(GERMANY_BERLIN));

            final UUID workingTimeId_1 = UUID.randomUUID();
            final UUID workingTimeId_2 = UUID.randomUUID();
            final UUID workingTimeId_3 = UUID.randomUUID();
            final WorkingTimeEntity entity_1 = anyWorkingTimeEntity(workingTimeId_1, user.userLocalId().value(), null);
            final WorkingTimeEntity entity_2 = anyWorkingTimeEntity(workingTimeId_2, user.userLocalId().value(), today.minusDays(1));
            final WorkingTimeEntity entity_3 = anyWorkingTimeEntity(workingTimeId_3, user.userLocalId().value(), today);
            when(workingTimeRepository.findAllByUserId(user.userLocalId().value())).thenReturn(List.of(entity_1, entity_2, entity_3));

            final List<WorkingTime> actual = sut.getAllWorkingTimesByUser(user.userLocalId());

            assertThat(actual).hasSize(3);
            assertThat(actual.get(0).isCurrent()).isTrue();
            assertThat(actual.get(1).isCurrent()).isFalse();
            assertThat(actual.get(2).isCurrent()).isFalse();
        }

        @Test
        void setsCurrentFlagCorrectlyWhenThirdOneIsValidFrom() {

            final LocalDate today = LocalDate.now(clockFixed);

            final User user = anyUser();
            when(userManagementService.findUserByLocalId(user.userLocalId())).thenReturn(Optional.of(user));

            when(federalStateSettingsService.getFederalStateSettings()).thenReturn(federalStateSettings(GERMANY_BERLIN));

            final UUID workingTimeId_1 = UUID.randomUUID();
            final UUID workingTimeId_2 = UUID.randomUUID();
            final UUID workingTimeId_3 = UUID.randomUUID();
            final WorkingTimeEntity entity_1 = anyWorkingTimeEntity(workingTimeId_1, user.userLocalId().value(), null);
            final WorkingTimeEntity entity_2 = anyWorkingTimeEntity(workingTimeId_2, user.userLocalId().value(), today.minusDays(2));
            final WorkingTimeEntity entity_3 = anyWorkingTimeEntity(workingTimeId_3, user.userLocalId().value(), today.minusDays(1));
            when(workingTimeRepository.findAllByUserId(user.userLocalId().value())).thenReturn(List.of(entity_1, entity_2, entity_3));

            final List<WorkingTime> actual = sut.getAllWorkingTimesByUser(user.userLocalId());

            assertThat(actual).hasSize(3);
            assertThat(actual.get(0).isCurrent()).isTrue();
            assertThat(actual.get(1).isCurrent()).isFalse();
            assertThat(actual.get(2).isCurrent()).isFalse();
        }
    }

    @Test
    void ensureGetWorkingTimeByUsers() {

        when(federalStateSettingsService.getFederalStateSettings()).thenReturn(federalStateSettings(GERMANY_BERLIN));

        final UUID workingTimeId_1 = UUID.randomUUID();
        final WorkingTimeEntity entity_1 = new WorkingTimeEntity();
        entity_1.setId(workingTimeId_1);
        entity_1.setUserId(1L);
        entity_1.setFederalState(FederalState.NONE);
        entity_1.setWorksOnPublicHoliday(false);
        entity_1.setMonday("PT1H");
        entity_1.setTuesday("PT2H");
        entity_1.setWednesday("PT3H");
        entity_1.setThursday("PT4H");
        entity_1.setFriday("PT5H");
        entity_1.setSaturday("PT6H");
        entity_1.setSunday("PT7H");

        final UUID workingTimeId_2 = UUID.randomUUID();
        final WorkingTimeEntity entity_2 = new WorkingTimeEntity();
        entity_2.setId(workingTimeId_2);
        entity_2.setUserId(2L);
        entity_2.setFederalState(GERMANY_BADEN_WUERTTEMBERG);
        entity_2.setWorksOnPublicHoliday(true);
        entity_2.setMonday("PT7H");
        entity_2.setTuesday("PT6H");
        entity_2.setWednesday("PT5H");
        entity_2.setThursday("PT4H");
        entity_2.setFriday("PT3H");
        entity_2.setSaturday("PT2H");
        entity_2.setSunday("PT1H");

        when(workingTimeRepository.findAllByUserIdIsIn(List.of(1L, 2L))).thenReturn(List.of(entity_1, entity_2));

        final UserId userId_1 = new UserId("uuid-1");
        final UserLocalId userLocalId_1 = new UserLocalId(1L);
        final UserIdComposite userIdComposite_1 = new UserIdComposite(userId_1, userLocalId_1);
        final User user_1 = new User(userIdComposite_1, "Bruce", "Wayne", new EMailAddress(""), Set.of());

        final UserId userId_2 = new UserId("uuid-2");
        final UserLocalId userLocalId_2 = new UserLocalId(2L);
        final UserIdComposite userIdComposite_2 = new UserIdComposite(userId_2, userLocalId_2);
        final User user_2 = new User(userIdComposite_2, "Clark", "Kent", new EMailAddress(""), Set.of());

        when(userManagementService.findAllUsersByLocalIds(List.of(userLocalId_1, userLocalId_2)))
            .thenReturn(List.of(user_1, user_2));

        final LocalDate from = LocalDate.now(clockFixed);
        final LocalDate toExclusive = LocalDate.now(clockFixed).plusWeeks(1);
        final Map<UserIdComposite, List<WorkingTime>> actual = sut.getWorkingTimesByUsers(from, toExclusive, List.of(new UserLocalId(1L), new UserLocalId(2L)));

        assertThat(actual)
            .hasEntrySatisfying(userIdComposite_1, workingTimes -> {
                assertThat(workingTimes).hasSize(1);
                assertThat(workingTimes.getFirst()).satisfies(workingTime -> {
                    assertThat(workingTime.userIdComposite()).isEqualTo(userIdComposite_1);
                    assertThat(workingTime.id()).isEqualTo(new WorkingTimeId(workingTimeId_1));
                    assertThat(workingTime.federalState()).isEqualTo(NONE);
                    assertThat(workingTime.worksOnPublicHoliday()).isFalse();
                    assertThat(workingTime.getMonday().duration()).isEqualTo(Duration.ofHours(1));
                    assertThat(workingTime.getTuesday().duration()).isEqualTo(Duration.ofHours(2));
                    assertThat(workingTime.getWednesday().duration()).isEqualTo(Duration.ofHours(3));
                    assertThat(workingTime.getThursday().duration()).isEqualTo(Duration.ofHours(4));
                    assertThat(workingTime.getFriday().duration()).isEqualTo(Duration.ofHours(5));
                    assertThat(workingTime.getSaturday().duration()).isEqualTo(Duration.ofHours(6));
                    assertThat(workingTime.getSunday().duration()).isEqualTo(Duration.ofHours(7));
                });
            })
            .hasEntrySatisfying(userIdComposite_2, workingTimes -> {
                assertThat(workingTimes).hasSize(1);
                assertThat(workingTimes.getFirst()).satisfies(workingTime -> {
                    assertThat(workingTime.userIdComposite()).isEqualTo(userIdComposite_2);
                    assertThat(workingTime.id()).isEqualTo(new WorkingTimeId(workingTimeId_2));
                    assertThat(workingTime.federalState()).isEqualTo(GERMANY_BADEN_WUERTTEMBERG);
                    assertThat(workingTime.worksOnPublicHoliday()).isTrue();
                    assertThat(workingTime.getMonday().duration()).isEqualTo(Duration.ofHours(7));
                    assertThat(workingTime.getTuesday().duration()).isEqualTo(Duration.ofHours(6));
                    assertThat(workingTime.getWednesday().duration()).isEqualTo(Duration.ofHours(5));
                    assertThat(workingTime.getThursday().duration()).isEqualTo(Duration.ofHours(4));
                    assertThat(workingTime.getFriday().duration()).isEqualTo(Duration.ofHours(3));
                    assertThat(workingTime.getSaturday().duration()).isEqualTo(Duration.ofHours(2));
                    assertThat(workingTime.getSunday().duration()).isEqualTo(Duration.ofHours(1));
                });
            });
    }

    @Test
    void ensureGetWorkingTimeByUsersAddsDefaultWorkingTimeForUsersWithoutExplicitOne() {

        when(workingTimeRepository.findAllByUserIdIsIn(List.of(1L))).thenReturn(List.of());

        final UserId userId_1 = new UserId("uuid-1");
        final UserLocalId userLocalId_1 = new UserLocalId(1L);
        final UserIdComposite userIdComposite_1 = new UserIdComposite(userId_1, userLocalId_1);
        final User user_1 = new User(userIdComposite_1, "Bruce", "Wayne", new EMailAddress(""), Set.of());

        when(userManagementService.findAllUsersByLocalIds(List.of(userLocalId_1)))
            .thenReturn(List.of(user_1));

        when(federalStateSettingsService.getFederalStateSettings()).thenReturn(federalStateSettings(GERMANY_BERLIN));

        final LocalDate from = LocalDate.now(clockFixed);
        final LocalDate toExclusive = LocalDate.now(clockFixed).plusWeeks(1);
        final Map<UserIdComposite, List<WorkingTime>> actual = sut.getWorkingTimesByUsers(from, toExclusive, List.of(new UserLocalId(1L)));

        assertThat(actual)
            .hasEntrySatisfying(userIdComposite_1, workingTimes -> {
                assertThat(workingTimes).hasSize(1);
                assertThat(workingTimes.getFirst()).satisfies(workingTime -> {
                    assertThat(workingTime.userIdComposite()).isEqualTo(userIdComposite_1);
                    assertThat(workingTime.federalState()).isEqualTo(GLOBAL);
                    assertThat(workingTime.worksOnPublicHoliday()).isFalse();
                    assertThat(workingTime.getMonday().duration()).isEqualTo(Duration.ofHours(8));
                    assertThat(workingTime.getTuesday().duration()).isEqualTo(Duration.ofHours(8));
                    assertThat(workingTime.getWednesday().duration()).isEqualTo(Duration.ofHours(8));
                    assertThat(workingTime.getThursday().duration()).isEqualTo(Duration.ofHours(8));
                    assertThat(workingTime.getFriday().duration()).isEqualTo(Duration.ofHours(8));
                    assertThat(workingTime.getSaturday().duration()).isEqualTo(Duration.ofHours(0));
                    assertThat(workingTime.getSunday().duration()).isEqualTo(Duration.ofHours(0));
                });
            });
    }

    @Test
    void ensureGetAllWorkingTimeByUsers() {

        when(federalStateSettingsService.getFederalStateSettings()).thenReturn(federalStateSettings(GERMANY_BERLIN));

        final UUID workingTimeId_1 = UUID.randomUUID();
        final WorkingTimeEntity entity_1 = new WorkingTimeEntity();
        entity_1.setId(workingTimeId_1);
        entity_1.setUserId(1L);
        entity_1.setFederalState(FederalState.NONE);
        entity_1.setWorksOnPublicHoliday(false);
        entity_1.setMonday("PT1H");
        entity_1.setTuesday("PT2H");
        entity_1.setWednesday("PT3H");
        entity_1.setThursday("PT4H");
        entity_1.setFriday("PT5H");
        entity_1.setSaturday("PT6H");
        entity_1.setSunday("PT7H");

        final UUID workingTimeId_2 = UUID.randomUUID();
        final WorkingTimeEntity entity_2 = new WorkingTimeEntity();
        entity_2.setId(workingTimeId_2);
        entity_2.setUserId(2L);
        entity_2.setFederalState(GERMANY_BADEN_WUERTTEMBERG);
        entity_2.setWorksOnPublicHoliday(true);
        entity_2.setMonday("PT7H");
        entity_2.setTuesday("PT6H");
        entity_2.setWednesday("PT5H");
        entity_2.setThursday("PT4H");
        entity_2.setFriday("PT3H");
        entity_2.setSaturday("PT2H");
        entity_2.setSunday("PT1H");

        final UserId userId_1 = new UserId("uuid-1");
        final UserLocalId userLocalId_1 = new UserLocalId(1L);
        final UserIdComposite userIdComposite_1 = new UserIdComposite(userId_1, userLocalId_1);
        final User user_1 = new User(userIdComposite_1, "Bruce", "Wayne", new EMailAddress(""), Set.of());

        final UserId userId_2 = new UserId("uuid-2");
        final UserLocalId userLocalId_2 = new UserLocalId(2L);
        final UserIdComposite userIdComposite_2 = new UserIdComposite(userId_2, userLocalId_2);
        final User user_2 = new User(userIdComposite_2, "Clark", "Kent", new EMailAddress(""), Set.of());

        when(userManagementService.findAllUsers()).thenReturn(List.of(user_1, user_2));
        when(workingTimeRepository.findAllByUserIdIsIn(List.of(1L, 2L))).thenReturn(List.of(entity_1, entity_2));

        final LocalDate from = LocalDate.now(clockFixed);
        final LocalDate toExclusive = LocalDate.now(clockFixed).plusWeeks(1);
        final Map<UserIdComposite, List<WorkingTime>> actual = sut.getAllWorkingTimes(from, toExclusive);

        assertThat(actual)
            .hasEntrySatisfying(userIdComposite_1, workingTimes -> {
                assertThat(workingTimes).hasSize(1);
                assertThat(workingTimes.getFirst()).satisfies(workingTime -> {
                    assertThat(workingTime.userIdComposite()).isEqualTo(userIdComposite_1);
                    assertThat(workingTime.id()).isEqualTo(new WorkingTimeId(workingTimeId_1));
                    assertThat(workingTime.federalState()).isEqualTo(NONE);
                    assertThat(workingTime.worksOnPublicHoliday()).isFalse();
                    assertThat(workingTime.getMonday().duration()).isEqualTo(Duration.ofHours(1));
                    assertThat(workingTime.getTuesday().duration()).isEqualTo(Duration.ofHours(2));
                    assertThat(workingTime.getWednesday().duration()).isEqualTo(Duration.ofHours(3));
                    assertThat(workingTime.getThursday().duration()).isEqualTo(Duration.ofHours(4));
                    assertThat(workingTime.getFriday().duration()).isEqualTo(Duration.ofHours(5));
                    assertThat(workingTime.getSaturday().duration()).isEqualTo(Duration.ofHours(6));
                    assertThat(workingTime.getSunday().duration()).isEqualTo(Duration.ofHours(7));
                });
            })
            .hasEntrySatisfying(userIdComposite_2, workingTimes -> {
                assertThat(workingTimes).hasSize(1);
                assertThat(workingTimes.getFirst()).satisfies(workingTime -> {
                    assertThat(workingTime.userIdComposite()).isEqualTo(userIdComposite_2);
                    assertThat(workingTime.id()).isEqualTo(new WorkingTimeId(workingTimeId_2));
                    assertThat(workingTime.federalState()).isEqualTo(GERMANY_BADEN_WUERTTEMBERG);
                    assertThat(workingTime.worksOnPublicHoliday()).isTrue();
                    assertThat(workingTime.getMonday().duration()).isEqualTo(Duration.ofHours(7));
                    assertThat(workingTime.getTuesday().duration()).isEqualTo(Duration.ofHours(6));
                    assertThat(workingTime.getWednesday().duration()).isEqualTo(Duration.ofHours(5));
                    assertThat(workingTime.getThursday().duration()).isEqualTo(Duration.ofHours(4));
                    assertThat(workingTime.getFriday().duration()).isEqualTo(Duration.ofHours(3));
                    assertThat(workingTime.getSaturday().duration()).isEqualTo(Duration.ofHours(2));
                    assertThat(workingTime.getSunday().duration()).isEqualTo(Duration.ofHours(1));
                });
            });
    }

    @Test
    void ensureGetAllWorkingTimesByUsersAddsDefaultWorkingTimeForUsersWithoutExplicitOne() {

        final UserId userId_1 = new UserId("uuid-1");
        final UserLocalId userLocalId_1 = new UserLocalId(1L);
        final UserIdComposite userIdComposite_1 = new UserIdComposite(userId_1, userLocalId_1);
        final User user_1 = new User(userIdComposite_1, "Bruce", "Wayne", new EMailAddress(""), Set.of());

        when(userManagementService.findAllUsers()).thenReturn(List.of(user_1));
        when(workingTimeRepository.findAllByUserIdIsIn(List.of(userLocalId_1.value()))).thenReturn(List.of());

        when(federalStateSettingsService.getFederalStateSettings()).thenReturn(federalStateSettings(GERMANY_BERLIN));

        final LocalDate from = LocalDate.now(clockFixed);
        final LocalDate toExclusive = LocalDate.now(clockFixed).plusWeeks(1);
        final Map<UserIdComposite, List<WorkingTime>> actual = sut.getAllWorkingTimes(from, toExclusive);

        assertThat(actual)
            .hasEntrySatisfying(userIdComposite_1, workingTimes -> {
                assertThat(workingTimes).hasSize(1);
                assertThat(workingTimes.getFirst()).satisfies(workingTime -> {
                    assertThat(workingTime.userIdComposite()).isEqualTo(userIdComposite_1);
                    assertThat(workingTime.federalState()).isEqualTo(GLOBAL);
                    assertThat(workingTime.worksOnPublicHoliday()).isFalse();
                    assertThat(workingTime.getMonday().duration()).isEqualTo(Duration.ofHours(8));
                    assertThat(workingTime.getTuesday().duration()).isEqualTo(Duration.ofHours(8));
                    assertThat(workingTime.getWednesday().duration()).isEqualTo(Duration.ofHours(8));
                    assertThat(workingTime.getThursday().duration()).isEqualTo(Duration.ofHours(8));
                    assertThat(workingTime.getFriday().duration()).isEqualTo(Duration.ofHours(8));
                    assertThat(workingTime.getSaturday().duration()).isEqualTo(Duration.ofHours(0));
                    assertThat(workingTime.getSunday().duration()).isEqualTo(Duration.ofHours(0));
                });
            });
    }

    @Test
    void ensureUpdateWorkingTimeThrowsWhenWorkingTimeIdIsUnknown() {

        final WorkingTimeId workingTimeId = new WorkingTimeId(UUID.randomUUID());
        when(workingTimeRepository.findById(workingTimeId.uuid())).thenReturn(Optional.empty());

        final EnumMap<DayOfWeek, Duration> workdays = new EnumMap<>(Map.of(
            MONDAY, Duration.ofHours(1),
            TUESDAY, Duration.ofHours(2),
            WEDNESDAY, Duration.ofHours(3),
            THURSDAY, Duration.ofHours(4),
            FRIDAY, Duration.ofHours(5),
            SATURDAY, Duration.ofHours(6),
            SUNDAY, Duration.ofHours(7)
        ));

        assertThatThrownBy(() -> sut.updateWorkingTime(workingTimeId, null, NONE, false, workdays))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void ensureUpdateWorkingTimeThrowsWhenValidFromIsNotSet() {

        final UUID id = UUID.randomUUID();
        final WorkingTimeEntity entity = new WorkingTimeEntity();
        entity.setId(id);
        entity.setValidFrom(LocalDate.of(2023, 12, 9));

        when(workingTimeRepository.findById(id)).thenReturn(Optional.of(entity));

        final EnumMap<DayOfWeek, Duration> workdays = new EnumMap<>(Map.of(
            MONDAY, Duration.ofHours(1),
            TUESDAY, Duration.ofHours(2),
            WEDNESDAY, Duration.ofHours(3),
            THURSDAY, Duration.ofHours(4),
            FRIDAY, Duration.ofHours(5),
            SATURDAY, Duration.ofHours(6),
            SUNDAY, Duration.ofHours(7)
        ));

        final WorkingTimeId workingTimeId = new WorkingTimeId(id);

        assertThatThrownBy(() -> sut.updateWorkingTime(workingTimeId, null, NONE, false, workdays))
            .isInstanceOf(WorkingTimeUpdateException.class);
    }

    @Test
    void ensureUpdateWorkingTime() {

        when(federalStateSettingsService.getFederalStateSettings()).thenReturn(federalStateSettings(GERMANY_BERLIN));

        final UserId userId = new UserId("uuid-1");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress(""), Set.of());
        when(userManagementService.findUserByLocalId(userLocalId)).thenReturn(Optional.of(user));

        final UUID id = UUID.randomUUID();
        final WorkingTimeEntity entity = new WorkingTimeEntity();
        entity.setId(id);
        entity.setUserId(42L);
        entity.setFederalState(NONE);
        entity.setWorksOnPublicHoliday(false);
        entity.setMonday("PT24H");
        entity.setTuesday("PT24H");
        entity.setWednesday("PT24H");
        entity.setThursday("PT24H");
        entity.setFriday("PT24H");
        entity.setSaturday("PT24H");
        entity.setSunday("PT24H");

        when(workingTimeRepository.findById(id)).thenReturn(Optional.of(entity));
        when(workingTimeRepository.save(any())).thenAnswer(returnsFirstArg());

        when(workingTimeRepository.findAllByUserId(userLocalId.value())).thenReturn(List.of(
            anyWorkingTimeEntity(id, userLocalId.value(), null)
        ));

        final EnumMap<DayOfWeek, Duration> workdays = new EnumMap<>(Map.of(
            MONDAY, Duration.ofHours(1),
            TUESDAY, Duration.ofHours(2),
            WEDNESDAY, Duration.ofHours(3),
            THURSDAY, Duration.ofHours(4),
            FRIDAY, Duration.ofHours(5),
            SATURDAY, Duration.ofHours(6),
            SUNDAY, Duration.ofHours(7)
        ));

        final WorkingTime actual = sut.updateWorkingTime(new WorkingTimeId(id), null, GERMANY_BADEN_WUERTTEMBERG, true, workdays);

        assertThat(actual.userIdComposite()).isEqualTo(userIdComposite);
        assertThat(actual.isCurrent()).isTrue();
        assertThat(actual.federalState()).isEqualTo(GERMANY_BADEN_WUERTTEMBERG);
        assertThat(actual.worksOnPublicHoliday()).isTrue();
        assertThat(actual.getMonday()).isEqualTo(new PlannedWorkingHours(Duration.ofHours(1)));
        assertThat(actual.getTuesday()).isEqualTo(new PlannedWorkingHours(Duration.ofHours(2)));
        assertThat(actual.getWednesday()).isEqualTo(new PlannedWorkingHours(Duration.ofHours(3)));
        assertThat(actual.getThursday()).isEqualTo(new PlannedWorkingHours(Duration.ofHours(4)));
        assertThat(actual.getFriday()).isEqualTo(new PlannedWorkingHours(Duration.ofHours(5)));
        assertThat(actual.getSaturday()).isEqualTo(new PlannedWorkingHours(Duration.ofHours(6)));
        assertThat(actual.getSunday()).isEqualTo(new PlannedWorkingHours(Duration.ofHours(7)));

        final ArgumentCaptor<WorkingTimeEntity> captor = ArgumentCaptor.forClass(WorkingTimeEntity.class);
        verify(workingTimeRepository).save(captor.capture());

        final WorkingTimeEntity actualEntity = captor.getValue();
        assertThat(actualEntity.getId()).isEqualTo(id);
        assertThat(actualEntity.getUserId()).isEqualTo(42L);
        assertThat(actualEntity.getFederalState()).isEqualTo(GERMANY_BADEN_WUERTTEMBERG);
        assertThat(actualEntity.isWorksOnPublicHoliday()).isTrue();
        assertThat(actualEntity.getMonday()).isEqualTo("PT1H");
        assertThat(actualEntity.getTuesday()).isEqualTo("PT2H");
        assertThat(actualEntity.getWednesday()).isEqualTo("PT3H");
        assertThat(actualEntity.getThursday()).isEqualTo("PT4H");
        assertThat(actualEntity.getFriday()).isEqualTo("PT5H");
        assertThat(actualEntity.getSaturday()).isEqualTo("PT6H");
        assertThat(actualEntity.getSunday()).isEqualTo("PT7H");
    }

    @Test
    void ensureCreateWorkingTimeWithNullableDays() {

        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserId userId = new UserId("user-id");
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "", "", new EMailAddress(""), Set.of());

        when(userManagementService.findUserByLocalId(userLocalId))
            .thenReturn(Optional.of(user));

        when(federalStateSettingsService.getFederalStateSettings()).thenReturn(federalStateSettings(GERMANY_BERLIN));

        final UUID uuid = UUID.randomUUID();
        when(workingTimeRepository.save(any(WorkingTimeEntity.class))).thenAnswer(invocation -> {
            final WorkingTimeEntity entity = cloneEntity(invocation.getArgument(0));
            entity.setId(uuid);
            return entity;
        });

        when(workingTimeRepository.findAllByUserId(userLocalId.value())).thenReturn(List.of(
            anyWorkingTimeEntity(uuid, userLocalId.value(), null),
            anyWorkingTimeEntity(uuid, userLocalId.value(), LocalDate.of(2023, 12, 9))
        ));

        final EnumMap<DayOfWeek, Duration> durations = new EnumMap<>(Map.of(
            MONDAY, Duration.ofHours(1)
        ));

        final WorkingTime actual = sut.createWorkingTime(userLocalId, LocalDate.of(2023, 12, 9), GERMANY_BADEN_WUERTTEMBERG, true, durations);
        assertThat(actual.id()).isEqualTo(new WorkingTimeId(uuid));
        assertThat(actual.userIdComposite()).isEqualTo(userIdComposite);
        assertThat(actual.federalState()).isEqualTo(GERMANY_BADEN_WUERTTEMBERG);
        assertThat(actual.worksOnPublicHoliday()).isTrue();
        assertThat(actual.validFrom()).hasValue(LocalDate.of(2023, 12, 9));
        assertThat(actual.getMonday()).isEqualTo(new PlannedWorkingHours(Duration.ofHours(1)));
        assertThat(actual.getTuesday()).isEqualTo(new PlannedWorkingHours(Duration.ZERO));
        assertThat(actual.getWednesday()).isEqualTo(new PlannedWorkingHours(Duration.ZERO));
        assertThat(actual.getThursday()).isEqualTo(new PlannedWorkingHours(Duration.ZERO));
        assertThat(actual.getFriday()).isEqualTo(new PlannedWorkingHours(Duration.ZERO));
        assertThat(actual.getSaturday()).isEqualTo(new PlannedWorkingHours(Duration.ZERO));
        assertThat(actual.getSunday()).isEqualTo(new PlannedWorkingHours(Duration.ZERO));
    }

    @Test
    void ensureCreateWorkingTime() {

        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserId userId = new UserId("user-id");
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "", "", new EMailAddress(""), Set.of());

        when(userManagementService.findUserByLocalId(userLocalId))
            .thenReturn(Optional.of(user));

        when(federalStateSettingsService.getFederalStateSettings()).thenReturn(federalStateSettings(GERMANY_BERLIN));

        final UUID uuid = UUID.randomUUID();
        when(workingTimeRepository.save(any(WorkingTimeEntity.class))).thenAnswer(invocation -> {
            final WorkingTimeEntity entity = cloneEntity(invocation.getArgument(0));
            entity.setId(uuid);
            return entity;
        });

        when(workingTimeRepository.findAllByUserId(userLocalId.value())).thenReturn(List.of(
            anyWorkingTimeEntity(uuid, userLocalId.value(), null),
            anyWorkingTimeEntity(uuid, userLocalId.value(), LocalDate.of(2023, 12, 9))
        ));

        final EnumMap<DayOfWeek, Duration> durations = new EnumMap<>(Map.of(
            MONDAY, Duration.ofHours(1),
            TUESDAY, Duration.ofHours(2),
            WEDNESDAY, Duration.ofHours(3),
            THURSDAY, Duration.ofHours(4),
            FRIDAY, Duration.ofHours(5),
            SATURDAY, Duration.ofHours(6),
            SUNDAY, Duration.ofHours(7)
        ));

        final WorkingTime actual = sut.createWorkingTime(userLocalId, LocalDate.of(2023, 12, 9), GERMANY_BADEN_WUERTTEMBERG, true, durations);
        assertThat(actual.id()).isEqualTo(new WorkingTimeId(uuid));
        assertThat(actual.userIdComposite()).isEqualTo(userIdComposite);
        assertThat(actual.federalState()).isEqualTo(GERMANY_BADEN_WUERTTEMBERG);
        assertThat(actual.worksOnPublicHoliday()).isTrue();
        assertThat(actual.validFrom()).hasValue(LocalDate.of(2023, 12, 9));
        assertThat(actual.getMonday()).isEqualTo(new PlannedWorkingHours(Duration.ofHours(1)));
        assertThat(actual.getTuesday()).isEqualTo(new PlannedWorkingHours(Duration.ofHours(2)));
        assertThat(actual.getWednesday()).isEqualTo(new PlannedWorkingHours(Duration.ofHours(3)));
        assertThat(actual.getThursday()).isEqualTo(new PlannedWorkingHours(Duration.ofHours(4)));
        assertThat(actual.getFriday()).isEqualTo(new PlannedWorkingHours(Duration.ofHours(5)));
        assertThat(actual.getSaturday()).isEqualTo(new PlannedWorkingHours(Duration.ofHours(6)));
        assertThat(actual.getSunday()).isEqualTo(new PlannedWorkingHours(Duration.ofHours(7)));
    }

    @Test
    void ensureDeleteWorkingTimeThrowsWhenWorkingTimeCannotBeFound() {
        final WorkingTimeId workingTimeId = new WorkingTimeId(UUID.randomUUID());
        assertThatThrownBy(() -> sut.deleteWorkingTime(workingTimeId)).isInstanceOf(IllegalStateException.class);
    }

    static Stream<Arguments> nowAndPastDate() {
        return Stream.of(
            Arguments.of(LocalDate.now(clockFixed)),
            Arguments.of(LocalDate.now(clockFixed).minusDays(1))
        );
    }

    @Test
    void ensureDeleteWorkingTimeReturnsFalseWhenWorkingTimeIsVeryFirstOne() {


        final WorkingTimeId workingTimeId = new WorkingTimeId(UUID.randomUUID());

        final WorkingTimeEntity entity = new WorkingTimeEntity();
        entity.setId(workingTimeId.uuid());
        entity.setUserId(42L);
        entity.setValidFrom(null);

        when(workingTimeRepository.findById(workingTimeId.uuid())).thenReturn(Optional.of(entity));

        final boolean actual = sut.deleteWorkingTime(workingTimeId);
        assertThat(actual).isFalse();
    }

    @ParameterizedTest
    @MethodSource("nowAndPastDate")
    void ensureDeleteWorkingTime(LocalDate givenValidFromDate) {

        final WorkingTimeId workingTimeId = new WorkingTimeId(UUID.randomUUID());

        final WorkingTimeEntity entity = new WorkingTimeEntity();
        entity.setId(workingTimeId.uuid());
        entity.setUserId(42L);
        entity.setValidFrom(givenValidFromDate);

        when(workingTimeRepository.findById(workingTimeId.uuid())).thenReturn(Optional.of(entity));

        final boolean actual = sut.deleteWorkingTime(workingTimeId);
        assertThat(actual).isTrue();
    }

    private FederalStateSettings federalStateSettings(FederalState globalFederalState) {
        return new FederalStateSettings(globalFederalState, false);
    }

    private User anyUser() {
        final UserId userId = new UserId("user-id");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        return new User(userIdComposite, "Bruce", "Wayne", new EMailAddress(""), Set.of());
    }

    private WorkingTimeEntity anyWorkingTimeEntity(UUID id, Long userId, LocalDate validFrom) {
        final WorkingTimeEntity entity = new WorkingTimeEntity();
        entity.setId(id);
        entity.setUserId(userId);
        entity.setValidFrom(validFrom);
        entity.setFederalState(NONE);
        entity.setWorksOnPublicHoliday(false);
        entity.setMonday("PT0S");
        entity.setTuesday("PT0S");
        entity.setWednesday("PT0S");
        entity.setThursday("PT0S");
        entity.setFriday("PT0S");
        entity.setSaturday("PT0S");
        entity.setSunday("PT0S");
        return entity;
    }

    private WorkingTimeEntity cloneEntity(WorkingTimeEntity entity) {
        final WorkingTimeEntity clone = new WorkingTimeEntity();
        clone.setTenantId(entity.getTenantId());
        clone.setId(entity.getId());
        clone.setUserId(entity.getUserId());
        clone.setFederalState(entity.getFederalState());
        clone.setWorksOnPublicHoliday(entity.isWorksOnPublicHoliday());
        clone.setValidFrom(entity.getValidFrom());
        clone.setMonday(entity.getMonday());
        clone.setTuesday(entity.getTuesday());
        clone.setWednesday(entity.getWednesday());
        clone.setThursday(entity.getThursday());
        clone.setFriday(entity.getFriday());
        clone.setSaturday(entity.getSaturday());
        clone.setSunday(entity.getSunday());
        return clone;
    }
}
