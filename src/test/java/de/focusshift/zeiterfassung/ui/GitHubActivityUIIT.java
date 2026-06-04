package de.focusshift.zeiterfassung.ui;

import com.microsoft.playwright.Page;
import de.focusshift.zeiterfassung.SingleTenantPostgreSQLContainer;
import de.focusshift.zeiterfassung.TestKeycloakContainer;
import de.focusshift.zeiterfassung.githubactivity.GitHubRawEventEntity;
import de.focusshift.zeiterfassung.githubactivity.GitHubRawEventRepository;
import de.focusshift.zeiterfassung.githubactivity.GitHubSyncService;
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
    private GitHubRawEventRepository gitHubRawEventRepository;

    @MockitoBean
    private GitHubSyncService gitHubSyncService;

    @BeforeEach
    void setUpDefaultMocks() {
        when(gitHubSyncService.isConfigured()).thenReturn(true);
        when(gitHubSyncService.missingConfig()).thenReturn("");
        when(gitHubSyncService.getLastSyncTime(anyString())).thenReturn(null);
        when(gitHubSyncService.isRateLimitSafe()).thenReturn(true);
        when(gitHubSyncService.getRateLimitReset()).thenReturn(java.time.Instant.MIN);
        when(gitHubRawEventRepository.findDistinctRepoAndHeadBranchesByUsernameUpToDate(anyString(), any()))
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

    private GitHubRawEventEntity prEntity(
        String eventId, String repo, String prNumber, String title, String summary) {
        final var e = new GitHubRawEventEntity();
        e.setGithubEventId(eventId);
        e.setGithubUsername("tronical");
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

    private GitHubRawEventEntity reviewEntity(
        String eventId, String repo, String prNumber, String title, String summary) {
        final var e = new GitHubRawEventEntity();
        e.setGithubEventId(eventId);
        e.setGithubUsername("tronical");
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

    private GitHubRawEventEntity issueEntity(
        String eventId, String repo, String issueNumber, String title, String summary) {
        final var e = new GitHubRawEventEntity();
        e.setGithubEventId(eventId);
        e.setGithubUsername("tronical");
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

    private GitHubRawEventEntity commitEntity(
        String sha, String repo, String branch, String message) {
        final var e = new GitHubRawEventEntity();
        e.setGithubEventId("tronical_commit_" + sha);
        e.setGithubUsername("tronical");
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
            when(gitHubRawEventRepository
                .findByGithubUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
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
            when(gitHubSyncService.isConfigured()).thenReturn(false);
            when(gitHubSyncService.missingConfig()).thenReturn("GITHUB_APP_ID, GITHUB_APP_PRIVATE_KEY");
            when(gitHubRawEventRepository
                .findByGithubUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
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
            when(gitHubRawEventRepository
                .findByGithubUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                    anyString(), any(), any()))
                .thenReturn(List.of(pr));
            when(gitHubRawEventRepository
                .findFirstByGithubUsernameAndRepoNameAndAnchorTypeAndAnchorIdOrderByEventTimestampAsc(
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
            when(gitHubRawEventRepository
                .findByGithubUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
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
            when(gitHubRawEventRepository
                .findByGithubUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
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
            when(gitHubRawEventRepository
                .findByGithubUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
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
            when(gitHubRawEventRepository
                .findByGithubUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
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

            private void stubPrEvents(GitHubRawEventEntity pr) {
                when(gitHubRawEventRepository
                    .findByGithubUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                        anyString(), any(), any()))
                    .thenReturn(List.of(pr));
                when(gitHubRawEventRepository
                    .findFirstByGithubUsernameAndRepoNameAndAnchorTypeAndAnchorIdOrderByEventTimestampAsc(
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

            private void stubReviewEvents(GitHubRawEventEntity review) {
                when(gitHubRawEventRepository
                    .findByGithubUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
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

            private void stubIssueEvents(GitHubRawEventEntity issue) {
                when(gitHubRawEventRepository
                    .findByGithubUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                        anyString(), any(), any()))
                    .thenReturn(List.of(issue));
            }
        }

        // ── Standalone commits ─────────────────────────────────────────────

        @Nested
        class StandaloneCommits {

            @Test
            void ensureLogItButtonIsVisibleForUnloggedCommit(Page page) {
                final var commit = commitEntity("abc123", "slint-ui/slint", "main", "Fix typo in docs");
                stubCommitEvents(commit);
                final var loginPage = new LoginPage(page, port);
                final var activityPage = new GitHubActivityPage(page, port);

                loginPage.login(LoginPage.Credentials.USER);
                activityPage.navigate();

                assertThat(activityPage.standaloneLogItButton(0, 0)).isVisible();
                assertThat(activityPage.standaloneLoggedLabel(0, 0)).not().isVisible();
            }

            @Test
            void ensureLoggedLabelIsRenderedServerSideWhenCommitAlreadyLogged(Page page) {
                final var commit = commitEntity("abc123", "slint-ui/slint", "main", "Fix typo in docs");
                commit.setLoggedAt(Instant.now());
                stubCommitEvents(commit);
                final var loginPage = new LoginPage(page, port);
                final var activityPage = new GitHubActivityPage(page, port);

                loginPage.login(LoginPage.Credentials.USER);
                activityPage.navigate();

                assertThat(activityPage.standaloneLoggedLabel(0, 0)).isVisible();
                assertThat(activityPage.standaloneLogItButton(0, 0)).not().isVisible();
            }

            @Test
            void ensureClickingLogItOpensCommitInlineForm(Page page) {
                final var commit = commitEntity("abc123", "slint-ui/slint", "main", "Fix typo in docs");
                stubCommitEvents(commit);
                final var loginPage = new LoginPage(page, port);
                final var activityPage = new GitHubActivityPage(page, port);

                loginPage.login(LoginPage.Credentials.USER);
                activityPage.navigate();

                activityPage.standaloneLogItButton(0, 0).click();

                assertThat(activityPage.inlineFormSaveButton()).isVisible();
            }

            @Test
            void ensureLoggedLabelAppearsClientSideAfterSubmittingCommitForm(Page page) {
                final var commit = commitEntity("abc123", "slint-ui/slint", "main", "Fix typo in docs");
                stubCommitEvents(commit);
                final var loginPage = new LoginPage(page, port);
                final var activityPage = new GitHubActivityPage(page, port);

                loginPage.login(LoginPage.Credentials.USER);
                activityPage.navigate();

                activityPage.standaloneLogItButton(0, 0).click();
                activityPage.inlineFormSaveButton().click();

                assertThat(activityPage.standaloneSection().getByText("✓ Logged")).isVisible();
            }

            private void stubCommitEvents(GitHubRawEventEntity commit) {
                when(gitHubRawEventRepository
                    .findByGithubUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                        anyString(), any(), any()))
                    .thenReturn(List.of(commit));
            }
        }
    }

    // ── Standalone commits section ────────────────────────────────────────────

    @Nested
    class StandaloneCommitsSection {

        @Test
        void ensureStandaloneCommitMessageIsALinkToGitHubCommit(Page page) {
            // userSettingsService default stub set in setUpDefaultMocks() — no override needed
            final String sha = "abc1234567890abcdef1234567890abcdef123456";
            final var commit = commitEntity(sha, "slint-ui/slint", "simon/license",
                "Replace cargo-about with a self-contained xtask");
            when(gitHubRawEventRepository
                .findByGithubUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                    anyString(), any(), any()))
                .thenReturn(List.of(commit));

            final var loginPage = new LoginPage(page, port);
            final var activityPage = new GitHubActivityPage(page, port);

            loginPage.login(LoginPage.Credentials.USER);
            activityPage.navigate();

            assertThat(activityPage.standaloneSection()).isVisible();
            assertThat(activityPage.standaloneCommitLinks().first())
                .hasAttribute("href", "https://github.com/slint-ui/slint/commit/" + sha);
        }

        @Test
        void ensureStandaloneCommitShowsBranchName(Page page) {
            // userSettingsService default stub set in setUpDefaultMocks() — no override needed
            final var commit = commitEntity("abc123", "slint-ui/slint", "simon/license",
                "Replace cargo-about");
            when(gitHubRawEventRepository
                .findByGithubUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
                    anyString(), any(), any()))
                .thenReturn(List.of(commit));

            final var loginPage = new LoginPage(page, port);
            final var activityPage = new GitHubActivityPage(page, port);

            loginPage.login(LoginPage.Credentials.USER);
            activityPage.navigate();

            assertThat(activityPage.standaloneSection().getByText("simon/license")).isVisible();
        }
    }
}
