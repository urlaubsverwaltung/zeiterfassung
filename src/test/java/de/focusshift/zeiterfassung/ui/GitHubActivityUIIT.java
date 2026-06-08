package de.focusshift.zeiterfassung.ui;

import com.microsoft.playwright.Page;
import de.focusshift.zeiterfassung.SingleTenantPostgreSQLContainer;
import de.focusshift.zeiterfassung.TestKeycloakContainer;
import de.focusshift.zeiterfassung.gitactivity.GitActivityRawEventEntity;
import de.focusshift.zeiterfassung.gitactivity.GitActivityRawEventRepository;
import de.focusshift.zeiterfassung.gitactivity.GitHubActivityProvider;
import de.focusshift.zeiterfassung.user.UserSettings;
import de.focusshift.zeiterfassung.user.UserSettingsService;
import de.focusshift.zeiterfassung.ui.extension.UiTest;
import de.focusshift.zeiterfassung.ui.pages.GitHubActivityPage;
import de.focusshift.zeiterfassung.ui.pages.LoginPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;

import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers(parallel = true)
@UiTest
class GitHubActivityUIIT {

    @Container
    private static final SingleTenantPostgreSQLContainer postgre = new SingleTenantPostgreSQLContainer();
    @Container
    private static final TestKeycloakContainer keycloak = new TestKeycloakContainer();

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        postgre.configureSpringDataSource(registry);
        keycloak.configureSpringDataSource(registry);
    }

    @LocalServerPort
    private int port;

    @MockitoBean
    private UserSettingsService userSettingsService;

    @MockitoBean
    private GitActivityRawEventRepository gitActivityRawEventRepository;

    @MockitoBean
    private GitHubActivityProvider gitHubProvider;

    @BeforeEach
    void setUpDefaultMocks() {
        when(gitHubProvider.isConfigured()).thenReturn(true);
        when(gitHubProvider.missingConfig()).thenReturn("");
        when(gitHubProvider.getLastSyncTime(anyString())).thenReturn(null);
        when(gitHubProvider.isRateLimitSafe()).thenReturn(true);
        when(gitHubProvider.getRateLimitReset()).thenReturn(java.time.Instant.MIN);
        when(gitHubProvider.getRateLimitPercent()).thenReturn(100);
        when(gitHubProvider.getRateLimitRemaining()).thenReturn(5000);
        when(gitHubProvider.getRateLimitTotal()).thenReturn(5000);
        when(gitActivityRawEventRepository.findDistinctRepoAndHeadBranchesByUsernameUpToDate(anyString(), any()))
            .thenReturn(Set.of());
        // Default user settings — extracted before thenReturn() to avoid UnfinishedStubbingException
        final UserSettings defaultSettings = userSettingsWithLogin("tronical");
        when(userSettingsService.getUserSettings(any())).thenReturn(defaultSettings);
    }

    private UserSettings userSettingsWithLogin(String login) {
        final UserSettings settings = mock(UserSettings.class);
        when(settings.githubLoginVerified()).thenReturn(true);
        when(settings.githubLogin()).thenReturn(Optional.of(login));
        return settings;
    }

    private UserSettings userSettingsWithNoLogin() {
        final UserSettings settings = mock(UserSettings.class);
        when(settings.githubLoginVerified()).thenReturn(false);
        when(settings.githubLogin()).thenReturn(Optional.empty());
        return settings;
    }

    private GitActivityRawEventEntity prEntity(
        String eventId, String repo, String prNumber, String title, String summary) {
        final var e = new GitActivityRawEventEntity();
        e.setPlatformEventId(eventId);
        e.setPlatformUsername("tronical");
        e.setEventType("PullRequestEvent");
        e.setRepoName(repo);
        e.setAnchorType("PR");
        e.setAnchorId(prNumber);
        e.setAnchorTitle(title);
        e.setEventIcon("🔀");
        e.setEventSummary(summary);
        e.setEventTimestamp(Instant.parse("2026-06-03T14:00:00Z"));
        return e;
    }

    private GitActivityRawEventEntity reviewEntity(
        String eventId, String repo, String prNumber, String title, String summary) {
        final var e = new GitActivityRawEventEntity();
        e.setPlatformEventId(eventId);
        e.setPlatformUsername("tronical");
        e.setEventType("PullRequestReviewEvent");
        e.setRepoName(repo);
        e.setAnchorType("PR");
        e.setAnchorId(prNumber);
        e.setAnchorTitle(title);
        e.setEventIcon("👁");
        e.setEventSummary(summary);
        e.setEventTimestamp(Instant.parse("2026-06-03T15:00:00Z"));
        return e;
    }

    private GitActivityRawEventEntity issueEntity(
        String eventId, String repo, String issueNumber, String title, String summary) {
        final var e = new GitActivityRawEventEntity();
        e.setPlatformEventId(eventId);
        e.setPlatformUsername("tronical");
        e.setEventType("IssuesEvent");
        e.setRepoName(repo);
        e.setAnchorType("ISSUE");
        e.setAnchorId(issueNumber);
        e.setAnchorTitle(title);
        e.setEventIcon("🐛");
        e.setEventSummary(summary);
        e.setEventTimestamp(Instant.parse("2026-06-03T16:00:00Z"));
        return e;
    }

    private GitActivityRawEventEntity commitEntity(
        String sha, String repo, String branch, String message) {
        final var e = new GitActivityRawEventEntity();
        e.setPlatformEventId("tronical_commit_" + sha);
        e.setPlatformUsername("tronical");
        e.setEventType("PushEvent");
        e.setRepoName(repo);
        e.setAnchorType("REPO");
        e.setAnchorId(branch);
        e.setAnchorTitle(branch);
        e.setEventIcon("📝");
        e.setEventSummary(message);
        e.setEventTimestamp(Instant.parse("2026-06-03T17:00:00Z"));
        return e;
    }

    // ── No GitHub login ───────────────────────────────────────────────────────

    @Nested
    class NoGithubLogin {

        @Test
        void ensureNoLoginPanelIsShown(Page page) {
            final UserSettings noLogin = userSettingsWithNoLogin();
            when(userSettingsService.getUserSettings(any())).thenReturn(noLogin);

            final var loginPage = new LoginPage(page, port);
            final var activityPage = new GitHubActivityPage(page, port);

            loginPage.login(LoginPage.Credentials.USER);
            activityPage.navigate();

            assertThat(activityPage.noGithubLoginSection()).isVisible();
        }
    }

    // ── Empty state ───────────────────────────────────────────────────────────

    @Nested
    class EmptyState {

        @Test
        void ensureEmptyStateIsShownWhenNoEvents(Page page) {
            // userSettingsService default stub set in setUpDefaultMocks() — no override needed
            when(gitActivityRawEventRepository
                .findByPlatformUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                    anyString(), any(), any()))
                .thenReturn(List.of());

            final var loginPage = new LoginPage(page, port);
            final var activityPage = new GitHubActivityPage(page, port);

            loginPage.login(LoginPage.Credentials.USER);
            activityPage.navigate();

            assertThat(activityPage.emptyState()).isVisible();
            assertThat(activityPage.prSection()).not().isVisible();
            assertThat(activityPage.reviewsSection()).not().isVisible();
            assertThat(activityPage.issuesSection()).not().isVisible();
            assertThat(activityPage.standaloneSection()).not().isVisible();
        }
    }

    // ── Sync not configured ───────────────────────────────────────────────────

    @Nested
    class SyncNotConfigured {

        @Test
        void ensureSyncNotConfiguredBannerIsShown(Page page) {
            // userSettingsService default stub set in setUpDefaultMocks() — no override needed
            when(gitHubProvider.isConfigured()).thenReturn(false);
            when(gitHubProvider.missingConfig()).thenReturn("GITHUB_APP_ID, GITHUB_APP_PRIVATE_KEY");
            when(gitActivityRawEventRepository
                .findByPlatformUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                    anyString(), any(), any()))
                .thenReturn(List.of());

            final var loginPage = new LoginPage(page, port);
            final var activityPage = new GitHubActivityPage(page, port);

            loginPage.login(LoginPage.Credentials.USER);
            activityPage.navigate();

            assertThat(activityPage.syncNotConfiguredBanner()).isVisible();
        }
    }

    // ── Pull Requests section ─────────────────────────────────────────────────

    @Nested
    class PullRequestsSection {

        @BeforeEach
        void setUp() {
            // userSettingsService default stub set in setUpDefaultMocks() — no override needed
            final var pr = prEntity("e1", "slint-ui/slint", "11950",
                "Upgrade fontique and parley", "Merged PR #11950: Upgrade fontique and parley");
            when(gitActivityRawEventRepository
                .findByPlatformUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                    anyString(), any(), any()))
                .thenReturn(List.of(pr));
            when(gitActivityRawEventRepository
                .findFirstByPlatformUsernameAndRepoNameAndAnchorTypeAndAnchorIdOrderByEventTimestampAsc(
                    anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(pr));
        }

        @Test
        void ensurePrSectionIsVisible(Page page) {
            final var loginPage = new LoginPage(page, port);
            final var activityPage = new GitHubActivityPage(page, port);

            loginPage.login(LoginPage.Credentials.USER);
            activityPage.navigate();

            assertThat(activityPage.prSection()).isVisible();
        }

        @Test
        void ensurePrTableHasCorrectColumns(Page page) {
            final var loginPage = new LoginPage(page, port);
            final var activityPage = new GitHubActivityPage(page, port);

            loginPage.login(LoginPage.Credentials.USER);
            activityPage.navigate();

            final List<String> headers = activityPage.prTableColumnHeaders();
            assertThat(headers).containsExactly("Repo", "PR", "Title", "Opened", "Status");
        }

        @Test
        void ensurePrBadgeIsALinkToGitHub(Page page) {
            final var loginPage = new LoginPage(page, port);
            final var activityPage = new GitHubActivityPage(page, port);

            loginPage.login(LoginPage.Credentials.USER);
            activityPage.navigate();

            assertThat(activityPage.prBadgeLink(0)).hasAttribute("href",
                "https://github.com/slint-ui/slint/pull/11950");
        }

        @Test
        void ensurePrTitleIsALinkToGitHub(Page page) {
            final var loginPage = new LoginPage(page, port);
            final var activityPage = new GitHubActivityPage(page, port);

            loginPage.login(LoginPage.Credentials.USER);
            activityPage.navigate();

            assertThat(activityPage.prTitleLink(0)).isVisible();
            assertThat(activityPage.prTitleLink(0)).hasAttribute("href",
                "https://github.com/slint-ui/slint/pull/11950");
            assertThat(activityPage.prTitleLink(0)).hasText("Upgrade fontique and parley");
        }

        @Test
        void ensureMergedPrShowsMergedStatus(Page page) {
            final var loginPage = new LoginPage(page, port);
            final var activityPage = new GitHubActivityPage(page, port);

            loginPage.login(LoginPage.Credentials.USER);
            activityPage.navigate();

            assertThat(activityPage.prSection().getByText("Merged")).isVisible();
        }
    }

    // ── Reviews section ───────────────────────────────────────────────────────

    @Nested
    class ReviewsSection {

        @Test
        void ensureReviewsSectionHasCorrectColumns(Page page) {
            // userSettingsService default stub set in setUpDefaultMocks() — no override needed
            final var review = reviewEntity("e2", "slint-ui/slint", "11958",
                "Accessibility enhancements", "Approved PR #11958: Accessibility enhancements");
            when(gitActivityRawEventRepository
                .findByPlatformUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                    anyString(), any(), any()))
                .thenReturn(List.of(review));

            final var loginPage = new LoginPage(page, port);
            final var activityPage = new GitHubActivityPage(page, port);

            loginPage.login(LoginPage.Credentials.USER);
            activityPage.navigate();

            assertThat(activityPage.reviewsSection()).isVisible();
            final List<String> headers = activityPage.reviewsTableColumnHeaders();
            assertThat(headers).containsExactly("Repo", "PR", "Title", "Outcome");
        }

        @Test
        void ensureApprovedReviewShowsApprovedBadge(Page page) {
            // userSettingsService default stub set in setUpDefaultMocks() — no override needed
            final var review = reviewEntity("e2", "slint-ui/slint", "11958",
                "Accessibility enhancements", "Approved PR #11958: Accessibility enhancements");
            when(gitActivityRawEventRepository
                .findByPlatformUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                    anyString(), any(), any()))
                .thenReturn(List.of(review));

            final var loginPage = new LoginPage(page, port);
            final var activityPage = new GitHubActivityPage(page, port);

            loginPage.login(LoginPage.Credentials.USER);
            activityPage.navigate();

            assertThat(activityPage.reviewsSection().getByText("✓ Approved")).isVisible();
        }
    }

    // ── Issues section ────────────────────────────────────────────────────────

    @Nested
    class IssuesSection {

        @Test
        void ensureIssuesSectionHasCorrectColumns(Page page) {
            // userSettingsService default stub set in setUpDefaultMocks() — no override needed
            final var issue = issueEntity("e3", "slint-ui/slint", "11949",
                "Keys is anonymous type", "Opened issue #11949: Keys is anonymous type");
            when(gitActivityRawEventRepository
                .findByPlatformUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                    anyString(), any(), any()))
                .thenReturn(List.of(issue));

            final var loginPage = new LoginPage(page, port);
            final var activityPage = new GitHubActivityPage(page, port);

            loginPage.login(LoginPage.Credentials.USER);
            activityPage.navigate();

            assertThat(activityPage.issuesSection()).isVisible();
            final List<String> headers = activityPage.issuesTableColumnHeaders();
            assertThat(headers).containsExactly("Repo", "Issue", "Title", "Action");
        }

        @Test
        void ensureIssueTitleIsALinkToGitHub(Page page) {
            // userSettingsService default stub set in setUpDefaultMocks() — no override needed
            final var issue = issueEntity("e3", "slint-ui/slint", "11949",
                "Keys is anonymous type", "Opened issue #11949: Keys is anonymous type");
            when(gitActivityRawEventRepository
                .findByPlatformUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                    anyString(), any(), any()))
                .thenReturn(List.of(issue));

            final var loginPage = new LoginPage(page, port);
            final var activityPage = new GitHubActivityPage(page, port);

            loginPage.login(LoginPage.Credentials.USER);
            activityPage.navigate();

            final var titleLink = activityPage.issuesSection().locator("td:nth-child(3) a");
            assertThat(titleLink).hasAttribute("href",
                "https://github.com/slint-ui/slint/issues/11949");
        }
    }

    // ── Log it / Logged state ─────────────────────────────────────────────────

    /**
     * Tests for the "Log it" → "✓ Logged" feature across all activity types.
     *
     * <p>Covers two scenarios per type:
     * <ol>
     *   <li>Server-side: the page renders "✓ Logged" (instead of a Log it button) when the
     *       entity already has a {@code loggedAt} timestamp.</li>
     *   <li>Client-side: clicking "Log it", submitting the inline form, and verifying
     *       the button is replaced by "✓ Logged" without a page reload.</li>
     * </ol>
     */
    @Nested
    class LoggedState {

        @BeforeEach
        void setUp() {
            // userSettingsService default stub set in setUpDefaultMocks() — no override needed
        }

        // ── Pull Requests ──────────────────────────────────────────────────

        @Nested
        class PullRequests {

            @Test
            void ensureLogItButtonIsVisibleForUnloggedPR(Page page) {
                final var pr = prEntity("e1", "slint-ui/slint", "200", "Fix bug", "Merged PR #200: Fix bug");
                stubPrEvents(pr);
                final var loginPage = new LoginPage(page, port);
                final var activityPage = new GitHubActivityPage(page, port);

                loginPage.login(LoginPage.Credentials.USER);
                activityPage.navigate();

                assertThat(activityPage.prLogItButton(0)).isVisible();
                assertThat(activityPage.prLoggedLabel(0)).not().isVisible();
            }

            @Test
            void ensureLoggedLabelIsRenderedServerSideWhenPRAlreadyLogged(Page page) {
                final var pr = prEntity("e1", "slint-ui/slint", "200", "Fix bug", "Merged PR #200: Fix bug");
                pr.setLoggedAt(Instant.now());
                stubPrEvents(pr);
                final var loginPage = new LoginPage(page, port);
                final var activityPage = new GitHubActivityPage(page, port);

                loginPage.login(LoginPage.Credentials.USER);
                activityPage.navigate();

                assertThat(activityPage.prLoggedLabel(0)).isVisible();
                assertThat(activityPage.prLogItButton(0)).not().isVisible();
            }

            @Test
            void ensureClickingLogItOpensPRInlineForm(Page page) {
                final var pr = prEntity("e1", "slint-ui/slint", "200", "Fix bug", "Merged PR #200: Fix bug");
                stubPrEvents(pr);
                final var loginPage = new LoginPage(page, port);
                final var activityPage = new GitHubActivityPage(page, port);

                loginPage.login(LoginPage.Credentials.USER);
                activityPage.navigate();

                activityPage.prLogItButton(0).click();

                assertThat(activityPage.inlineFormSaveButton()).isVisible();
            }

            @Test
            void ensureLoggedLabelAppearsClientSideAfterSubmittingPRForm(Page page) {
                final var pr = prEntity("e1", "slint-ui/slint", "200", "Fix bug", "Merged PR #200: Fix bug");
                stubPrEvents(pr);
                final var loginPage = new LoginPage(page, port);
                final var activityPage = new GitHubActivityPage(page, port);

                loginPage.login(LoginPage.Credentials.USER);
                activityPage.navigate();

                activityPage.prLogItButton(0).click();
                activityPage.inlineFormSaveButton().click();

                assertThat(activityPage.prSection().getByText("✓ Logged")).isVisible();
            }

            private void stubPrEvents(GitActivityRawEventEntity pr) {
                when(gitActivityRawEventRepository
                    .findByPlatformUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                        anyString(), any(), any()))
                    .thenReturn(List.of(pr));
                when(gitActivityRawEventRepository
                    .findFirstByPlatformUsernameAndRepoNameAndAnchorTypeAndAnchorIdOrderByEventTimestampAsc(
                        anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(Optional.of(pr));
            }
        }

        // ── Reviews ────────────────────────────────────────────────────────

        @Nested
        class Reviews {

            @Test
            void ensureLogItButtonIsVisibleForUnloggedReview(Page page) {
                final var review = reviewEntity("e2", "slint-ui/slint", "201", "Add feature", "Approved PR #201: Add feature");
                stubReviewEvents(review);
                final var loginPage = new LoginPage(page, port);
                final var activityPage = new GitHubActivityPage(page, port);

                loginPage.login(LoginPage.Credentials.USER);
                activityPage.navigate();

                assertThat(activityPage.reviewLogItButton(0)).isVisible();
                assertThat(activityPage.reviewLoggedLabel(0)).not().isVisible();
            }

            @Test
            void ensureLoggedLabelIsRenderedServerSideWhenReviewAlreadyLogged(Page page) {
                final var review = reviewEntity("e2", "slint-ui/slint", "201", "Add feature", "Approved PR #201: Add feature");
                review.setLoggedAt(Instant.now());
                stubReviewEvents(review);
                final var loginPage = new LoginPage(page, port);
                final var activityPage = new GitHubActivityPage(page, port);

                loginPage.login(LoginPage.Credentials.USER);
                activityPage.navigate();

                assertThat(activityPage.reviewLoggedLabel(0)).isVisible();
                assertThat(activityPage.reviewLogItButton(0)).not().isVisible();
            }

            @Test
            void ensureClickingLogItOpensReviewInlineForm(Page page) {
                final var review = reviewEntity("e2", "slint-ui/slint", "201", "Add feature", "Approved PR #201: Add feature");
                stubReviewEvents(review);
                final var loginPage = new LoginPage(page, port);
                final var activityPage = new GitHubActivityPage(page, port);

                loginPage.login(LoginPage.Credentials.USER);
                activityPage.navigate();

                activityPage.reviewLogItButton(0).click();

                assertThat(activityPage.inlineFormSaveButton()).isVisible();
            }

            @Test
            void ensureLoggedLabelAppearsClientSideAfterSubmittingReviewForm(Page page) {
                final var review = reviewEntity("e2", "slint-ui/slint", "201", "Add feature", "Approved PR #201: Add feature");
                stubReviewEvents(review);
                final var loginPage = new LoginPage(page, port);
                final var activityPage = new GitHubActivityPage(page, port);

                loginPage.login(LoginPage.Credentials.USER);
                activityPage.navigate();

                activityPage.reviewLogItButton(0).click();
                activityPage.inlineFormSaveButton().click();

                assertThat(activityPage.reviewsSection().getByText("✓ Logged")).isVisible();
            }

            private void stubReviewEvents(GitActivityRawEventEntity review) {
                when(gitActivityRawEventRepository
                    .findByPlatformUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                        anyString(), any(), any()))
                    .thenReturn(List.of(review));
            }
        }

        // ── Issues ─────────────────────────────────────────────────────────

        @Nested
        class Issues {

            @Test
            void ensureLogItButtonIsVisibleForUnloggedIssue(Page page) {
                final var issue = issueEntity("e3", "slint-ui/slint", "202", "Crash on startup", "Opened issue #202: Crash on startup");
                stubIssueEvents(issue);
                final var loginPage = new LoginPage(page, port);
                final var activityPage = new GitHubActivityPage(page, port);

                loginPage.login(LoginPage.Credentials.USER);
                activityPage.navigate();

                assertThat(activityPage.issueLogItButton(0)).isVisible();
                assertThat(activityPage.issueLoggedLabel(0)).not().isVisible();
            }

            @Test
            void ensureLoggedLabelIsRenderedServerSideWhenIssueAlreadyLogged(Page page) {
                final var issue = issueEntity("e3", "slint-ui/slint", "202", "Crash on startup", "Opened issue #202: Crash on startup");
                issue.setLoggedAt(Instant.now());
                stubIssueEvents(issue);
                final var loginPage = new LoginPage(page, port);
                final var activityPage = new GitHubActivityPage(page, port);

                loginPage.login(LoginPage.Credentials.USER);
                activityPage.navigate();

                assertThat(activityPage.issueLoggedLabel(0)).isVisible();
                assertThat(activityPage.issueLogItButton(0)).not().isVisible();
            }

            @Test
            void ensureClickingLogItOpensIssueInlineForm(Page page) {
                final var issue = issueEntity("e3", "slint-ui/slint", "202", "Crash on startup", "Opened issue #202: Crash on startup");
                stubIssueEvents(issue);
                final var loginPage = new LoginPage(page, port);
                final var activityPage = new GitHubActivityPage(page, port);

                loginPage.login(LoginPage.Credentials.USER);
                activityPage.navigate();

                activityPage.issueLogItButton(0).click();

                assertThat(activityPage.inlineFormSaveButton()).isVisible();
            }

            @Test
            void ensureLoggedLabelAppearsClientSideAfterSubmittingIssueForm(Page page) {
                final var issue = issueEntity("e3", "slint-ui/slint", "202", "Crash on startup", "Opened issue #202: Crash on startup");
                stubIssueEvents(issue);
                final var loginPage = new LoginPage(page, port);
                final var activityPage = new GitHubActivityPage(page, port);

                loginPage.login(LoginPage.Credentials.USER);
                activityPage.navigate();

                activityPage.issueLogItButton(0).click();
                activityPage.inlineFormSaveButton().click();

                assertThat(activityPage.issuesSection().getByText("✓ Logged")).isVisible();
            }

            private void stubIssueEvents(GitActivityRawEventEntity issue) {
                when(gitActivityRawEventRepository
                    .findByPlatformUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                        anyString(), any(), any()))
                    .thenReturn(List.of(issue));
            }
        }

        // ── Standalone commits ─────────────────────────────────────────────

        @Nested
        class StandaloneCommits {

            @Test
            void ensureStandaloneSectionIsNotVisible(Page page) {
                final var commit = commitEntity("abc123", "slint-ui/slint", "main", "Fix typo in docs");
                when(gitActivityRawEventRepository
                    .findByPlatformUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                        anyString(), any(), any()))
                    .thenReturn(List.of(commit));
                final var loginPage = new LoginPage(page, port);
                final var activityPage = new GitHubActivityPage(page, port);

                loginPage.login(LoginPage.Credentials.USER);
                activityPage.navigate();

                assertThat(activityPage.standaloneSection()).not().isVisible();
            }
        }
    }

    // ── Search modal ──────────────────────────────────────────────────────────

    @Nested
    class SearchModal {

        // ── trigger & open/close ──────────────────────────────────────────────

        @Test
        void ensureSearchTriggerButtonIsVisible(Page page) {
            when(gitActivityRawEventRepository
                .findByPlatformUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                    anyString(), any(), any()))
                .thenReturn(List.of());

            final var loginPage = new LoginPage(page, port);
            final var activityPage = new GitHubActivityPage(page, port);

            loginPage.login(LoginPage.Credentials.USER);
            activityPage.navigate();

            assertThat(activityPage.searchTrigger()).isVisible();
        }

        @Test
        void ensureSearchModalOpensByClickingTrigger(Page page) {
            when(gitActivityRawEventRepository
                .findByPlatformUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                    anyString(), any(), any()))
                .thenReturn(List.of());

            final var loginPage = new LoginPage(page, port);
            final var activityPage = new GitHubActivityPage(page, port);

            loginPage.login(LoginPage.Credentials.USER);
            activityPage.navigate();

            activityPage.openSearchModal();

            assertThat(activityPage.searchModal()).isVisible();
            assertThat(activityPage.searchInput()).isVisible();
            assertThat(activityPage.searchTabDay()).isVisible();
            assertThat(activityPage.searchTabAll()).isVisible();
        }

        @Test
        void ensureSearchModalOpensWithKeyboardShortcut(Page page) {
            when(gitActivityRawEventRepository
                .findByPlatformUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                    anyString(), any(), any()))
                .thenReturn(List.of());

            final var loginPage = new LoginPage(page, port);
            final var activityPage = new GitHubActivityPage(page, port);

            loginPage.login(LoginPage.Credentials.USER);
            activityPage.navigate();

            page.keyboard().press("Meta+k");

            assertThat(activityPage.searchModal()).isVisible();
        }

        @Test
        void ensureSearchModalClosesOnEscapeKey(Page page) {
            when(gitActivityRawEventRepository
                .findByPlatformUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                    anyString(), any(), any()))
                .thenReturn(List.of());

            final var loginPage = new LoginPage(page, port);
            final var activityPage = new GitHubActivityPage(page, port);

            loginPage.login(LoginPage.Credentials.USER);
            activityPage.navigate();

            activityPage.openSearchModal();
            assertThat(activityPage.searchModal()).isVisible();

            page.keyboard().press("Escape");

            assertThat(activityPage.searchModal()).isHidden();
        }

        @Test
        void ensureSearchModalClosesOnBackdropClick(Page page) {
            when(gitActivityRawEventRepository
                .findByPlatformUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                    anyString(), any(), any()))
                .thenReturn(List.of());

            final var loginPage = new LoginPage(page, port);
            final var activityPage = new GitHubActivityPage(page, port);

            loginPage.login(LoginPage.Credentials.USER);
            activityPage.navigate();

            activityPage.openSearchModal();
            // Click the backdrop element directly (first child of the modal overlay)
            page.locator("#gh-search-modal > div").first().click();

            assertThat(activityPage.searchModal()).isHidden();
        }

        // ── This day filter ───────────────────────────────────────────────────

        @Test
        void ensureSearchFilterShowsMatchingPrInThisDayMode(Page page) {
            final var pr1 = prEntity("e1", "slint-ui/slint", "100",
                "Fix layout bug", "Merged PR #100: Fix layout bug");
            final var pr2 = prEntity("e2", "slint-ui/slint", "101",
                "Add dark mode", "Opened PR #101: Add dark mode");
            when(gitActivityRawEventRepository
                .findByPlatformUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                    anyString(), any(), any()))
                .thenReturn(List.of(pr1, pr2));
            when(gitActivityRawEventRepository
                .findFirstByPlatformUsernameAndRepoNameAndAnchorTypeAndAnchorIdOrderByEventTimestampAsc(
                    anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

            final var loginPage = new LoginPage(page, port);
            final var activityPage = new GitHubActivityPage(page, port);

            loginPage.login(LoginPage.Credentials.USER);
            activityPage.navigate();

            activityPage.openSearchModal();
            activityPage.typeInSearch("layout");

            assertThat(activityPage.searchResultItems()).hasCount(1);
            assertThat(activityPage.searchResultItems().first()).containsText("Fix layout bug");
        }

        @Test
        void ensureSearchFilterShowsResultsFromAllSections(Page page) {
            final var pr     = prEntity("e1", "slint-ui/slint", "100", "Fix renderer", "Merged PR #100: Fix renderer");
            final var review = reviewEntity("e2", "slint-ui/slint", "101", "Renderer review", "Approved PR #101: Renderer review");
            final var issue  = issueEntity("e3", "slint-ui/slint", "202", "Renderer crash", "Opened issue #202: Renderer crash");
            final var commit = commitEntity("abc123", "slint-ui/slint", "main", "Fix renderer path");
            when(gitActivityRawEventRepository
                .findByPlatformUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                    anyString(), any(), any()))
                .thenReturn(List.of(pr, review, issue, commit));
            when(gitActivityRawEventRepository
                .findFirstByPlatformUsernameAndRepoNameAndAnchorTypeAndAnchorIdOrderByEventTimestampAsc(
                    anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

            final var loginPage = new LoginPage(page, port);
            final var activityPage = new GitHubActivityPage(page, port);

            loginPage.login(LoginPage.Credentials.USER);
            activityPage.navigate();

            activityPage.openSearchModal();
            activityPage.typeInSearch("renderer");

            // Standalone commits are excluded from the search modal
            assertThat(activityPage.searchResultItems()).hasCount(3);
        }

        @Test
        void ensureSearchFilterShowsNoMatchStateWhenNothingMatches(Page page) {
            final var pr = prEntity("e1", "slint-ui/slint", "100", "Fix layout", "Merged PR #100: Fix layout");
            when(gitActivityRawEventRepository
                .findByPlatformUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                    anyString(), any(), any()))
                .thenReturn(List.of(pr));
            when(gitActivityRawEventRepository
                .findFirstByPlatformUsernameAndRepoNameAndAnchorTypeAndAnchorIdOrderByEventTimestampAsc(
                    anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

            final var loginPage = new LoginPage(page, port);
            final var activityPage = new GitHubActivityPage(page, port);

            loginPage.login(LoginPage.Credentials.USER);
            activityPage.navigate();

            activityPage.openSearchModal();
            activityPage.typeInSearch("zzznomatch");

            assertThat(activityPage.searchEmptyState()).isVisible();
            assertThat(activityPage.searchEmptyState()).containsText("No matches");
            assertThat(activityPage.searchResultItems()).hasCount(0);
        }

        @Test
        void ensureSearchFilterIsCaseInsensitive(Page page) {
            final var pr = prEntity("e1", "slint-ui/slint", "100",
                "Fix Layout Bug", "Merged PR #100: Fix Layout Bug");
            when(gitActivityRawEventRepository
                .findByPlatformUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                    anyString(), any(), any()))
                .thenReturn(List.of(pr));
            when(gitActivityRawEventRepository
                .findFirstByPlatformUsernameAndRepoNameAndAnchorTypeAndAnchorIdOrderByEventTimestampAsc(
                    anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

            final var loginPage = new LoginPage(page, port);
            final var activityPage = new GitHubActivityPage(page, port);

            loginPage.login(LoginPage.Credentials.USER);
            activityPage.navigate();

            activityPage.openSearchModal();
            activityPage.typeInSearch("LAYOUT");

            assertThat(activityPage.searchResultItems()).hasCount(1);
        }

        @Test
        void ensureSearchAllDatesLinkAppearsAfterTyping(Page page) {
            when(gitActivityRawEventRepository
                .findByPlatformUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                    anyString(), any(), any()))
                .thenReturn(List.of());

            final var loginPage = new LoginPage(page, port);
            final var activityPage = new GitHubActivityPage(page, port);

            loginPage.login(LoginPage.Credentials.USER);
            activityPage.navigate();

            activityPage.openSearchModal();
            assertThat(activityPage.searchAllDatesLink()).isHidden();

            activityPage.typeInSearch("slint");

            assertThat(activityPage.searchAllDatesLink()).isVisible();
        }

        // ── All dates tab ─────────────────────────────────────────────────────

        @Test
        void ensureClickingAllDatesTabSwitchesMode(Page page) {
            when(gitActivityRawEventRepository
                .findByPlatformUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                    anyString(), any(), any()))
                .thenReturn(List.of());
            when(gitActivityRawEventRepository.searchEvents(anyString(), anyString(), any(), any()))
                .thenReturn(List.of());

            final var loginPage = new LoginPage(page, port);
            final var activityPage = new GitHubActivityPage(page, port);

            loginPage.login(LoginPage.Credentials.USER);
            activityPage.navigate();

            activityPage.openSearchModal();
            activityPage.typeInSearch("slint");
            activityPage.searchTabAll().click();

            // "All dates" tab should now appear active (contains active colour class)
            assertThat(activityPage.searchTabAll()).hasClass(java.util.regex.Pattern.compile("bg-blue-100|bg-blue-900"));
        }

        @Test
        void ensureAllDatesSearchShowsServerResults(Page page) {
            when(gitActivityRawEventRepository
                .findByPlatformUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                    anyString(), any(), any()))
                .thenReturn(List.of());
            final var pr = prEntity("e1", "slint-ui/slint", "200",
                "Fix rendering pipeline", "Merged PR #200: Fix rendering pipeline");
            pr.setEventTimestamp(Instant.parse("2026-05-20T10:00:00Z"));
            when(gitActivityRawEventRepository.searchEvents(anyString(), anyString(), any(), any()))
                .thenReturn(List.of(pr));

            final var loginPage = new LoginPage(page, port);
            final var activityPage = new GitHubActivityPage(page, port);

            loginPage.login(LoginPage.Credentials.USER);
            activityPage.navigate();

            activityPage.openSearchModal();
            activityPage.typeInSearch("rendering");
            activityPage.searchTabAll().click();

            assertThat(activityPage.searchResultItems()).hasCount(1);
            assertThat(activityPage.searchResultItems().first()).containsText("Fix rendering pipeline");
        }

        @Test
        void ensureAllDatesSearchShowsEmptyStateWhenNoResults(Page page) {
            when(gitActivityRawEventRepository
                .findByPlatformUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                    anyString(), any(), any()))
                .thenReturn(List.of());
            when(gitActivityRawEventRepository.searchEvents(anyString(), anyString(), any(), any()))
                .thenReturn(List.of());

            final var loginPage = new LoginPage(page, port);
            final var activityPage = new GitHubActivityPage(page, port);

            loginPage.login(LoginPage.Credentials.USER);
            activityPage.navigate();

            activityPage.openSearchModal();
            activityPage.typeInSearch("zzznomatch");
            activityPage.searchTabAll().click();

            assertThat(page.getByTestId("gh-search-result")).hasCount(0);
        }

        // ── Log it from modal ─────────────────────────────────────────────────

        @Test
        void ensureClickingLogItInModalClosesModalAndOpensInlineForm(Page page) {
            final var pr = prEntity("e1", "slint-ui/slint", "100", "Fix bug", "Merged PR #100: Fix bug");
            when(gitActivityRawEventRepository
                .findByPlatformUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                    anyString(), any(), any()))
                .thenReturn(List.of(pr));
            when(gitActivityRawEventRepository
                .findFirstByPlatformUsernameAndRepoNameAndAnchorTypeAndAnchorIdOrderByEventTimestampAsc(
                    anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

            final var loginPage = new LoginPage(page, port);
            final var activityPage = new GitHubActivityPage(page, port);

            loginPage.login(LoginPage.Credentials.USER);
            activityPage.navigate();

            activityPage.openSearchModal();
            activityPage.typeInSearch("fix");
            activityPage.searchLogItButtons().first().click();

            assertThat(activityPage.searchModal()).isHidden();
            assertThat(activityPage.inlineFormSaveButton()).isVisible();
        }
    }

    // ── Standalone commits section ────────────────────────────────────────────

    @Nested
    class StandaloneCommitsSection {

        @Test
        void ensureStandaloneSectionIsHiddenEvenWhenCommitsExist(Page page) {
            final var commit = commitEntity("abc123", "slint-ui/slint", "simon/license",
                "Replace cargo-about");
            when(gitActivityRawEventRepository
                .findByPlatformUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                    anyString(), any(), any()))
                .thenReturn(List.of(commit));

            final var loginPage = new LoginPage(page, port);
            final var activityPage = new GitHubActivityPage(page, port);

            loginPage.login(LoginPage.Credentials.USER);
            activityPage.navigate();

            assertThat(activityPage.standaloneSection()).not().isVisible();
        }
    }

    // ── cross-fork PR filtering ───────────────────────────────────────────────

    @Nested
    class CrossForkPrSection {

        @Test
        void ensureCommitsOnCrossForkBranchDoNotAppearAsStandalone(Page page) {
            // Commits in the fork (LeonMatthes/slint) on the PR head branch must not
            // appear as standalone once the COALESCE query returns the fork repo|branch key.
            final var commit = commitEntity("abc123def456abc123def456abc123def456abc1",
                "LeonMatthes/slint", "fix-tree-sitter-grammar", "Setup improved tree-sitter harness");
            when(gitActivityRawEventRepository
                .findByPlatformUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                    anyString(), any(), any()))
                .thenReturn(List.of(commit));
            when(gitActivityRawEventRepository.findDistinctRepoAndHeadBranchesByUsernameUpToDate(anyString(), any()))
                .thenReturn(Set.of("LeonMatthes/slint|fix-tree-sitter-grammar"));

            final var loginPage = new LoginPage(page, port);
            final var activityPage = new GitHubActivityPage(page, port);

            loginPage.login(LoginPage.Credentials.USER);
            activityPage.navigate();

            assertThat(activityPage.standaloneSection()).not().isVisible();
        }

        @Test
        void ensureCrossForkCommitsAppearUnderSyntheticPrAnchor(Page page) {
            // Same fork commit — but now the cross-fork repo query returns a PR entity,
            // so the commit should appear in the PR section, not standalone.
            final var commit = commitEntity("abc123def456abc123def456abc123def456abc1",
                "LeonMatthes/slint", "fix-tree-sitter-grammar", "Setup improved tree-sitter harness");
            when(gitActivityRawEventRepository
                .findByPlatformUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                    anyString(), any(), any()))
                .thenReturn(List.of(commit));
            when(gitActivityRawEventRepository.findDistinctRepoAndHeadBranchesByUsernameUpToDate(anyString(), any()))
                .thenReturn(Set.of("LeonMatthes/slint|fix-tree-sitter-grammar"));

            // Standard same-repo lookup returns nothing
            when(gitActivityRawEventRepository.findFirstByPlatformUsernameAndRepoNameAndHeadBranchOrderByEventTimestampDesc(
                anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

            // Cross-fork lookup returns the upstream PR
            final var prEntity = prEntity("e-pr-fork", "slint-ui/tree-sitter-slint", "1",
                "Fix tree-sitter grammar", "Opened PR #1: Fix tree-sitter grammar");
            prEntity.setHeadBranch("fix-tree-sitter-grammar");
            when(gitActivityRawEventRepository.findFirstByPlatformUsernameAndHeadRepoNameAndHeadBranchOrderByEventTimestampDesc(
                anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(prEntity));
            when(gitActivityRawEventRepository.findFirstByPlatformUsernameAndRepoNameAndAnchorTypeAndAnchorIdOrderByEventTimestampAsc(
                anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(prEntity));

            final var loginPage = new LoginPage(page, port);
            final var activityPage = new GitHubActivityPage(page, port);

            loginPage.login(LoginPage.Credentials.USER);
            activityPage.navigate();

            assertThat(activityPage.prSection()).isVisible();
            assertThat(activityPage.standaloneSection()).not().isVisible();
        }
    }
}
