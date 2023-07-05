package de.focusshift.zeiterfassung.absence;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbsenceServiceImplTest {

    private AbsenceServiceImpl sut;

    @Mock
    private AbsenceRepository repository;
    @Mock
    private UserSettingsProvider userSettingsProvider;
    @Mock
    private TenantContextHolder tenantContextHolder;

    @BeforeEach
    void setUp() {
        sut = new AbsenceServiceImpl(repository, userSettingsProvider, tenantContextHolder);
    }

    @Test
    void ensureFindAllAbsences() {

        final ZonedDateTime today = LocalDate.now().atStartOfDay(UTC);
        final Instant startDate = today.toInstant();
        final Instant endDateExclusive = today.plusWeeks(1).toInstant();

        final ZoneId berlin = ZoneId.of("Europe/Berlin");
        when(userSettingsProvider.zoneId()).thenReturn(berlin);
        when(tenantContextHolder.getCurrentTenantId()).thenReturn(Optional.of(new TenantId("tenant")));

        final AbsenceWriteEntity entity_1 = new AbsenceWriteEntity();
        entity_1.setId(1L);
        entity_1.setUserId("user");
        entity_1.setStartDate(today.plusDays(1).toInstant());
        entity_1.setEndDate(today.plusDays(2).toInstant());
        entity_1.setDayLength(DayLength.FULL);
        entity_1.setType(AbsenceType.HOLIDAY);
        entity_1.setColor(AbsenceColor.PINK);

        final AbsenceWriteEntity entity_2 = new AbsenceWriteEntity();
        entity_2.setId(2L);
        entity_2.setUserId("user");
        entity_2.setStartDate(today.plusDays(4).toInstant());
        entity_2.setEndDate(today.plusDays(4).toInstant());
        entity_2.setDayLength(DayLength.MORNING);
        entity_2.setType(AbsenceType.SPECIALLEAVE);
        entity_2.setColor(AbsenceColor.VIOLET);

        when(repository.findAllByTenantIdAndUserIdAndStartDateGreaterThanEqualAndEndDateLessThan("tenant", "user", startDate, endDateExclusive))
            .thenReturn(List.of(entity_1, entity_2));

        final Absence expectedAbsence_1 = new Absence(
            new UserId("user"),
            today.plusDays(1).withZoneSameInstant(berlin),
            today.plusDays(2).withZoneSameInstant(berlin),
            DayLength.FULL,
            AbsenceType.HOLIDAY,
            AbsenceColor.PINK
        );

        final Absence expectedAbsence_2 = new Absence(
            new UserId("user"),
            today.plusDays(4).withZoneSameInstant(berlin),
            today.plusDays(4).withZoneSameInstant(berlin),
            DayLength.MORNING,
            AbsenceType.SPECIALLEAVE,
            AbsenceColor.VIOLET
        );

        final Map<LocalDate, List<Absence>> actual = sut.findAllAbsences(new UserId("user"), startDate, endDateExclusive);
        assertThat(actual).hasSize(7);
        assertThat(actual).containsEntry(today.toLocalDate(), List.of());
        assertThat(actual).containsEntry(today.plusDays(1).toLocalDate(), List.of(expectedAbsence_1));
        assertThat(actual).containsEntry(today.plusDays(2).toLocalDate(), List.of(expectedAbsence_1));
        assertThat(actual).containsEntry(today.plusDays(3).toLocalDate(), List.of());
        assertThat(actual).containsEntry(today.plusDays(4).toLocalDate(), List.of(expectedAbsence_2));
        assertThat(actual).containsEntry(today.plusDays(5).toLocalDate(), List.of());
        assertThat(actual).containsEntry(today.plusDays(6).toLocalDate(), List.of());
    }
}
