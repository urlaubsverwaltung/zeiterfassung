package de.focusshift.zeiterfassung.companyvacation;


import de.focusshift.zeiterfassung.SingleTenantTestContainersBase;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class CompanyVacationRepositoryIT extends SingleTenantTestContainersBase {

    private static final TenantId TENANT_ID = new TenantId("default");

    @Autowired
    private CompanyVacationRepository sut;

    @MockitoBean
    private TenantContextHolder tenantContextHolder;

    /**
     * Tested period is KW 31 2023: 31.07.2023-06.08.2023
     */
    @Test
    void findForGivenWeek() {
        final CompanyVacationEntity oneDayBeforeRequestedWeek = companyVacation("2023-07-29T22:00:00.000Z", "2023-07-29T22:00:00.000Z");
        final CompanyVacationEntity startOutsideEndOnStartOfWeek = companyVacation("2023-07-29T22:00:00.000Z", "2023-07-30T22:00:00.000Z");
        final CompanyVacationEntity startAndEndInside = companyVacation("2023-08-01T22:00:00.000Z", "2023-08-04T22:00:00.000Z");
        final CompanyVacationEntity startBeforeEndAfter = companyVacation("2023-07-29T22:00:00.000Z", "2023-08-06T22:00:00.000Z");
        final CompanyVacationEntity startOnEndAndEndOutside = companyVacation("2023-08-05T22:00:00.000Z", "2023-08-06T22:00:00.000Z");
        final CompanyVacationEntity oneDayAfterRequestedWeek = companyVacation("2023-08-06T22:00:00.000Z", "2023-08-06T22:00:00.000Z");

        when(tenantContextHolder.getCurrentTenantId()).thenReturn(Optional.of(TENANT_ID));

        sut.saveAll(List.of(oneDayBeforeRequestedWeek, startOutsideEndOnStartOfWeek, startAndEndInside, startBeforeEndAfter, startOnEndAndEndOutside, oneDayAfterRequestedWeek));

        final Instant startOfWeek = Instant.parse("2023-07-30T22:00:00.000Z");
        final Instant endOfWeekExclusive = Instant.parse("2023-08-06T22:00:00.000Z");
        final List<CompanyVacationEntity> matchingAbsences = sut.findAllByStartDateLessThanAndEndDateGreaterThanEqual(endOfWeekExclusive, startOfWeek);

        assertThat(matchingAbsences)
            .contains(startOutsideEndOnStartOfWeek, startAndEndInside, startBeforeEndAfter, startOnEndAndEndOutside)
            .doesNotContain(oneDayBeforeRequestedWeek, oneDayAfterRequestedWeek);

        verify(tenantContextHolder, times(6)).getCurrentTenantId();
    }

    private static CompanyVacationEntity companyVacation(String start, String end) {

        final CompanyVacationEntity companyVacation = new CompanyVacationEntity();
        companyVacation.setSourceId(UUID.randomUUID().toString());
        companyVacation.setStartDate(Instant.parse(start));
        companyVacation.setEndDate(Instant.parse(end));
        companyVacation.setDayLength(DayLength.FULL);
        return companyVacation;
    }
}
