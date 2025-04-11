package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.timeentry.TimeEntryLockService;
import de.focusshift.zeiterfassung.user.UserDateService;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServicePermissionAwareTest {

    private ReportServicePermissionAware sut;

    @Mock
    private ReportPermissionService reportPermissionService;
    @Mock
    private ReportServiceRaw reportServiceRaw;
    @Mock
    private UserDateService userDateService;
    @Mock
    private TimeEntryLockService timeEntryLockService;

    @BeforeEach
    void setUp() {
        sut = new ReportServicePermissionAware(reportPermissionService, reportServiceRaw, userDateService, timeEntryLockService);
    }

    @Test
    void getReportWeekForMultipleUsersWhenCurrentUserHasNoPermissionForAnyGivenOne() {

        final UserLocalId localId_1 = new UserLocalId(1L);
        final UserLocalId localId_2 = new UserLocalId(2L);
        final List<UserLocalId> userIds = List.of(localId_1, localId_2);

        when(reportPermissionService.filterUserLocalIdsByCurrentUserHasPermissionFor(userIds)).thenReturn(List.of());

        when(userDateService.firstDayOfWeek(Year.of(2023), 7))
            .thenReturn(LocalDate.of(2023, 2, 13));

        final ReportWeek actual = sut.getReportWeek(Year.of(2023), 7, userIds);

        assertThat(actual).isNotNull();
        assertThat(actual.firstDateOfWeek()).isEqualTo(LocalDate.of(2023, 2, 13));
        assertThat(actual.reportDays()).containsExactly(
            new ReportDay(LocalDate.of(2023, 2, 13), false, Map.of(), Map.of(), Map.of()),
            new ReportDay(LocalDate.of(2023, 2, 14), false, Map.of(), Map.of(), Map.of()),
            new ReportDay(LocalDate.of(2023, 2, 15), false, Map.of(), Map.of(), Map.of()),
            new ReportDay(LocalDate.of(2023, 2, 16), false, Map.of(), Map.of(), Map.of()),
            new ReportDay(LocalDate.of(2023, 2, 17), false, Map.of(), Map.of(), Map.of()),
            new ReportDay(LocalDate.of(2023, 2, 18), false, Map.of(), Map.of(), Map.of()),
            new ReportDay(LocalDate.of(2023, 2, 19), false, Map.of(), Map.of(), Map.of())
        );
    }

    @Test
    void getReportMonthForMultipleUsersWhenCurrentUserHasNoPermissionForAnyGivenOne() {

        final UserLocalId localId_1 = new UserLocalId(1L);
        final UserLocalId localId_2 = new UserLocalId(2L);
        final List<UserLocalId> userIds = List.of(localId_1, localId_2);

        when(reportPermissionService.filterUserLocalIdsByCurrentUserHasPermissionFor(userIds))
            .thenReturn(List.of());

        when(userDateService.localDateToFirstDateOfWeek(LocalDate.of(2023, 2, 1)))
            .thenReturn(LocalDate.of(2023, 1, 30));

        final ReportMonth actual = sut.getReportMonth(YearMonth.of(2023, 2), userIds);

        assertThat(actual).isNotNull();
        assertThat(actual.yearMonth()).isEqualTo(YearMonth.of(2023, 2));
        assertThat(actual.weeks()).hasSize(5);

        assertThat(actual.weeks().get(0)).satisfies(week -> {
            assertThat(week.firstDateOfWeek()).isEqualTo(LocalDate.of(2023, 1, 30));
            assertThat(week.reportDays()).containsExactly(
                new ReportDay(LocalDate.of(2023, 1, 30), false, Map.of(), Map.of(), Map.of()),
                new ReportDay(LocalDate.of(2023, 1, 31), false, Map.of(), Map.of(), Map.of()),
                new ReportDay(LocalDate.of(2023, 2, 1), false, Map.of(), Map.of(), Map.of()),
                new ReportDay(LocalDate.of(2023, 2, 2), false, Map.of(), Map.of(), Map.of()),
                new ReportDay(LocalDate.of(2023, 2, 3), false, Map.of(), Map.of(), Map.of()),
                new ReportDay(LocalDate.of(2023, 2, 4), false, Map.of(), Map.of(), Map.of()),
                new ReportDay(LocalDate.of(2023, 2, 5), false, Map.of(), Map.of(), Map.of())
            );
        });

        assertThat(actual.weeks().get(1)).satisfies(week -> {
            assertThat(week.firstDateOfWeek()).isEqualTo(LocalDate.of(2023, 2, 6));
            assertThat(week.reportDays()).containsExactly(
                new ReportDay(LocalDate.of(2023, 2, 6), false, Map.of(), Map.of(), Map.of()),
                new ReportDay(LocalDate.of(2023, 2, 7), false, Map.of(), Map.of(), Map.of()),
                new ReportDay(LocalDate.of(2023, 2, 8), false, Map.of(), Map.of(), Map.of()),
                new ReportDay(LocalDate.of(2023, 2, 9), false, Map.of(), Map.of(), Map.of()),
                new ReportDay(LocalDate.of(2023, 2, 10), false, Map.of(), Map.of(), Map.of()),
                new ReportDay(LocalDate.of(2023, 2, 11), false, Map.of(), Map.of(), Map.of()),
                new ReportDay(LocalDate.of(2023, 2, 12), false, Map.of(), Map.of(), Map.of())
            );
        });

        assertThat(actual.weeks().get(2)).satisfies(week -> {
            assertThat(week.firstDateOfWeek()).isEqualTo(LocalDate.of(2023, 2, 13));
            assertThat(week.reportDays()).containsExactly(
                new ReportDay(LocalDate.of(2023, 2, 13), false, Map.of(), Map.of(), Map.of()),
                new ReportDay(LocalDate.of(2023, 2, 14), false, Map.of(), Map.of(), Map.of()),
                new ReportDay(LocalDate.of(2023, 2, 15), false, Map.of(), Map.of(), Map.of()),
                new ReportDay(LocalDate.of(2023, 2, 16), false, Map.of(), Map.of(), Map.of()),
                new ReportDay(LocalDate.of(2023, 2, 17), false, Map.of(), Map.of(), Map.of()),
                new ReportDay(LocalDate.of(2023, 2, 18), false, Map.of(), Map.of(), Map.of()),
                new ReportDay(LocalDate.of(2023, 2, 19), false, Map.of(), Map.of(), Map.of())
            );
        });

        assertThat(actual.weeks().get(3)).satisfies(week -> {
            assertThat(week.firstDateOfWeek()).isEqualTo(LocalDate.of(2023, 2, 20));
            assertThat(week.reportDays()).containsExactly(
                new ReportDay(LocalDate.of(2023, 2, 20), false, Map.of(), Map.of(), Map.of()),
                new ReportDay(LocalDate.of(2023, 2, 21), false, Map.of(), Map.of(), Map.of()),
                new ReportDay(LocalDate.of(2023, 2, 22), false, Map.of(), Map.of(), Map.of()),
                new ReportDay(LocalDate.of(2023, 2, 23), false, Map.of(), Map.of(), Map.of()),
                new ReportDay(LocalDate.of(2023, 2, 24), false, Map.of(), Map.of(), Map.of()),
                new ReportDay(LocalDate.of(2023, 2, 25), false, Map.of(), Map.of(), Map.of()),
                new ReportDay(LocalDate.of(2023, 2, 26), false, Map.of(), Map.of(), Map.of())
            );
        });

        assertThat(actual.weeks().get(4)).satisfies(week -> {
            assertThat(week.firstDateOfWeek()).isEqualTo(LocalDate.of(2023, 2, 27));
            assertThat(week.reportDays()).containsExactly(
                new ReportDay(LocalDate.of(2023, 2, 27), false, Map.of(), Map.of(), Map.of()),
                new ReportDay(LocalDate.of(2023, 2, 28), false, Map.of(), Map.of(), Map.of()),
                new ReportDay(LocalDate.of(2023, 3, 1), false, Map.of(), Map.of(), Map.of()),
                new ReportDay(LocalDate.of(2023, 3, 2), false, Map.of(), Map.of(), Map.of()),
                new ReportDay(LocalDate.of(2023, 3, 3), false, Map.of(), Map.of(), Map.of()),
                new ReportDay(LocalDate.of(2023, 3, 4), false, Map.of(), Map.of(), Map.of()),
                new ReportDay(LocalDate.of(2023, 3, 5), false, Map.of(), Map.of(), Map.of())
            );
        });
    }
}
