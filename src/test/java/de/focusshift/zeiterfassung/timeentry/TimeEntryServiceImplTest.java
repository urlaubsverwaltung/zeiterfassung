package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.TenantAwareRevisionMetadata;
import de.focusshift.zeiterfassung.data.history.EntityRevisionMapper;
import de.focusshift.zeiterfassung.data.history.EntityRevisionMetadata;
import de.focusshift.zeiterfassung.data.history.EntityRevisionType;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantAwareRevisionEntity;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.timeentry.events.TimeEntryCreatedEvent;
import de.focusshift.zeiterfassung.timeentry.events.TimeEntryUpdatedEvent;
import de.focusshift.zeiterfassung.user.UserDateService;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeCalendarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.history.Revision;
import org.springframework.data.history.Revisions;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.data.history.RevisionMetadata.RevisionType.INSERT;
import static org.springframework.data.history.RevisionMetadata.RevisionType.UPDATE;

@ExtendWith(MockitoExtension.class)
class TimeEntryServiceImplTest {

    private static final ZoneId ZONE_ID_UTC = ZoneId.of("UTC");

    private TimeEntryServiceImpl sut;

    @Mock
    private TimeEntryRepository timeEntryRepository;
    @Mock
    private TimeEntryLockService timeEntryLockService;
    @Mock
    private WorkDurationCalculationService workDurationCalculationService;
    @Mock
    private UserDateService userDateService;
    @Mock
    private WorkingTimeCalendarService workingTimeCalendarService;
    @Mock
    private UserManagementService userManagementService;
    @Mock
    private UserSettingsProvider userSettingsProvider;
    @Mock
    private EntityRevisionMapper entityRevisionMapper;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private static final Clock clockFixed = Clock.fixed(Instant.now(), UTC);

    @BeforeEach
    void setUp() {
        sut = new TimeEntryServiceImpl(timeEntryRepository, timeEntryLockService, workDurationCalculationService, userManagementService,
            workingTimeCalendarService, userDateService, userSettingsProvider, entityRevisionMapper, applicationEventPublisher, clockFixed);
    }

    @Nested
    class FindTimeEntryHistory {

        @Test
        void ensureFindTimeEntryHistoryEmptyWhenThereAreNoRevisions() {

            when(timeEntryRepository.findRevisions(1L)).thenReturn(Revisions.none());

            final Optional<TimeEntryHistory> actual = sut.findTimeEntryHistory(new TimeEntryId(1L));
            assertThat(actual).isEmpty();
        }

        @Test
        void ensureFindTimeEntryHistory() {

            final UserId userId = new UserId("batman");
            final UserLocalId userLocalId = new UserLocalId(1L);
            final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
            final User user = createUser(userIdComposite, "Bruce", "Wayne");

            final Instant revisionTimestamp = Instant.now(clockFixed);
            final Instant start = Instant.parse("2025-03-03T21:00:00.00Z");
            final Instant end = Instant.parse("2025-03-03T21:30:00.00Z");

            final TenantAwareRevisionEntity revisionEntity = new TenantAwareRevisionEntity();
            revisionEntity.setId(1L);
            revisionEntity.setTimestamp(revisionTimestamp.toEpochMilli());
            revisionEntity.setUpdatedBy(userId.value());

            final TimeEntryEntity entityCreated = new TimeEntryEntity();
            entityCreated.setId(42L);
            entityCreated.setOwner(userId.value());
            entityCreated.setStart(start);
            entityCreated.setStartZoneId("UTC");
            entityCreated.setEnd(end);
            entityCreated.setEndZoneId("UTC");
            entityCreated.setComment("Kickoff");

            final Revision<Long, TimeEntryEntity> revision = Revision.of(new TenantAwareRevisionMetadata(revisionEntity, INSERT), entityCreated);

            when(timeEntryRepository.findRevisions(1L)).thenReturn(Revisions.of(List.of(revision)));

            when(userManagementService.findUserById(userId)).thenReturn(Optional.of(user));

            final EntityRevisionMetadata metadata = anyEntityRevisionMetadata();
            when(entityRevisionMapper.toEntityRevisionMetadata(revision)).thenReturn(metadata);

            final Optional<TimeEntryHistory> actual = sut.findTimeEntryHistory(new TimeEntryId(1L));
            assertThat(actual).hasValueSatisfying(history -> {

                assertThat(history.timeEntryId()).isEqualTo(new TimeEntryId(1L));
                assertThat(history.revisions()).hasSize(1);

                final TimeEntryId timeEntryId = new TimeEntryId(42L);
                final ZonedDateTime startDateTime = ZonedDateTime.ofInstant(start, ZONE_ID_UTC);
                final ZonedDateTime endDateTime = ZonedDateTime.ofInstant(end, ZONE_ID_UTC);

                final TimeEntry timeEntry = new TimeEntry(timeEntryId, userIdComposite, "Kickoff", startDateTime, endDateTime, false);
                final TimeEntryHistoryItem historyItem = new TimeEntryHistoryItem(metadata, timeEntry, true, true, true, true);

                assertThat(history.revisions().getFirst()).isEqualTo(historyItem);
                assertThat(history.revisions().getFirst().metadata()).isSameAs(metadata);
            });
        }

        @Test
        void ensureFindTimeEntryHistoryWithOrderedHistoryItems() {

            final TenantAwareRevisionEntity revisionEntityInsert = new TenantAwareRevisionEntity();
            revisionEntityInsert.setId(1L);
            final TenantAwareRevisionEntity revisionEntityUpdate = new TenantAwareRevisionEntity();
            revisionEntityUpdate.setId(2L);

            final TimeEntryEntity entityCreated = anyTimeEntryEntity();
            final TimeEntryEntity entityModified = anyTimeEntryEntity();

            final Revision<Long, TimeEntryEntity> revisionInsert = Revision.of(new TenantAwareRevisionMetadata(revisionEntityInsert, INSERT), entityCreated);
            final Revision<Long, TimeEntryEntity> revisionUpdate = Revision.of(new TenantAwareRevisionMetadata(revisionEntityUpdate, UPDATE), entityModified);

            when(timeEntryRepository.findRevisions(1L)).thenReturn(Revisions.of(List.of(revisionInsert, revisionUpdate)));

            when(userManagementService.findUserById(any())).thenReturn(Optional.of(anyUser()));

            final EntityRevisionMetadata metadataCreated = anyEntityRevisionMetadata();
            final EntityRevisionMetadata metadataModified = anyEntityRevisionMetadata();
            when(entityRevisionMapper.toEntityRevisionMetadata(revisionInsert)).thenReturn(metadataCreated);
            when(entityRevisionMapper.toEntityRevisionMetadata(revisionUpdate)).thenReturn(metadataModified);

            final Optional<TimeEntryHistory> actual = sut.findTimeEntryHistory(new TimeEntryId(1L));
            assertThat(actual).hasValueSatisfying(history -> {
                assertThat(history.revisions()).hasSize(2);
                assertThat(history.revisions().get(0).metadata()).isSameAs(metadataCreated);
                assertThat(history.revisions().get(1).metadata()).isSameAs(metadataModified);
            });
        }
    }

    @Test
    void ensureFindTimeEntry() {

        final LocalDateTime entryStart = LocalDateTime.of(LocalDate.of(2023, 2, 25), LocalTime.of(10, 0, 0));
        final LocalDateTime entryEnd = LocalDateTime.of(LocalDate.of(2023, 2, 25), LocalTime.of(12, 0, 0));

        final TimeEntryEntity entity = new TimeEntryEntity(
            42L,
            "batman",
            "",
            entryStart.toInstant(UTC),
            ZONE_ID_UTC,
            entryEnd.toInstant(UTC),
            ZONE_ID_UTC,
            Instant.now(),
            false);

        when(timeEntryRepository.findById(42L)).thenReturn(Optional.of(entity));

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress(""), Set.of());
        when(userManagementService.findUserById(userId)).thenReturn(Optional.of(user));

        final Optional<TimeEntry> actual = sut.findTimeEntry(new TimeEntryId(42L));
        assertThat(actual).isPresent();
        assertThat(actual.get()).satisfies(timeEntry -> {
            assertThat(timeEntry.id()).isEqualTo(new TimeEntryId(42L));
            assertThat(timeEntry.userIdComposite()).isEqualTo(userIdComposite);
            assertThat(timeEntry.comment()).isEmpty();
            assertThat(timeEntry.start()).isEqualTo(ZonedDateTime.of(entryStart, ZONE_ID_UTC));
            assertThat(timeEntry.end()).isEqualTo(ZonedDateTime.of(entryEnd, ZONE_ID_UTC));
            assertThat(timeEntry.isBreak()).isFalse();
            assertThat(timeEntry.workDuration().duration()).isEqualTo(Duration.ofHours(2));
        });
    }

    @Test
    void ensureFindTimeEntryReturnsEmptyOptional() {
        when(timeEntryRepository.findById(42L)).thenReturn(Optional.empty());
        assertThat(sut.findTimeEntry(new TimeEntryId(42L))).isEmpty();
    }

    @Test
    void ensureCreateTimeEntry() {

        final LocalDateTime entryStart = LocalDateTime.of(2023, 1, 1, 10, 0, 0);
        final LocalDateTime entryEnd = LocalDateTime.of(2023, 1, 1, 12, 0, 0);

        when(timeEntryRepository.save(any(TimeEntryEntity.class))).thenAnswer(invocationOnMock -> {
            final TimeEntryEntity entity = invocationOnMock.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress(""), Set.of());
        when(userManagementService.findUserByLocalId(userLocalId)).thenReturn(Optional.of(user));

        final TimeEntry actual = sut.createTimeEntry(
            userLocalId,
            "hard work",
            ZonedDateTime.of(entryStart, ZONE_ID_UTC),
            ZonedDateTime.of(entryEnd, ZONE_ID_UTC),
            false
        );

        assertThat(actual).isEqualTo(new TimeEntry(new TimeEntryId(1L), userIdComposite, "hard work", ZonedDateTime.of(entryStart, ZONE_ID_UTC), ZonedDateTime.of(entryEnd, ZONE_ID_UTC), false));

        final ArgumentCaptor<TimeEntryEntity> captor = ArgumentCaptor.forClass(TimeEntryEntity.class);
        verify(timeEntryRepository).save(captor.capture());

        assertThat(captor.getValue()).satisfies(entity -> {
            assertThat(entity.getOwner()).isEqualTo("batman");
            assertThat(entity.getComment()).isEqualTo("hard work");
            assertThat(entity.getStart()).isEqualTo(entryStart.toInstant(UTC));
            assertThat(entity.getStartZoneId()).isEqualTo(ZONE_ID_UTC.getId());
            assertThat(entity.getEnd()).isEqualTo(entryEnd.toInstant(UTC));
            assertThat(entity.getEndZoneId()).isEqualTo(ZONE_ID_UTC.getId());
            assertThat(entity.getUpdatedAt()).isEqualTo(Instant.now(clockFixed));
            assertThat(entity.isBreak()).isFalse();
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @NullSource
    void ensureCreateTimeEntryWithComment(String givenComment) {

        final LocalDateTime entryStart = LocalDateTime.of(2023, 1, 1, 10, 0, 0);
        final LocalDateTime entryEnd = LocalDateTime.of(2023, 1, 1, 12, 0, 0);

        when(timeEntryRepository.save(any(TimeEntryEntity.class))).thenAnswer(invocationOnMock -> {
            final TimeEntryEntity entity = invocationOnMock.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress(""), Set.of());
        when(userManagementService.findUserByLocalId(userLocalId)).thenReturn(Optional.of(user));

        final TimeEntry actual = sut.createTimeEntry(
            userLocalId,
            givenComment,
            ZonedDateTime.of(entryStart, ZONE_ID_UTC),
            ZonedDateTime.of(entryEnd, ZONE_ID_UTC),
            false
        );

        assertThat(actual).isEqualTo(new TimeEntry(new TimeEntryId(1L), userIdComposite, "", ZonedDateTime.of(entryStart, ZONE_ID_UTC), ZonedDateTime.of(entryEnd, ZONE_ID_UTC), false));

        final ArgumentCaptor<TimeEntryEntity> captor = ArgumentCaptor.forClass(TimeEntryEntity.class);
        verify(timeEntryRepository).save(captor.capture());

        assertThat(captor.getValue()).satisfies(entity -> {
            assertThat(entity.getComment()).isEmpty();
        });
    }

    @Test
    void ensureCreateTimeEntryPublishesEvent() {

        final LocalDateTime entryStart = LocalDateTime.of(2023, 1, 1, 10, 0, 0);
        final LocalDateTime entryEnd = LocalDateTime.of(2023, 1, 1, 12, 0, 0);

        when(timeEntryRepository.save(any(TimeEntryEntity.class))).thenAnswer(invocationOnMock -> {
            final TimeEntryEntity entity = invocationOnMock.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress(""), Set.of());
        when(userManagementService.findUserByLocalId(userLocalId)).thenReturn(Optional.of(user));

        sut.createTimeEntry(
            userLocalId,
            "hard work",
            ZonedDateTime.of(entryStart, ZONE_ID_UTC),
            ZonedDateTime.of(entryEnd, ZONE_ID_UTC),
            false
        );

        final ArgumentCaptor<TimeEntryCreatedEvent> captor = ArgumentCaptor.forClass(TimeEntryCreatedEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());

        assertThat(captor.getValue()).satisfies(actual -> {
            assertThat(actual.locked()).isEqualTo(false);
            assertThat(actual.date()).isEqualTo(entryStart.toLocalDate());
            assertThat(actual.ownerUserIdComposite()).isEqualTo(userIdComposite);
            assertThat(actual.timeEntryId()).isEqualTo(new TimeEntryId(1L));
        });
    }

    @Test
    void ensureUpdateTimeEntryThrowsWhenTimeEntryIsUnknown() {
        final TimeEntryId id = new TimeEntryId(42L);
        assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(() -> sut.updateTimeEntry(id, "", null, null, null, false));
    }

    @Test
    void ensureUpdateTimeEntryThrowsWhenStarEndDurationAreGiven() {

        final ZonedDateTime now = ZonedDateTime.now(clockFixed);
        final Duration duration = Duration.ofMinutes(60);

        final LocalDate from = LocalDate.of(2023, 1, 1);
        final LocalDate toExclusive = LocalDate.of(2023, 2, 1);

        final LocalDateTime entryStart = LocalDateTime.of(from, LocalTime.of(10, 0, 0));
        final LocalDateTime entryEnd = LocalDateTime.of(toExclusive, LocalTime.of(12, 0, 0));

        final TimeEntryEntity entity = new TimeEntryEntity(
            42L,
            "batman",
            "",
            entryStart.toInstant(UTC),
            ZONE_ID_UTC,
            entryEnd.toInstant(UTC),
            ZONE_ID_UTC,
            Instant.now(),
            false);

        when(timeEntryRepository.findById(42L)).thenReturn(Optional.of(entity));

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress(""), Set.of());
        when(userManagementService.findUserById(userId)).thenReturn(Optional.of(user));

        assertThatExceptionOfType(TimeEntryUpdateNotPlausibleException.class).isThrownBy(
            () -> sut.updateTimeEntry(new TimeEntryId(42L), "", now, now, duration, false)
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void ensureUpdateTimeEntryStart(boolean isBreak) throws Exception {

        final LocalDate from = LocalDate.of(2023, 1, 1);

        final LocalDateTime entryStart = LocalDateTime.of(from, LocalTime.of(10, 0, 0));
        final LocalDateTime entryEnd = entryStart.plusHours(2);

        final TimeEntryEntity existingEntity = new TimeEntryEntity(
            42L,
            "batman",
            "",
            entryStart.toInstant(UTC),
            ZONE_ID_UTC,
            entryEnd.toInstant(UTC),
            ZONE_ID_UTC,
            Instant.now(),
            isBreak);

        when(timeEntryRepository.findById(42L)).thenReturn(Optional.of(existingEntity));
        when(timeEntryRepository.save(any(TimeEntryEntity.class))).thenAnswer(returnsFirstArg());

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress(""), Set.of());
        when(userManagementService.findUserById(userId)).thenReturn(Optional.of(user));

        final ZonedDateTime newStart = ZonedDateTime.ofInstant(entryStart.toInstant(UTC), ZONE_ID_UTC).plusMinutes(30);
        final ZonedDateTime sameEnd = ZonedDateTime.ofInstant(entryEnd.toInstant(UTC), ZONE_ID_UTC);
        final Duration sameDuration = Duration.ofHours(2);

        final TimeEntry actualUpdatedTimeEntry = sut.updateTimeEntry(new TimeEntryId(42L), "", newStart, sameEnd, sameDuration, false);

        assertThat(actualUpdatedTimeEntry.id()).isEqualTo(new TimeEntryId(42L));
        assertThat(actualUpdatedTimeEntry.userIdComposite()).isEqualTo(userIdComposite);
        assertThat(actualUpdatedTimeEntry.comment()).isEmpty();
        assertThat(actualUpdatedTimeEntry.start()).isEqualTo(newStart);
        assertThat(actualUpdatedTimeEntry.end()).isEqualTo(sameEnd);
        assertThat(actualUpdatedTimeEntry.isBreak()).isFalse();
        assertThat(actualUpdatedTimeEntry.workDuration().duration()).isEqualTo(Duration.ofMinutes(90));

        final ArgumentCaptor<TimeEntryEntity> captor = ArgumentCaptor.forClass(TimeEntryEntity.class);
        verify(timeEntryRepository).save(captor.capture());

        final TimeEntryEntity actualPersisted = captor.getValue();

        assertThat(actualPersisted.getId()).isEqualTo(42L);
        assertThat(actualPersisted.getOwner()).isEqualTo("batman");
        assertThat(actualPersisted.getComment()).isEmpty();
        assertThat(actualPersisted.getStart()).isEqualTo(newStart.toInstant());
        assertThat(actualPersisted.getStartZoneId()).isEqualTo(newStart.getZone().getId());
        assertThat(actualPersisted.getEnd()).isEqualTo(sameEnd.toInstant());
        assertThat(actualPersisted.getEndZoneId()).isEqualTo(sameEnd.getZone().getId());
        assertThat(actualPersisted.getUpdatedAt()).isEqualTo(Instant.now(clockFixed));
        assertThat(actualPersisted.isBreak()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void ensureUpdateTimeEntryStartAndEnd(boolean isBreak) throws Exception {

        final LocalDate from = LocalDate.of(2023, 1, 1);

        final LocalDateTime entryStart = LocalDateTime.of(from, LocalTime.of(10, 0, 0));
        final LocalDateTime entryEnd = entryStart.plusHours(2);

        final TimeEntryEntity existingEntity = new TimeEntryEntity(
            42L,
            "batman",
            "",
            entryStart.toInstant(UTC),
            ZONE_ID_UTC,
            entryEnd.toInstant(UTC),
            ZONE_ID_UTC,
            Instant.now(),
            isBreak);

        when(timeEntryRepository.findById(42L)).thenReturn(Optional.of(existingEntity));
        when(timeEntryRepository.save(any(TimeEntryEntity.class))).thenAnswer(returnsFirstArg());

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress(""), Set.of());
        when(userManagementService.findUserById(userId)).thenReturn(Optional.of(user));

        final ZonedDateTime newStart = ZonedDateTime.ofInstant(entryStart.toInstant(UTC), ZONE_ID_UTC).minusMinutes(30);
        final ZonedDateTime newEnd = ZonedDateTime.ofInstant(entryEnd.toInstant(UTC), ZONE_ID_UTC).plusMinutes(30);
        final Duration sameDuration = Duration.ofHours(2);

        final TimeEntry actualUpdatedTimeEntry = sut.updateTimeEntry(new TimeEntryId(42L), "", newStart, newEnd, sameDuration, false);

        assertThat(actualUpdatedTimeEntry.id()).isEqualTo(new TimeEntryId(42L));
        assertThat(actualUpdatedTimeEntry.userIdComposite()).isEqualTo(userIdComposite);
        assertThat(actualUpdatedTimeEntry.comment()).isEmpty();
        assertThat(actualUpdatedTimeEntry.start()).isEqualTo(newStart);
        assertThat(actualUpdatedTimeEntry.end()).isEqualTo(newEnd);
        assertThat(actualUpdatedTimeEntry.isBreak()).isFalse();
        assertThat(actualUpdatedTimeEntry.workDuration().duration()).isEqualTo(Duration.ofHours(3));

        final ArgumentCaptor<TimeEntryEntity> captor = ArgumentCaptor.forClass(TimeEntryEntity.class);
        verify(timeEntryRepository).save(captor.capture());

        final TimeEntryEntity actualPersisted = captor.getValue();

        assertThat(actualPersisted.getId()).isEqualTo(42L);
        assertThat(actualPersisted.getOwner()).isEqualTo("batman");
        assertThat(actualPersisted.getComment()).isEmpty();
        assertThat(actualPersisted.getStart()).isEqualTo(newStart.toInstant());
        assertThat(actualPersisted.getStartZoneId()).isEqualTo(newStart.getZone().getId());
        assertThat(actualPersisted.getEnd()).isEqualTo(newEnd.toInstant());
        assertThat(actualPersisted.getEndZoneId()).isEqualTo(newEnd.getZone().getId());
        assertThat(actualPersisted.getUpdatedAt()).isEqualTo(Instant.now(clockFixed));
        assertThat(actualPersisted.isBreak()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void ensureUpdateTimeEntryStartAndDuration(boolean isBreak) throws Exception {

        final LocalDate from = LocalDate.of(2023, 1, 1);

        final LocalDateTime entryStart = LocalDateTime.of(from, LocalTime.of(10, 0, 0));
        final LocalDateTime entryEnd = entryStart.plusHours(2);

        final TimeEntryEntity existingEntity = new TimeEntryEntity(
            42L,
            "batman",
            "",
            entryStart.toInstant(UTC),
            ZONE_ID_UTC,
            entryEnd.toInstant(UTC),
            ZONE_ID_UTC,
            Instant.now(),
            isBreak);

        when(timeEntryRepository.findById(42L)).thenReturn(Optional.of(existingEntity));
        when(timeEntryRepository.save(any(TimeEntryEntity.class))).thenAnswer(returnsFirstArg());

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress(""), Set.of());
        when(userManagementService.findUserById(userId)).thenReturn(Optional.of(user));

        final ZonedDateTime newStart = ZonedDateTime.ofInstant(entryStart.toInstant(UTC), ZONE_ID_UTC).minusMinutes(30);
        final ZonedDateTime sameEnd = ZonedDateTime.ofInstant(entryEnd.toInstant(UTC), ZONE_ID_UTC);
        final Duration newDuration = Duration.ofHours(3);

        final TimeEntry actualUpdatedTimeEntry = sut.updateTimeEntry(new TimeEntryId(42L), "", newStart, sameEnd, newDuration, false);

        assertThat(actualUpdatedTimeEntry.id()).isEqualTo(new TimeEntryId(42L));
        assertThat(actualUpdatedTimeEntry.userIdComposite()).isEqualTo(userIdComposite);
        assertThat(actualUpdatedTimeEntry.comment()).isEmpty();
        assertThat(actualUpdatedTimeEntry.start()).isEqualTo(newStart);
        assertThat(actualUpdatedTimeEntry.end()).isEqualTo(newStart.plusHours(3));
        assertThat(actualUpdatedTimeEntry.isBreak()).isFalse();
        assertThat(actualUpdatedTimeEntry.workDuration().duration()).isEqualTo(Duration.ofHours(3));

        final ArgumentCaptor<TimeEntryEntity> captor = ArgumentCaptor.forClass(TimeEntryEntity.class);
        verify(timeEntryRepository).save(captor.capture());

        final TimeEntryEntity actualPersisted = captor.getValue();

        assertThat(actualPersisted.getId()).isEqualTo(42L);
        assertThat(actualPersisted.getOwner()).isEqualTo("batman");
        assertThat(actualPersisted.getComment()).isEmpty();
        assertThat(actualPersisted.getStart()).isEqualTo(newStart.toInstant());
        assertThat(actualPersisted.getStartZoneId()).isEqualTo(newStart.getZone().getId());
        assertThat(actualPersisted.getEnd()).isEqualTo(newStart.plusHours(3).toInstant());
        assertThat(actualPersisted.getEndZoneId()).isEqualTo(sameEnd.getZone().getId());
        assertThat(actualPersisted.getUpdatedAt()).isEqualTo(Instant.now(clockFixed));
        assertThat(actualPersisted.isBreak()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void ensureUpdateTimeEntryEnd(boolean isBreak) throws Exception {

        final LocalDate from = LocalDate.of(2023, 1, 1);

        final LocalDateTime entryStart = LocalDateTime.of(from, LocalTime.of(10, 0, 0));
        final LocalDateTime entryEnd = entryStart.plusHours(2);

        final TimeEntryEntity existingEntity = new TimeEntryEntity(
            42L,
            "batman",
            "",
            entryStart.toInstant(UTC),
            ZONE_ID_UTC,
            entryEnd.toInstant(UTC),
            ZONE_ID_UTC,
            Instant.now(),
            isBreak);

        when(timeEntryRepository.findById(42L)).thenReturn(Optional.of(existingEntity));
        when(timeEntryRepository.save(any(TimeEntryEntity.class))).thenAnswer(returnsFirstArg());

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress(""), Set.of());
        when(userManagementService.findUserById(userId)).thenReturn(Optional.of(user));

        final ZonedDateTime sameStart = ZonedDateTime.ofInstant(entryStart.toInstant(UTC), ZONE_ID_UTC);
        final ZonedDateTime newEnd = ZonedDateTime.ofInstant(entryEnd.toInstant(UTC), ZONE_ID_UTC).minusMinutes(30);
        final Duration sameDuration = Duration.ofHours(2);

        final TimeEntry actualUpdatedTimeEntry = sut.updateTimeEntry(new TimeEntryId(42L), "", sameStart, newEnd, sameDuration, false);

        assertThat(actualUpdatedTimeEntry.id()).isEqualTo(new TimeEntryId(42L));
        assertThat(actualUpdatedTimeEntry.userIdComposite()).isEqualTo(userIdComposite);
        assertThat(actualUpdatedTimeEntry.comment()).isEmpty();
        assertThat(actualUpdatedTimeEntry.start()).isEqualTo(sameStart);
        assertThat(actualUpdatedTimeEntry.end()).isEqualTo(newEnd);
        assertThat(actualUpdatedTimeEntry.isBreak()).isFalse();
        assertThat(actualUpdatedTimeEntry.workDuration().duration()).isEqualTo(Duration.ofMinutes(90));

        final ArgumentCaptor<TimeEntryEntity> captor = ArgumentCaptor.forClass(TimeEntryEntity.class);
        verify(timeEntryRepository).save(captor.capture());

        final TimeEntryEntity actualPersisted = captor.getValue();

        assertThat(actualPersisted.getId()).isEqualTo(42L);
        assertThat(actualPersisted.getOwner()).isEqualTo("batman");
        assertThat(actualPersisted.getComment()).isEmpty();
        assertThat(actualPersisted.getStart()).isEqualTo(sameStart.toInstant());
        assertThat(actualPersisted.getStartZoneId()).isEqualTo(sameStart.getZone().getId());
        assertThat(actualPersisted.getEnd()).isEqualTo(newEnd.toInstant());
        assertThat(actualPersisted.getEndZoneId()).isEqualTo(newEnd.getZone().getId());
        assertThat(actualPersisted.getUpdatedAt()).isEqualTo(Instant.now(clockFixed));
        assertThat(actualPersisted.isBreak()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void ensureUpdateTimeEntryEndAndDuration(boolean isBreak) throws Exception {

        final LocalDate from = LocalDate.of(2023, 1, 1);

        final LocalDateTime entryStart = LocalDateTime.of(from, LocalTime.of(10, 0, 0));
        final LocalDateTime entryEnd = entryStart.plusHours(2);

        final TimeEntryEntity existingEntity = new TimeEntryEntity(
            42L,
            "batman",
            "",
            entryStart.toInstant(UTC),
            ZONE_ID_UTC,
            entryEnd.toInstant(UTC),
            ZONE_ID_UTC,
            Instant.now(),
            isBreak);

        when(timeEntryRepository.findById(42L)).thenReturn(Optional.of(existingEntity));
        when(timeEntryRepository.save(any(TimeEntryEntity.class))).thenAnswer(returnsFirstArg());

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress(""), Set.of());
        when(userManagementService.findUserById(userId)).thenReturn(Optional.of(user));

        final ZonedDateTime sameStart = ZonedDateTime.ofInstant(entryStart.toInstant(UTC), ZONE_ID_UTC);
        final ZonedDateTime newEnd = ZonedDateTime.ofInstant(entryEnd.toInstant(UTC), ZONE_ID_UTC).minusMinutes(30);
        final Duration newDuration = Duration.ofHours(3);

        final TimeEntry actualUpdatedTimeEntry = sut.updateTimeEntry(new TimeEntryId(42L), "", sameStart, newEnd, newDuration, false);

        assertThat(actualUpdatedTimeEntry.id()).isEqualTo(new TimeEntryId(42L));
        assertThat(actualUpdatedTimeEntry.userIdComposite()).isEqualTo(userIdComposite);
        assertThat(actualUpdatedTimeEntry.comment()).isEmpty();
        assertThat(actualUpdatedTimeEntry.start()).isEqualTo(newEnd.minusHours(3));
        assertThat(actualUpdatedTimeEntry.end()).isEqualTo(newEnd);
        assertThat(actualUpdatedTimeEntry.isBreak()).isFalse();
        assertThat(actualUpdatedTimeEntry.workDuration().duration()).isEqualTo(newDuration);

        final ArgumentCaptor<TimeEntryEntity> captor = ArgumentCaptor.forClass(TimeEntryEntity.class);
        verify(timeEntryRepository).save(captor.capture());

        final TimeEntryEntity actualPersisted = captor.getValue();

        assertThat(actualPersisted.getId()).isEqualTo(42L);
        assertThat(actualPersisted.getOwner()).isEqualTo("batman");
        assertThat(actualPersisted.getComment()).isEmpty();
        assertThat(actualPersisted.getStart()).isEqualTo(newEnd.minusHours(3).toInstant());
        assertThat(actualPersisted.getStartZoneId()).isEqualTo(sameStart.getZone().getId());
        assertThat(actualPersisted.getEnd()).isEqualTo(newEnd.toInstant());
        assertThat(actualPersisted.getEndZoneId()).isEqualTo(newEnd.getZone().getId());
        assertThat(actualPersisted.getUpdatedAt()).isEqualTo(Instant.now(clockFixed));
        assertThat(actualPersisted.isBreak()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void ensureUpdateTimeEntryDuration(boolean isBreak) throws Exception {

        final LocalDate from = LocalDate.of(2023, 1, 1);

        final LocalDateTime entryStart = LocalDateTime.of(from, LocalTime.of(10, 0, 0));
        final LocalDateTime entryEnd = entryStart.plusHours(2);

        final TimeEntryEntity existingEntity = new TimeEntryEntity(
            42L,
            "batman",
            "",
            entryStart.toInstant(UTC),
            ZONE_ID_UTC,
            entryEnd.toInstant(UTC),
            ZONE_ID_UTC,
            Instant.now(),
            isBreak);

        when(timeEntryRepository.findById(42L)).thenReturn(Optional.of(existingEntity));
        when(timeEntryRepository.save(any(TimeEntryEntity.class))).thenAnswer(returnsFirstArg());

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress(""), Set.of());
        when(userManagementService.findUserById(userId)).thenReturn(Optional.of(user));

        final ZonedDateTime sameStart = ZonedDateTime.ofInstant(entryStart.toInstant(UTC), ZONE_ID_UTC);
        final ZonedDateTime sameEnd = ZonedDateTime.ofInstant(entryEnd.toInstant(UTC), ZONE_ID_UTC);
        final Duration newDuration = Duration.ofHours(4);

        final TimeEntry actualUpdatedTimeEntry = sut.updateTimeEntry(new TimeEntryId(42L), "", sameStart, sameEnd, newDuration, false);

        assertThat(actualUpdatedTimeEntry.id()).isEqualTo(new TimeEntryId(42L));
        assertThat(actualUpdatedTimeEntry.userIdComposite()).isEqualTo(userIdComposite);
        assertThat(actualUpdatedTimeEntry.comment()).isEmpty();
        assertThat(actualUpdatedTimeEntry.start()).isEqualTo(sameStart);
        assertThat(actualUpdatedTimeEntry.end()).isEqualTo(sameStart.plusMinutes(newDuration.toMinutes()));
        assertThat(actualUpdatedTimeEntry.isBreak()).isFalse();
        assertThat(actualUpdatedTimeEntry.workDuration().duration()).isEqualTo(newDuration);

        final ArgumentCaptor<TimeEntryEntity> captor = ArgumentCaptor.forClass(TimeEntryEntity.class);
        verify(timeEntryRepository).save(captor.capture());

        final TimeEntryEntity actualPersisted = captor.getValue();

        assertThat(actualPersisted.getId()).isEqualTo(42L);
        assertThat(actualPersisted.getOwner()).isEqualTo("batman");
        assertThat(actualPersisted.getComment()).isEmpty();
        assertThat(actualPersisted.getStart()).isEqualTo(sameStart.toInstant());
        assertThat(actualPersisted.getStartZoneId()).isEqualTo(sameStart.getZone().getId());
        assertThat(actualPersisted.getEnd()).isEqualTo(sameStart.plusMinutes(newDuration.toMinutes()).toInstant());
        assertThat(actualPersisted.getEndZoneId()).isEqualTo(sameStart.getZone().getId());
        assertThat(actualPersisted.getUpdatedAt()).isEqualTo(Instant.now(clockFixed));
        assertThat(actualPersisted.isBreak()).isFalse();
    }

    @Test
    void ensureUpdateTimeEntryWhenStartEndDurationAreGivenAndPlausible() throws Exception {

        final LocalDate date = LocalDate.of(2023, 2, 27);
        final LocalDateTime previousStart = LocalDateTime.of(date, LocalTime.of(15, 0, 0));
        final LocalDateTime previousEnd = LocalDateTime.of(date, LocalTime.of(16, 0, 0));

        final TimeEntryEntity entity = new TimeEntryEntity(
            42L,
            "batman",
            "",
            previousStart.toInstant(UTC),
            ZONE_ID_UTC,
            previousEnd.toInstant(UTC),
            ZONE_ID_UTC,
            Instant.now(),
            false);

        when(timeEntryRepository.findById(42L)).thenReturn(Optional.of(entity));
        when(timeEntryRepository.save(any(TimeEntryEntity.class))).thenAnswer(returnsFirstArg());

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress(""), Set.of());
        when(userManagementService.findUserById(userId)).thenReturn(Optional.of(user));

        final ZonedDateTime newStart = previousStart.minusHours(1).atZone(ZONE_ID_UTC);
        final ZonedDateTime newEnd = previousEnd.plusHours(1).atZone(ZONE_ID_UTC);
        final Duration newDuration = Duration.ofHours(3);

        final TimeEntry actualUpdatedTimeEntry = sut.updateTimeEntry(new TimeEntryId(42L), "", newStart, newEnd, newDuration, false);

        assertThat(actualUpdatedTimeEntry.id()).isEqualTo(new TimeEntryId(42L));
        assertThat(actualUpdatedTimeEntry.userIdComposite()).isEqualTo(userIdComposite);
        assertThat(actualUpdatedTimeEntry.comment()).isEmpty();
        assertThat(actualUpdatedTimeEntry.start()).isEqualTo(newStart);
        assertThat(actualUpdatedTimeEntry.end()).isEqualTo(newEnd);
        assertThat(actualUpdatedTimeEntry.isBreak()).isFalse();
        assertThat(actualUpdatedTimeEntry.workDuration().duration()).isEqualTo(newDuration);

        final ArgumentCaptor<TimeEntryEntity> captor = ArgumentCaptor.forClass(TimeEntryEntity.class);
        verify(timeEntryRepository).save(captor.capture());

        final TimeEntryEntity actualPersisted = captor.getValue();

        assertThat(actualPersisted.getId()).isEqualTo(42L);
        assertThat(actualPersisted.getOwner()).isEqualTo("batman");
        assertThat(actualPersisted.getComment()).isEmpty();
        assertThat(actualPersisted.getStart()).isEqualTo(newStart.toInstant());
        assertThat(actualPersisted.getStartZoneId()).isEqualTo(newStart.getZone().getId());
        assertThat(actualPersisted.getEnd()).isEqualTo(newEnd.toInstant());
        assertThat(actualPersisted.getEndZoneId()).isEqualTo(newEnd.getZone().getId());
        assertThat(actualPersisted.getUpdatedAt()).isEqualTo(Instant.now(clockFixed));
        assertThat(actualPersisted.isBreak()).isFalse();
    }

    @Test
    void ensureUpdateTimeEntryStartWithGivenEndAndDuration() throws Exception {

        final LocalDate date = LocalDate.of(2023, 2, 27);
        final LocalDateTime previousStart = LocalDateTime.of(date, LocalTime.of(15, 0, 0));
        final LocalDateTime previousEnd = LocalDateTime.of(date, LocalTime.of(16, 0, 0));

        final TimeEntryEntity entity = new TimeEntryEntity(
            42L,
            "batman",
            "",
            previousStart.toInstant(UTC),
            ZONE_ID_UTC,
            previousEnd.toInstant(UTC),
            ZONE_ID_UTC,
            Instant.now(),
            false);

        when(timeEntryRepository.findById(42L)).thenReturn(Optional.of(entity));
        when(timeEntryRepository.save(any(TimeEntryEntity.class))).thenAnswer(returnsFirstArg());

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress(""), Set.of());
        when(userManagementService.findUserById(userId)).thenReturn(Optional.of(user));

        final ZonedDateTime newEnd = previousEnd.plusHours(1).atZone(ZONE_ID_UTC);
        final Duration newDuration = Duration.ofHours(3);

        sut.updateTimeEntry(new TimeEntryId(42L), "", null, newEnd, newDuration, false);

        final ArgumentCaptor<TimeEntryEntity> captor = ArgumentCaptor.forClass(TimeEntryEntity.class);
        verify(timeEntryRepository).save(captor.capture());

        final TimeEntryEntity actualPersisted = captor.getValue();
        final ZonedDateTime expectedStart = previousStart.minusHours(1).atZone(ZONE_ID_UTC);

        assertThat(actualPersisted.getStart()).isEqualTo(expectedStart.toInstant());
        assertThat(actualPersisted.getStartZoneId()).isEqualTo(expectedStart.getZone().getId());
        assertThat(actualPersisted.getEnd()).isEqualTo(newEnd.toInstant());
        assertThat(actualPersisted.getEndZoneId()).isEqualTo(newEnd.getZone().getId());
    }

    @Test
    void ensureUpdateTimeEntryEndWithGivenStartAndDuration() throws Exception {

        final LocalDate date = LocalDate.of(2023, 2, 27);
        final LocalDateTime previousStart = LocalDateTime.of(date, LocalTime.of(15, 0, 0));
        final LocalDateTime previousEnd = LocalDateTime.of(date, LocalTime.of(16, 0, 0));

        final TimeEntryEntity entity = new TimeEntryEntity(
            42L,
            "batman",
            "",
            previousStart.toInstant(UTC),
            ZONE_ID_UTC,
            previousEnd.toInstant(UTC),
            ZONE_ID_UTC,
            Instant.now(),
            false);

        when(timeEntryRepository.findById(42L)).thenReturn(Optional.of(entity));
        when(timeEntryRepository.save(any(TimeEntryEntity.class))).thenAnswer(returnsFirstArg());

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress(""), Set.of());
        when(userManagementService.findUserById(userId)).thenReturn(Optional.of(user));

        final ZonedDateTime newStart = previousStart.minusHours(1).atZone(ZONE_ID_UTC);
        final Duration newDuration = Duration.ofHours(3);

        sut.updateTimeEntry(new TimeEntryId(42L), "", newStart, null, newDuration, false);

        final ArgumentCaptor<TimeEntryEntity> captor = ArgumentCaptor.forClass(TimeEntryEntity.class);
        verify(timeEntryRepository).save(captor.capture());

        final TimeEntryEntity actualPersisted = captor.getValue();
        final ZonedDateTime expectedEnd = previousEnd.plusHours(1).atZone(ZONE_ID_UTC);

        assertThat(actualPersisted.getStart()).isEqualTo(newStart.toInstant());
        assertThat(actualPersisted.getStartZoneId()).isEqualTo(newStart.getZone().getId());
        assertThat(actualPersisted.getEnd()).isEqualTo(expectedEnd.toInstant());
        assertThat(actualPersisted.getEndZoneId()).isEqualTo(expectedEnd.getZone().getId());
    }

    static Stream<Duration> emptyDuration() {
        return Stream.of(null, Duration.ZERO);
    }

    @ParameterizedTest
    @MethodSource("emptyDuration")
    void ensureUpdateTimeEntryStartAndEndWithoutDuration(Duration givenDuration) throws Exception {

        final LocalDate date = LocalDate.of(2023, 2, 27);
        final LocalDateTime previousStart = LocalDateTime.of(date, LocalTime.of(15, 0, 0));
        final LocalDateTime previousEnd = LocalDateTime.of(date, LocalTime.of(16, 0, 0));

        final TimeEntryEntity entity = new TimeEntryEntity(
            42L,
            "batman",
            "",
            previousStart.toInstant(UTC),
            ZONE_ID_UTC,
            previousEnd.toInstant(UTC),
            ZONE_ID_UTC,
            Instant.now(),
            false);

        when(timeEntryRepository.findById(42L)).thenReturn(Optional.of(entity));
        when(timeEntryRepository.save(any(TimeEntryEntity.class))).thenAnswer(returnsFirstArg());

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress(""), Set.of());
        when(userManagementService.findUserById(userId)).thenReturn(Optional.of(user));

        final ZonedDateTime newStart = previousStart.minusHours(1).atZone(ZONE_ID_UTC);
        final ZonedDateTime newEnd = previousEnd.plusHours(1).atZone(ZONE_ID_UTC);

        sut.updateTimeEntry(new TimeEntryId(42L), "", newStart, newEnd, givenDuration, false);

        final ArgumentCaptor<TimeEntryEntity> captor = ArgumentCaptor.forClass(TimeEntryEntity.class);
        verify(timeEntryRepository).save(captor.capture());

        final TimeEntryEntity actualPersisted = captor.getValue();

        assertThat(actualPersisted.getStart()).isEqualTo(newStart.toInstant());
        assertThat(actualPersisted.getStartZoneId()).isEqualTo(newStart.getZone().getId());
        assertThat(actualPersisted.getEnd()).isEqualTo(newEnd.toInstant());
        assertThat(actualPersisted.getEndZoneId()).isEqualTo(newEnd.getZone().getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @NullSource
    void ensureUpdateTimeEntryWithComment(String givenComment) throws Exception {

        final LocalDate from = LocalDate.of(2023, 1, 1);

        final LocalDateTime entryStart = LocalDateTime.of(from, LocalTime.of(10, 0, 0));
        final LocalDateTime entryEnd = entryStart.plusHours(2);

        final TimeEntryEntity existingEntity = new TimeEntryEntity(
            42L,
            "batman",
            givenComment,
            entryStart.toInstant(UTC),
            ZONE_ID_UTC,
            entryEnd.toInstant(UTC),
            ZONE_ID_UTC,
            Instant.now(),
            false);

        when(timeEntryRepository.findById(42L)).thenReturn(Optional.of(existingEntity));
        when(timeEntryRepository.save(any(TimeEntryEntity.class))).thenAnswer(returnsFirstArg());

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress(""), Set.of());
        when(userManagementService.findUserById(userId)).thenReturn(Optional.of(user));

        final ZonedDateTime sameStart = ZonedDateTime.ofInstant(entryStart.toInstant(UTC), ZONE_ID_UTC);
        final ZonedDateTime sameEnd = ZonedDateTime.ofInstant(entryEnd.toInstant(UTC), ZONE_ID_UTC);
        final Duration sameDuration = Duration.ofHours(2);

        final TimeEntry actualUpdatedTimeEntry = sut.updateTimeEntry(new TimeEntryId(42L), "", sameStart, sameEnd, sameDuration, true);

        assertThat(actualUpdatedTimeEntry.id()).isEqualTo(new TimeEntryId(42L));
        assertThat(actualUpdatedTimeEntry.comment()).isEmpty();

        final ArgumentCaptor<TimeEntryEntity> captor = ArgumentCaptor.forClass(TimeEntryEntity.class);
        verify(timeEntryRepository).save(captor.capture());

        final TimeEntryEntity actualPersisted = captor.getValue();
        assertThat(actualPersisted.getComment()).isEmpty();
    }

    @Test
    void ensureUpdateTimeEntryIsBreak() throws Exception {

        final LocalDate from = LocalDate.of(2023, 1, 1);

        final LocalDateTime entryStart = LocalDateTime.of(from, LocalTime.of(10, 0, 0));
        final LocalDateTime entryEnd = entryStart.plusHours(2);

        final TimeEntryEntity existingEntity = new TimeEntryEntity(
            42L,
            "batman",
            "",
            entryStart.toInstant(UTC),
            ZONE_ID_UTC,
            entryEnd.toInstant(UTC),
            ZONE_ID_UTC,
            Instant.now(),
            false);

        when(timeEntryRepository.findById(42L)).thenReturn(Optional.of(existingEntity));
        when(timeEntryRepository.save(any(TimeEntryEntity.class))).thenAnswer(returnsFirstArg());

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress(""), Set.of());
        when(userManagementService.findUserById(userId)).thenReturn(Optional.of(user));

        final ZonedDateTime sameStart = ZonedDateTime.ofInstant(entryStart.toInstant(UTC), ZONE_ID_UTC);
        final ZonedDateTime sameEnd = ZonedDateTime.ofInstant(entryEnd.toInstant(UTC), ZONE_ID_UTC);
        final Duration sameDuration = Duration.ofHours(2);

        final TimeEntry actualUpdatedTimeEntry = sut.updateTimeEntry(new TimeEntryId(42L), "", sameStart, sameEnd, sameDuration, true);

        assertThat(actualUpdatedTimeEntry.id()).isEqualTo(new TimeEntryId(42L));
        assertThat(actualUpdatedTimeEntry.userIdComposite()).isEqualTo(userIdComposite);
        assertThat(actualUpdatedTimeEntry.comment()).isEmpty();
        assertThat(actualUpdatedTimeEntry.start()).isEqualTo(sameStart);
        assertThat(actualUpdatedTimeEntry.end()).isEqualTo(sameEnd);
        assertThat(actualUpdatedTimeEntry.isBreak()).isTrue();
        assertThat(actualUpdatedTimeEntry.workDuration().duration()).isEqualTo(Duration.ZERO);

        final ArgumentCaptor<TimeEntryEntity> captor = ArgumentCaptor.forClass(TimeEntryEntity.class);
        verify(timeEntryRepository).save(captor.capture());

        final TimeEntryEntity actualPersisted = captor.getValue();

        assertThat(actualPersisted.getId()).isEqualTo(42L);
        assertThat(actualPersisted.getOwner()).isEqualTo("batman");
        assertThat(actualPersisted.getComment()).isEmpty();
        assertThat(actualPersisted.getStart()).isEqualTo(sameStart.toInstant());
        assertThat(actualPersisted.getStartZoneId()).isEqualTo(sameStart.getZone().getId());
        assertThat(actualPersisted.getEnd()).isEqualTo(sameEnd.toInstant());
        assertThat(actualPersisted.getEndZoneId()).isEqualTo(sameEnd.getZone().getId());
        assertThat(actualPersisted.getUpdatedAt()).isEqualTo(Instant.now(clockFixed));
        assertThat(actualPersisted.isBreak()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void ensureUpdateTimeEntryPublishesUpdatedEvent(boolean locked) throws Exception {

        final LocalDate from = LocalDate.of(2023, 1, 1);

        final LocalDateTime entryStart = LocalDateTime.of(from, LocalTime.of(10, 0, 0));
        final LocalDateTime entryEnd = entryStart.plusHours(2);

        final TimeEntryEntity existingEntity = new TimeEntryEntity(
            42L,
            "batman",
            "",
            entryStart.toInstant(UTC),
            ZONE_ID_UTC,
            entryEnd.toInstant(UTC),
            ZONE_ID_UTC,
            Instant.now(),
            false);

        final ZonedDateTime sameStart = ZonedDateTime.ofInstant(entryStart.toInstant(UTC), ZONE_ID_UTC);
        final ZonedDateTime sameEnd = ZonedDateTime.ofInstant(entryEnd.toInstant(UTC), ZONE_ID_UTC);
        final Duration sameDuration = Duration.ofHours(2);

        when(timeEntryRepository.findById(42L)).thenReturn(Optional.of(existingEntity));
        when(timeEntryRepository.save(any(TimeEntryEntity.class))).thenAnswer(returnsFirstArg());

        when(timeEntryLockService.isLocked(sameStart.toLocalDate())).thenReturn(locked);

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress(""), Set.of());
        when(userManagementService.findUserById(userId)).thenReturn(Optional.of(user));

        sut.updateTimeEntry(new TimeEntryId(42L), "", sameStart, sameEnd, sameDuration, true);

        final ArgumentCaptor<TimeEntryUpdatedEvent> captor = ArgumentCaptor.forClass(TimeEntryUpdatedEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());

        assertThat(captor.getValue()).satisfies(actual -> {
            assertThat(actual.lockedCandidate().current()).isEqualTo(locked);
            assertThat(actual.dateCandidate().current()).isEqualTo(sameStart.toLocalDate());
            assertThat(actual.ownerUserIdComposite()).isEqualTo(userIdComposite);
            assertThat(actual.timeEntryId()).isEqualTo(new TimeEntryId(42L));
        });
    }

    @Test
    void ensureGetEntriesForAllUsers() {

        final ZoneId userZoneId = ZoneId.of("Europe/Berlin");
        when(userSettingsProvider.zoneId()).thenReturn(userZoneId);

        final Instant now = Instant.now();
        final LocalDate from = LocalDate.of(2023, 1, 1);
        final LocalDate toExclusive = LocalDate.of(2023, 2, 1);

        final LocalDateTime entryStart = LocalDateTime.of(from, LocalTime.of(10, 0, 0));
        final LocalDateTime entryEnd = LocalDateTime.of(toExclusive, LocalTime.of(12, 0, 0));
        final TimeEntryEntity timeEntryEntity = new TimeEntryEntity(1L, "batman", "hard work", entryStart.toInstant(UTC), ZONE_ID_UTC, entryEnd.toInstant(UTC), ZONE_ID_UTC, now, false);

        final LocalDateTime entryBreakStart = LocalDateTime.of(from, LocalTime.of(12, 0, 0));
        final LocalDateTime entryBreakEnd = LocalDateTime.of(toExclusive, LocalTime.of(13, 0, 0));
        final TimeEntryEntity timeEntryBreakEntity = new TimeEntryEntity(2L, "pinguin", "deserved break", entryBreakStart.toInstant(UTC), ZONE_ID_UTC, entryBreakEnd.toInstant(UTC), ZONE_ID_UTC, now, true);

        when(timeEntryRepository.findAllByStartGreaterThanEqualAndStartLessThanOrderByStartDesc(from.atStartOfDay().atZone(userZoneId).toInstant(), toExclusive.atStartOfDay().atZone(userZoneId).toInstant()))
            .thenReturn(List.of(timeEntryEntity, timeEntryBreakEntity));

        final UserId batmanId = new UserId("batman");
        final UserLocalId batmanLocalId = new UserLocalId(1L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);

        final UserId pinguinId = new UserId("pinguin");
        final UserLocalId pinguinLocalId = new UserLocalId(2L);
        final UserIdComposite pinguinIdComposite = new UserIdComposite(pinguinId, pinguinLocalId);

        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        final User pinguin = new User(pinguinIdComposite, "ping", "uin", new EMailAddress("pinguin@example.org"), Set.of());
        when(userManagementService.findAllUsers()).thenReturn(List.of(batman, pinguin));

        final Map<UserIdComposite, List<TimeEntry>> actual = sut.getEntriesForAllUsers(from, toExclusive);

        final ZonedDateTime expectedStart = ZonedDateTime.of(entryStart, ZONE_ID_UTC);
        final ZonedDateTime expectedEnd = ZonedDateTime.of(entryEnd, ZONE_ID_UTC);

        final ZonedDateTime expectedBreakStart = ZonedDateTime.of(entryBreakStart, ZONE_ID_UTC);
        final ZonedDateTime expectedBreakEnd = ZonedDateTime.of(entryBreakEnd, ZONE_ID_UTC);

        assertThat(actual)
            .hasSize(2)
            .hasEntrySatisfying(batmanIdComposite, timeEntries -> {
                assertThat(timeEntries).containsExactly(
                    new TimeEntry(new TimeEntryId(1L), batmanIdComposite, "hard work", expectedStart, expectedEnd, false)
                );
            })
            .hasEntrySatisfying(pinguinIdComposite, timeEntries -> {
                assertThat(timeEntries).containsExactly(
                    new TimeEntry(new TimeEntryId(2L), pinguinIdComposite, "deserved break", expectedBreakStart, expectedBreakEnd, true)
                );
            });
    }

    @Test
    void ensureGetEntries() {

        final ZoneId userZoneId = ZoneId.of("Europe/Berlin");
        when(userSettingsProvider.zoneId()).thenReturn(userZoneId);

        final UserId batmanId = new UserId("uuid-1");
        final UserLocalId batmanLocalId = new UserLocalId(1L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);

        final UserId robinId = new UserId("uuid-2");
        final UserLocalId robinLocalId = new UserLocalId(2L);
        final UserIdComposite robinIdComposite = new UserIdComposite(robinId, robinLocalId);

        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
        final User robin = new User(robinIdComposite, "Dick", "Grayson", new EMailAddress("robin@example.org"), Set.of());

        when(userManagementService.findAllUsersByLocalIds(List.of(batmanLocalId, robinLocalId))).thenReturn(List.of(batman, robin));

        final Instant now = Instant.now();
        final LocalDate from = LocalDate.of(2023, 1, 1);
        final LocalDate toExclusive = LocalDate.of(2023, 2, 1);

        final LocalDateTime entryStart = LocalDateTime.of(from, LocalTime.of(10, 0, 0));
        final LocalDateTime entryEnd = LocalDateTime.of(toExclusive, LocalTime.of(12, 0, 0));
        final TimeEntryEntity timeEntryEntity = new TimeEntryEntity(1L, "uuid-1", "hard work", entryStart.toInstant(UTC), ZONE_ID_UTC, entryEnd.toInstant(UTC), ZONE_ID_UTC, now, false);

        final LocalDateTime entryBreakStart = LocalDateTime.of(from, LocalTime.of(12, 0, 0));
        final LocalDateTime entryBreakEnd = LocalDateTime.of(toExclusive, LocalTime.of(13, 0, 0));
        final TimeEntryEntity timeEntryBreakEntity = new TimeEntryEntity(2L, "uuid-2", "deserved break", entryBreakStart.toInstant(UTC), ZONE_ID_UTC, entryBreakEnd.toInstant(UTC), ZONE_ID_UTC, now, true);

        when(timeEntryRepository.findAllByOwnerIsInAndStartGreaterThanEqualAndStartLessThanOrderByStartDesc(List.of("uuid-1", "uuid-2"), from.atStartOfDay().atZone(userZoneId).toInstant(), toExclusive.atStartOfDay().atZone(userZoneId).toInstant()))
            .thenReturn(List.of(timeEntryEntity, timeEntryBreakEntity));

        final Map<UserIdComposite, List<TimeEntry>> actual = sut.getEntries(from, toExclusive, List.of(batmanLocalId, robinLocalId));

        final ZonedDateTime expectedStart = ZonedDateTime.of(entryStart, ZONE_ID_UTC);
        final ZonedDateTime expectedEnd = ZonedDateTime.of(entryEnd, ZONE_ID_UTC);

        final ZonedDateTime expectedBreakStart = ZonedDateTime.of(entryBreakStart, ZONE_ID_UTC);
        final ZonedDateTime expectedBreakEnd = ZonedDateTime.of(entryBreakEnd, ZONE_ID_UTC);

        assertThat(actual)
            .hasSize(2)
            .hasEntrySatisfying(batmanIdComposite, timeEntries -> {
                assertThat(timeEntries).containsExactly(
                    new TimeEntry(new TimeEntryId(1L), batmanIdComposite, "hard work", expectedStart, expectedEnd, false)
                );
            })
            .hasEntrySatisfying(robinIdComposite, timeEntries -> {
                assertThat(timeEntries).containsExactly(
                    new TimeEntry(new TimeEntryId(2L), robinIdComposite, "deserved break", expectedBreakStart, expectedBreakEnd, true)
                );
            });
    }

    @Test
    void ensureGetEntriesByUserLocalIdsReturnsValuesForEveryAskedUserLocalId() {

        final ZoneId userZoneId = ZoneId.of("Europe/Berlin");
        when(userSettingsProvider.zoneId()).thenReturn(userZoneId);

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress(""), Set.of());
        when(userManagementService.findAllUsersByLocalIds(List.of(userLocalId))).thenReturn(List.of(user));

        final LocalDate from = LocalDate.of(2023, 1, 1);
        final LocalDate toExclusive = LocalDate.of(2023, 2, 1);

        when(timeEntryRepository.findAllByOwnerIsInAndStartGreaterThanEqualAndStartLessThanOrderByStartDesc(List.of("batman"), from.atStartOfDay().atZone(userZoneId).toInstant(), toExclusive.atStartOfDay().atZone(userZoneId).toInstant()))
            .thenReturn(List.of());

        final Map<UserIdComposite, List<TimeEntry>> actual = sut.getEntries(from, toExclusive, List.of(userLocalId));

        assertThat(actual)
            .hasSize(1)
            .hasEntrySatisfying(userIdComposite, timeEntries -> {
                assertThat(timeEntries).isEmpty();
            });
    }

    @Test
    void ensureGetEntriesSortedByStart_NewestFirst() {

        final ZoneId userZoneId = ZoneId.of("Europe/Berlin");
        when(userSettingsProvider.zoneId()).thenReturn(userZoneId);

        final LocalDate periodFrom = LocalDate.of(2022, 1, 3);
        final LocalDate periodToExclusive = LocalDate.of(2022, 1, 10);

        final LocalDateTime entryStart = LocalDateTime.of(periodFrom, LocalTime.of(10, 0, 0));
        final LocalDateTime entryEnd = LocalDateTime.of(periodToExclusive, LocalTime.of(12, 0, 0));
        final TimeEntryEntity timeEntryEntity = new TimeEntryEntity(1L, "batman", "hard work", entryStart.toInstant(UTC), ZoneId.of("UTC"), entryEnd.toInstant(UTC), ZoneId.of("UTC"), Instant.now(), false);

        final LocalDateTime entryBreakStart = LocalDateTime.of(periodFrom, LocalTime.of(12, 0, 0));
        final LocalDateTime entryBreakEnd = LocalDateTime.of(periodToExclusive, LocalTime.of(13, 0, 0));
        final TimeEntryEntity timeEntryBreakEntity = new TimeEntryEntity(2L, "batman", "deserved break", entryBreakStart.toInstant(UTC), ZoneId.of("UTC"), entryBreakEnd.toInstant(UTC), ZoneId.of("UTC"), Instant.now(), true);

        final LocalDateTime entryStart2 = LocalDateTime.of(periodFrom, LocalTime.of(8, 0, 0));
        final LocalDateTime entryEnd2 = LocalDateTime.of(periodToExclusive, LocalTime.of(8, 30, 0));
        final TimeEntryEntity timeEntryEntity2 = new TimeEntryEntity(3L, "batman", "waking up *zzzz", entryStart2.toInstant(UTC), ZoneId.of("UTC"), entryEnd2.toInstant(UTC), ZoneId.of("UTC"), Instant.now(), false);

        final Instant periodStartInstant = periodFrom.atStartOfDay().atZone(userZoneId).toInstant();
        final Instant periodEndInstant = periodToExclusive.atStartOfDay().atZone(userZoneId).toInstant();
        when(timeEntryRepository.findAllByOwnerAndStartGreaterThanEqualAndStartLessThanOrderByStartDesc("batman", periodStartInstant, periodEndInstant))
            .thenReturn(List.of(timeEntryEntity, timeEntryBreakEntity, timeEntryEntity2));

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress(""), Set.of());
        when(userManagementService.findUserByLocalId(userLocalId)).thenReturn(Optional.of(user));

        final List<TimeEntry> actualEntries = sut.getEntries(periodFrom, periodToExclusive, userLocalId);

        final ZonedDateTime expectedStart = ZonedDateTime.of(entryStart, ZONE_ID_UTC);
        final ZonedDateTime expectedEnd = ZonedDateTime.of(entryEnd, ZONE_ID_UTC);

        final ZonedDateTime expectedBreakStart = ZonedDateTime.of(entryBreakStart, ZONE_ID_UTC);
        final ZonedDateTime expectedBreakEnd = ZonedDateTime.of(entryBreakEnd, ZONE_ID_UTC);

        final ZonedDateTime expectedStart2 = ZonedDateTime.of(entryStart2, ZONE_ID_UTC);
        final ZonedDateTime expectedEnd2 = ZonedDateTime.of(entryEnd2, ZONE_ID_UTC);

        assertThat(actualEntries).containsExactly(
            new TimeEntry(new TimeEntryId(2L), userIdComposite, "deserved break", expectedBreakStart, expectedBreakEnd, true),
            new TimeEntry(new TimeEntryId(1L), userIdComposite, "hard work", expectedStart, expectedEnd, false),
            new TimeEntry(new TimeEntryId(3L), userIdComposite, "waking up *zzzz", expectedStart2, expectedEnd2, false)
        );
    }

//    @Test
//    void ensureGetEntryWeekPageWithFirstDayOfMonth() {
//
//        final ZoneId userZoneId = ZoneId.of("Europe/Berlin");
//        when(userSettingsProvider.zoneId()).thenReturn(userZoneId);
//
//        final LocalDate firstDayOfWeek = LocalDate.of(2022, 1, 3);
//        when(userDateService.firstDayOfWeek(Year.of(2022), 1)).thenReturn(firstDayOfWeek);
//
//        final ZonedDateTime timeEntryStart = ZonedDateTime.of(2022, 1, 4, 9, 0, 0, 0, userZoneId);
//        final ZonedDateTime timeEntryEnd = ZonedDateTime.of(2022, 1, 4, 12, 0, 0, 0, userZoneId);
//        final TimeEntryEntity timeEntryEntity = new TimeEntryEntity("tenantId", 1L, "batman", "hack the planet!", timeEntryStart.toInstant(), userZoneId, timeEntryEnd.toInstant(), userZoneId, Instant.now(), false);
//
//        final ZonedDateTime timeEntryBreakStart = ZonedDateTime.of(2022, 1, 4, 12, 0, 0, 0, userZoneId);
//        final ZonedDateTime timeEntryBreakEnd = ZonedDateTime.of(2022, 1, 4, 13, 0, 0, 0, userZoneId);
//        final TimeEntryEntity timeEntryBreakEntity = new TimeEntryEntity(2L, "batman", "deserved break", timeEntryBreakStart.toInstant(), userZoneId, timeEntryBreakEnd.toInstant(), userZoneId, Instant.now(), true);
//
//        final ZonedDateTime fromDateTime = firstDayOfWeek.atStartOfDay(userZoneId);
//        final Instant from = Instant.from(fromDateTime);
//        final Instant to = Instant.from(fromDateTime.plusWeeks(1));
//
//        when(timeEntryRepository.findAllByOwnerAndStartGreaterThanEqualAndStartLessThanOrderByStartDesc("batman", from, to))
//            .thenReturn(List.of(timeEntryEntity, timeEntryBreakEntity));
//
//        when(timeEntryRepository.countAllByOwner("batman")).thenReturn(3L);
//
//        final UserId userId = new UserId("batman");
//        final UserLocalId userLocalId = new UserLocalId(1337L);
//        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
//
//        final User batman = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
//        when(userManagementService.findUserByLocalId(userLocalId)).thenReturn(Optional.of(batman));
//
//        when(workingTimeCalendarService.getWorkingTimeCalender(firstDayOfWeek, firstDayOfWeek.plusWeeks(1), userLocalId))
//            .thenReturn(new WorkingTimeCalendar(Map.of(
//                LocalDate.of(2022, 1, 3), PlannedWorkingHours.EIGHT,
//                LocalDate.of(2022, 1, 4), PlannedWorkingHours.EIGHT,
//                LocalDate.of(2022, 1, 5), PlannedWorkingHours.EIGHT,
//                LocalDate.of(2022, 1, 6), PlannedWorkingHours.EIGHT,
//                LocalDate.of(2022, 1, 7), PlannedWorkingHours.EIGHT,
//                LocalDate.of(2022, 1, 8), PlannedWorkingHours.ZERO, // saturday
//                LocalDate.of(2022, 1, 9), PlannedWorkingHours.ZERO  // sunday
//            ), Map.of()));
//
//        final TimeEntryWeekPage actual = sut.getEntryWeekPage(userLocalId, 2022, 1);
//
//        assertThat(actual).isEqualTo(
//            new TimeEntryWeekPage(
//                new TimeEntryWeek(
//                    firstDayOfWeek,
//                    new PlannedWorkingHours(Duration.ofHours(40)),
//                    List.of(
//                        new TimeEntryDay(
//                            false,
//                            LocalDate.of(2022, 1, 9),
//                            PlannedWorkingHours.ZERO,
//                            ShouldWorkingHours.ZERO,
//                            List.of(),
//                            List.of()),
//                        new TimeEntryDay(
//                            false,
//                            LocalDate.of(2022, 1, 8),
//                            PlannedWorkingHours.ZERO,
//                            ShouldWorkingHours.ZERO,
//                            List.of(),
//                            List.of()),
//                        new TimeEntryDay(
//                            false,
//                            LocalDate.of(2022, 1, 7),
//                            PlannedWorkingHours.EIGHT,
//                            ShouldWorkingHours.EIGHT,
//                            List.of(),
//                            List.of()),
//                        new TimeEntryDay(
//                            false,
//                            LocalDate.of(2022, 1, 6),
//                            PlannedWorkingHours.EIGHT,
//                            ShouldWorkingHours.EIGHT,
//                            List.of(),
//                            List.of()),
//                        new TimeEntryDay(
//                            false,
//                            LocalDate.of(2022, 1, 5),
//                            PlannedWorkingHours.EIGHT,
//                            ShouldWorkingHours.EIGHT,
//                            List.of(),
//                            List.of()),
//                        new TimeEntryDay(
//                            false,
//                            LocalDate.of(2022, 1, 4),
//                            PlannedWorkingHours.EIGHT,
//                            ShouldWorkingHours.EIGHT,
//                            List.of(
//                                new TimeEntry(new TimeEntryId(1L), userIdComposite, "hack the planet!", timeEntryStart, timeEntryEnd, false),
//                                new TimeEntry(new TimeEntryId(2L), userIdComposite, "deserved break", timeEntryBreakStart, timeEntryBreakEnd, true)
//                            ),
//                            List.of()),
//                        new TimeEntryDay(
//                            false,
//                            firstDayOfWeek,
//                            PlannedWorkingHours.EIGHT,
//                            ShouldWorkingHours.EIGHT,
//                            List.of(),
//                            List.of())
//                    )
//                ),
//                3
//            )
//        );
//    }
//
//    @Test
//    void ensureGetEntryWeekPageWithDaysInCorrectOrder() {
//
//        final ZoneId userZoneId = ZoneId.of("Europe/Berlin");
//        when(userSettingsProvider.zoneId()).thenReturn(userZoneId);
//
//        final LocalDate firstDateOfWeek = LocalDate.of(2023, 1, 30);
//        when(userDateService.firstDayOfWeek(Year.of(2023), 5)).thenReturn(firstDateOfWeek);
//
//        final ZonedDateTime firstDayOfWeekTimeEntryStart = ZonedDateTime.of(2023, 1, 30, 9, 0, 0, 0, userZoneId);
//        final ZonedDateTime firstDayOfWeekTimeEntryEnd = ZonedDateTime.of(2023, 1, 30, 12, 0, 0, 0, userZoneId);
//        final TimeEntryEntity firstDayOfWeekTimeEntry = new TimeEntryEntity("tenantId", 1L, "batman", "hack the planet!", firstDayOfWeekTimeEntryStart.toInstant(), userZoneId, firstDayOfWeekTimeEntryEnd.toInstant(), userZoneId, Instant.now(), false);
//
//        final ZonedDateTime lastDayOfWeekTimeEntryStart = ZonedDateTime.of(2023, 2, 5, 9, 0, 0, 0, userZoneId);
//        final ZonedDateTime lastDayOfWeekTimeEntryEnd = ZonedDateTime.of(2023, 2, 5, 12, 0, 0, 0, userZoneId);
//        final TimeEntryEntity lastDayOfWeekTimeEntry = new TimeEntryEntity("tenantId", 2L, "batman", "hack the planet, second time!", lastDayOfWeekTimeEntryStart.toInstant(), userZoneId, lastDayOfWeekTimeEntryEnd.toInstant(), userZoneId, Instant.now(), false);
//
//        final ZonedDateTime fromDateTime = firstDateOfWeek.atStartOfDay(userZoneId);
//        final Instant from = Instant.from(fromDateTime);
//        final Instant to = Instant.from(fromDateTime.plusWeeks(1));
//
//        when(timeEntryRepository.findAllByOwnerAndStartGreaterThanEqualAndStartLessThanOrderByStartDesc("batman", from, to))
//            .thenReturn(List.of(lastDayOfWeekTimeEntry, firstDayOfWeekTimeEntry));
//
//        when(timeEntryRepository.countAllByOwner("batman")).thenReturn(6L);
//
//        final UserId userId = new UserId("batman");
//        final UserLocalId userLocalId = new UserLocalId(1337L);
//        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
//
//        final User batman = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
//        when(userManagementService.findUserByLocalId(userLocalId)).thenReturn(Optional.of(batman));
//
//        when(workingTimeCalendarService.getWorkingTimeCalender(firstDateOfWeek, firstDateOfWeek.plusWeeks(1), userLocalId))
//            .thenReturn(new WorkingTimeCalendar(Map.of(
//                firstDateOfWeek, PlannedWorkingHours.EIGHT,
//                LocalDate.of(2023, 1, 31), PlannedWorkingHours.EIGHT,
//                LocalDate.of(2023, 2, 1), PlannedWorkingHours.EIGHT,
//                LocalDate.of(2023, 2, 2), PlannedWorkingHours.EIGHT,
//                LocalDate.of(2023, 2, 3), PlannedWorkingHours.EIGHT,
//                LocalDate.of(2023, 2, 4), PlannedWorkingHours.ZERO,
//                LocalDate.of(2023, 2, 5), PlannedWorkingHours.ZERO
//            ), Map.of()));
//
//        final TimeEntryWeekPage actual = sut.getEntryWeekPage(userLocalId, 2023, 5);
//
//        assertThat(actual).isEqualTo(
//            new TimeEntryWeekPage(
//                new TimeEntryWeek(
//                    firstDateOfWeek,
//                    new PlannedWorkingHours(Duration.ofHours(40)),
//                    List.of(
//                        new TimeEntryDay(
//                            false,
//                            LocalDate.of(2023, 2, 5),
//                            PlannedWorkingHours.ZERO,
//                            ShouldWorkingHours.ZERO,
//                            List.of(
//                                new TimeEntry(new TimeEntryId(2L), userIdComposite, "hack the planet, second time!", lastDayOfWeekTimeEntryStart, lastDayOfWeekTimeEntryEnd, false)
//                            ),
//                            List.of()),
//                        new TimeEntryDay(
//                            false,
//                            LocalDate.of(2023, 2, 4),
//                            PlannedWorkingHours.ZERO,
//                            ShouldWorkingHours.ZERO,
//                            List.of(),
//                            List.of()),
//                        new TimeEntryDay(
//                            false,
//                            LocalDate.of(2023, 2, 3),
//                            PlannedWorkingHours.EIGHT,
//                            ShouldWorkingHours.EIGHT,
//                            List.of(),
//                            List.of()),
//                        new TimeEntryDay(
//                            false,
//                            LocalDate.of(2023, 2, 2),
//                            PlannedWorkingHours.EIGHT,
//                            ShouldWorkingHours.EIGHT,
//                            List.of(),
//                            List.of()),
//                        new TimeEntryDay(
//                            false,
//                            LocalDate.of(2023, 2, 1),
//                            PlannedWorkingHours.EIGHT,
//                            ShouldWorkingHours.EIGHT,
//                            List.of(),
//                            List.of()),
//                        new TimeEntryDay(
//                            false,
//                            LocalDate.of(2023, 1, 31),
//                            PlannedWorkingHours.EIGHT,
//                            ShouldWorkingHours.EIGHT,
//                            List.of(),
//                            List.of()),
//                        new TimeEntryDay(
//                            false,
//                            firstDateOfWeek,
//                            PlannedWorkingHours.EIGHT,
//                            ShouldWorkingHours.EIGHT,
//                            List.of(
//                                new TimeEntry(new TimeEntryId(1L), userIdComposite, "hack the planet!", firstDayOfWeekTimeEntryStart, firstDayOfWeekTimeEntryEnd, false)
//                            ),
//                            List.of())
//                    )
//                ),
//                6
//            )
//        );
//    }
//
//    @Test
//    void ensureGetEntryWeekPageWithTimeEntryActuallyNotInWeekOfYear() {
//
//        final ZoneId userZoneId = ZoneId.of("Europe/Berlin");
//        when(userSettingsProvider.zoneId()).thenReturn(userZoneId);
//
//        final LocalDate firstDateOfWeek = LocalDate.of(2023, 6, 12);
//        final LocalDate toDateExclusive = firstDateOfWeek.plusWeeks(1);
//        when(userDateService.firstDayOfWeek(Year.of(2023), 24)).thenReturn(firstDateOfWeek);
//
//        final Instant from = Instant.from(firstDateOfWeek.atStartOfDay().atZone(userZoneId));
//        final Instant to = Instant.from(toDateExclusive.atStartOfDay().atZone(userZoneId));
//        when(timeEntryRepository.findAllByOwnerAndStartGreaterThanEqualAndStartLessThanOrderByStartDesc("batman", from, to))
//            .thenReturn(List.of());
//
//        when(timeEntryRepository.countAllByOwner("batman")).thenReturn(6L);
//
//        final UserId userId = new UserId("batman");
//        final UserLocalId userLocalId = new UserLocalId(1337L);
//        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
//
//        final User batman = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());
//        when(userManagementService.findUserByLocalId(userLocalId)).thenReturn(Optional.of(batman));
//
//        when(workingTimeCalendarService.getWorkingTimeCalender(firstDateOfWeek, firstDateOfWeek.plusWeeks(1), userLocalId))
//            .thenReturn(new WorkingTimeCalendar(Map.of(
//                LocalDate.of(2023, 6, 12), PlannedWorkingHours.EIGHT,
//                LocalDate.of(2023, 6, 13), PlannedWorkingHours.EIGHT,
//                LocalDate.of(2023, 6, 14), PlannedWorkingHours.EIGHT,
//                LocalDate.of(2023, 6, 15), PlannedWorkingHours.EIGHT,
//                LocalDate.of(2023, 6, 16), PlannedWorkingHours.EIGHT,
//                LocalDate.of(2023, 6, 17), PlannedWorkingHours.ZERO,
//                LocalDate.of(2023, 6, 18), PlannedWorkingHours.ZERO
//            ), Map.of()));
//
//        final TimeEntryWeekPage actual = sut.getEntryWeekPage(userLocalId, 2023, 24);
//
//        assertThat(actual).isEqualTo(
//            new TimeEntryWeekPage(
//                new TimeEntryWeek(
//                    firstDateOfWeek,
//                    new PlannedWorkingHours(Duration.ofHours(40)),
//                    List.of(
//                        new TimeEntryDay(
//                            false,
//                            LocalDate.of(2023, 6, 18),
//                            PlannedWorkingHours.ZERO,
//                            ShouldWorkingHours.ZERO,
//                            List.of(),
//                            List.of()),
//                        new TimeEntryDay(
//                            false,
//                            LocalDate.of(2023, 6, 17),
//                            PlannedWorkingHours.ZERO,
//                            ShouldWorkingHours.ZERO,
//                            List.of(),
//                            List.of()),
//                        new TimeEntryDay(
//                            false,
//                            LocalDate.of(2023, 6, 16),
//                            PlannedWorkingHours.EIGHT,
//                            ShouldWorkingHours.EIGHT,
//                            List.of(),
//                            List.of()),
//                        new TimeEntryDay(
//                            false,
//                            LocalDate.of(2023, 6, 15),
//                            PlannedWorkingHours.EIGHT,
//                            ShouldWorkingHours.EIGHT,
//                            List.of(),
//                            List.of()),
//                        new TimeEntryDay(
//                            false,
//                            LocalDate.of(2023, 6, 14),
//                            PlannedWorkingHours.EIGHT,
//                            ShouldWorkingHours.EIGHT,
//                            List.of(),
//                            List.of()),
//                        new TimeEntryDay(
//                            false,
//                            LocalDate.of(2023, 6, 13),
//                            PlannedWorkingHours.EIGHT,
//                            ShouldWorkingHours.EIGHT,
//                            List.of(),
//                            List.of()),
//                        new TimeEntryDay(
//                            false,
//                            LocalDate.of(2023, 6, 12),
//                            PlannedWorkingHours.EIGHT,
//                            ShouldWorkingHours.EIGHT,
//                            List.of(),
//                            List.of())
//                    )
//                ),
//                6
//            )
//        );
//    }

    private static TimeEntryEntity anyTimeEntryEntity() {

        final Instant start = Instant.parse("2025-03-03T21:00:00.00Z");
        final Instant end = Instant.parse("2025-03-03T21:30:00.00Z");

        final TimeEntryEntity entity = new TimeEntryEntity();

        entity.setId(42L);
        entity.setOwner("");
        entity.setStart(start);
        entity.setStartZoneId("UTC");
        entity.setEnd(end);
        entity.setEndZoneId("UTC");
        entity.setComment("Kickoff");

        return entity;
    }

    private static User anyUser() {
        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        return createUser(userIdComposite, "Bruce", "Wayne");
    }

    private static User createUser(UserIdComposite userIdComposite, String givenName, String familyName) {
        return new User(userIdComposite, givenName, familyName, new EMailAddress(""), Set.of());
    }

    private static EntityRevisionMetadata anyEntityRevisionMetadata() {
        return new EntityRevisionMetadata(1L, EntityRevisionType.CREATED, Instant.now(), Optional.empty());
    }
}
