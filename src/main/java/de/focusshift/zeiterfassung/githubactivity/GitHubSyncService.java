package de.focusshift.zeiterfassung.githubactivity;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import de.focusshift.zeiterfassung.user.UserSettingsService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Fetches GitHub activity for all users with a verified GitHub login and
 * persists it to {@link GitHubRawEventEntity} for display on the activity page.
 *
 * Authentication uses a GitHub App installed on the organization — no per-user
 * tokens are required. The App needs Read-only access to:
 * Metadata, Contents, Issues, Pull Requests.
 *
 * Required application properties:
 *   github.app.id            — numeric App ID from the GitHub App settings page
 *   github.app.private-key-path — absolute path to the downloaded .pem file
 *   github.organization      — organization login name (e.g. "slint-ui")
 */
@Service
public class GitHubSyncService {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    @Value("${github.app.id:}")
    private String appId;

    @Value("${github.app.private-key-path:}")
    private String privateKeyPath;

    @Value("${github.organization:}")
    private String orgName;

    /** Stop making extra API calls when fewer than this many requests remain in the current window. */
    @Value("${github.rate-limit.safety-threshold:200}")
    private int rateLimitSafetyThreshold;

    private final GitHubRawEventRepository repository;
    private final UserSettingsService userSettingsService;
    private final RestClient restClient;

    private volatile String cachedInstallationToken;
    private volatile Instant tokenExpiry = Instant.MIN;

    private final java.util.concurrent.ConcurrentHashMap<String, Instant> lastSyncTimes = new java.util.concurrent.ConcurrentHashMap<>();

    // Rate limit state — updated from response headers on every API call
    private final java.util.concurrent.atomic.AtomicInteger rateLimitRemaining =
        new java.util.concurrent.atomic.AtomicInteger(5000);
    private volatile Instant rateLimitReset = Instant.MIN;

    public Instant getLastSyncTime(String login) { return lastSyncTimes.get(login); }
    public int getRateLimitRemaining() { return rateLimitRemaining.get(); }
    public Instant getRateLimitReset() { return rateLimitReset; }
    public boolean isRateLimitSafe() { return rateLimitRemaining.get() > rateLimitSafetyThreshold; }

    // package-private for testing
    void setRateLimitRemaining(int remaining) { this.rateLimitRemaining.set(remaining); }

    @org.springframework.beans.factory.annotation.Autowired
    GitHubSyncService(GitHubRawEventRepository repository, UserSettingsService userSettingsService) {
        this.repository = repository;
        this.userSettingsService = userSettingsService;
        this.restClient = RestClient.builder()
            .defaultHeader("User-Agent", "zeiterfassung-github-sync")
            .defaultHeader("Accept", "application/vnd.github+json")
            .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
            .requestInterceptor((request, body, execution) -> {
                final org.springframework.http.client.ClientHttpResponse response = execution.execute(request, body);
                updateRateLimitFromHeaders(response.getHeaders());
                return response;
            })
            .build();
    }

    // package-private for testing — allows injecting a mock RestClient
    GitHubSyncService(GitHubRawEventRepository repository, UserSettingsService userSettingsService, RestClient restClient) {
        this.repository = repository;
        this.userSettingsService = userSettingsService;
        this.restClient = restClient;
    }

    private void updateRateLimitFromHeaders(org.springframework.http.HttpHeaders headers) {
        try {
            final String remaining = headers.getFirst("X-RateLimit-Remaining");
            if (remaining != null) rateLimitRemaining.set(Integer.parseInt(remaining));
            final String reset = headers.getFirst("X-RateLimit-Reset");
            if (reset != null) rateLimitReset = Instant.ofEpochSecond(Long.parseLong(reset));
        } catch (NumberFormatException ignored) {}
    }

    @Scheduled(fixedDelay = 600_000)
    @SchedulerLock(name = "githubActivitySync", lockAtMostFor = "PT9M", lockAtLeastFor = "PT30S")
    void sync() {
        if (!isConfigured()) {
            LOG.debug("GitHub App not configured (github.app.id / github.app.private-key-path / github.organization), skipping sync");
            return;
        }

        final List<String> logins = userSettingsService.findAllVerifiedGithubLogins();
        if (logins.isEmpty()) {
            return;
        }

        final String token;
        try {
            token = getInstallationToken();
        } catch (Exception e) {
            LOG.error("Failed to obtain GitHub App installation token", e);
            return;
        }

        for (String login : logins) {
            try {
                syncUser(login, token);
            } catch (Exception e) {
                LOG.error("Failed to sync GitHub activity for user {}", login, e);
            }
        }
    }

    /** Immediately syncs activity for a single user. Called on demand from the UI. */
    void syncNow(String login) {
        if (!isConfigured()) {
            LOG.warn("GitHub App not configured, cannot sync now for user {}", login);
            return;
        }
        try {
            syncUser(login, getInstallationToken());
        } catch (Exception e) {
            LOG.error("On-demand sync failed for user {}", login, e);
        }
    }

    public boolean isConfigured() {
        return !appId.isBlank() && !privateKeyPath.isBlank() && !orgName.isBlank();
    }

    /** Returns a human-readable description of which env vars are still missing, or empty string if fully configured. */
    public String missingConfig() {
        final List<String> missing = new java.util.ArrayList<>();
        if (appId.isBlank())         missing.add("GITHUB_APP_ID");
        if (privateKeyPath.isBlank()) missing.add("GITHUB_APP_PRIVATE_KEY");
        if (orgName.isBlank())        missing.add("GITHUB_ORGANIZATION");
        return String.join(", ", missing);
    }

    @SuppressWarnings("unchecked")
    void syncUser(String login, String token) {
        // Remove any commit entities stored in the old format ({pushEventId}_{shortSha}).
        // They will be re-synced immediately below using the new login_commit_sha format,
        // which also applies the author filter so commits from other users are excluded.
        final int removed = repository.deleteOldFormatCommits(login, login + "_commit_%");
        if (removed > 0) {
            LOG.info("Removed {} old-format commit entities for {} — will re-sync with author filter", removed, login);
        }

        final List<Map<String, Object>> events = fetchEvents(login, token);
        int saved = 0;
        for (Map<String, Object> raw : events) {
            final String eventId = (String) raw.get("id");
            final String type = (String) raw.get("type");
            if (eventId == null || type == null) continue;

            if ("PushEvent".equals(type)) {
                for (GitHubRawEventEntity e : parsePushToEntities(raw, login, token)) {
                    final java.util.Optional<GitHubRawEventEntity> existingCommit =
                        repository.findByGithubEventId(e.getGithubEventId());
                    if (existingCommit.isPresent()) {
                        // Re-timestamp if the committer date changed (force-push / rebase).
                        // This ensures the activity appears on the day the rebase actually happened.
                        final GitHubRawEventEntity ex = existingCommit.get();
                        if (!ex.getEventTimestamp().equals(e.getEventTimestamp())) {
                            ex.setEventTimestamp(e.getEventTimestamp());
                            repository.save(ex);
                        }
                    } else {
                        repository.save(e);
                        saved++;
                    }
                }
            } else {
                final java.util.Optional<GitHubRawEventEntity> existing = repository.findByGithubEventId(eventId);
                if (existing.isPresent()) {
                    // Backfill title and/or headBranch for already-stored events that are missing them
                    final GitHubRawEventEntity e = existing.get();
                    final boolean missingTitle = e.getAnchorTitle() == null || e.getAnchorTitle().isBlank();
                    final boolean missingHeadBranch = "PR".equals(e.getAnchorType())
                        && (e.getHeadBranch() == null || e.getHeadBranch().isBlank());
                    if (missingTitle || missingHeadBranch) {
                        enrichPrDetails(e, token);
                        repository.save(e);
                    }
                    continue;
                }
                final GitHubRawEventEntity entity = parseToEntity(raw, login);
                if (entity != null) {
                    enrichPrDetails(entity, token);
                    repository.save(entity);
                    saved++;
                }
            }
        }
        lastSyncTimes.put(login, Instant.now());
        if (saved > 0) {
            LOG.info("Synced {} new GitHub event(s) for user {}", saved, login);
        }

        // After processing the Events API feed, ensure every open PR's commits are stored.
        // This covers days where the user only pushed (no PullRequestEvent in the feed),
        // force-push re-datings, and history predating the sync setup.
        syncOpenPrCommits(login, token);
    }

    /**
     * For each currently open PR known in the DB, fetches its full commit list and stores
     * any commits by {@code login} that are not yet recorded — using committer date so that
     * force-push days are attributed correctly.
     */
    @SuppressWarnings("unchecked")
    private void syncOpenPrCommits(String login, String token) {
        // Load all PullRequestEvent entities for this user, then group by repo+PR number
        // keeping the most recent event per PR to determine open/closed status.
        final List<GitHubRawEventEntity> allPrEvents = repository
            .findByGithubUsernameAndAnchorTypeAndEventType(login, "PR", "PullRequestEvent");

        final Map<String, GitHubRawEventEntity> latestPerPr = new java.util.LinkedHashMap<>();
        for (GitHubRawEventEntity e : allPrEvents) {
            final String key = e.getRepoName() + "|" + e.getAnchorId();
            final GitHubRawEventEntity existing = latestPerPr.get(key);
            if (existing == null || e.getEventTimestamp().isAfter(existing.getEventTimestamp())) {
                latestPerPr.put(key, e);
            }
        }

        for (GitHubRawEventEntity pr : latestPerPr.values()) {
            // Skip closed / merged PRs — their history is done
            final String summary = pr.getEventSummary();
            if (summary.startsWith("Merged") || summary.startsWith("Closed")) continue;

            // Skip if we don't know which branch to attach commits to
            final String headBranch = pr.getHeadBranch();
            if (headBranch == null || headBranch.isBlank()) {
                LOG.debug("Skipping open PR commit sync for {}/{} — headBranch not set yet",
                    pr.getRepoName(), pr.getAnchorId());
                continue;
            }

            if (!isRateLimitSafe()) {
                LOG.warn("Rate limit low ({} remaining) — stopping open PR commit sync", rateLimitRemaining.get());
                break;
            }

            fetchAndStorePrCommits(login, pr.getRepoName(), pr.getAnchorId(), headBranch, token);
        }
    }

    @SuppressWarnings("unchecked")
    private void fetchAndStorePrCommits(String login, String repoName,
                                         String prNumber, String headBranch, String token) {
        try {
            final String url = "https://api.github.com/repos/" + repoName
                + "/pulls/" + prNumber + "/commits?per_page=100";
            final List<Map<String, Object>> commits = restClient.get()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            if (commits == null) return;

            for (Map<String, Object> commit : commits) {
                final String sha = strOrEmpty(commit.get("sha"));
                if (sha.isEmpty()) continue;

                // Author filter — skip commits by other contributors
                final Map<String, Object> authorGh = (Map<String, Object>) commit.get("author");
                final String authorLogin = authorGh != null ? strOrEmpty(authorGh.get("login")) : "";
                if (!authorLogin.isEmpty() && !authorLogin.equals(login)) continue;

                final Map<String, Object> commitData = (Map<String, Object>) commit.get("commit");
                if (commitData == null) continue;

                // Use committer date so force-push days are correctly attributed
                final Map<String, Object> committerData = (Map<String, Object>) commitData.get("committer");
                final Map<String, Object> authorData = (Map<String, Object>) commitData.get("author");
                final String dateStr = committerData != null && committerData.get("date") != null
                    ? (String) committerData.get("date")
                    : authorData != null ? (String) authorData.get("date") : null;
                if (dateStr == null) continue;

                final Instant ts = Instant.parse(dateStr);
                final String eventId = login + "_commit_" + sha;
                final java.util.Optional<GitHubRawEventEntity> existing = repository.findByGithubEventId(eventId);

                if (existing.isPresent()) {
                    // Re-timestamp if committer date changed (force push)
                    final GitHubRawEventEntity ex = existing.get();
                    if (!ex.getEventTimestamp().equals(ts)) {
                        ex.setEventTimestamp(ts);
                        repository.save(ex);
                    }
                } else {
                    final String message = firstLine(strOrEmpty(commitData.get("message")), 200);
                    final String shortSha = sha.substring(0, Math.min(7, sha.length()));
                    final GitHubRawEventEntity e = new GitHubRawEventEntity();
                    e.setGithubEventId(eventId);
                    e.setGithubUsername(login);
                    e.setEventType("PushEvent");
                    e.setRepoName(repoName);
                    e.setAnchorType("REPO");
                    e.setAnchorId(headBranch);
                    e.setAnchorTitle(headBranch);
                    e.setEventIcon("📝");
                    e.setEventSummary(message.isEmpty() ? shortSha : message);
                    e.setEventTimestamp(ts);
                    repository.save(e);
                }
            }
        } catch (Exception ex) {
            LOG.warn("Could not fetch commits for open PR {}/{}: {}", repoName, prNumber, ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<GitHubRawEventEntity> parsePushToEntities(Map<String, Object> raw, String login, String token) {
        final String eventId = (String) raw.get("id");
        final String createdAt = (String) raw.get("created_at");
        if (createdAt == null) return List.of();

        final Map<String, Object> repoMap = (Map<String, Object>) raw.get("repo");
        final String repoName = repoMap != null ? (String) repoMap.get("name") : null;
        if (repoName == null) return List.of();

        final Map<String, Object> payload = raw.get("payload") instanceof Map<?, ?> m
            ? (Map<String, Object>) m : Map.of();

        final String ref = strOrEmpty(payload.get("ref"));
        final String branch = !ref.isEmpty() ? ref.replaceFirst("^refs/heads/", "") : "";
        final String head = strOrEmpty(payload.get("head"));
        final String before = strOrEmpty(payload.get("before"));
        final Instant pushTime = Instant.parse(createdAt);

        if (head.isEmpty()) return List.of();

        // GitHub App tokens strip commit details from the Events API payload.
        // Use the Compare API to recover the actual commit messages.
        final List<Map<String, Object>> commits = fetchCommitsInPush(repoName, before, head, token);
        final boolean compareApiReturnedCommits = !commits.isEmpty();

        final List<GitHubRawEventEntity> result = new ArrayList<>();
        for (Map<String, Object> commit : commits) {
            final String sha = strOrEmpty(commit.get("sha"));
            if (sha.isEmpty()) continue;
            final String shortSha = sha.substring(0, Math.min(7, sha.length()));

            // Skip commits authored by other users — the Compare API can return merge commits
            // from master or rebased history that were written by someone else.
            @SuppressWarnings("unchecked")
            final Map<String, Object> githubAuthor = (Map<String, Object>) commit.get("author");
            final String authorLogin = githubAuthor != null ? strOrEmpty(githubAuthor.get("login")) : "";
            if (!authorLogin.isEmpty() && !authorLogin.equals(login)) continue;

            @SuppressWarnings("unchecked")
            final Map<String, Object> commitData = (Map<String, Object>) commit.get("commit");
            if (commitData == null) continue;

            // Use committer.date rather than author.date for the entity timestamp.
            // For rebased / force-pushed commits the committer date reflects when
            // the rebase actually happened (the real day of work), whereas author.date
            // stays frozen at the original commit time and would make the activity
            // invisible on the day the developer actually worked on it.
            @SuppressWarnings("unchecked")
            final Map<String, Object> committerData = (Map<String, Object>) commitData.get("committer");
            @SuppressWarnings("unchecked")
            final Map<String, Object> authorData = (Map<String, Object>) commitData.get("author");
            final String dateStr = committerData != null && committerData.get("date") != null
                ? (String) committerData.get("date")
                : authorData != null ? (String) authorData.get("date") : null;
            final Instant ts = dateStr != null ? Instant.parse(dateStr) : pushTime;
            final String message = firstLine(strOrEmpty(commitData.get("message")), 200);

            // Use login+sha as the event ID so the same commit is stored only once,
            // even if it appears in multiple push events (e.g. after a force-push or rebase).
            final GitHubRawEventEntity e = new GitHubRawEventEntity();
            e.setGithubEventId(login + "_commit_" + sha);
            e.setGithubUsername(login);
            e.setEventType("PushEvent");
            e.setRepoName(repoName);
            e.setAnchorType("REPO");
            e.setAnchorId(branch.isEmpty() ? null : branch);
            e.setAnchorTitle(branch.isEmpty() ? null : branch);
            e.setEventIcon("📝");
            e.setEventSummary(message.isEmpty() ? shortSha : message);
            e.setEventTimestamp(ts);
            result.add(e);
        }

        // Fallback: compare API returned nothing — store a single placeholder using HEAD sha.
        // Only trigger when the API genuinely returned no commits; if commits were returned but
        // all filtered by the author check, there is nothing to record for this user.
        if (result.isEmpty() && !compareApiReturnedCommits) {
            final String shortHead = head.substring(0, Math.min(7, head.length()));
            final GitHubRawEventEntity e = new GitHubRawEventEntity();
            e.setGithubEventId(eventId + "_" + shortHead);
            e.setGithubUsername(login);
            e.setEventType("PushEvent");
            e.setRepoName(repoName);
            e.setAnchorType("REPO");
            e.setAnchorId(branch.isEmpty() ? null : branch);
            e.setAnchorTitle(branch.isEmpty() ? null : branch);
            e.setEventIcon("📝");
            e.setEventSummary("Pushed to " + (branch.isEmpty() ? repoName : branch));
            e.setEventTimestamp(pushTime);
            result.add(e);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchCommitsInPush(String repoName, String before, String head, String token) {
        // Build URLs without URI template expansion to avoid %2F encoding of the slash in "owner/repo"
        try {
            if (before.isEmpty() || before.matches("0+")) {
                // Initial push to a new branch — fetch just the HEAD commit
                final String url = "https://api.github.com/repos/" + repoName + "/commits/" + head;
                final Map<String, Object> commit = restClient.get()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
                return commit != null ? List.of(commit) : List.of();
            }

            // Compare API returns all commits between before and head in chronological order
            final String url = "https://api.github.com/repos/" + repoName + "/compare/" + before + "..." + head;
            final Map<String, Object> comparison = restClient.get()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (comparison == null) return List.of();
            final Object commits = comparison.get("commits");
            return commits instanceof List<?> l ? (List<Map<String, Object>>) (List<?>) l : List.of();
        } catch (Exception ex) {
            LOG.warn("Could not fetch commits for {} ({} -> {}): {}", repoName, before, head, ex.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchEvents(String login, String token) {
        try {
            final List<Map<String, Object>> events = restClient.get()
                .uri("https://api.github.com/users/{login}/events?per_page=100", login)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
            return events != null ? events : List.of();
        } catch (Exception e) {
            LOG.error("Failed to fetch events from GitHub for user {}", login, e);
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private GitHubRawEventEntity parseToEntity(Map<String, Object> raw, String login) {
        final String type = (String) raw.get("type");
        final String createdAt = (String) raw.get("created_at");
        if (type == null || createdAt == null) return null;

        final Map<String, Object> repoMap = (Map<String, Object>) raw.get("repo");
        final String repoName = repoMap != null ? (String) repoMap.get("name") : "unknown";
        final Map<String, Object> payload = raw.get("payload") instanceof Map<?, ?> m
            ? (Map<String, Object>) m : Map.of();

        final String[] parsed = parseAnchorInfo(type, payload);
        if (parsed == null) return null; // skip empty pushes etc.

        final GitHubRawEventEntity entity = new GitHubRawEventEntity();
        entity.setGithubEventId((String) raw.get("id"));
        entity.setGithubUsername(login);
        entity.setEventType(type);
        entity.setRepoName(repoName);
        entity.setAnchorType(parsed[0]);
        entity.setAnchorId(parsed[1]);
        entity.setAnchorTitle(parsed[2]);
        entity.setEventIcon(parsed[3]);
        entity.setEventSummary(parsed[4]);
        entity.setHeadBranch(parsed.length > 5 && !parsed[5].isEmpty() ? parsed[5] : null);
        entity.setEventTimestamp(Instant.parse(createdAt));
        return entity;
    }

    /**
     * Returns [anchorType, anchorId, anchorTitle, icon, summary, headBranch?], or null to skip the event.
     * PullRequestEvent returns 6 elements (headBranch at index 5); all others return 5.
     * PushEvent is handled separately in parsePushToEntities and must not reach this method.
     */
    @SuppressWarnings("unchecked")
    private String[] parseAnchorInfo(String type, Map<String, Object> payload) {
        return switch (type) {
            case "PullRequestEvent" -> {
                final Map<String, Object> pr = (Map<String, Object>) payload.get("pull_request");
                final String action = (String) payload.get("action");
                final int number = pr != null ? toInt(pr.get("number")) : 0;
                final String title = pr != null ? strOrEmpty(pr.get("title")) : "";
                final boolean merged = pr != null && Boolean.TRUE.equals(pr.get("merged"));
                final String verb = merged ? "Merged" : capitalize(action);
                @SuppressWarnings("unchecked")
                final Map<String, Object> head = pr != null ? (Map<String, Object>) pr.get("head") : null;
                final String headBranch = head != null ? strOrEmpty(head.get("ref")) : "";
                yield new String[]{"PR", String.valueOf(number), title, "🔀",
                    verb + " PR #" + number + (title.isEmpty() ? "" : ": " + title),
                    headBranch};
            }
            case "PullRequestReviewEvent" -> {
                final Map<String, Object> pr = (Map<String, Object>) payload.get("pull_request");
                final Map<String, Object> review = (Map<String, Object>) payload.get("review");
                final int number = pr != null ? toInt(pr.get("number")) : 0;
                final String title = pr != null ? strOrEmpty(pr.get("title")) : "";
                final String body = review != null ? firstLine(strOrEmpty(review.get("body")), 100) : "";
                final String state = review != null ? strOrEmpty(review.get("state")) : "";
                final String verb = switch (state) {
                    case "approved" -> "Approved";
                    case "changes_requested" -> "Requested changes on";
                    default -> "Reviewed";
                };
                final String summary = verb + " PR #" + number + (title.isEmpty() ? "" : ": " + title)
                    + (body.isEmpty() ? "" : " — " + body);
                yield new String[]{"PR", String.valueOf(number), title, "👁", summary};
            }
            case "PullRequestReviewCommentEvent" -> {
                final Map<String, Object> pr = (Map<String, Object>) payload.get("pull_request");
                final Map<String, Object> comment = (Map<String, Object>) payload.get("comment");
                final int number = pr != null ? toInt(pr.get("number")) : 0;
                final String title = pr != null ? strOrEmpty(pr.get("title")) : "";
                final String body = comment != null ? firstLine(strOrEmpty(comment.get("body")), 100) : "";
                final String summary = "Commented on PR #" + number + (title.isEmpty() ? "" : ": " + title)
                    + (body.isEmpty() ? "" : " — " + body);
                yield new String[]{"PR", String.valueOf(number), title, "💬", summary};
            }
            case "IssuesEvent" -> {
                final Map<String, Object> issue = (Map<String, Object>) payload.get("issue");
                final String action = (String) payload.get("action");
                final int number = issue != null ? toInt(issue.get("number")) : 0;
                final String title = issue != null ? strOrEmpty(issue.get("title")) : "";
                yield new String[]{"ISSUE", String.valueOf(number), title, "🐛",
                    capitalize(action) + " issue #" + number + (title.isEmpty() ? "" : ": " + title)};
            }
            case "IssueCommentEvent" -> {
                final Map<String, Object> issue = (Map<String, Object>) payload.get("issue");
                final Map<String, Object> comment = (Map<String, Object>) payload.get("comment");
                final int number = issue != null ? toInt(issue.get("number")) : 0;
                final String title = issue != null ? strOrEmpty(issue.get("title")) : "";
                final String body = comment != null ? firstLine(strOrEmpty(comment.get("body")), 120) : "";
                // GitHub fires IssueCommentEvent for PR thread comments too — detect via
                // the "pull_request" key that GitHub adds to the issue object for PRs.
                final boolean isPrComment = issue != null && issue.containsKey("pull_request");
                if (isPrComment) {
                    final String summary = "Commented on PR #" + number + (title.isEmpty() ? "" : ": " + title)
                        + (body.isEmpty() ? "" : " — " + body);
                    yield new String[]{"PR", String.valueOf(number), title, "💬", summary};
                } else {
                    final String summary = "Commented on issue #" + number + (title.isEmpty() ? "" : ": " + title)
                        + (body.isEmpty() ? "" : " — " + body);
                    yield new String[]{"ISSUE", String.valueOf(number), title, "💬", summary};
                }
            }
            case "CreateEvent" -> {
                final String refType = strOrEmpty(payload.get("ref_type"));
                final String ref = strOrEmpty(payload.get("ref"));
                final String label = !ref.isEmpty() ? ref : refType;
                yield new String[]{"REPO", label, label, "🌿", "Created " + refType + (label.isEmpty() ? "" : " " + label)};
            }
            case "DeleteEvent" -> {
                final String refType = strOrEmpty(payload.get("ref_type"));
                final String ref = strOrEmpty(payload.get("ref"));
                final String label = !ref.isEmpty() ? ref : refType;
                yield new String[]{"REPO", null, null, "🗑", "Deleted " + refType + (label.isEmpty() ? "" : " " + label)};
            }
            case "ReleaseEvent" -> {
                final Map<String, Object> release = (Map<String, Object>) payload.get("release");
                final String tag = release != null ? strOrEmpty(release.get("tag_name")) : "";
                final String name = release != null && release.get("name") != null ? strOrEmpty(release.get("name")) : tag;
                yield new String[]{"REPO", tag, name, "🚀", "Released " + (name.isEmpty() ? tag : name + " (" + tag + ")")};
            }
            case "CommitCommentEvent" -> {
                final Map<String, Object> comment = (Map<String, Object>) payload.get("comment");
                final String body = comment != null ? firstLine(strOrEmpty(comment.get("body")), 120) : "";
                yield new String[]{"REPO", null, null, "💬", "Commit comment" + (body.isEmpty() ? "" : ": " + body)};
            }
            case "ForkEvent" -> {
                final Map<String, Object> forkee = (Map<String, Object>) payload.get("forkee");
                final String forkName = forkee != null ? strOrEmpty(forkee.get("full_name")) : "";
                yield new String[]{"REPO", null, null, "🍴", "Forked" + (forkName.isEmpty() ? " repository" : " to " + forkName)};
            }
            case "WatchEvent" -> {
                yield new String[]{"REPO", null, null, "⭐", "Starred repository"};
            }
            case "GollumEvent" -> {
                final Object pages = payload.get("pages");
                final int count = pages instanceof List<?> l ? l.size() : 0;
                yield new String[]{"REPO", null, null, "📖", "Updated " + count + " wiki page" + (count != 1 ? "s" : "")};
            }
            default -> {
                final String displayType = type.replaceAll("Event$", "");
                yield new String[]{"REPO", null, null, "⚡", displayType};
            }
        };
    }

    // --- GitHub App authentication ---

    private synchronized String getInstallationToken() throws Exception {
        if (cachedInstallationToken != null && Instant.now().isBefore(tokenExpiry.minusSeconds(60))) {
            return cachedInstallationToken;
        }

        final String jwt = createJwt();

        // Resolve the installation ID for our org
        @SuppressWarnings("unchecked")
        final Map<String, Object> installation = restClient.get()
            .uri("https://api.github.com/orgs/{org}/installation", orgName)
            .header("Authorization", "Bearer " + jwt)
            .retrieve()
            .body(new ParameterizedTypeReference<Map<String, Object>>() {});

        final int installationId = toInt(installation != null ? installation.get("id") : null);

        // Exchange for a short-lived installation token (valid 1 hour)
        @SuppressWarnings("unchecked")
        final Map<String, Object> tokenResponse = restClient.post()
            .uri("https://api.github.com/app/installations/{id}/access_tokens", installationId)
            .header("Authorization", "Bearer " + jwt)
            .retrieve()
            .body(new ParameterizedTypeReference<Map<String, Object>>() {});

        cachedInstallationToken = tokenResponse != null ? (String) tokenResponse.get("token") : null;
        final String expiresAt = tokenResponse != null ? (String) tokenResponse.get("expires_at") : null;
        tokenExpiry = expiresAt != null ? Instant.parse(expiresAt) : Instant.now().plusSeconds(3600);

        return cachedInstallationToken;
    }

    /**
     * Generates a signed RS256 JWT for GitHub App authentication.
     * Uses Nimbus JOSE+JWT (available transitively via spring-security-oauth2-jose).
     * Handles both PKCS#1 ("BEGIN RSA PRIVATE KEY") and PKCS#8 ("BEGIN PRIVATE KEY") PEM files.
     */
    private String createJwt() throws Exception {
        final String pem = Files.readString(Path.of(privateKeyPath));
        final RSAKey rsaKey = (RSAKey) JWK.parseFromPEMEncodedObjects(pem);
        final JWSSigner signer = new RSASSASigner(rsaKey);

        final long now = Instant.now().getEpochSecond();
        final JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .issuer(appId)
            .issueTime(new Date((now - 60) * 1000))
            .expirationTime(new Date((now + 600) * 1000))
            .build();

        final SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        jwt.sign(signer);
        return jwt.serialize();
    }

    // --- Title enrichment ---

    /**
     * Fetches PR or issue details from the GitHub API to backfill fields that the
     * GitHub App token strips from the Events API payload (title, and for PRs, head branch).
     */
    @SuppressWarnings("unchecked")
    private void enrichPrDetails(GitHubRawEventEntity entity, String token) {
        if (entity.getAnchorId() == null) return;

        final String apiSegment = switch (entity.getAnchorType()) {
            case "PR"    -> "pulls";
            case "ISSUE" -> "issues";
            default      -> null;
        };
        if (apiSegment == null) return;

        try {
            final String url = "https://api.github.com/repos/" + entity.getRepoName()
                + "/" + apiSegment + "/" + entity.getAnchorId();
            final Map<String, Object> response = restClient.get()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            if (response == null) return;

            final String title = strOrEmpty(response.get("title"));
            if (!title.isBlank()) {
                entity.setAnchorTitle(title);
                final String summary = entity.getEventSummary();
                if (!summary.contains(": ")) {
                    entity.setEventSummary(summary + ": " + title);
                }
            }

            // For PRs, also capture the head branch so we can exclude these commits from Standalone
            if ("pulls".equals(apiSegment) && (entity.getHeadBranch() == null || entity.getHeadBranch().isBlank())) {
                final Map<String, Object> head = (Map<String, Object>) response.get("head");
                final String headBranch = head != null ? strOrEmpty(head.get("ref")) : "";
                if (!headBranch.isBlank()) {
                    entity.setHeadBranch(headBranch);
                }
            }
        } catch (Exception e) {
            LOG.debug("Could not fetch details for {}/{}/{}: {}",
                entity.getRepoName(), apiSegment, entity.getAnchorId(), e.getMessage());
        }
    }

    // --- Utilities ---

    private static int toInt(Object o) {
        if (o instanceof Number n) return n.intValue();
        return 0;
    }

    private static String strOrEmpty(Object o) {
        return o instanceof String s ? s : "";
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String firstLine(String s, int maxLen) {
        if (s == null || s.isEmpty()) return "";
        final String first = s.split("\n")[0].trim();
        return first.length() > maxLen ? first.substring(0, maxLen) : first;
    }
}
