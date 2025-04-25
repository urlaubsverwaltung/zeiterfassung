package de.focusshift.zeiterfassung.timeclock;

import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.timeentry.TimeEntryLockService;
import de.focusshift.zeiterfassung.user.UserId;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.ui.Model;
import org.springframework.validation.support.BindingAwareModelMap;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeClockControllerAdviceTest {

    @Mock
    private TimeClockService timeClockService;

    @Mock
    private TimeEntryLockService timeEntryLockService;

    static Stream<Arguments> timeClockArguments() {
        return Stream.of(
            Arguments.of(0, 0, 16, "00:00:16"),
            Arguments.of(0, 16, 0, "00:16:00"),
            Arguments.of(16, 0, 0, "16:00:00"),
            Arguments.of(8, 0, 0, "08:00:00"),
            Arguments.of(0, 8, 0, "00:08:00"),
            Arguments.of(0, 0, 8, "00:00:08")
        );
    }

    @ParameterizedTest
    @MethodSource("timeClockArguments")
    void ensureTimeClockModelAttributeElapsedTime(int hours, int minutes, int seconds, String elapsedTimeString) {

        final ZonedDateTime dateTimePivot = ZonedDateTime.now(ZoneId.of("UTC"));

        final TimeClockControllerAdvice sut = createSut();

        final Model model = new BindingAwareModelMap();
        final CurrentOidcUser principal = mock(CurrentOidcUser.class);

        final OidcUserInfo oidcUserInfo = new OidcUserInfo(Map.of("sub", "batman"));
        when(principal.getUserInfo()).thenReturn(oidcUserInfo);

        final ZonedDateTime startedAt = dateTimePivot.minusHours(hours).minusMinutes(minutes).minusSeconds(seconds);
        final TimeClock timeClock = new TimeClock(1L, new UserId("batman"), startedAt, "awesome comment", true, null);
        when(timeClockService.getCurrentTimeClock(new UserId("batman"))).thenReturn(Optional.of(timeClock));

        sut.addAttributes(model, principal);


        assertThat(model.getAttribute("timeClock")).satisfies(object -> {
            assertThat(object).isInstanceOf(TimeClockDto.class);

            final TimeClockDto timeClockDto = (TimeClockDto) object;

            final ZonedDateTime expectedStartedAt = dateTimePivot.minusHours(hours).minusMinutes(minutes).minusSeconds(seconds);
            assertThat(timeClockDto.getStartedAt()).isEqualTo(expectedStartedAt.toInstant());

            assertThat(timeClockDto.getZoneId()).isEqualTo(timeClock.startedAt().getZone());
            assertThat(timeClockDto.getComment()).isEqualTo("awesome comment");
            assertThat(timeClockDto.isBreak()).isTrue();
            assertThat(timeClockDto.getDuration()).isEqualTo(elapsedTimeString);
        });
    }

    private TimeClockControllerAdvice createSut() {
        return new TimeClockControllerAdvice(timeClockService, timeEntryLockService);
    }
}
