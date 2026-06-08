package de.focusshift.zeiterfassung.gitactivity;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class GitHubActivityProviderTest {

    private static final String LOGIN = "tronical";
    private static final String TOKEN = "test-token";

    @Mock private GitActivityRawEventRepository repository;
    @Mock private UserSettingsService userSettingsService;
    @Mock private RestClient restClient;
    @Mock @SuppressWarnings("rawtypes") private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock @SuppressWarnings("rawtypes") private RestClient.RequestHeadersSpec requestHeadersSpec;
    @Mock private RestClient.ResponseSpec responseSpec;

    private GitHubActivityProvider sut;

    @BeforeEach
    void setUp() {
        sut = new GitHubActivityProvider(repository, userSettingsService, restClient);
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
        return commitWithDates(sha, authorLogin, message, "2026-06-02T14:00:00Z", "2026-06-02T14:00:00Z");
    }

    private Map<String, Object> commitWithDates(String sha, String authorLogin, String message,
                                                 String authorDate, String committerDate) {
        final Map<String, Object> authorGh = authorLogin != null ? Map.of("login", authorLogin) : Map.of();
        return Map.of(
            "sha", sha,
            "author", authorGh,
            "commit", Map.of(
                "author",    Map.of("date", authorDate),
                "committer", Map.of("date", committerDate),
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

    private Map<String, Object> crossForkPrEvent(String eventId, String baseRepo, int prNumber,
                                                  String title, String headBranch, String forkRepo) {
        return Map.of(
            "id", eventId,
            "type", "PullRequestEvent",
            "created_at", "2026-06-02T14:00:00Z",
            "repo", Map.of("name", baseRepo),
            "payload", Map.of(
                "action", "opened",
                "pull_request", Map.of(
                    "number", prNumber,
                    "title", title,
                    "merged", false,
                    "head", Map.of("ref", headBranch, "repo", Map.of("full_name", forkRepo))
                )
            )
        );
    }

    private GitActivityRawEventEntity existingPrEntity(String eventId, String repo, String prNumber,
                                                        String title, String headBranch) {
        final GitActivityRawEventEntity e = new GitActivityRawEventEntity();
        e.setPlatformEventId(eventId);
        e.setPlatformUsername(LOGIN);
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

    private List<Map<String, Object>> mockEventPage(int count, int firstId) {
        final List<Map<String, Object>> page = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            page.add(Map.of(
                "id", "evt-" + (firstId + i),
                "type", "UnknownEvent",
                "created_at", "2026-06-02T14:00:00Z",
                "repo", Map.of("name", "slint-ui/slint"),
                "payload", Map.of()
            ));
        }
        return page;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void stubPages(List<Map<String, Object>>... pages) {
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        var stub = when(responseSpec.body(any(ParameterizedTypeReference.class)));
        for (List<Map<String, Object>> page : pages) {
            stub = stub.thenReturn(page);
        }
    }

    // ── events pagination ─────────────────────────────────────────────────────

    @Nested
    class EventsPagination {

        @Test
        void ensureSinglePageFetchedWhenFewerThan100Events() {
            stubPages(mockEventPage(3, 0));

            sut.syncUser(LOGIN, TOKEN);

            verify(restClient, times(1)).get();
        }

        @Test
        void ensureSinglePageFetchedWhenPageIsEmpty() {
            stubPages(List.of());

            sut.syncUser(LOGIN, TOKEN);

            verify(restClient, times(1)).get();
        }

        @SuppressWarnings("unchecked")
        @Test
        void ensureSecondPageFetchedWhenFirstPageIsFull() {
            stubPages(
                mockEventPage(100, 0),
                mockEventPage(30, 100)
            );

            sut.syncUser(LOGIN, TOKEN);

            verify(restClient, times(2)).get();
        }

        @SuppressWarnings("unchecked")
        @Test
        void ensureAllThreePagesFetchedOnFirstSync() {
            stubPages(
                mockEventPage(100, 0),
                mockEventPage(100, 100),
                mockEventPage(50, 200)
            );

            sut.syncUser(LOGIN, TOKEN);

            verify(restClient, times(3)).get();
        }

        @SuppressWarnings("unchecked")
        @Test
        void ensureAtMostThreePagesEvenWhenAllFull() {
            stubPages(
                mockEventPage(100, 0),
                mockEventPage(100, 100),
                mockEventPage(100, 200)
            );

            sut.syncUser(LOGIN, TOKEN);

            verify(restClient, times(3)).get();
        }

        @SuppressWarnings("unchecked")
        @Test
        void ensurePaginationStopsAtFirstKnownEventId() {
            final List<Map<String, Object>> page1 = new ArrayList<>(mockEventPage(3, 0));
            page1.add(Map.of("id", "known-evt", "type", "UnknownEvent",
                "created_at", "2026-06-01T09:00:00Z", "repo", Map.of("name", "r"), "payload", Map.of()));
            page1.addAll(mockEventPage(96, 4));

            when(repository.existsByPlatformEventId("known-evt")).thenReturn(true);
            stubPages(page1);

            sut.syncUser(LOGIN, TOKEN);

            verify(restClient, times(1)).get();
        }

        @Test
        void ensureEventsAfterKnownIdAreNotExamined() {
            final var new1  = Map.of("id", "new-1",    "type", "UnknownEvent",
                "created_at", "2026-06-02T12:00:00Z", "repo", Map.of("name", "r"), "payload", Map.of());
            final var new2  = Map.of("id", "new-2",    "type", "UnknownEvent",
                "created_at", "2026-06-02T11:00:00Z", "repo", Map.of("name", "r"), "payload", Map.of());
            final var known = Map.of("id", "known-evt", "type", "UnknownEvent",
                "created_at", "2026-06-01T10:00:00Z", "repo", Map.of("name", "r"), "payload", Map.of());
            final var old1  = Map.of("id", "old-1",    "type", "UnknownEvent",
                "created_at", "2026-06-01T09:00:00Z", "repo", Map.of("name", "r"), "payload", Map.of());

            when(repository.existsByPlatformEventId("known-evt")).thenReturn(true);
            stubPages(List.of(new1, new2, known, old1));

            sut.syncUser(LOGIN, TOKEN);

            verify(repository).existsByPlatformEventId("new-1");
            verify(repository).existsByPlatformEventId("new-2");
            verify(repository).existsByPlatformEventId("known-evt");
            verify(repository, never()).existsByPlatformEventId("old-1");
        }
    }

    // ── old-format commit cleanup ─────────────────────────────────────────────

    @Nested
    class OldFormatCleanup {

        @Test
        void ensureOldFormatCommitsAreDeletedBeforeSync() {
            stubRestGet(List.of());

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

            when(restClient.get())
                .thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
            when(requestHeadersUriSpec.uri(any(java.net.URI.class))).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(any(ParameterizedTypeReference.class)))
                .thenReturn(List.of(push))
                .thenReturn(Map.of("commits", List.of(otherCommit)));

            sut.syncUser(LOGIN, TOKEN);

            verify(repository, never()).save(any(GitActivityRawEventEntity.class));
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

            when(repository.existsByPlatformEventId(LOGIN + "_commit_" + sha)).thenReturn(false);

            sut.syncUser(LOGIN, TOKEN);

            final var captor = ArgumentCaptor.forClass(GitActivityRawEventEntity.class);
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

            when(repository.existsByPlatformEventId(LOGIN + "_commit_" + sha)).thenReturn(false);

            sut.syncUser(LOGIN, TOKEN);

            final var captor = ArgumentCaptor.forClass(GitActivityRawEventEntity.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getPlatformEventId()).isEqualTo(LOGIN + "_commit_" + sha);
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

            when(repository.existsByPlatformEventId("evt-pr-1")).thenReturn(false);

            sut.syncUser(LOGIN, TOKEN);

            final var captor = ArgumentCaptor.forClass(GitActivityRawEventEntity.class);
            verify(repository).save(captor.capture());
            final GitActivityRawEventEntity saved = captor.getValue();
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

            when(repository.existsByPlatformEventId("evt-pr-2")).thenReturn(false);

            sut.syncUser(LOGIN, TOKEN);

            final var captor = ArgumentCaptor.forClass(GitActivityRawEventEntity.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getEventSummary()).startsWith("Merged PR #11950");
        }

        @SuppressWarnings("unchecked")
        @Test
        void ensureHeadRepoNameStoredForCrossForkPR() {
            final var pr = crossForkPrEvent("evt-pr-fork", "slint-ui/tree-sitter-slint", 1,
                "Fix grammar", "fix-tree-sitter-grammar", "LeonMatthes/slint");

            when(restClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(List.of(pr));
            when(repository.existsByPlatformEventId("evt-pr-fork")).thenReturn(false);

            sut.syncUser(LOGIN, TOKEN);

            final var captor = ArgumentCaptor.forClass(GitActivityRawEventEntity.class);
            verify(repository).save(captor.capture());
            final GitActivityRawEventEntity saved = captor.getValue();
            assertThat(saved.getRepoName()).isEqualTo("slint-ui/tree-sitter-slint");
            assertThat(saved.getHeadBranch()).isEqualTo("fix-tree-sitter-grammar");
            assertThat(saved.getHeadRepoName()).isEqualTo("LeonMatthes/slint");
        }

        @SuppressWarnings("unchecked")
        @Test
        void ensureHeadRepoNameNullForSameRepoPR() {
            final var pr = crossForkPrEvent("evt-pr-same", "slint-ui/slint", 11940,
                "My feature", "nigel/my-feature", "slint-ui/slint");

            when(restClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(List.of(pr));
            when(repository.existsByPlatformEventId("evt-pr-same")).thenReturn(false);

            sut.syncUser(LOGIN, TOKEN);

            final var captor = ArgumentCaptor.forClass(GitActivityRawEventEntity.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getHeadRepoName()).isNull();
        }

        @SuppressWarnings("unchecked")
        @Test
        void ensureHeadRepoNameNullWhenHeadRepoAbsentFromPayload() {
            final var pr = prEvent("evt-pr-noheadrepo", "slint-ui/slint", 11940,
                "My feature", "nigel/my-feature", "opened");

            when(restClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(List.of(pr));
            when(repository.existsByPlatformEventId("evt-pr-noheadrepo")).thenReturn(false);

            sut.syncUser(LOGIN, TOKEN);

            final var captor = ArgumentCaptor.forClass(GitActivityRawEventEntity.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getHeadRepoName()).isNull();
        }
    }

    // ── Committer date for force-push / rebase tracking ──────────────────────

    @Nested
    class CommitterDateTracking {

        @SuppressWarnings("unchecked")
        @Test
        void ensureCommitterDateIsUsedAsTimestampNotAuthorDate() {
            final String sha = "abc1234567890abcdef1234567890abcdef123456";
            final var rebased = commitWithDates(sha, LOGIN, "Implement DragIcon",
                "2026-06-02T20:06:46Z", "2026-06-03T19:25:03Z");
            final var push = pushEvent("evt1", "slint-ui/winit", "simon/win32-drag", "head123", "prev456");

            when(restClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
            when(requestHeadersUriSpec.uri(any(java.net.URI.class))).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(any(ParameterizedTypeReference.class)))
                .thenReturn(List.of(push))
                .thenReturn(Map.of("commits", List.of(rebased)));
            when(repository.existsByPlatformEventId(LOGIN + "_commit_" + sha)).thenReturn(false);
            when(repository.findByPlatformEventId(LOGIN + "_commit_" + sha)).thenReturn(java.util.Optional.empty());

            sut.syncUser(LOGIN, TOKEN);

            final var captor = ArgumentCaptor.forClass(GitActivityRawEventEntity.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getEventTimestamp())
                .isEqualTo(java.time.Instant.parse("2026-06-03T19:25:03Z"));
        }

        @SuppressWarnings("unchecked")
        @Test
        void ensureExistingEntityTimestampUpdatedWhenCommitterDateChanges() {
            final String sha = "abc1234567890abcdef1234567890abcdef123456";
            final GitActivityRawEventEntity existing = new GitActivityRawEventEntity();
            existing.setPlatformEventId(LOGIN + "_commit_" + sha);
            existing.setPlatformUsername(LOGIN);
            existing.setEventType("PushEvent");
            existing.setRepoName("slint-ui/winit");
            existing.setAnchorType("REPO");
            existing.setEventIcon("📝");
            existing.setEventSummary("Implement DragIcon");
            existing.setEventTimestamp(java.time.Instant.parse("2026-06-02T20:06:46Z"));

            final var rebased = commitWithDates(sha, LOGIN, "Implement DragIcon",
                "2026-06-02T20:06:46Z", "2026-06-03T19:25:03Z");
            final var push = pushEvent("evt1", "slint-ui/winit", "simon/win32-drag", "head123", "prev456");

            when(restClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
            when(requestHeadersUriSpec.uri(any(java.net.URI.class))).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(any(ParameterizedTypeReference.class)))
                .thenReturn(List.of(push))
                .thenReturn(Map.of("commits", List.of(rebased)));
            when(repository.findByPlatformEventId(LOGIN + "_commit_" + sha))
                .thenReturn(java.util.Optional.of(existing));

            sut.syncUser(LOGIN, TOKEN);

            final var captor = ArgumentCaptor.forClass(GitActivityRawEventEntity.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getEventTimestamp())
                .isEqualTo(java.time.Instant.parse("2026-06-03T19:25:03Z"));
        }
    }

    // ── Open PR daily commit sync ─────────────────────────────────────────────

    @Nested
    class OpenPrCommitSync {

        private GitActivityRawEventEntity openPrEntity(String repoName, String prNumber, String headBranch) {
            final GitActivityRawEventEntity e = new GitActivityRawEventEntity();
            e.setPlatformEventId("evt-pr-" + prNumber);
            e.setPlatformUsername(LOGIN);
            e.setEventType("PullRequestEvent");
            e.setRepoName(repoName);
            e.setAnchorType("PR");
            e.setAnchorId(prNumber);
            e.setAnchorTitle("My PR");
            e.setHeadBranch(headBranch);
            e.setEventIcon("🔀");
            e.setEventSummary("Opened PR #" + prNumber + ": My PR");
            e.setEventTimestamp(Instant.parse("2026-06-01T10:00:00Z"));
            return e;
        }

        private GitActivityRawEventEntity mergedPrEntity(String repoName, String prNumber) {
            final GitActivityRawEventEntity e = openPrEntity(repoName, prNumber, "feature/x");
            e.setEventSummary("Merged PR #" + prNumber + ": My PR");
            return e;
        }

        @SuppressWarnings("unchecked")
        @Test
        void ensureCommitsAreFetchedForOpenPr() {
            final String sha = "abc1234567890abcdef1234567890abcdef123456";
            final var prEntity = openPrEntity("slint-ui/slint", "11940", "nigel/my-feature");
            when(repository.findByPlatformUsernameAndAnchorTypeAndEventType(LOGIN, "PR", "PullRequestEvent"))
                .thenReturn(List.of(prEntity));
            when(repository.findByPlatformEventId(LOGIN + "_commit_" + sha)).thenReturn(java.util.Optional.empty());

            when(restClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
            when(requestHeadersUriSpec.uri(any(java.net.URI.class))).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(any(ParameterizedTypeReference.class)))
                .thenReturn(List.of())
                .thenReturn(List.of(commitWithDates(sha, LOGIN, "Fix crash",
                    "2026-06-02T14:00:00Z", "2026-06-03T19:25:00Z")));

            sut.syncUser(LOGIN, TOKEN);

            final var captor = ArgumentCaptor.forClass(GitActivityRawEventEntity.class);
            verify(repository).save(captor.capture());
            final GitActivityRawEventEntity saved = captor.getValue();
            assertThat(saved.getAnchorId()).isEqualTo("nigel/my-feature");
            assertThat(saved.getEventSummary()).isEqualTo("Fix crash");
            assertThat(saved.getEventTimestamp()).isEqualTo(Instant.parse("2026-06-03T19:25:00Z"));
        }

        @Test
        void ensureCommitsAreNotFetchedForMergedPr() {
            final var prEntity = mergedPrEntity("slint-ui/slint", "11950");
            when(repository.findByPlatformUsernameAndAnchorTypeAndEventType(LOGIN, "PR", "PullRequestEvent"))
                .thenReturn(List.of(prEntity));
            stubRestGet(List.of());

            sut.syncUser(LOGIN, TOKEN);

            verify(repository, never()).save(any(GitActivityRawEventEntity.class));
        }

        @Test
        void ensureCommitsAreNotFetchedWhenRateLimitLow() {
            final var prEntity = openPrEntity("slint-ui/slint", "11940", "nigel/my-feature");
            when(repository.findByPlatformUsernameAndAnchorTypeAndEventType(LOGIN, "PR", "PullRequestEvent"))
                .thenReturn(List.of(prEntity));
            stubRestGet(List.of());

            sut.setRateLimitRemaining(50);

            sut.syncUser(LOGIN, TOKEN);

            verify(repository, never()).save(any(GitActivityRawEventEntity.class));
        }

        @Test
        void ensureCommitsAreSkippedWhenHeadBranchNotSet() {
            final var prEntity = openPrEntity("slint-ui/slint", "11940", null);
            when(repository.findByPlatformUsernameAndAnchorTypeAndEventType(LOGIN, "PR", "PullRequestEvent"))
                .thenReturn(List.of(prEntity));
            stubRestGet(List.of());

            sut.syncUser(LOGIN, TOKEN);

            verify(repository, never()).save(any(GitActivityRawEventEntity.class));
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
            when(repository.existsByPlatformEventId("evt-ic-pr")).thenReturn(false);

            sut.syncUser(LOGIN, TOKEN);

            final var captor = ArgumentCaptor.forClass(GitActivityRawEventEntity.class);
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
                    ),
                    "comment", Map.of("body", "Works for me on Windows")
                )
            );

            when(restClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(List.of(event));
            when(repository.existsByPlatformEventId("evt-ic-issue")).thenReturn(false);

            sut.syncUser(LOGIN, TOKEN);

            final var captor = ArgumentCaptor.forClass(GitActivityRawEventEntity.class);
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
                .thenReturn(events)
                .thenReturn(prDetails);

            when(repository.findByPlatformEventId("evt-pr-old")).thenReturn(Optional.of(existing));

            sut.syncUser(LOGIN, TOKEN);

            final var captor = ArgumentCaptor.forClass(GitActivityRawEventEntity.class);
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

            when(repository.findByPlatformEventId("evt-pr-old2")).thenReturn(Optional.of(existing));

            sut.syncUser(LOGIN, TOKEN);

            final var captor = ArgumentCaptor.forClass(GitActivityRawEventEntity.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getHeadBranch()).isEqualTo("nigel/feature");
        }

        @SuppressWarnings("unchecked")
        @Test
        void ensureHeadRepoNameBackfilledDuringEnrichmentForCrossForkPR() {
            final var existing = existingPrEntity("evt-pr-fork-old", "slint-ui/tree-sitter-slint", "1", "", null);
            final var events = List.of(Map.of(
                "id", "evt-pr-fork-old", "type", "PullRequestEvent",
                "created_at", "2026-06-02T14:00:00Z",
                "repo", Map.of("name", "slint-ui/tree-sitter-slint"),
                "payload", Map.of(
                    "action", "opened",
                    "pull_request", Map.of(
                        "number", 1, "title", "", "merged", false,
                        "head", Map.of("ref", "fix-tree-sitter-grammar")
                    )
                )
            ));
            final Map<String, Object> prDetails = Map.of(
                "title", "Fix grammar",
                "head", Map.of(
                    "ref", "fix-tree-sitter-grammar",
                    "repo", Map.of("full_name", "LeonMatthes/slint")
                )
            );

            when(restClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
            when(requestHeadersUriSpec.uri(any(java.net.URI.class))).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(any(ParameterizedTypeReference.class)))
                .thenReturn(events)
                .thenReturn(prDetails);

            when(repository.findByPlatformEventId("evt-pr-fork-old")).thenReturn(Optional.of(existing));

            sut.syncUser(LOGIN, TOKEN);

            final var captor = ArgumentCaptor.forClass(GitActivityRawEventEntity.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getHeadBranch()).isEqualTo("fix-tree-sitter-grammar");
            assertThat(captor.getValue().getHeadRepoName()).isEqualTo("LeonMatthes/slint");
        }
    }
}
