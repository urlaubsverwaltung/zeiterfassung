package de.focusshift.zeiterfassung.timeentry.republish;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextRunner;
import de.focusshift.zeiterfassung.timeentry.events.DayLockedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.ZoneId;

import static java.time.Month.JULY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DayLockedRepublishServiceMultiTenantTest {

    private DayLockedRepublishServiceMultiTenant sut;

    @Mock
    private TenantContextRunner tenantContextRunner;
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private TenantContextHolder tenantContextHolder;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @BeforeEach
    void setUp() {
        sut = new DayLockedRepublishServiceMultiTenant(tenantContextRunner, tenantContextHolder, applicationEventPublisher);
    }

    private void runnableExecutesForSingleTenant() {
        // run the given runnable once, simulating a single active tenant
        when(tenantContextRunner.runForEachActiveTenant(any())).thenAnswer(invocation -> (Runnable) invocation.getArgument(0));
    }

    @Test
    void ensurePublishesDayLockedEventForEachDayInInclusiveRange() {

        runnableExecutesForSingleTenant();

        sut.republishDayLockedEvents(LocalDate.of(2026, JULY, 1), LocalDate.of(2026, JULY, 3));

        final ArgumentCaptor<DayLockedEvent> captor = ArgumentCaptor.forClass(DayLockedEvent.class);
        verify(applicationEventPublisher, times(3)).publishEvent(captor.capture());

        assertThat(captor.getAllValues())
            .extracting(DayLockedEvent::date)
            .containsExactly(
                LocalDate.of(2026, JULY, 1),
                LocalDate.of(2026, JULY, 2),
                LocalDate.of(2026, JULY, 3)
            );

        assertThat(captor.getAllValues())
            .extracting(DayLockedEvent::zoneId)
            .containsOnly(ZoneId.of("Europe/Berlin"));
    }

    @Test
    void ensurePublishesSingleEventWhenFromEqualsTo() {

        runnableExecutesForSingleTenant();

        sut.republishDayLockedEvents(LocalDate.of(2026, JULY, 1), LocalDate.of(2026, JULY, 1));

        verify(applicationEventPublisher, times(1)).publishEvent(any(DayLockedEvent.class));
    }

    @Test
    void ensureIgnoresRequestWithNullFrom() {

        sut.republishDayLockedEvents(null, LocalDate.of(2026, JULY, 3));

        verifyNoInteractions(tenantContextRunner, applicationEventPublisher);
    }

    @Test
    void ensureIgnoresRequestWithNullTo() {

        sut.republishDayLockedEvents(LocalDate.of(2026, JULY, 1), null);

        verifyNoInteractions(tenantContextRunner, applicationEventPublisher);
    }

    @Test
    void ensureIgnoresRequestWhenFromIsAfterTo() {

        sut.republishDayLockedEvents(LocalDate.of(2026, JULY, 3), LocalDate.of(2026, JULY, 1));

        verifyNoInteractions(tenantContextRunner, applicationEventPublisher);
    }

    @Test
    void ensurePublishesForGivenTenantOnlyWithoutIteratingActiveTenants() {

        sut.republishDayLockedEvents("tenant-1", LocalDate.of(2026, JULY, 1), LocalDate.of(2026, JULY, 3));

        final ArgumentCaptor<DayLockedEvent> captor = ArgumentCaptor.forClass(DayLockedEvent.class);
        verify(applicationEventPublisher, times(3)).publishEvent(captor.capture());

        assertThat(captor.getAllValues())
            .extracting(DayLockedEvent::date)
            .containsExactly(
                LocalDate.of(2026, JULY, 1),
                LocalDate.of(2026, JULY, 2),
                LocalDate.of(2026, JULY, 3)
            );

        verifyNoInteractions(tenantContextRunner);
    }

    @Test
    void ensureIgnoresSingleTenantRequestWithInvalidTenantId() {

        sut.republishDayLockedEvents(" ", LocalDate.of(2026, JULY, 1), LocalDate.of(2026, JULY, 3));

        verifyNoInteractions(tenantContextRunner, tenantContextHolder, applicationEventPublisher);
    }

    @Test
    void ensureIgnoresSingleTenantRequestWhenFromIsAfterTo() {

        sut.republishDayLockedEvents("tenant-1", LocalDate.of(2026, JULY, 3), LocalDate.of(2026, JULY, 1));

        verifyNoInteractions(tenantContextRunner, tenantContextHolder, applicationEventPublisher);
    }
}
