package de.focusshift.zeiterfassung.githubactivity;

import de.focusshift.zeiterfassung.user.UserSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class GitHubSyncServiceTest {

    private static final String LOGIN = "tronical";
    private static final String TOKEN = "test-token";

    @Mock private GitHubRawEventRepository repository;
    @Mock private UserSettingsService userSettingsService;
    @Mock private RestClient restClient;
    @Mock @SuppressWarnings("rawtypes") private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock @SuppressWarnings("rawtypes") private RestClient.RequestHeadersSpec requestHeadersSpec;
    @Mock private RestClient.ResponseSpec responseSpec;

    private GitHubSyncService sut;

    @BeforeEach
    void setUp() {
        sut = new GitHubSyncService(repository, userSettingsService, restClient);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void stubRestGet(Object returnValue) {
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
        when(requestHeadersUriSpec.uri(any(java.net.URI.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(returnValue);
    }

    private Map<String, Object> pushEvent(String eventId, String repo, String branch,
                                           String head, String before) {
        return Map.of(
            "id", eventId,
            "type", "PushEvent",
            "created_at", "2026-06-02T14:00:00Z",
            "repo", Map.of("name", repo),
            "payload", Map.of(
                "ref", "refs/heads/" + branch,
                "head", head,
                "before", before,
                "commits", List.of()
            )
        );
    }

    private Map<String, Object> commit(String sha, String authorLogin, String message) {
        final Map<String, Object> authorGh = authorLogin != null ? Map.of("login", authorLogin) : Map.of();
        return Map.of(
            "sha", sha,
            "author", authorGh,
            "commit", Map.of(
                "author", Map.of("date", "2026-06-02T14:00:00Z"),
                "message", message
            ),
            "parents", List.of(Map.of("sha", "aaa"))
        );
    }

    private Map<String, Object> prEvent(String eventId, String repo, int prNumber,
                                         String title, String headBranch, String action) {
        return Map.of(
            "id", eventId,
            "type", "PullRequestEvent",
            "created_at", "2026-06-02T14:00:00Z",
            "repo", Map.of("name", repo),
            "payload", Map.of(
                "action", action,
                "pull_request", Map.of(
                    "number", prNumber,
                    "title", title,
                    "merged", "merged".equals(action),
                    "head", Map.of("ref", headBranch)
                )
            )
        );
    }

    private GitHubRawEventEntity existingPrEntity(String eventId, String repo, String prNumber,
                                                   String title, String headBranch) {
        final GitHubRawEventEntity e = new GitHubRawEventEntity();
        e.setGithubEventId(eventId);
        e.setGithubUsername(LOGIN);
        e.setEventType("PullRequestEvent");
        e.setRepoName(repo);
        e.setAnchorType("PR");
        e.setAnchorId(prNumber);
        e.setAnchorTitle(title);
        e.setHeadBranch(headBranch);
        e.setEventIcon("🔀");
        e.setEventSummary("Opened PR #" + prNumber + (title.isEmpty() ? "" : ": " + title));
        e.setEventTimestamp(Instant.parse("2026-06-01T10:00:00Z"));
        return e;
    }

    // ── old-format commit cleanup ─────────────────────────────────────────────

    @Nested
    class OldFormatCleanup {

        @Test
        void ensureOldFormatCommitsAreDeletedBeforeSync() {
            stubRestGet(List.of()); // no events returned

            sut.syncUser(LOGIN, TOKEN);

            verify(repository).deleteOldFormatCommits(LOGIN, LOGIN + "_commit_%");
        }
    }

    // ── commit author filtering ───────────────────────────────────────────────

    @Nested
    class CommitAuthorFiltering {

        @SuppressWarnings("unchecked")
        @Test
        void ensureCommitsByOtherAuthorsAreNotStored() {
            final var push = pushEvent("evt1", "slint-ui/slint", "simon/feature", "head123", "prev456");
            final var otherCommit = commit("abc1234567890abcdef1234567890abcdef123456", "ogoffart", "Some commit");
            stubRestGet(List.of(push));

            // Second get() call returns compare result
            when(restClient.get())
                .thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
            when(requestHeadersUriSpec.uri(any(java.net.URI.class))).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(any(ParameterizedTypeReference.class)))
                .thenReturn(List.of(push))  // events
                .thenReturn(Map.of("commits", List.of(otherCommit)));  // compare

            sut.syncUser(LOGIN, TOKEN);

            verify(repository, never()).save(any(GitHubRawEventEntity.class));
        }

        @SuppressWarnings("unchecked")
        @Test
        void ensureCommitsWithNullAuthorLoginAreStored() {
            final String sha = "abc1234567890abcdef1234567890abcdef123456";
            final var commitData = Map.of(
                "sha", sha,
                "commit", Map.of(
                    "author", Map.of("date", "2026-06-02T14:00:00Z"),
                    "message", "My commit"
                ),
                "parents", List.of(Map.of("sha", "aaa"))
                // no "author" key → GitHub user not linked
            );
            final var push = pushEvent("evt1", "slint-ui/slint", "simon/feature", "head123", "prev456");

            when(restClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
            when(requestHeadersUriSpec.uri(any(java.net.URI.class))).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(any(ParameterizedTypeReference.class)))
                .thenReturn(List.of(push))
                .thenReturn(Map.of("commits", List.of(commitData)));

            when(repository.existsByGithubEventId(LOGIN + "_commit_" + sha)).thenReturn(false);

            sut.syncUser(LOGIN, TOKEN);

            final var captor = ArgumentCaptor.forClass(GitHubRawEventEntity.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getEventSummary()).isEqualTo("My commit");
        }
    }

    // ── commit event ID format ────────────────────────────────────────────────

    @Nested
    class CommitEventId {

        @SuppressWarnings("unchecked")
        @Test
        void ensureCommitEventIdUsesLoginAndFullSha() {
            final String sha = "abc1234567890abcdef1234567890abcdef123456";
            final var c = commit(sha, LOGIN, "Fix bug");
            final var push = pushEvent("evt1", "slint-ui/slint", "my-branch", "head123", "prev456");

            when(restClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
            when(requestHeadersUriSpec.uri(any(java.net.URI.class))).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(any(ParameterizedTypeReference.class)))
                .thenReturn(List.of(push))
                .thenReturn(Map.of("commits", List.of(c)));

            when(repository.existsByGithubEventId(LOGIN + "_commit_" + sha)).thenReturn(false);

            sut.syncUser(LOGIN, TOKEN);

            final var captor = ArgumentCaptor.forClass(GitHubRawEventEntity.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getGithubEventId()).isEqualTo(LOGIN + "_commit_" + sha);
        }
    }

    // ── PullRequestEvent headBranch extraction ────────────────────────────────

    @Nested
    class PullRequestHeadBranch {

        @SuppressWarnings("unchecked")
        @Test
        void ensureHeadBranchIsStoredFromPullRequestEvent() {
            final var pr = prEvent("evt-pr-1", "slint-ui/slint", 11940,
                "My feature", "nigel/my-feature", "opened");

            when(restClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(List.of(pr));

            when(repository.existsByGithubEventId("evt-pr-1")).thenReturn(false);

            sut.syncUser(LOGIN, TOKEN);

            final var captor = ArgumentCaptor.forClass(GitHubRawEventEntity.class);
            verify(repository).save(captor.capture());
            final GitHubRawEventEntity saved = captor.getValue();
            assertThat(saved.getAnchorType()).isEqualTo("PR");
            assertThat(saved.getHeadBranch()).isEqualTo("nigel/my-feature");
            assertThat(saved.getAnchorTitle()).isEqualTo("My feature");
        }

        @SuppressWarnings("unchecked")
        @Test
        void ensureMergedPrSummaryIncludesMergedVerb() {
            final var pr = prEvent("evt-pr-2", "slint-ui/slint", 11950,
                "Upgrade deps", "simon/upgrade", "merged");

            when(restClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(List.of(pr));

            when(repository.existsByGithubEventId("evt-pr-2")).thenReturn(false);

            sut.syncUser(LOGIN, TOKEN);

            final var captor = ArgumentCaptor.forClass(GitHubRawEventEntity.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getEventSummary()).startsWith("Merged PR #11950");
        }
    }

    // ── IssueCommentEvent routing ─────────────────────────────────────────────

    @Nested
    class IssueCommentEventRouting {

        @SuppressWarnings("unchecked")
        @Test
        void ensureIssueCommentOnPrStoredWithPrAnchorType() {
            final var event = Map.of(
                "id", "evt-ic-pr",
                "type", "IssueCommentEvent",
                "created_at", "2026-06-03T14:00:00Z",
                "repo", Map.of("name", "slint-ui/slint"),
                "payload", Map.of(
                    "action", "created",
                    "issue", Map.of(
                        "number", 11952,
                        "title", "Expose Keys API",
                        "pull_request", Map.of("url", "https://api.github.com/repos/slint-ui/slint/pulls/11952")
                    ),
                    "comment", Map.of("body", "Still missing from docs")
                )
            );

            when(restClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(List.of(event));
            when(repository.existsByGithubEventId("evt-ic-pr")).thenReturn(false);

            sut.syncUser(LOGIN, TOKEN);

            final var captor = ArgumentCaptor.forClass(GitHubRawEventEntity.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getAnchorType()).isEqualTo("PR");
            assertThat(captor.getValue().getAnchorId()).isEqualTo("11952");
            assertThat(captor.getValue().getEventSummary()).startsWith("Commented on PR #11952");
        }

        @SuppressWarnings("unchecked")
        @Test
        void ensureIssueCommentOnPlainIssueStoredWithIssueAnchorType() {
            final var event = Map.of(
                "id", "evt-ic-issue",
                "type", "IssueCommentEvent",
                "created_at", "2026-06-03T14:00:00Z",
                "repo", Map.of("name", "slint-ui/slint"),
                "payload", Map.of(
                    "action", "created",
                    "issue", Map.of(
                        "number", 11876,
                        "title", "Skia Vulkan error"
                        // no "pull_request" key → plain issue
                    ),
                    "comment", Map.of("body", "Works for me on Windows")
                )
            );

            when(restClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(List.of(event));
            when(repository.existsByGithubEventId("evt-ic-issue")).thenReturn(false);

            sut.syncUser(LOGIN, TOKEN);

            final var captor = ArgumentCaptor.forClass(GitHubRawEventEntity.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getAnchorType()).isEqualTo("ISSUE");
            assertThat(captor.getValue().getEventSummary()).startsWith("Commented on issue #11876");
        }
    }

    // ── backfill for existing entities ────────────────────────────────────────

    @Nested
    class BackfillExistingEntities {

        @SuppressWarnings("unchecked")
        @Test
        void ensureBlankTitleIsBackfilledForExistingPrEntity() {
            // Existing PR entity with blank title and no headBranch
            final var existing = existingPrEntity("evt-pr-old", "slint-ui/slint", "11940", "", null);
            final var events = List.of(Map.of(
                "id", "evt-pr-old", "type", "PullRequestEvent",
                "created_at", "2026-06-02T14:00:00Z",
                "repo", Map.of("name", "slint-ui/slint"),
                "payload", Map.of(
                    "action", "opened",
                    "pull_request", Map.of(
                        "number", 11940, "title", "", "merged", false,
                        "head", Map.of("ref", "nigel/feature")
                    )
                )
            ));
            // PR details API response
            final Map<String, Object> prDetails = Map.of(
                "title", "Very simple remote viewer screen",
                "head", Map.of("ref", "nigel/feature")
            );

            when(restClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
            when(requestHeadersUriSpec.uri(any(java.net.URI.class))).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(any(ParameterizedTypeReference.class)))
                .thenReturn(events)    // fetchEvents
                .thenReturn(prDetails); // enrichPrDetails → GET /pulls/11940

            when(repository.findByGithubEventId("evt-pr-old")).thenReturn(Optional.of(existing));

            sut.syncUser(LOGIN, TOKEN);

            final var captor = ArgumentCaptor.forClass(GitHubRawEventEntity.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getAnchorTitle()).isEqualTo("Very simple remote viewer screen");
            assertThat(captor.getValue().getHeadBranch()).isEqualTo("nigel/feature");
        }

        @SuppressWarnings("unchecked")
        @Test
        void ensureMissingHeadBranchIsBackfilledEvenWhenTitleAlreadyPresent() {
            final var existing = existingPrEntity("evt-pr-old2", "slint-ui/slint", "11940",
                "Existing title", null);
            final var events = List.of(Map.of(
                "id", "evt-pr-old2", "type", "PullRequestEvent",
                "created_at", "2026-06-02T14:00:00Z",
                "repo", Map.of("name", "slint-ui/slint"),
                "payload", Map.of(
                    "action", "opened",
                    "pull_request", Map.of(
                        "number", 11940, "title", "Existing title", "merged", false,
                        "head", Map.of("ref", "nigel/feature")
                    )
                )
            ));
            final Map<String, Object> prDetails = Map.of(
                "title", "Existing title",
                "head", Map.of("ref", "nigel/feature")
            );

            when(restClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
            when(requestHeadersUriSpec.uri(any(java.net.URI.class))).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(any(ParameterizedTypeReference.class)))
                .thenReturn(events)
                .thenReturn(prDetails);

            when(repository.findByGithubEventId("evt-pr-old2")).thenReturn(Optional.of(existing));

            sut.syncUser(LOGIN, TOKEN);

            final var captor = ArgumentCaptor.forClass(GitHubRawEventEntity.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getHeadBranch()).isEqualTo("nigel/feature");
        }
    }
}
