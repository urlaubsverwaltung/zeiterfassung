package de.focusshift.zeiterfassung.absence;


import de.focusshift.zeiterfassung.TestContainersBase;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class AbsenceRepositoryIT extends TestContainersBase {

    private static final TenantId TENANT_ID = new TenantId("default");
    private static final String USER = "user-id";

    @Autowired
    private AbsenceRepository sut;

    @MockBean
    private TenantContextHolder tenantContextHolder;

    /**
     * Tested period is KW 31 2023: 31.07.2023-06.08.2023
     */
    @Test
    void findForGivenWeek() {
        long sourceId = 21L;

        final AbsenceWriteEntity oneDayBeforeRequestedWeek = absence(sourceId, "2023-07-29T22:00:00.000Z", "2023-07-29T22:00:00.000Z");
        final AbsenceWriteEntity startOutsideEndOnStartOfWeek = absence(++sourceId, "2023-07-29T22:00:00.000Z", "2023-07-30T22:00:00.000Z");
        final AbsenceWriteEntity startAndEndInside = absence(++sourceId, "2023-08-01T22:00:00.000Z", "2023-08-04T22:00:00.000Z");
        final AbsenceWriteEntity startBeforeEndAfter = absence(++sourceId, "2023-07-29T22:00:00.000Z", "2023-08-06T22:00:00.000Z");
        final AbsenceWriteEntity startOnEndAndEndOutside = absence(++sourceId, "2023-08-05T22:00:00.000Z", "2023-08-06T22:00:00.000Z");
        final AbsenceWriteEntity oneDayAfterRequestedWeek = absence(++sourceId, "2023-08-06T22:00:00.000Z", "2023-08-06T22:00:00.000Z");

        when(tenantContextHolder.getCurrentTenantId()).thenReturn(Optional.of(TENANT_ID));

        sut.saveAll(List.of(oneDayBeforeRequestedWeek, startOutsideEndOnStartOfWeek, startAndEndInside, startBeforeEndAfter, startOnEndAndEndOutside, oneDayAfterRequestedWeek));

        final Instant startOfWeek = Instant.parse("2023-07-30T22:00:00.000Z");
        final Instant endOfWeekExclusive = Instant.parse("2023-08-06T22:00:00.000Z");
        final List<AbsenceWriteEntity> matchingAbsences = sut.findAllByUserIdInAndStartDateLessThanAndEndDateGreaterThanEqual(List.of(USER), endOfWeekExclusive, startOfWeek);

        assertThat(matchingAbsences)
                .contains(startOutsideEndOnStartOfWeek, startAndEndInside, startBeforeEndAfter, startOnEndAndEndOutside)
                .doesNotContain(oneDayBeforeRequestedWeek, oneDayAfterRequestedWeek);

        verify(tenantContextHolder, times(6)).getCurrentTenantId();
    }

    private static AbsenceWriteEntity absence(long sourceId, String start, String end) {

        final AbsenceWriteEntity oneDayBeforeRequestedWeek = new AbsenceWriteEntity();
        oneDayBeforeRequestedWeek.setSourceId(sourceId);
        oneDayBeforeRequestedWeek.setUserId(USER);
        oneDayBeforeRequestedWeek.setStartDate(Instant.parse(start));
        oneDayBeforeRequestedWeek.setEndDate(Instant.parse(end));
        oneDayBeforeRequestedWeek.setDayLength(DayLength.FULL);
        oneDayBeforeRequestedWeek.setType(new AbsenceTypeEntityEmbeddable(AbsenceTypeCategory.HOLIDAY, 1000L));
        return oneDayBeforeRequestedWeek;
    }
}
