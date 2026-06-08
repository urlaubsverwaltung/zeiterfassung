package de.focusshift.zeiterfassung.gitactivity;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import de.focusshift.zeiterfassung.user.UserSettingsService;
import org.slf4j.Logger;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Fetches GitHub activity for all users with a verified GitHub login.
 *
 * <p>All credentials are stored in the database via {@link GitActivityPlatformSettingsService}
 * and configured by an admin through the Settings → Git Activity page.
 * No YAML properties are required.
 *
 * <p>Two authentication paths — both can be active simultaneously for a user:
 *
 * <p><b>Path 1 — Org installation (internal repos):</b>
 * GitHub App installed at the org level by admin. Server mints installation tokens from
 * the stored private key. Users only need to verify their GitHub username.
 *
 * <p><b>Path 2 — Personal installation (customer repos):</b>
 * Developer installs the app in their personal account. The installation ID is stored
 * in user settings. The server mints tokens for that installation ID using the same key.
 */
@Service
public class GitHubActivityProvider implements GitActivityProvider {

    private static final Logger LOG = getLogger(lookup().lookupClass());
    private static final int RATE_LIMIT_SAFETY_THRESHOLD = 200;

    private final GitActivityRawEventRepository repository;
    private final UserSettingsService userSettingsService;
    private final GitActivityPlatformSettingsService platformSettingsService;
    private final RestClient restClient;

    // --- Org installation token cache ---
    private volatile String cachedOrgToken;
    private volatile Instant orgTokenExpiry = Instant.MIN;

    // --- Per-installation token cache: installationId → (token, expiry) ---
    private record CachedToken(String token, Instant expiry) {
        boolean isValid() { return Instant.now().isBefore(expiry.minusSeconds(60)); }
    }
    private final java.util.concurrent.ConcurrentHashMap<Long, CachedToken> cachedPersonalTokens =
        new java.util.concurrent.ConcurrentHashMap<>();

    private final java.util.concurrent.ConcurrentHashMap<String, Instant> lastSyncTimes = new java.util.concurrent.ConcurrentHashMap<>();

    // Rate limit state — updated from response headers on every API call
    private final java.util.concurrent.atomic.AtomicInteger rateLimitRemaining =
        new java.util.concurrent.atomic.AtomicInteger(5000);
    private final java.util.concurrent.atomic.AtomicInteger rateLimitTotal =
        new java.util.concurrent.atomic.AtomicInteger(5000);
    private volatile Instant rateLimitReset = Instant.MIN;

    @Override public String platform() { return "GITHUB"; }

    @Override public Instant getLastSyncTime(String login) { return lastSyncTimes.get(login); }

    public int getRateLimitRemaining() { return rateLimitRemaining.get(); }
    public int getRateLimitTotal() { return rateLimitTotal.get(); }
    public Instant getRateLimitReset() { return rateLimitReset; }
    public boolean isRateLimitSafe() { return rateLimitRemaining.get() > RATE_LIMIT_SAFETY_THRESHOLD; }

    public int getRateLimitPercent() {
        final int total = rateLimitTotal.get();
        return total > 0 ? Math.min(100, (int) (rateLimitRemaining.get() * 100.0 / total)) : 100;
    }

    // package-private for testing
    void setRateLimitRemaining(int remaining) { this.rateLimitRemaining.set(remaining); }

    @org.springframework.beans.factory.annotation.Autowired
    GitHubActivityProvider(GitActivityRawEventRepository repository,
                            UserSettingsService userSettingsService,
                            GitActivityPlatformSettingsService platformSettingsService) {
        this.repository = repository;
        this.userSettingsService = userSettingsService;
        this.platformSettingsService = platformSettingsService;
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

    // package-private for testing — platformSettingsService may be null (tests use syncUser(login,token) directly)
    GitHubActivityProvider(GitActivityRawEventRepository repository,
                            UserSettingsService userSettingsService,
                            RestClient restClient) {
        this.repository = repository;
        this.userSettingsService = userSettingsService;
        this.platformSettingsService = null;
        this.restClient = restClient;
    }

    private void updateRateLimitFromHeaders(org.springframework.http.HttpHeaders headers) {
        try {
            final String remaining = headers.getFirst("X-RateLimit-Remaining");
            if (remaining != null) rateLimitRemaining.set(Integer.parseInt(remaining));
            final String limit = headers.getFirst("X-RateLimit-Limit");
            if (limit != null) rateLimitTotal.set(Integer.parseInt(limit));
            final String reset = headers.getFirst("X-RateLimit-Reset");
            if (reset != null) rateLimitReset = Instant.ofEpochSecond(Long.parseLong(reset));
        } catch (NumberFormatException ignored) {}
    }

    @Override
    public boolean isConfigured() {
        return platformSettingsService != null
            && platformSettingsService.getGitHubSettings().isConfigured();
    }

    public boolean isOrgConfigured() {
        return platformSettingsService != null
            && platformSettingsService.getGitHubSettings().isOrgConfigured();
    }

    public boolean isPersonalInstallConfigured() {
        return platformSettingsService != null
            && platformSettingsService.getGitHubSettings().isPersonalInstallConfigured();
    }

    @Override
    public String missingConfig() {
        if (platformSettingsService == null) return "GITHUB_APP_ID, GITHUB_APP_PRIVATE_KEY";
        final GitActivityPlatformSettings s = platformSettingsService.getGitHubSettings();
        final List<String> missing = new java.util.ArrayList<>();
        if (s.appId() == null || s.appId().isBlank())     missing.add("App ID");
        if (s.appSecret() == null || s.appSecret().isBlank()) missing.add("Private key");
        return String.join(", ", missing);
    }

    @Override
    public List<String> resolveUsernames() {
        return userSettingsService.findAllVerifiedGithubLogins();
    }

    @Override
    public void syncUser(String login) {
        if (platformSettingsService == null) return;
        final GitActivityPlatformSettings settings = platformSettingsService.getGitHubSettings();
        if (!settings.isConfigured()) {
            LOG.warn("GitHub App not configured in admin settings, cannot sync for user {}", login);
            return;
        }

        // Path 1 — org installation token (covers internal repos)
        if (settings.isOrgConfigured()) {
            try {
                syncUser(login, getOrgInstallationToken(settings));
            } catch (Exception e) {
                LOG.error("Org-installation sync failed for user {}", login, e);
            }
        }

        // Path 2 — personal installation token (covers customer repos)
        userSettingsService.findGithubInstallationIdByLogin(login).ifPresent(installationId -> {
            try {
                syncUser(login, getPersonalInstallationToken(settings, installationId));
            } catch (Exception e) {
                LOG.error("Personal-installation sync failed for user {} (installation {})", login, installationId, e);
            }
        });

        lastSyncTimes.put(login, Instant.now());
    }

    /** Invalidates all cached tokens — call this after admin saves new credentials. */
    public void invalidateTokenCache() {
        cachedOrgToken = null;
        orgTokenExpiry = Instant.MIN;
        cachedPersonalTokens.clear();
        LOG.info("GitHub installation token cache invalidated");
    }

    @SuppressWarnings("unchecked")
    void syncUser(String login, String token) {
        final List<Map<String, Object>> events = fetchEvents(login, token);
        int saved = 0;
        for (Map<String, Object> raw : events) {
            final String eventId = (String) raw.get("id");
            final String type = (String) raw.get("type");
            if (eventId == null || type == null) continue;

            if ("PushEvent".equals(type)) {
                for (GitActivityRawEventEntity e : parsePushToEntities(raw, login, token)) {
                    final java.util.Optional<GitActivityRawEventEntity> existingCommit =
                        repository.findByPlatformEventId(e.getPlatformEventId());
                    if (existingCommit.isPresent()) {
                        final GitActivityRawEventEntity ex = existingCommit.get();
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
                final java.util.Optional<GitActivityRawEventEntity> existing = repository.findByPlatformEventId(eventId);
                if (existing.isPresent()) {
                    final GitActivityRawEventEntity e = existing.get();
                    final boolean missingTitle = e.getAnchorTitle() == null || e.getAnchorTitle().isBlank();
                    final boolean missingHeadBranch = "PR".equals(e.getAnchorType())
                        && (e.getHeadBranch() == null || e.getHeadBranch().isBlank());
                    if (missingTitle || missingHeadBranch) {
                        enrichPrDetails(e, token);
                        repository.save(e);
                    }
                    continue;
                }
                final GitActivityRawEventEntity entity = parseToEntity(raw, login);
                if (entity != null) {
                    enrichPrDetails(entity, token);
                    repository.save(entity);
                    saved++;
                }
            }
        }
        if (saved > 0) {
            LOG.info("Synced {} new GitHub event(s) for user {}", saved, login);
        }
        syncOpenPrCommits(login, token);
    }

    @SuppressWarnings("unchecked")
    private void syncOpenPrCommits(String login, String token) {
        final List<GitActivityRawEventEntity> allPrEvents = repository
            .findByPlatformUsernameAndAnchorTypeAndEventType(login, "PR", "PullRequestEvent");

        final Map<String, GitActivityRawEventEntity> latestPerPr = new java.util.LinkedHashMap<>();
        for (GitActivityRawEventEntity e : allPrEvents) {
            final String key = e.getRepoName() + "|" + e.getAnchorId();
            final GitActivityRawEventEntity existing = latestPerPr.get(key);
            if (existing == null || e.getEventTimestamp().isAfter(existing.getEventTimestamp())) {
                latestPerPr.put(key, e);
            }
        }

        for (GitActivityRawEventEntity pr : latestPerPr.values()) {
            final String summary = pr.getEventSummary();
            if (summary.startsWith("Merged") || summary.startsWith("Closed")) continue;
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
                final Map<String, Object> authorGh = (Map<String, Object>) commit.get("author");
                final String authorLogin = authorGh != null ? strOrEmpty(authorGh.get("login")) : "";
                if (!authorLogin.isEmpty() && !authorLogin.equals(login)) continue;
                final Map<String, Object> commitData = (Map<String, Object>) commit.get("commit");
                if (commitData == null) continue;
                final Map<String, Object> committerData = (Map<String, Object>) commitData.get("committer");
                final Map<String, Object> authorData = (Map<String, Object>) commitData.get("author");
                final String dateStr = committerData != null && committerData.get("date") != null
                    ? (String) committerData.get("date")
                    : authorData != null ? (String) authorData.get("date") : null;
                if (dateStr == null) continue;
                final Instant ts = Instant.parse(dateStr);
                final String eventId = login + "_commit_" + sha;
                final java.util.Optional<GitActivityRawEventEntity> existing = repository.findByPlatformEventId(eventId);
                if (existing.isPresent()) {
                    final GitActivityRawEventEntity ex = existing.get();
                    if (!ex.getEventTimestamp().equals(ts)) {
                        ex.setEventTimestamp(ts);
                        repository.save(ex);
                    }
                } else {
                    final String message = firstLine(strOrEmpty(commitData.get("message")), 200);
                    final String shortSha = sha.substring(0, Math.min(7, sha.length()));
                    final GitActivityRawEventEntity e = new GitActivityRawEventEntity();
                    e.setPlatformEventId(eventId);
                    e.setPlatformUsername(login);
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
    private List<GitActivityRawEventEntity> parsePushToEntities(Map<String, Object> raw, String login, String token) {
        final String eventId = (String) raw.get("id");
        final String createdAt = (String) raw.get("created_at");
        if (createdAt == null) return List.of();
        final Map<String, Object> repoMap = (Map<String, Object>) raw.get("repo");
        final String repoName = repoMap != null ? (String) repoMap.get("name") : null;
        if (repoName == null) return List.of();
        final Map<String, Object> payload = raw.get("payload") instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
        final String ref = strOrEmpty(payload.get("ref"));
        final String branch = !ref.isEmpty() ? ref.replaceFirst("^refs/heads/", "") : "";
        final String head = strOrEmpty(payload.get("head"));
        final String before = strOrEmpty(payload.get("before"));
        final Instant pushTime = Instant.parse(createdAt);
        if (head.isEmpty()) return List.of();

        final List<Map<String, Object>> commits = fetchCommitsInPush(repoName, before, head, token);
        final boolean compareApiReturnedCommits = !commits.isEmpty();
        final List<GitActivityRawEventEntity> result = new ArrayList<>();

        for (Map<String, Object> commit : commits) {
            final String sha = strOrEmpty(commit.get("sha"));
            if (sha.isEmpty()) continue;
            final String shortSha = sha.substring(0, Math.min(7, sha.length()));
            @SuppressWarnings("unchecked")
            final Map<String, Object> githubAuthor = (Map<String, Object>) commit.get("author");
            final String authorLogin = githubAuthor != null ? strOrEmpty(githubAuthor.get("login")) : "";
            if (!authorLogin.isEmpty() && !authorLogin.equals(login)) continue;
            @SuppressWarnings("unchecked")
            final Map<String, Object> commitData = (Map<String, Object>) commit.get("commit");
            if (commitData == null) continue;
            @SuppressWarnings("unchecked")
            final Map<String, Object> committerData = (Map<String, Object>) commitData.get("committer");
            @SuppressWarnings("unchecked")
            final Map<String, Object> authorData = (Map<String, Object>) commitData.get("author");
            final String dateStr = committerData != null && committerData.get("date") != null
                ? (String) committerData.get("date")
                : authorData != null ? (String) authorData.get("date") : null;
            final Instant ts = dateStr != null ? Instant.parse(dateStr) : pushTime;
            final String message = firstLine(strOrEmpty(commitData.get("message")), 200);
            final GitActivityRawEventEntity e = new GitActivityRawEventEntity();
            e.setPlatformEventId(login + "_commit_" + sha);
            e.setPlatformUsername(login);
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

        if (result.isEmpty() && !compareApiReturnedCommits) {
            final String shortHead = head.substring(0, Math.min(7, head.length()));
            final GitActivityRawEventEntity e = new GitActivityRawEventEntity();
            e.setPlatformEventId(eventId + "_" + shortHead);
            e.setPlatformUsername(login);
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
        try {
            if (before.isEmpty() || before.matches("0+")) {
                final String url = "https://api.github.com/repos/" + repoName + "/commits/" + head;
                final Map<String, Object> commit = restClient.get()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
                return commit != null ? List.of(commit) : List.of();
            }
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
        final List<Map<String, Object>> result = new ArrayList<>();
        for (int page = 1; page <= 3; page++) {
            final List<Map<String, Object>> pageEvents = fetchEventsPage(login, token, page);
            if (pageEvents.isEmpty()) break;
            boolean hitKnown = false;
            for (Map<String, Object> event : pageEvents) {
                final String id = (String) event.get("id");
                if (id != null && repository.existsByPlatformEventId(id)) {
                    hitKnown = true;
                    break;
                }
                result.add(event);
            }
            if (hitKnown || pageEvents.size() < 100) break;
        }
        if (!result.isEmpty()) {
            LOG.debug("Fetched {} new event(s) for {} across up to 3 pages", result.size(), login);
        }
        return result;
    }

    private List<Map<String, Object>> fetchEventsPage(String login, String token, int page) {
        try {
            final List<Map<String, Object>> events = restClient.get()
                .uri("https://api.github.com/users/{login}/events?per_page=100&page={page}", login, page)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
            return events != null ? events : List.of();
        } catch (Exception e) {
            LOG.error("Failed to fetch GitHub events page {} for user {}", page, login, e);
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private GitActivityRawEventEntity parseToEntity(Map<String, Object> raw, String login) {
        final String type = (String) raw.get("type");
        final String createdAt = (String) raw.get("created_at");
        if (type == null || createdAt == null) return null;
        final Map<String, Object> repoMap = (Map<String, Object>) raw.get("repo");
        final String repoName = repoMap != null ? (String) repoMap.get("name") : "unknown";
        final Map<String, Object> payload = raw.get("payload") instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
        final String[] parsed = parseAnchorInfo(type, payload);
        if (parsed == null) return null;
        final GitActivityRawEventEntity entity = new GitActivityRawEventEntity();
        entity.setPlatformEventId((String) raw.get("id"));
        entity.setPlatformUsername(login);
        entity.setEventType(type);
        entity.setRepoName(repoName);
        entity.setAnchorType(parsed[0]);
        entity.setAnchorId(parsed[1]);
        entity.setAnchorTitle(parsed[2]);
        entity.setEventIcon(parsed[3]);
        entity.setEventSummary(parsed[4]);
        entity.setHeadBranch(parsed.length > 5 && !parsed[5].isEmpty() ? parsed[5] : null);
        final String parsedHeadRepo = parsed.length > 6 ? parsed[6] : "";
        entity.setHeadRepoName(!parsedHeadRepo.isEmpty() && !parsedHeadRepo.equals(repoName) ? parsedHeadRepo : null);
        entity.setEventTimestamp(Instant.parse(createdAt));
        return entity;
    }

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
                final Map<String, Object> head = pr != null ? (Map<String, Object>) pr.get("head") : null;
                final String headBranch = head != null ? strOrEmpty(head.get("ref")) : "";
                final Map<String, Object> headRepo = head != null ? (Map<String, Object>) head.get("repo") : null;
                final String headRepoName = headRepo != null ? strOrEmpty(headRepo.get("full_name")) : "";
                yield new String[]{"PR", String.valueOf(number), title, "🔀",
                    verb + " PR #" + number + (title.isEmpty() ? "" : ": " + title), headBranch, headRepoName};
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
                yield new String[]{"PR", String.valueOf(number), title, "👁",
                    verb + " PR #" + number + (title.isEmpty() ? "" : ": " + title) + (body.isEmpty() ? "" : " — " + body)};
            }
            case "PullRequestReviewCommentEvent" -> {
                final Map<String, Object> pr = (Map<String, Object>) payload.get("pull_request");
                final Map<String, Object> comment = (Map<String, Object>) payload.get("comment");
                final int number = pr != null ? toInt(pr.get("number")) : 0;
                final String title = pr != null ? strOrEmpty(pr.get("title")) : "";
                final String body = comment != null ? firstLine(strOrEmpty(comment.get("body")), 100) : "";
                yield new String[]{"PR", String.valueOf(number), title, "💬",
                    "Commented on PR #" + number + (title.isEmpty() ? "" : ": " + title) + (body.isEmpty() ? "" : " — " + body)};
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
                final boolean isPrComment = issue != null && issue.containsKey("pull_request");
                if (isPrComment) {
                    yield new String[]{"PR", String.valueOf(number), title, "💬",
                        "Commented on PR #" + number + (title.isEmpty() ? "" : ": " + title) + (body.isEmpty() ? "" : " — " + body)};
                } else {
                    yield new String[]{"ISSUE", String.valueOf(number), title, "💬",
                        "Commented on issue #" + number + (title.isEmpty() ? "" : ": " + title) + (body.isEmpty() ? "" : " — " + body)};
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
            case "WatchEvent" -> new String[]{"REPO", null, null, "⭐", "Starred repository"};
            case "GollumEvent" -> {
                final Object pages = payload.get("pages");
                final int count = pages instanceof List<?> l ? l.size() : 0;
                yield new String[]{"REPO", null, null, "📖", "Updated " + count + " wiki page" + (count != 1 ? "s" : "")};
            }
            default -> new String[]{"REPO", null, null, "⚡", type.replaceAll("Event$", "")};
        };
    }

    // --- GitHub App authentication ---

    private synchronized String getOrgInstallationToken(GitActivityPlatformSettings settings) throws Exception {
        if (cachedOrgToken != null && Instant.now().isBefore(orgTokenExpiry.minusSeconds(60))) {
            return cachedOrgToken;
        }
        final String jwt = createJwt(settings);
        @SuppressWarnings("unchecked")
        final Map<String, Object> installation = restClient.get()
            .uri("https://api.github.com/orgs/{org}/installation", settings.orgName())
            .header("Authorization", "Bearer " + jwt)
            .retrieve()
            .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        final int installationId = toInt(installation != null ? installation.get("id") : null);
        cachedOrgToken = mintInstallationToken(installationId, jwt);
        orgTokenExpiry = Instant.now().plusSeconds(3600);
        return cachedOrgToken;
    }

    private String getPersonalInstallationToken(GitActivityPlatformSettings settings, long installationId) throws Exception {
        final CachedToken cached = cachedPersonalTokens.get(installationId);
        if (cached != null && cached.isValid()) return cached.token();
        final String jwt = createJwt(settings);
        final String token = mintInstallationToken((int) installationId, jwt);
        cachedPersonalTokens.put(installationId, new CachedToken(token, Instant.now().plusSeconds(3600)));
        return token;
    }

    @SuppressWarnings("unchecked")
    private String mintInstallationToken(int installationId, String jwt) throws Exception {
        final Map<String, Object> tokenResponse = restClient.post()
            .uri("https://api.github.com/app/installations/{id}/access_tokens", installationId)
            .header("Authorization", "Bearer " + jwt)
            .retrieve()
            .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        if (tokenResponse == null) throw new IllegalStateException("No token response for installation " + installationId);
        final String token = (String) tokenResponse.get("token");
        if (token == null) throw new IllegalStateException("Null token for installation " + installationId);
        return token;
    }

    /**
     * Creates a signed RS256 JWT from the PEM private key stored in the DB.
     * Handles both PKCS#1 ("BEGIN RSA PRIVATE KEY") and PKCS#8 ("BEGIN PRIVATE KEY") formats.
     */
    private String createJwt(GitActivityPlatformSettings settings) throws Exception {
        final RSAKey rsaKey = (RSAKey) JWK.parseFromPEMEncodedObjects(settings.appSecret());
        final JWSSigner signer = new RSASSASigner(rsaKey);
        final long now = Instant.now().getEpochSecond();
        final JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .issuer(settings.appId())
            .issueTime(new Date((now - 60) * 1000))
            .expirationTime(new Date((now + 600) * 1000))
            .build();
        final SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        jwt.sign(signer);
        return jwt.serialize();
    }

    // --- Title enrichment ---

    @SuppressWarnings("unchecked")
    private void enrichPrDetails(GitActivityRawEventEntity entity, String token) {
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
                if (!entity.getEventSummary().contains(": ")) {
                    entity.setEventSummary(entity.getEventSummary() + ": " + title);
                }
            }
            if ("pulls".equals(apiSegment)) {
                final Map<String, Object> head = (Map<String, Object>) response.get("head");
                if (entity.getHeadBranch() == null || entity.getHeadBranch().isBlank()) {
                    final String headBranch = head != null ? strOrEmpty(head.get("ref")) : "";
                    if (!headBranch.isBlank()) entity.setHeadBranch(headBranch);
                }
                if (entity.getHeadRepoName() == null) {
                    final Map<String, Object> headRepo = head != null ? (Map<String, Object>) head.get("repo") : null;
                    final String headRepoName = headRepo != null ? strOrEmpty(headRepo.get("full_name")) : "";
                    if (!headRepoName.isBlank() && !headRepoName.equals(entity.getRepoName())) {
                        entity.setHeadRepoName(headRepoName);
                    }
                }
            }
        } catch (Exception e) {
            LOG.debug("Could not fetch details for {}/{}/{}: {}",
                entity.getRepoName(), apiSegment, entity.getAnchorId(), e.getMessage());
        }
    }

    // --- Utilities ---
    private static int toInt(Object o) { return o instanceof Number n ? n.intValue() : 0; }
    private static String strOrEmpty(Object o) { return o instanceof String s ? s : ""; }
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
