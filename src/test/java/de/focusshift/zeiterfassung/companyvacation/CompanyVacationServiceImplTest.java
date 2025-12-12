package de.focusshift.zeiterfassung.companyvacation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyVacationServiceImplTest {

    private CompanyVacationServiceImpl sut;

    @Mock
    private CompanyVacationRepository repository;

    @BeforeEach
    void setUp() {
        sut = new CompanyVacationServiceImpl(repository);
    }

    @Test
    void ensureGetCompanyVacations() {

        final Instant rangeFrom = Instant.now();
        final Instant rangeToExclusive = rangeFrom.plus(42, DAYS);

        final Instant start1 = rangeFrom;
        final Instant end1 =rangeFrom.plus(1, DAYS);
        final CompanyVacationEntity entity1 = new CompanyVacationEntity();
        entity1.setId(1L);
        entity1.setDayLength(DayLength.NOON);
        entity1.setStartDate(start1);
        entity1.setEndDate(end1);

        final Instant start2 = rangeFrom.plus(10, DAYS);
        final Instant end2 = rangeFrom.plus(11, DAYS);
        final CompanyVacationEntity entity2 = new CompanyVacationEntity();
        entity2.setId(2L);
        entity2.setDayLength(DayLength.FULL);
        entity2.setStartDate(start2);
        entity2.setEndDate(end2);

        when(repository.findAllByStartDateLessThanAndEndDateGreaterThanEqual(rangeToExclusive, rangeFrom))
            .thenReturn(List.of(entity1, entity2));

        final List<CompanyVacation> actual = sut.getCompanyVacations(rangeFrom, rangeToExclusive);

        assertThat(actual).containsExactlyInAnyOrder(
            new CompanyVacation(start1, end1, DayLength.NOON),
            new CompanyVacation(start2, end2, DayLength.FULL)
        );
    }
}
