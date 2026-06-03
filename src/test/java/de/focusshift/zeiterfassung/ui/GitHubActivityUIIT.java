package de.focusshift.zeiterfassung.ui;

import com.microsoft.playwright.Page;
import de.focusshift.zeiterfassung.SingleTenantTestContainersBase;
import de.focusshift.zeiterfassung.githubactivity.GitHubRawEventEntity;
import de.focusshift.zeiterfassung.githubactivity.GitHubRawEventRepository;
import de.focusshift.zeiterfassung.githubactivity.GitHubSyncService;
import de.focusshift.zeiterfassung.user.UserSettings;
import de.focusshift.zeiterfassung.user.UserSettingsService;
import de.focusshift.zeiterfassung.ui.extension.UiTest;
import de.focusshift.zeiterfassung.ui.pages.GitHubActivityPage;
import de.focusshift.zeiterfassung.ui.pages.LoginPage;
import de.focusshift.zeiterfassung.ui.pages.NavigationPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

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
@UiTest
class GitHubActivityUIIT extends SingleTenantTestContainersBase {

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
        when(gitHubRawEventRepository.findDistinctRepoAndHeadBranchesByUsername(anyString()))
            .thenReturn(Set.of());
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
            when(userSettingsService.getUserSettings(any())).thenReturn(userSettingsWithLogin("tronical"));
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
            when(userSettingsService.getUserSettings(any())).thenReturn(userSettingsWithLogin("tronical"));
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
            when(userSettingsService.getUserSettings(any())).thenReturn(userSettingsWithLogin("tronical"));
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
            when(userSettingsService.getUserSettings(any())).thenReturn(userSettingsWithLogin("tronical"));
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
            when(userSettingsService.getUserSettings(any())).thenReturn(userSettingsWithLogin("tronical"));
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
            when(userSettingsService.getUserSettings(any())).thenReturn(userSettingsWithLogin("tronical"));
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
            when(userSettingsService.getUserSettings(any())).thenReturn(userSettingsWithLogin("tronical"));
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

    // ── Standalone commits section ────────────────────────────────────────────

    @Nested
    class StandaloneCommitsSection {

        @Test
        void ensureStandaloneCommitMessageIsALinkToGitHubCommit(Page page) {
            when(userSettingsService.getUserSettings(any())).thenReturn(userSettingsWithLogin("tronical"));
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
            when(userSettingsService.getUserSettings(any())).thenReturn(userSettingsWithLogin("tronical"));
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
