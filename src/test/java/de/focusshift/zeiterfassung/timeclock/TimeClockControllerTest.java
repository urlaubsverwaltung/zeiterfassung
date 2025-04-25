package de.focusshift.zeiterfassung.timeclock;

import de.focusshift.zeiterfassung.ControllerTest;
import de.focusshift.zeiterfassung.timeentry.TimeEntryLockService;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.web.MenuProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Import;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.AllOf.allOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
@Import({MenuProperties.class})
class TimeClockControllerTest implements ControllerTest {

    private static final ZoneId ZONE_EUROPE_BERLIN = ZoneId.of("Europe/Berlin");

    private TimeClockController sut;

    @Mock
    private TimeClockService timeClockService;

    @Mock
    private TimeEntryLockService timeEntryLockService;

    @BeforeEach
    void setUp() {
        sut = new TimeClockController(timeClockService, timeEntryLockService);
    }

    @Test
    void ensureStartTimeClock() throws Exception {

        perform(
            post("/timeclock/start")
                .with(oidcSubject("batman"))
                .header("Referer", "referer-url")
        )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("referer-url"));

        verify(timeClockService).getCurrentTimeClock(new UserId("batman"));
        verify(timeClockService).startTimeClock(new UserId("batman"));
        verifyNoMoreInteractions(timeClockService);
    }

    @Test
    void ensureStartTimeClockThrowsWhenClockIsRunningAlready() throws Exception {

        final ZonedDateTime startedAt = ZonedDateTime.of(2023, 1, 11, 13, 37, 0, 0, ZONE_EUROPE_BERLIN);
        final TimeClock timeClock = new TimeClock(1L, new UserId("batman"), startedAt, "awesome comment", false, Optional.empty());
        when(timeClockService.getCurrentTimeClock(new UserId("batman"))).thenReturn(Optional.of(timeClock));

        perform(
            post("/timeclock/start")
                .with(oidcSubject("batman"))
        )
            .andExpect(status().isConflict());

        verify(timeClockService).getCurrentTimeClock(new UserId("batman"));
        verifyNoMoreInteractions(timeClockService);
    }

    @Test
    void ensureStopTimeClock() throws Exception {

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        perform(
            post("/timeclock/stop")
                .with(oidcSubject(userIdComposite))
                .header("Referer", "referer-url")
        )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("referer-url"));

        verify(timeClockService).stopTimeClock(userIdComposite);
        verifyNoMoreInteractions(timeClockService);
    }

    @Test
    void ensureEditTimeClockGetMappingWithoutRunningTimeClock() throws Exception {

        when(timeClockService.getCurrentTimeClock(new UserId("batman"))).thenReturn(Optional.empty());

        perform(
            get("/timeclock")
                .with(oidcSubject("batman"))
        )
            .andExpect(status().isOk())
            .andExpect(model().attributeDoesNotExist("timeClockUpdate"))
            .andExpect(view().name("timeclock/timeclock-edit"));
    }

    @Test
    void ensureEditTimeClockGetMappingWithRunningTimeClock() throws Exception {

        final ZonedDateTime startedAt = ZonedDateTime.of(2023, 1, 11, 13, 37, 0, 0, ZONE_EUROPE_BERLIN);
        final TimeClock timeClock = new TimeClock(1L, new UserId("batman"), startedAt, "awesome comment", true, Optional.empty());
        when(timeClockService.getCurrentTimeClock(new UserId("batman"))).thenReturn(Optional.of(timeClock));

        perform(
            get("/timeclock")
                .with(oidcSubject("batman"))
        )
            .andExpect(status().isOk())
            .andExpect(model().attribute("timeClockUpdate",
                allOf(
                    hasProperty("startedAt", is(Instant.from(startedAt))),
                    hasProperty("zoneId", is(ZONE_EUROPE_BERLIN)),
                    hasProperty("comment", is("awesome comment")),
                    hasProperty("break", is(true)),
                    hasProperty("date", is(LocalDate.parse("2023-01-11"))),
                    hasProperty("time", is(LocalTime.parse("13:37")))
                )
            ))
            .andExpect(view().name("timeclock/timeclock-edit"));
    }

    @Test
    void ensureEditTimeClock() throws Exception {
        perform(
            post("/timeclock")
                .with(oidcSubject("batman"))
                .header("Referer", "referer-url")
                .param("zoneId", "Europe/Berlin")
                .param("comment", "awesome comment")
                .param("break", "on")
                .param("date", "2023-01-11")
                .param("time", "13:37")
        )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("referer-url"));

        final UserId expectedUserId = new UserId("batman");
        final ZonedDateTime expectedZonedDateTime = ZonedDateTime.of(2023, 1, 11, 13, 37, 0, 0, ZONE_EUROPE_BERLIN);
        verify(timeClockService).updateTimeClock(expectedUserId, new TimeClockUpdate(expectedUserId, expectedZonedDateTime, "awesome comment", true));
    }

    @Test
    void ensureEditTimeClockDisablingBreak() throws Exception {
        perform(
            post("/timeclock")
                .with(oidcSubject("batman"))
                .header("Referer", "referer-url")
                .param("zoneId", "Europe/Berlin")
                .param("comment", "awesome comment")
                .param("date", "2023-01-11")
                .param("time", "13:37")
            // break: NOP->no param, YEP->param(break,on)
        )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("referer-url"));

        final UserId expectedUserId = new UserId("batman");
        final ZonedDateTime expectedZonedDateTime = ZonedDateTime.of(2023, 1, 11, 13, 37, 0, 0, ZONE_EUROPE_BERLIN);
        verify(timeClockService).updateTimeClock(expectedUserId, new TimeClockUpdate(expectedUserId, expectedZonedDateTime, "awesome comment", false));
    }

    @Test
    void ensureEditTimeClockThrowsWhenThereIsNoClockRunning() throws Exception {

        when(timeClockService.updateTimeClock(eq(new UserId("batman")), any(TimeClockUpdate.class)))
            .thenThrow(new TimeClockNotStartedException(new UserId("batman")));

        perform(
            post("/timeclock")
                .with(oidcSubject("batman"))
                .header("Referer", "referer-url")
                .param("zoneId", "Europe/Berlin")
                .param("comment", "awesome comment")
                .param("date", "2023-01-11")
                .param("time", "13:37")
        )
            .andExpect(status().isPreconditionRequired());
    }

    @Test
    void ensureEditTimeClockValidationForCommentWithJavaScript() throws Exception {
        perform(
            post("/timeclock")
                .with(oidcSubject("batman"))
                .header("Referer", "referer-url")
                .header("Turbo-Frame", "any-value")
                // must be defined
                .param("zoneId", "Europe/Berlin")
                // max 255 chars allowed for comment
                .param("comment", IntStream.range(0, 256).mapToObj((nr) -> "0").collect(joining("")))
                // actual pattern "yyyy-MM-dd"
                .param("date", "2023-11-01")
                // actual pattern "HH:mm"
                .param("time", "13:37:00")
        )
            .andExpect(status().isUnprocessableEntity())
            .andExpect(model().attributeHasFieldErrors("timeClockUpdate", "comment"))
            .andExpect(view().name("timeclock/timeclock-edit-form::navigation-box-update"));
    }

    @Test
    void ensureEditTimeClockValidationForComment() throws Exception {
        perform(
            post("/timeclock")
                .with(oidcSubject("batman"))
                .header("Referer", "referer-url")
                // must be defined
                .param("zoneId", "Europe/Berlin")
                // max 255 chars allowed for comment
                .param("comment", IntStream.range(0, 256).mapToObj((nr) -> "0").collect(joining("")))
                // actual pattern "yyyy-MM-dd"
                .param("date", "2023-11-01")
                // actual pattern "HH:mm"
                .param("time", "13:37:00")
        )
            .andExpect(status().isOk())
            .andExpect(model().attributeHasFieldErrors("timeClockUpdate", "comment"))
            .andExpect(view().name("timeclock/timeclock-edit"));
    }

    @Test
    void ensureValidationErrorForLockedDateInEditTimeClockAndUserIsNotAllowedToBypassLock() throws Exception {
        when(timeEntryLockService.isLocked(LocalDate.of(2023, 1, 11))).thenReturn(true);
        when(timeEntryLockService.isUserAllowedToBypassLock(any())).thenReturn(false);

        perform(
            post("/timeclock")
                .with(oidcSubject("batman"))
                .header("Referer", "referer-url")
                .param("zoneId", "Europe/Berlin")
                .param("comment", "awesome comment")
                .param("break", "on")
                .param("date", "2023-01-11")
                .param("time", "13:37")
        )
            .andExpect(status().isOk());

        verifyNoInteractions(timeClockService);
    }

    @Test
    void ensureNoValidationErrorForLockedDateInEditTimeClockAndUserIsAllowedToBypassLock() throws Exception {
        when(timeEntryLockService.isLocked(LocalDate.of(2023, 1, 11))).thenReturn(true);
        when(timeEntryLockService.isUserAllowedToBypassLock(any())).thenReturn(true);

        perform(
            post("/timeclock")
                .with(oidcSubject("batman"))
                .header("Referer", "referer-url")
                .param("zoneId", "Europe/Berlin")
                .param("comment", "awesome comment")
                .param("break", "on")
                .param("date", "2023-01-11")
                .param("time", "13:37")
        )
            .andExpect(status().is3xxRedirection());

        final UserId expectedUserId = new UserId("batman");
        verify(timeClockService).updateTimeClock(eq(expectedUserId), any(TimeClockUpdate.class));
    }

    private ResultActions perform(MockHttpServletRequestBuilder builder) throws Exception {
        return standaloneSetup(sut)
            .addFilters(new SecurityContextPersistenceFilter())
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build()
            .perform(builder);
    }
}
