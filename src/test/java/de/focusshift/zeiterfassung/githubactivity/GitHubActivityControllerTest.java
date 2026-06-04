package de.focusshift.zeiterfassung.githubactivity;

import de.focusshift.zeiterfassung.ControllerTest;
import de.focusshift.zeiterfassung.activitytype.ActivityTypeService;
import de.focusshift.zeiterfassung.project.ProjectService;
import de.focusshift.zeiterfassung.search.UserSearchViewHelper;
import de.focusshift.zeiterfassung.timeentry.TimeEntryLockService;
import de.focusshift.zeiterfassung.timeentry.TimeEntryService;
import de.focusshift.zeiterfassung.user.UserSettings;
import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import de.focusshift.zeiterfassung.user.UserSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class GitHubActivityControllerTest implements ControllerTest {

    @Mock private UserSettingsService userSettingsService;
    @Mock private UserSettingsProvider userSettingsProvider;
    @Mock private TimeEntryService timeEntryService;
    @Mock private UserSearchViewHelper userSearchViewHelper;
    @Mock private ProjectService projectService;
    @Mock private ActivityTypeService activityTypeService;
    @Mock private TimeEntryLockService timeEntryLockService;
    @Mock private GitHubRawEventRepository eventRepository;
    @Mock private GitHubSyncService syncService;

    private GitHubActivityController sut;

    @BeforeEach
    void setUp() {
        sut = new GitHubActivityController(
            userSettingsService, userSettingsProvider, timeEntryService,
            userSearchViewHelper, projectService, activityTypeService,
            timeEntryLockService, eventRepository, syncService
        );
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private UserSettings userSettingsWith(String githubLogin) {
        final UserSettings settings = mock(UserSettings.class);
        when(settings.githubLoginVerified()).thenReturn(true);
        when(settings.githubLogin()).thenReturn(java.util.Optional.ofNullable(githubLogin));
        return settings;
    }

    private void stubCommonDependencies(String githubLogin) {
        // Create mock first, THEN pass to thenReturn — avoids UnfinishedStubbingException
        // caused by calling when() inside a thenReturn() argument evaluation
        final UserSettings settings = userSettingsWith(githubLogin);
        when(userSettingsService.getUserSettings(any())).thenReturn(settings);
        when(userSettingsProvider.zoneId()).thenReturn(ZoneOffset.UTC);
        when(timeEntryLockService.isLocked(any(LocalDate.class))).thenReturn(false);
        when(syncService.isConfigured()).thenReturn(true);
        when(syncService.getLastSyncTime(anyString())).thenReturn(null);
        when(syncService.isRateLimitSafe()).thenReturn(true);
        when(syncService.getRateLimitReset()).thenReturn(java.time.Instant.MIN);
        when(eventRepository.findDistinctRepoAndHeadBranchesByUsernameUpToDate(anyString(), any())).thenReturn(Set.of());
    }

    private GitHubRawEventEntity entity(String eventId, String type, String repo,
                                        String anchorType, String anchorId, String anchorTitle,
                                        String icon, String summary, Instant timestamp) {
        final GitHubRawEventEntity e = new GitHubRawEventEntity();
        e.setGithubEventId(eventId);
        e.setGithubUsername("tronical");
        e.setEventType(type);
        e.setRepoName(repo);
        e.setAnchorType(anchorType);
        e.setAnchorId(anchorId);
        e.setAnchorTitle(anchorTitle);
        e.setEventIcon(icon);
        e.setEventSummary(summary);
        e.setEventTimestamp(timestamp);
        return e;
    }

    private GitHubRawEventEntity prEntity(String eventId, String repo, String prNumber,
                                          String title, String summary) {
        return entity(eventId, "PullRequestEvent", repo, "PR", prNumber, title,
            "🔀", summary, Instant.parse("2026-06-03T14:00:00Z"));
    }

    private GitHubRawEventEntity reviewEntity(String eventId, String repo, String prNumber,
                                              String title, String summary) {
        return entity(eventId, "PullRequestReviewEvent", repo, "PR", prNumber, title,
            "👁", summary, Instant.parse("2026-06-03T15:00:00Z"));
    }

    private GitHubRawEventEntity issueEntity(String eventId, String repo, String issueNumber,
                                             String title, String summary) {
        return entity(eventId, "IssuesEvent", repo, "ISSUE", issueNumber, title,
            "🐛", summary, Instant.parse("2026-06-03T16:00:00Z"));
    }

    private GitHubRawEventEntity commitEntity(String sha, String repo, String branch, String message) {
        return entity("tronical_commit_" + sha, "PushEvent", repo, "REPO", branch, branch,
            "📝", message, Instant.parse("2026-06-03T17:00:00Z"));
    }

    private ResultActions perform(MockHttpServletRequestBuilder builder) throws Exception {
        return standaloneSetup(sut)
            .addFilters(new SecurityContextHolderFilter(new HttpSessionSecurityContextRepository()))
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build()
            .perform(builder);
    }

    // ── redirect when no GitHub login ─────────────────────────────────────────

    @Test
    void ensureRedirectsToAccountPageWhenNoGithubLogin() throws Exception {
        final UserSettings noLoginSettings = mock(UserSettings.class);
        when(noLoginSettings.githubLoginVerified()).thenReturn(false);
        when(noLoginSettings.githubLogin()).thenReturn(java.util.Optional.empty());
        when(userSettingsService.getUserSettings(any())).thenReturn(noLoginSettings);

        perform(get("/github-activity").with(oidcSubject("user-uuid")))
            .andExpect(status().isOk())
            .andExpect(view().name("github-activity/no-github-login"));
    }

    // ── anchor categorization ─────────────────────────────────────────────────

    @Nested
    class AnchorCategorization {

        @Test
        void ensurePullRequestEventsAppearInPrAnchors() throws Exception {
            stubCommonDependencies("tronical");
            final var pr = prEntity("e1", "slint-ui/slint", "11950",
                "Upgrade deps", "Merged PR #11950: Upgrade deps");
            when(eventRepository.findByGithubUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                anyString(), any(), any())).thenReturn(List.of(pr));
            when(eventRepository.findFirstByGithubUsernameAndRepoNameAndAnchorTypeAndAnchorIdOrderByEventTimestampAsc(
                anyString(), anyString(), anyString(), anyString()))
                .thenReturn(java.util.Optional.of(pr));

            perform(get("/github-activity").with(oidcSubject("user-uuid")))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("prAnchors"))
                .andExpect(model().attribute("reviewAnchors", List.of()))
                .andExpect(model().attribute("issueAnchors", List.of()))
                .andExpect(model().attribute("standaloneAnchors", List.of()));
        }

        @Test
        void ensureReviewEventsAppearInReviewAnchors() throws Exception {
            stubCommonDependencies("tronical");
            final var review = reviewEntity("e2", "slint-ui/slint", "11957",
                "Data Transfer API", "Approved PR #11957: Data Transfer API");
            when(eventRepository.findByGithubUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                anyString(), any(), any())).thenReturn(List.of(review));

            perform(get("/github-activity").with(oidcSubject("user-uuid")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("prAnchors", List.of()))
                .andExpect(model().attributeExists("reviewAnchors"))
                .andExpect(model().attribute("issueAnchors", List.of()))
                .andExpect(model().attribute("standaloneAnchors", List.of()));
        }

        @Test
        void ensureIssueEventsAppearInIssueAnchors() throws Exception {
            stubCommonDependencies("tronical");
            final var issue = issueEntity("e3", "slint-ui/slint", "11949",
                "Keys is anonymous type", "Opened issue #11949: Keys is anonymous type");
            when(eventRepository.findByGithubUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                anyString(), any(), any())).thenReturn(List.of(issue));

            perform(get("/github-activity").with(oidcSubject("user-uuid")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("prAnchors", List.of()))
                .andExpect(model().attribute("reviewAnchors", List.of()))
                .andExpect(model().attributeExists("issueAnchors"))
                .andExpect(model().attribute("standaloneAnchors", List.of()));
        }

        @Test
        void ensurePushEventsAppearInStandaloneAnchors() throws Exception {
            stubCommonDependencies("tronical");
            final var commit = commitEntity("abc123def456abc123def456abc123def456abc1",
                "slint-ui/slint", "simon/license", "Replace cargo-about");
            when(eventRepository.findByGithubUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                anyString(), any(), any())).thenReturn(List.of(commit));

            perform(get("/github-activity").with(oidcSubject("user-uuid")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("prAnchors", List.of()))
                .andExpect(model().attribute("reviewAnchors", List.of()))
                .andExpect(model().attribute("issueAnchors", List.of()))
                .andExpect(model().attributeExists("standaloneAnchors"));
        }
    }

    // ── PR status derivation ──────────────────────────────────────────────────

    @Nested
    class PrStatus {

        @SuppressWarnings("unchecked")
        @Test
        void ensureMergedStatusWhenSummaryStartsWithMerged() throws Exception {
            stubCommonDependencies("tronical");
            final var pr = prEntity("e1", "slint-ui/slint", "11950",
                "Upgrade deps", "Merged PR #11950: Upgrade deps");
            when(eventRepository.findByGithubUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                anyString(), any(), any())).thenReturn(List.of(pr));
            when(eventRepository.findFirstByGithubUsernameAndRepoNameAndAnchorTypeAndAnchorIdOrderByEventTimestampAsc(
                anyString(), anyString(), anyString(), anyString()))
                .thenReturn(java.util.Optional.of(pr));

            final var result = perform(get("/github-activity").with(oidcSubject("user-uuid")))
                .andExpect(status().isOk())
                .andReturn();

            final List<ActivityAnchor> prAnchors = (List<ActivityAnchor>)
                result.getModelAndView().getModel().get("prAnchors");
            assertThat(prAnchors).hasSize(1);
            assertThat(prAnchors.get(0).prStatus()).isEqualTo("Merged");
        }

        @SuppressWarnings("unchecked")
        @Test
        void ensureOpenStatusWhenNoMergedOrClosedEvent() throws Exception {
            stubCommonDependencies("tronical");
            final var pr = prEntity("e1", "slint-ui/slint", "11940",
                "My feature", "Opened PR #11940: My feature");
            when(eventRepository.findByGithubUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                anyString(), any(), any())).thenReturn(List.of(pr));
            when(eventRepository.findFirstByGithubUsernameAndRepoNameAndAnchorTypeAndAnchorIdOrderByEventTimestampAsc(
                anyString(), anyString(), anyString(), anyString()))
                .thenReturn(java.util.Optional.of(pr));

            final var result = perform(get("/github-activity").with(oidcSubject("user-uuid")))
                .andExpect(status().isOk())
                .andReturn();

            final List<ActivityAnchor> prAnchors = (List<ActivityAnchor>)
                result.getModelAndView().getModel().get("prAnchors");
            assertThat(prAnchors.get(0).prStatus()).isEqualTo("Open");
        }
    }

    // ── review outcome derivation ─────────────────────────────────────────────

    @Nested
    class ReviewOutcome {

        @SuppressWarnings("unchecked")
        @Test
        void ensureApprovedOutcomeWhenSummaryStartsWithApproved() throws Exception {
            stubCommonDependencies("tronical");
            final var review = reviewEntity("e1", "slint-ui/slint", "11958",
                "Accessibility", "Approved PR #11958: Accessibility");
            when(eventRepository.findByGithubUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                anyString(), any(), any())).thenReturn(List.of(review));

            final var result = perform(get("/github-activity").with(oidcSubject("user-uuid")))
                .andReturn();

            final List<ActivityAnchor> reviewAnchors = (List<ActivityAnchor>)
                result.getModelAndView().getModel().get("reviewAnchors");
            assertThat(reviewAnchors.get(0).reviewOutcome()).isEqualTo("Approved");
        }

        @SuppressWarnings("unchecked")
        @Test
        void ensureCommentedOutcomeWhenNeitherApprovedNorChangesRequested() throws Exception {
            stubCommonDependencies("tronical");
            final var review = reviewEntity("e1", "slint-ui/slint", "11957",
                "Data Transfer", "Reviewed PR #11957: Data Transfer");
            when(eventRepository.findByGithubUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                anyString(), any(), any())).thenReturn(List.of(review));

            final var result = perform(get("/github-activity").with(oidcSubject("user-uuid")))
                .andReturn();

            final List<ActivityAnchor> reviewAnchors = (List<ActivityAnchor>)
                result.getModelAndView().getModel().get("reviewAnchors");
            assertThat(reviewAnchors.get(0).reviewOutcome()).isEqualTo("Commented");
        }

        @SuppressWarnings("unchecked")
        @Test
        void ensureChangesRequestedOutcome() throws Exception {
            stubCommonDependencies("tronical");
            final var review = reviewEntity("e1", "slint-ui/slint", "11957",
                "Data Transfer", "Requested changes on PR #11957: Data Transfer");
            when(eventRepository.findByGithubUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                anyString(), any(), any())).thenReturn(List.of(review));

            final var result = perform(get("/github-activity").with(oidcSubject("user-uuid")))
                .andReturn();

            final List<ActivityAnchor> reviewAnchors = (List<ActivityAnchor>)
                result.getModelAndView().getModel().get("reviewAnchors");
            assertThat(reviewAnchors.get(0).reviewOutcome()).isEqualTo("Changes requested");
        }
    }

    // ── issue action derivation ───────────────────────────────────────────────

    @Nested
    class IssueAction {

        @SuppressWarnings("unchecked")
        @Test
        void ensureClosedActionTakesPriorityOverOpened() throws Exception {
            stubCommonDependencies("tronical");
            final var open = issueEntity("e1", "slint-ui/slint", "11949",
                "Keys bug", "Opened issue #11949: Keys bug");
            final var close = issueEntity("e2", "slint-ui/slint", "11949",
                "Keys bug", "Closed issue #11949: Keys bug");
            close.setEventTimestamp(Instant.parse("2026-06-03T18:00:00Z"));
            when(eventRepository.findByGithubUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                anyString(), any(), any())).thenReturn(List.of(open, close));

            final var result = perform(get("/github-activity").with(oidcSubject("user-uuid")))
                .andReturn();

            final List<ActivityAnchor> issueAnchors = (List<ActivityAnchor>)
                result.getModelAndView().getModel().get("issueAnchors");
            assertThat(issueAnchors.get(0).issueAction()).isEqualTo("Closed");
        }

        @SuppressWarnings("unchecked")
        @Test
        void ensureCommentedActionWhenOnlyCommentEvent() throws Exception {
            stubCommonDependencies("tronical");
            final var comment = issueEntity("e1", "slint-ui/slint", "11876",
                "Skia error", "Commented on issue #11876: Skia error");
            when(eventRepository.findByGithubUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                anyString(), any(), any())).thenReturn(List.of(comment));

            final var result = perform(get("/github-activity").with(oidcSubject("user-uuid")))
                .andReturn();

            final List<ActivityAnchor> issueAnchors = (List<ActivityAnchor>)
                result.getModelAndView().getModel().get("issueAnchors");
            assertThat(issueAnchors.get(0).issueAction()).isEqualTo("Commented");
        }
    }

    // ── standalone commit filtering ───────────────────────────────────────────

    @Nested
    class StandaloneFiltering {

        @SuppressWarnings("unchecked")
        @Test
        void ensureCommitsOnPrHeadBranchAreExcludedFromStandalone() throws Exception {
            stubCommonDependencies("tronical");
            final var commit = commitEntity("abc123def456abc123def456abc123def456abc1",
                "slint-ui/slint", "nigel/my-feature", "Very simple screen");
            when(eventRepository.findByGithubUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                anyString(), any(), any())).thenReturn(List.of(commit));
            // The branch is a known PR head branch (opened on or before the selected date)
            when(eventRepository.findDistinctRepoAndHeadBranchesByUsernameUpToDate(anyString(), any()))
                .thenReturn(Set.of("slint-ui/slint|nigel/my-feature"));

            final var result = perform(get("/github-activity").with(oidcSubject("user-uuid")))
                .andReturn();

            final List<ActivityAnchor> standalone = (List<ActivityAnchor>)
                result.getModelAndView().getModel().get("standaloneAnchors");
            assertThat(standalone).isEmpty();
        }

        @SuppressWarnings("unchecked")
        @Test
        void ensureCreateEventGroupsAreExcludedFromStandalone() throws Exception {
            stubCommonDependencies("tronical");
            final var createEvent = entity("e-create", "CreateEvent", "slint-ui/slint",
                "REPO", "nigel/my-feature", "nigel/my-feature",
                "🌿", "Created branch nigel/my-feature", Instant.parse("2026-06-03T11:00:00Z"));
            when(eventRepository.findByGithubUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                anyString(), any(), any())).thenReturn(List.of(createEvent));

            final var result = perform(get("/github-activity").with(oidcSubject("user-uuid")))
                .andReturn();

            final List<ActivityAnchor> standalone = (List<ActivityAnchor>)
                result.getModelAndView().getModel().get("standaloneAnchors");
            assertThat(standalone).isEmpty();
        }
    }

    // ── synthetic PR anchors (commit-only days) ───────────────────────────────

    @Nested
    class SyntheticPrAnchors {

        @SuppressWarnings("unchecked")
        @Test
        void ensurePrAppearsInPrSectionOnCommitOnlyDay() throws Exception {
            stubCommonDependencies("tronical");
            final var commit = commitEntity("abc123def456abc123def456abc123def456abc1",
                "slint-ui/slint", "nigel/my-feature", "Fix crash");
            when(eventRepository.findByGithubUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                anyString(), any(), any())).thenReturn(List.of(commit));
            when(eventRepository.findDistinctRepoAndHeadBranchesByUsernameUpToDate(anyString(), any()))
                .thenReturn(Set.of("slint-ui/slint|nigel/my-feature"));

            // The PR entity for that branch
            final var prEntity = prEntity("e-pr", "slint-ui/slint", "11940",
                "Simple remote viewer", "Opened PR #11940: Simple remote viewer");
            prEntity.setHeadBranch("nigel/my-feature");
            when(eventRepository.findFirstByGithubUsernameAndRepoNameAndHeadBranchOrderByEventTimestampDesc(
                anyString(), eq("slint-ui/slint"), eq("nigel/my-feature")))
                .thenReturn(java.util.Optional.of(prEntity));
            when(eventRepository.findFirstByGithubUsernameAndRepoNameAndAnchorTypeAndAnchorIdOrderByEventTimestampAsc(
                anyString(), anyString(), anyString(), anyString()))
                .thenReturn(java.util.Optional.of(prEntity));

            final var result = perform(get("/github-activity").with(oidcSubject("user-uuid")))
                .andReturn();

            final List<ActivityAnchor> prAnchors = (List<ActivityAnchor>)
                result.getModelAndView().getModel().get("prAnchors");
            final List<ActivityAnchor> standalone = (List<ActivityAnchor>)
                result.getModelAndView().getModel().get("standaloneAnchors");
            assertThat(prAnchors).hasSize(1);
            assertThat(prAnchors.get(0).anchorId()).isEqualTo("11940");
            assertThat(prAnchors.get(0).anchorTitle()).isEqualTo("Simple remote viewer");
            assertThat(standalone).isEmpty();
        }

        @SuppressWarnings("unchecked")
        @Test
        void ensureCommitsBeforePrOpenedShowAsStandalone() throws Exception {
            stubCommonDependencies("tronical");
            final var commit = commitEntity("abc123", "slint-ui/slint", "nigel/my-feature", "Initial commit");
            when(eventRepository.findByGithubUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                anyString(), any(), any())).thenReturn(List.of(commit));
            // On this selected day, the PR was NOT yet opened (empty prHeadKeys)
            when(eventRepository.findDistinctRepoAndHeadBranchesByUsernameUpToDate(anyString(), any()))
                .thenReturn(Set.of());

            final var result = perform(get("/github-activity").with(oidcSubject("user-uuid")))
                .andReturn();

            final List<ActivityAnchor> standalone = (List<ActivityAnchor>)
                result.getModelAndView().getModel().get("standaloneAnchors");
            final List<ActivityAnchor> prAnchors = (List<ActivityAnchor>)
                result.getModelAndView().getModel().get("prAnchors");
            assertThat(standalone).hasSize(1); // shows as standalone — PR not opened yet
            assertThat(prAnchors).isEmpty();
        }
    }

    // ── IssueCommentEvent on PRs ──────────────────────────────────────────────

    @Nested
    class IssueCommentOnPr {

        @SuppressWarnings("unchecked")
        @Test
        void ensureIssueCommentEventOnPrAppearsInReviewAnchors() throws Exception {
            stubCommonDependencies("tronical");
            // IssueCommentEvent where the issue object has a "pull_request" key → stored as PR type
            final var prComment = entity("e-prcomment", "IssueCommentEvent", "slint-ui/slint",
                "PR", "11952", "Expose Keys API",
                "💬", "Commented on PR #11952: Expose Keys API — Still missing from docs",
                Instant.parse("2026-06-03T14:00:00Z"));
            when(eventRepository.findByGithubUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                anyString(), any(), any())).thenReturn(List.of(prComment));

            final var result = perform(get("/github-activity").with(oidcSubject("user-uuid")))
                .andReturn();

            final List<ActivityAnchor> reviewAnchors = (List<ActivityAnchor>)
                result.getModelAndView().getModel().get("reviewAnchors");
            final List<ActivityAnchor> issueAnchors = (List<ActivityAnchor>)
                result.getModelAndView().getModel().get("issueAnchors");
            assertThat(reviewAnchors).hasSize(1);
            assertThat(reviewAnchors.get(0).anchorId()).isEqualTo("11952");
            assertThat(issueAnchors).isEmpty();
        }
    }

    // ── title resolution ──────────────────────────────────────────────────────

    @Nested
    class TitleResolution {

        @SuppressWarnings("unchecked")
        @Test
        void ensureAnchorTitleUsedWhenPresent() throws Exception {
            stubCommonDependencies("tronical");
            final var pr = prEntity("e1", "slint-ui/slint", "11950",
                "Upgrade deps", "Merged PR #11950: Upgrade deps");
            when(eventRepository.findByGithubUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                anyString(), any(), any())).thenReturn(List.of(pr));
            when(eventRepository.findFirstByGithubUsernameAndRepoNameAndAnchorTypeAndAnchorIdOrderByEventTimestampAsc(
                anyString(), anyString(), anyString(), anyString()))
                .thenReturn(java.util.Optional.of(pr));

            final var result = perform(get("/github-activity").with(oidcSubject("user-uuid")))
                .andReturn();

            final List<ActivityAnchor> prAnchors = (List<ActivityAnchor>)
                result.getModelAndView().getModel().get("prAnchors");
            assertThat(prAnchors.get(0).anchorTitle()).isEqualTo("Upgrade deps");
        }

        @SuppressWarnings("unchecked")
        @Test
        void ensureTitleExtractedFromSummaryWhenAnchorTitleIsBlank() throws Exception {
            stubCommonDependencies("tronical");
            final var pr = prEntity("e1", "slint-ui/slint", "11950",
                "", "Merged PR #11950: Upgrade deps");  // blank anchorTitle
            when(eventRepository.findByGithubUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                anyString(), any(), any())).thenReturn(List.of(pr));
            when(eventRepository.findFirstByGithubUsernameAndRepoNameAndAnchorTypeAndAnchorIdOrderByEventTimestampAsc(
                anyString(), anyString(), anyString(), anyString()))
                .thenReturn(java.util.Optional.of(pr));

            final var result = perform(get("/github-activity").with(oidcSubject("user-uuid")))
                .andReturn();

            final List<ActivityAnchor> prAnchors = (List<ActivityAnchor>)
                result.getModelAndView().getModel().get("prAnchors");
            assertThat(prAnchors.get(0).anchorTitle()).isEqualTo("Upgrade deps");
        }
    }

    // ── commit URL building ───────────────────────────────────────────────────

    @Nested
    class CommitUrl {

        @SuppressWarnings("unchecked")
        @Test
        void ensureCommitUrlIsBuiltFromEventId() throws Exception {
            stubCommonDependencies("tronical");
            final String sha = "abc1234567890abcdef1234567890abcdef123456";
            final var commit = commitEntity(sha, "slint-ui/slint", "simon/license", "Replace cargo-about");
            when(eventRepository.findByGithubUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                anyString(), any(), any())).thenReturn(List.of(commit));

            final var result = perform(get("/github-activity").with(oidcSubject("user-uuid")))
                .andReturn();

            final List<ActivityAnchor> standalone = (List<ActivityAnchor>)
                result.getModelAndView().getModel().get("standaloneAnchors");
            assertThat(standalone).hasSize(1);
            assertThat(standalone.get(0).events()).hasSize(1);
            assertThat(standalone.get(0).events().get(0).commitUrl())
                .isEqualTo("https://github.com/slint-ui/slint/commit/" + sha);
        }

        @SuppressWarnings("unchecked")
        @Test
        void ensurePrAndReviewEventsHaveNoCommitUrl() throws Exception {
            stubCommonDependencies("tronical");
            final var review = reviewEntity("e1", "slint-ui/slint", "11958",
                "Accessibility", "Approved PR #11958: Accessibility");
            when(eventRepository.findByGithubUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                anyString(), any(), any())).thenReturn(List.of(review));

            final var result = perform(get("/github-activity").with(oidcSubject("user-uuid")))
                .andReturn();

            final List<ActivityAnchor> reviewAnchors = (List<ActivityAnchor>)
                result.getModelAndView().getModel().get("reviewAnchors");
            assertThat(reviewAnchors.get(0).events().get(0).commitUrl()).isNull();
        }
    }

    // ── deduplication ─────────────────────────────────────────────────────────

    @Nested
    class Deduplication {

        @SuppressWarnings("unchecked")
        @Test
        void ensureDuplicateCommitSummariesDeduplicatedWithinBranchGroup() throws Exception {
            stubCommonDependencies("tronical");
            // Same commit message, different event IDs (old-format duplicates)
            final var c1 = entity("tronical_commit_sha1", "PushEvent", "slint-ui/slint",
                "REPO", "simon/license", "simon/license",
                "📝", "Replace cargo-about", Instant.parse("2026-06-03T17:00:00Z"));
            final var c2 = entity("tronical_commit_sha2", "PushEvent", "slint-ui/slint",
                "REPO", "simon/license", "simon/license",
                "📝", "Replace cargo-about", Instant.parse("2026-06-03T17:05:00Z"));
            when(eventRepository.findByGithubUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                anyString(), any(), any())).thenReturn(List.of(c1, c2));

            final var result = perform(get("/github-activity").with(oidcSubject("user-uuid")))
                .andReturn();

            final List<ActivityAnchor> standalone = (List<ActivityAnchor>)
                result.getModelAndView().getModel().get("standaloneAnchors");
            assertThat(standalone).hasSize(1);
            assertThat(standalone.get(0).events()).hasSize(1);
        }
    }

    // ── hasActivity flag ──────────────────────────────────────────────────────

    @Test
    void ensureHasActivityIsFalseWhenNoEvents() throws Exception {
        stubCommonDependencies("tronical");
        when(eventRepository.findByGithubUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
            anyString(), any(), any())).thenReturn(List.of());

        perform(get("/github-activity").with(oidcSubject("user-uuid")))
            .andExpect(status().isOk())
            .andExpect(model().attribute("hasActivity", false));
    }

    @Test
    void ensureHasActivityIsTrueWhenEventsExist() throws Exception {
        stubCommonDependencies("tronical");
        final var commit = commitEntity("abc123", "slint-ui/slint", "simon/license", "My commit");
        when(eventRepository.findByGithubUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
            anyString(), any(), any())).thenReturn(List.of(commit));

        perform(get("/github-activity").with(oidcSubject("user-uuid")))
            .andExpect(status().isOk())
            .andExpect(model().attribute("hasActivity", true));
    }
}
