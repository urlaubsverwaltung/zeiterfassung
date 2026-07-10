package de.focusshift.zeiterfassung.timeentry.republish;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static java.time.Month.JULY;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class DayLockedRepublishEventHandlerRabbitmqTest {

    private DayLockedRepublishEventHandlerRabbitmq sut;

    @Mock
    private DayLockedRepublishService dayLockedRepublishService;

    @BeforeEach
    void setUp() {
        sut = new DayLockedRepublishEventHandlerRabbitmq(dayLockedRepublishService);
    }

    @Test
    void ensureDelegatesRequestWithoutTenantIdToAllActiveTenants() {

        final LocalDate from = LocalDate.of(2026, JULY, 1);
        final LocalDate to = LocalDate.of(2026, JULY, 3);

        sut.on(new DayLockedRepublishRequest(null, from, to));

        verify(dayLockedRepublishService).republishDayLockedEvents(from, to);
        verifyNoMoreInteractions(dayLockedRepublishService);
    }

    @Test
    void ensureDelegatesRequestWithTenantIdToGivenTenant() {

        final LocalDate from = LocalDate.of(2026, JULY, 1);
        final LocalDate to = LocalDate.of(2026, JULY, 3);

        sut.on(new DayLockedRepublishRequest("tenant-1", from, to));

        verify(dayLockedRepublishService).republishDayLockedEvents("tenant-1", from, to);
        verifyNoMoreInteractions(dayLockedRepublishService);
    }

    @Test
    void ensureDelegatesRequestWithBlankTenantIdToAllActiveTenants() {

        final LocalDate from = LocalDate.of(2026, JULY, 1);
        final LocalDate to = LocalDate.of(2026, JULY, 3);

        sut.on(new DayLockedRepublishRequest(" ", from, to));

        verify(dayLockedRepublishService).republishDayLockedEvents(from, to);
        verifyNoMoreInteractions(dayLockedRepublishService);
    }
}
