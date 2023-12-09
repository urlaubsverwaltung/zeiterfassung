package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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

    private static final Clock clockFixed = Clock.fixed(Clock.systemUTC().instant(), UTC);

    @BeforeEach
    void setUp() {
        sut = new WorkTimeServiceImpl(workingTimeRepository, userManagementService, clockFixed);
    }

    @Test
    void ensureGetWorkingTimeByUsers() {

        final WorkingTimeEntity entity_1 = new WorkingTimeEntity();
        entity_1.setId(UUID.randomUUID());
        entity_1.setUserId(1L);
        entity_1.setMonday("PT1H");
        entity_1.setTuesday("PT2H");
        entity_1.setWednesday("PT3H");
        entity_1.setThursday("PT4H");
        entity_1.setFriday("PT5H");
        entity_1.setSaturday("PT6H");
        entity_1.setSunday("PT7H");

        final WorkingTimeEntity entity_2 = new WorkingTimeEntity();
        entity_2.setId(UUID.randomUUID());
        entity_2.setUserId(2L);
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

        final Map<UserIdComposite, List<WorkingTime>> actual = sut.getWorkingTimesByUsers(List.of(new UserLocalId(1L), new UserLocalId(2L)));

        assertThat(actual)
            .containsEntry(userIdComposite_1, List.of(
                WorkingTime.builder(userIdComposite_1, new WorkingTimeId(UUID.randomUUID()))
                    .monday(1)
                    .tuesday(2)
                    .wednesday(3)
                    .thursday(4)
                    .friday(5)
                    .saturday(6)
                    .sunday(8)
                    .build()
                )
            )
            .containsEntry(userIdComposite_2, List.of(
                WorkingTime.builder(userIdComposite_2, new WorkingTimeId(UUID.randomUUID()))
                    .monday(7)
                    .tuesday(6)
                    .wednesday(5)
                    .thursday(4)
                    .friday(4)
                    .saturday(2)
                    .sunday(1)
                    .build()
                )
            );
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

        final Map<UserIdComposite, List<WorkingTime>> actual = sut.getWorkingTimesByUsers(List.of(new UserLocalId(1L)));

        assertThat(actual)
            .containsEntry(userIdComposite_1, List.of(
                WorkingTime.builder(userIdComposite_1, null)
                    .monday(8)
                    .tuesday(8)
                    .wednesday(8)
                    .thursday(8)
                    .friday(8)
                    .saturday(0)
                    .sunday(0)
                    .build()
                )
            );
    }

    @Test
    void ensureGetAllWorkingTimeByUsers() {

        final WorkingTimeEntity entity_1 = new WorkingTimeEntity();
        entity_1.setId(UUID.randomUUID());
        entity_1.setUserId(1L);
        entity_1.setMonday("PT1H");
        entity_1.setTuesday("PT2H");
        entity_1.setWednesday("PT3H");
        entity_1.setThursday("PT4H");
        entity_1.setFriday("PT5H");
        entity_1.setSaturday("PT6H");
        entity_1.setSunday("PT7H");

        final WorkingTimeEntity entity_2 = new WorkingTimeEntity();
        entity_2.setId(UUID.randomUUID());
        entity_2.setUserId(2L);
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

        final Map<UserIdComposite, List<WorkingTime>> actual = sut.getAllWorkingTimesByUsers();

        assertThat(actual)
            .containsEntry(userIdComposite_1, List.of(
                WorkingTime.builder(userIdComposite_1, new WorkingTimeId(UUID.randomUUID()))
                    .monday(1)
                    .tuesday(2)
                    .wednesday(3)
                    .thursday(4)
                    .friday(5)
                    .saturday(6)
                    .sunday(8)
                    .build()
                )
            )
            .containsEntry(userIdComposite_2, List.of(
                WorkingTime.builder(userIdComposite_2, new WorkingTimeId(UUID.randomUUID()))
                    .monday(7)
                    .tuesday(6)
                    .wednesday(5)
                    .thursday(4)
                    .friday(4)
                    .saturday(2)
                    .sunday(1)
                    .build()
                )
            );
    }

    @Test
    void ensureGetAllWorkingTimeByUsersAddsDefaultWorkingTimeForUsersWithoutExplicitOne() {

        final UserId userId_1 = new UserId("uuid-1");
        final UserLocalId userLocalId_1 = new UserLocalId(1L);
        final UserIdComposite userIdComposite_1 = new UserIdComposite(userId_1, userLocalId_1);
        final User user_1 = new User(userIdComposite_1, "Bruce", "Wayne", new EMailAddress(""), Set.of());

        when(userManagementService.findAllUsers()).thenReturn(List.of(user_1));
        when(workingTimeRepository.findAllByUserIdIsIn(List.of(userLocalId_1.value()))).thenReturn(List.of());

        final Map<UserIdComposite, List<WorkingTime>> actual = sut.getAllWorkingTimesByUsers();

        assertThat(actual)
            .containsEntry(userIdComposite_1, List.of(
                WorkingTime.builder(userIdComposite_1, null)
                    .monday(8)
                    .tuesday(8)
                    .wednesday(8)
                    .thursday(8)
                    .friday(8)
                    .saturday(0)
                    .sunday(0)
                    .build()
                )
            );
    }

    @Test
    void ensureUpdateWorkingTimeThrowsWhenWorkingTimeIdIsUnknown() {

        final WorkingTimeId workingTimeId = new WorkingTimeId(UUID.randomUUID());
        when(workingTimeRepository.findById(workingTimeId.uuid())).thenReturn(Optional.empty());

        final WorkWeekUpdate workWeekUpdate = WorkWeekUpdate.builder()
            .monday(BigDecimal.valueOf(1))
            .tuesday(BigDecimal.valueOf(2))
            .wednesday(BigDecimal.valueOf(3))
            .thursday(BigDecimal.valueOf(4))
            .friday(BigDecimal.valueOf(5))
            .saturday(BigDecimal.valueOf(6))
            .sunday(BigDecimal.valueOf(7))
            .build();

        assertThatThrownBy(() -> sut.updateWorkingTime(workingTimeId, workWeekUpdate))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void ensureUpdateWorkingTimeThrowsWhenValidFromIsNotSet() {

        final UUID id = UUID.randomUUID();
        final WorkingTimeEntity entity = new WorkingTimeEntity();
        entity.setId(id);
        entity.setValidFrom(LocalDate.of(2023, 12, 9));

        when(workingTimeRepository.findById(id)).thenReturn(Optional.of(entity));

        final WorkWeekUpdate workWeekUpdate = WorkWeekUpdate.builder()
            .build();

        assertThatThrownBy(() -> sut.updateWorkingTime(new WorkingTimeId(id), workWeekUpdate))
            .isInstanceOf(WorkingTimeUpdateException.class);
    }

    @Test
    void ensureUpdateWorkingTime() {

        final UserId userId_1 = new UserId("uuid-1");
        final UserLocalId userLocalId_1 = new UserLocalId(42L);
        final UserIdComposite userIdComposite_1 = new UserIdComposite(userId_1, userLocalId_1);
        final User user_1 = new User(userIdComposite_1, "Bruce", "Wayne", new EMailAddress(""), Set.of());
        when(userManagementService.findUserByLocalId(userLocalId_1)).thenReturn(Optional.of(user_1));

        final UUID id = UUID.randomUUID();
        final WorkingTimeEntity entity = new WorkingTimeEntity();
        entity.setId(id);
        entity.setUserId(42L);
        entity.setMonday("PT24H");
        entity.setTuesday("PT24H");
        entity.setWednesday("PT24H");
        entity.setThursday("PT24H");
        entity.setFriday("PT24H");
        entity.setSaturday("PT24H");
        entity.setSunday("PT24H");

        when(workingTimeRepository.findById(id)).thenReturn(Optional.of(entity));
        when(workingTimeRepository.findByPersonAndValidityDateEqualsOrMinorDate(42L, LocalDate.now(clockFixed))).thenReturn(entity);
        when(workingTimeRepository.save(any())).thenAnswer(returnsFirstArg());

        final WorkWeekUpdate workWeekUpdate = WorkWeekUpdate.builder()
            .monday(BigDecimal.valueOf(1))
            .tuesday(BigDecimal.valueOf(2))
            .wednesday(BigDecimal.valueOf(3))
            .thursday(BigDecimal.valueOf(4))
            .friday(BigDecimal.valueOf(5))
            .saturday(BigDecimal.valueOf(6))
            .sunday(BigDecimal.valueOf(7))
            .build();

        final WorkingTime actual = sut.updateWorkingTime(new WorkingTimeId(id), workWeekUpdate);

        assertThat(actual.userIdComposite()).isEqualTo(userIdComposite_1);
        assertThat(actual.isCurrent()).isTrue();
        assertThat(actual.getMonday()).isEqualTo(WorkDay.monday(Duration.ofHours(1)));
        assertThat(actual.getTuesday()).isEqualTo(WorkDay.tuesday(Duration.ofHours(2)));
        assertThat(actual.getWednesday()).isEqualTo(WorkDay.wednesday(Duration.ofHours(3)));
        assertThat(actual.getThursday()).isEqualTo(WorkDay.thursday(Duration.ofHours(4)));
        assertThat(actual.getFriday()).isEqualTo(WorkDay.friday(Duration.ofHours(5)));
        assertThat(actual.getSaturday()).isEqualTo(WorkDay.saturday(Duration.ofHours(6)));
        assertThat(actual.getSunday()).isEqualTo(WorkDay.sunday(Duration.ofHours(7)));

        final ArgumentCaptor<WorkingTimeEntity> captor = ArgumentCaptor.forClass(WorkingTimeEntity.class);
        verify(workingTimeRepository).save(captor.capture());

        final WorkingTimeEntity actualEntity = captor.getValue();
        assertThat(actualEntity.getId()).isEqualTo(id);
        assertThat(actualEntity.getUserId()).isEqualTo(42L);
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

        final UUID uuid = UUID.randomUUID();
        when(workingTimeRepository.save(any(WorkingTimeEntity.class))).thenAnswer(invocation -> {
            final WorkingTimeEntity entity = invocation.getArgument(0);
            entity.setId(uuid);
            return entity;
        });

        final EnumMap<DayOfWeek, Duration> durations = new EnumMap<>(Map.of(
            MONDAY, Duration.ofHours(1)
        ));

        final WorkingTime actual = sut.createWorkingTime(userLocalId, LocalDate.of(2023, 12, 9), durations);
        assertThat(actual.id()).isEqualTo(new WorkingTimeId(uuid));
        assertThat(actual.userIdComposite()).isEqualTo(userIdComposite);
        assertThat(actual.validFrom()).hasValue(LocalDate.of(2023, 12, 9));
        assertThat(actual.getMonday()).isEqualTo(WorkDay.monday(Duration.ofHours(1)));
        assertThat(actual.getTuesday()).isEqualTo(WorkDay.tuesday(Duration.ZERO));
        assertThat(actual.getWednesday()).isEqualTo(WorkDay.wednesday(Duration.ZERO));
        assertThat(actual.getThursday()).isEqualTo(WorkDay.thursday(Duration.ZERO));
        assertThat(actual.getFriday()).isEqualTo(WorkDay.friday(Duration.ZERO));
        assertThat(actual.getSaturday()).isEqualTo(WorkDay.saturday(Duration.ZERO));
        assertThat(actual.getSunday()).isEqualTo(WorkDay.sunday(Duration.ZERO));
    }

    @Test
    void ensureCreateWorkingTime() {

        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserId userId = new UserId("user-id");
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "", "", new EMailAddress(""), Set.of());

        when(userManagementService.findUserByLocalId(userLocalId))
            .thenReturn(Optional.of(user));

        final UUID uuid = UUID.randomUUID();
        when(workingTimeRepository.save(any(WorkingTimeEntity.class))).thenAnswer(invocation -> {
            final WorkingTimeEntity entity = invocation.getArgument(0);
            entity.setId(uuid);
            return entity;
        });

        final EnumMap<DayOfWeek, Duration> durations = new EnumMap<>(Map.of(
            MONDAY, Duration.ofHours(1),
            TUESDAY, Duration.ofHours(2),
            WEDNESDAY, Duration.ofHours(3),
            THURSDAY, Duration.ofHours(4),
            FRIDAY, Duration.ofHours(5),
            SATURDAY, Duration.ofHours(6),
            SUNDAY, Duration.ofHours(7)
        ));

        final WorkingTime actual = sut.createWorkingTime(userLocalId, LocalDate.of(2023, 12, 9), durations);
        assertThat(actual.id()).isEqualTo(new WorkingTimeId(uuid));
        assertThat(actual.userIdComposite()).isEqualTo(userIdComposite);
        assertThat(actual.validFrom()).hasValue(LocalDate.of(2023, 12, 9));
        assertThat(actual.getMonday()).isEqualTo(WorkDay.monday(Duration.ofHours(1)));
        assertThat(actual.getTuesday()).isEqualTo(WorkDay.tuesday(Duration.ofHours(2)));
        assertThat(actual.getWednesday()).isEqualTo(WorkDay.wednesday(Duration.ofHours(3)));
        assertThat(actual.getThursday()).isEqualTo(WorkDay.thursday(Duration.ofHours(4)));
        assertThat(actual.getFriday()).isEqualTo(WorkDay.friday(Duration.ofHours(5)));
        assertThat(actual.getSaturday()).isEqualTo(WorkDay.saturday(Duration.ofHours(6)));
        assertThat(actual.getSunday()).isEqualTo(WorkDay.sunday(Duration.ofHours(7)));
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

    @ParameterizedTest
    @MethodSource("nowAndPastDate")
    void ensureDeleteWorkingTimeReturnsFalseWhenWorkingTimeValidFromIsNowOrInThePast(LocalDate givenValidFrom) {


        final WorkingTimeId workingTimeId = new WorkingTimeId(UUID.randomUUID());

        final WorkingTimeEntity entity = new WorkingTimeEntity();
        entity.setId(workingTimeId.uuid());
        entity.setUserId(42L);
        entity.setValidFrom(givenValidFrom);

        when(workingTimeRepository.findById(workingTimeId.uuid())).thenReturn(Optional.of(entity));

        final boolean actual = sut.deleteWorkingTime(workingTimeId);
        assertThat(actual).isFalse();
    }

    @Test
    void ensureDeleteWorkingTime() {

        final WorkingTimeId workingTimeId = new WorkingTimeId(UUID.randomUUID());

        final WorkingTimeEntity entity = new WorkingTimeEntity();
        entity.setId(workingTimeId.uuid());
        entity.setUserId(42L);
        entity.setValidFrom(LocalDate.now(clockFixed).plusDays(123));

        when(workingTimeRepository.findById(workingTimeId.uuid())).thenReturn(Optional.of(entity));

        final boolean actual = sut.deleteWorkingTime(workingTimeId);
        assertThat(actual).isTrue();
    }
}
