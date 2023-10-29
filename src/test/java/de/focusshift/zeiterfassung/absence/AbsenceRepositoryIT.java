package de.focusshift.zeiterfassung.absence;


import de.focusshift.zeiterfassung.TestContainersBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AbsenceRepositoryIT extends TestContainersBase {


    private static final String TENANT = "tenant";
    private static final String USER = "user-id";

    @Autowired
    AbsenceRepository sut;

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

        sut.saveAll(List.of(oneDayBeforeRequestedWeek, startOutsideEndOnStartOfWeek, startAndEndInside, startBeforeEndAfter, startOnEndAndEndOutside, oneDayAfterRequestedWeek));

        final Instant startOfWeek = Instant.parse("2023-07-30T22:00:00.000Z");
        final Instant endOfWeekExclusive = Instant.parse("2023-08-06T22:00:00.000Z");
        final List<AbsenceWriteEntity> matchingAbsences = sut.findAllByTenantIdAndUserIdInAndStartDateLessThanAndEndDateGreaterThanEqual(TENANT, List.of(USER), endOfWeekExclusive, startOfWeek);

        assertThat(matchingAbsences)
                .contains(startOutsideEndOnStartOfWeek, startAndEndInside, startBeforeEndAfter, startOnEndAndEndOutside)
                .doesNotContain(oneDayBeforeRequestedWeek, oneDayAfterRequestedWeek);
    }

    private static AbsenceWriteEntity absence(long sourceId, String start, String end) {

        final AbsenceWriteEntity oneDayBeforeRequestedWeek = new AbsenceWriteEntity();
        oneDayBeforeRequestedWeek.setTenantId(TENANT);
        oneDayBeforeRequestedWeek.setSourceId(sourceId);
        oneDayBeforeRequestedWeek.setUserId(USER);
        oneDayBeforeRequestedWeek.setStartDate(Instant.parse(start));
        oneDayBeforeRequestedWeek.setEndDate(Instant.parse(end));
        oneDayBeforeRequestedWeek.setDayLength(DayLength.FULL);
        oneDayBeforeRequestedWeek.setType(new AbsenceTypeEntityEmbeddable(AbsenceTypeCategory.HOLIDAY, 1000L));
        oneDayBeforeRequestedWeek.setColor(AbsenceColor.PINK);
        return oneDayBeforeRequestedWeek;
    }
}
