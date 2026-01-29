package de.focusshift.zeiterfassung.companyvacation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static de.focusshift.zeiterfassung.companyvacation.DayLength.FULL;
import static de.focusshift.zeiterfassung.companyvacation.DayLength.MORNING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyVacationWriteServiceImplTest {

    private CompanyVacationWriteServiceImpl sut;

    @Captor
    private ArgumentCaptor<CompanyVacationEntity> argumentCaptor;

    @Mock
    private CompanyVacationRepository companyVacationRepository;


    @BeforeEach
    void setUp() {
        sut = new CompanyVacationWriteServiceImpl(companyVacationRepository);
    }

    @Nested
    class AddOrUpdateCompanyVacation {

        @Test
        void ensureAddOrUpdateCompanyVacationAddsNewEntity() {

            final Instant startDate = Instant.parse("2018-10-17T00:00:00.00Z");
            final Instant endDate = Instant.parse("2018-10-17T00:00:00.00Z");
            final CompanyVacationWrite companyVacationWrite = new CompanyVacationWrite("sourceId", startDate, endDate, FULL);

            when(companyVacationRepository.findBySourceId("sourceId")).thenReturn(Optional.empty());

            sut.addOrUpdateCompanyVacation(companyVacationWrite);

            verify(companyVacationRepository).save(argumentCaptor.capture());

            assertThat(argumentCaptor.getValue()).satisfies(actual -> {
                assertThat(actual.getId()).isNull();
                assertThat(actual.getSourceId()).isEqualTo("sourceId");
                assertThat(actual.getStartDate()).isEqualTo(startDate);
                assertThat(actual.getEndDate()).isEqualTo(endDate);
                assertThat(actual.getDayLength()).isEqualTo(FULL);
            });
        }

        @Test
        void ensureAddOrUpdateCompanyVacationUpdatesExistingEntity() {

            final Instant startDate = Instant.parse("2018-10-17T00:00:00.00Z");
            final Instant endDate = Instant.parse("2018-10-17T00:00:00.00Z");
            final CompanyVacationWrite companyVacationWrite = new CompanyVacationWrite("sourceId", startDate, endDate, FULL);

            final CompanyVacationEntity companyVacationEntity = new CompanyVacationEntity();
            companyVacationEntity.setId(1L);
            companyVacationEntity.setStartDate(startDate.minus(1L, ChronoUnit.DAYS));
            companyVacationEntity.setEndDate(endDate.minus(1L, ChronoUnit.DAYS));
            companyVacationEntity.setDayLength(MORNING);
            companyVacationEntity.setSourceId("sourceId");
            when(companyVacationRepository.findBySourceId("sourceId")).thenReturn(Optional.of(companyVacationEntity));

            sut.addOrUpdateCompanyVacation(companyVacationWrite);

            verify(companyVacationRepository).save(argumentCaptor.capture());

            assertThat(argumentCaptor.getValue()).satisfies(actual -> {
                assertThat(actual.getId()).isEqualTo(1L);
                assertThat(actual.getSourceId()).isEqualTo("sourceId");
                assertThat(actual.getStartDate()).isEqualTo(startDate);
                assertThat(actual.getEndDate()).isEqualTo(endDate);
                assertThat(actual.getDayLength()).isEqualTo(FULL);
            });
        }
    }

    @Nested
    class DeleteCompanyVacation {

        @Test
        void ensureToDeleteCompanyVacationBySourceIdAndCorrectYear() {
            final Instant createdAt = Instant.parse("2025-07-29T22:00:00.000Z");
            final String sourceId = "sourceId";
            sut.deleteCompanyVacation(createdAt, sourceId);
            verify(companyVacationRepository).deleteBySourceIdAndStartAndEndInSameYearAsCreatedAt(sourceId, createdAt);
        }
    }
}
