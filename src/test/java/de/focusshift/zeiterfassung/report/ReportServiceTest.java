package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Year;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReportServiceTest {

    @Test
    void ensureGetReportWeekDelegates() {

        final UserLocalId userLocalId = new UserLocalId(1L);
        final ReportWeek reportWeek = Mockito.mock(ReportWeek.class);

        final ReportServiceAdapter sut = new ReportServiceAdapter() {
            @Override
            public ReportWeek getReportWeek(Year year, int week, List<UserLocalId> userLocalIds) {
                assertThat(userLocalIds).containsExactly(userLocalId);
                return reportWeek;
            }
        };

        final ReportWeek actual = sut.getReportWeek(Year.of(2025), 42, userLocalId);
        assertThat(actual).isSameAs(reportWeek);
    }

    private static class ReportServiceAdapter implements ReportService {

        @Override
        public ReportWeek getReportWeek(Year year, int week, List<UserLocalId> userLocalIds) {
            return null;
        }

        @Override
        public ReportWeek getReportWeekForAllUsers(Year year, int week) {
            return null;
        }

        @Override
        public ReportMonth getReportMonth(YearMonth yearMonth, UserId userId) {
            return null;
        }

        @Override
        public ReportMonth getReportMonth(YearMonth yearMonth, List<UserLocalId> userLocalIds) {
            return null;
        }

        @Override
        public ReportMonth getReportMonthForAllUsers(YearMonth yearMonth) {
            return null;
        }
    }
}
