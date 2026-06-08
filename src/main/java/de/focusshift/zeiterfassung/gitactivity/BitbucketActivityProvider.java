package de.focusshift.zeiterfassung.gitactivity;

import org.slf4j.Logger;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Fetches Bitbucket activity (PRs, reviews, commits) for all users who have
 * connected their Bitbucket account via OAuth.
 *
 * <p>Because Bitbucket has no Events API, we poll the user-centric PR endpoint
 * and derive activity from the current PR state and per-PR activity feeds.
 *
 * <p>Required configuration:
 * <pre>
 *   bitbucket.oauth.key    — OAuth consumer key (used for token refresh)
 *   bitbucket.oauth.secret — OAuth consumer secret
 * </pre>
 */
@Service
public class BitbucketActivityProvider implements GitActivityProvider {

    private static final Logger LOG = getLogger(lookup().lookupClass());
    private static final String PLATFORM = "BITBUCKET";

    private final GitOAuthTokenRepository tokenRepository;
    private final GitActivityRawEventRepository eventRepository;
    private final GitActivityPlatformSettingsService platformSettingsService;
    private final RestClient restClient;

    private final ConcurrentHashMap<String, Instant> lastSyncTimes = new ConcurrentHashMap<>();

    BitbucketActivityProvider(GitOAuthTokenRepository tokenRepository,
                               GitActivityRawEventRepository eventRepository,
                               GitActivityPlatformSettingsService platformSettingsService) {
        this.tokenRepository = tokenRepository;
        this.eventRepository = eventRepository;
        this.platformSettingsService = platformSettingsService;
        this.restClient = RestClient.builder()
            .defaultHeader("User-Agent", "zeiterfassung-bitbucket-sync")
            .defaultHeader("Accept", "application/json")
            .build();
    }

    @Override public String platform() { return PLATFORM; }

    @Override
    public boolean isConfigured() {
        return platformSettingsService.getBitbucketSettings().isConfigured();
    }

    @Override
    public String missingConfig() {
        final GitActivityPlatformSettings s = platformSettingsService.getBitbucketSettings();
        final List<String> missing = new ArrayList<>();
        if (s.appId() == null || s.appId().isBlank())         missing.add("OAuth consumer key");
        if (s.appSecret() == null || s.appSecret().isBlank()) missing.add("OAuth consumer secret");
        return String.join(", ", missing);
    }

    @Override
    public List<String> resolveUsernames() {
        return tokenRepository.findByPlatform(PLATFORM).stream()
            .map(GitOAuthTokenEntity::getPlatformAccountId)
            .toList();
    }

    @Override
    public void syncUser(String platformAccountId) {
        final GitOAuthTokenEntity token = tokenRepository
            .findByPlatformAndPlatformAccountId(PLATFORM, platformAccountId)
            .orElse(null);
        if (token == null) {
            LOG.warn("No Bitbucket token found for account {}", platformAccountId);
            return;
        }

        final String accessToken;
        try {
            accessToken = ensureFreshToken(token);
        } catch (Exception e) {
            LOG.error("Failed to refresh Bitbucket token for {}", platformAccountId, e);
            return;
        }

        try {
            syncPullRequests(platformAccountId, accessToken);
            lastSyncTimes.put(platformAccountId, Instant.now());
        } catch (Exception e) {
            LOG.error("Failed to sync Bitbucket activity for {}", platformAccountId, e);
        }
    }

    @Override
    public Instant getLastSyncTime(String platformAccountId) {
        return lastSyncTimes.get(platformAccountId);
    }

    // ── Token management ────────────────────────────────────────────────────

    private synchronized String ensureFreshToken(GitOAuthTokenEntity token) {
        if (!token.isExpired()) {
            return token.getAccessToken();
        }
        if (token.getRefreshToken() == null) {
            throw new IllegalStateException("Token expired and no refresh token available for " + token.getPlatformAccountId());
        }

        LOG.debug("Refreshing Bitbucket token for {}", token.getPlatformAccountId());
        final GitActivityPlatformSettings settings = platformSettingsService.getBitbucketSettings();
        final String credentials = Base64.getEncoder().encodeToString(
            (settings.appId() + ":" + settings.appSecret()).getBytes(StandardCharsets.UTF_8));

        @SuppressWarnings("unchecked")
        final Map<String, Object> response = restClient.post()
            .uri("https://bitbucket.org/site/oauth2/access_token")
            .header("Authorization", "Basic " + credentials)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body("grant_type=refresh_token&refresh_token=" + token.getRefreshToken())
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});

        if (response == null) throw new IllegalStateException("Empty refresh response for " + token.getPlatformAccountId());

        final String newAccess  = (String) response.get("access_token");
        final String newRefresh = (String) response.get("refresh_token");
        final Number expiresIn  = (Number) response.get("expires_in");

        token.setAccessToken(newAccess);
        if (newRefresh != null) token.setRefreshToken(newRefresh);
        if (expiresIn != null) token.setExpiresAt(Instant.now().plusSeconds(expiresIn.longValue()));
        tokenRepository.save(token);

        return newAccess;
    }

    // ── Sync logic ──────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void syncPullRequests(String accountId, String token) {
        // Fetch PRs updated in the last 90 days so we pick up recently merged/declined PRs.
        final String cutoff = Instant.now().minus(90, ChronoUnit.DAYS)
            .toString().replace("Z", "+00:00");
        final String url = "https://api.bitbucket.org/2.0/pullrequests/" + accountId
            + "?state=ALL&sort=-updated_on&pagelen=50&q=updated_on>\"" + cutoff + "\"";

        final List<Map<String, Object>> prs = fetchPaged(url, token, 3);
        int saved = 0;

        for (Map<String, Object> pr : prs) {
            saved += processPr(pr, accountId, token);
        }

        if (saved > 0) {
            LOG.info("Synced {} new Bitbucket event(s) for user {}", saved, accountId);
        }
    }

    @SuppressWarnings("unchecked")
    private int processPr(Map<String, Object> pr, String accountId, String token) {
        final int prId = toInt(pr.get("id"));
        if (prId == 0) return 0;

        final String title  = strOrEmpty(pr.get("title"));
        final String state  = strOrEmpty(pr.get("state"));   // OPEN, MERGED, DECLINED, SUPERSEDED

        final Map<String, Object> dest = (Map<String, Object>) pr.get("destination");
        final Map<String, Object> destRepo = dest != null ? (Map<String, Object>) dest.get("repository") : null;
        final String repoFullName = destRepo != null ? strOrEmpty(destRepo.get("full_name")) : "unknown/unknown";

        final Map<String, Object> source = (Map<String, Object>) pr.get("source");
        final Map<String, Object> srcBranch = source != null ? (Map<String, Object>) source.get("branch") : null;
        final String headBranch = srcBranch != null ? strOrEmpty(srcBranch.get("name")) : null;

        final Map<String, Object> srcRepo = source != null ? (Map<String, Object>) source.get("repository") : null;
        final String headRepoFullName = srcRepo != null ? strOrEmpty(srcRepo.get("full_name")) : null;

        // Use updated_on for merged/declined, created_on for open PRs
        final String rawTs = "OPEN".equals(state)
            ? strOrEmpty(pr.get("created_on"))
            : strOrEmpty(pr.get("updated_on"));
        final Instant ts = rawTs.isEmpty() ? Instant.now() : parseTs(rawTs);

        final String verb = switch (state) {
            case "MERGED"     -> "Merged";
            case "DECLINED"   -> "Closed";
            case "SUPERSEDED" -> "Superseded";
            default           -> "Opened";
        };
        final String summary = verb + " PR #" + prId + (title.isEmpty() ? "" : ": " + title);
        final String prEventId = accountId + "_bitbucket_pr_" + repoFullName.replace("/", "_") + "_" + prId;

        int saved = 0;

        final var existing = eventRepository.findByPlatformEventId(prEventId);
        if (existing.isPresent()) {
            // Update state if PR was merged/closed since we last synced
            final GitActivityRawEventEntity e = existing.get();
            final boolean stateChanged = !e.getEventSummary().startsWith(verb);
            if (stateChanged && !"OPEN".equals(state)) {
                e.setEventSummary(summary);
                e.setEventTimestamp(ts);
                eventRepository.save(e);
            }
        } else {
            final GitActivityRawEventEntity e = new GitActivityRawEventEntity();
            e.setPlatformEventId(prEventId);
            e.setPlatformUsername(accountId);
            e.setPlatform(PLATFORM);
            e.setEventType("PullRequestEvent");
            e.setRepoName(repoFullName);
            e.setAnchorType("PR");
            e.setAnchorId(String.valueOf(prId));
            e.setAnchorTitle(title);
            e.setEventIcon("🔀");
            e.setEventSummary(summary);
            e.setEventTimestamp(ts);
            e.setHeadBranch(headBranch);
            if (headRepoFullName != null && !headRepoFullName.equals(repoFullName)) {
                e.setHeadRepoName(headRepoFullName);
            }
            eventRepository.save(e);
            saved++;
        }

        // For OPEN PRs: sync commits and activity (approvals/comments)
        if ("OPEN".equals(state)) {
            final String[] repoParts = repoFullName.split("/", 2);
            if (repoParts.length == 2) {
                final String workspace = repoParts[0];
                final String repoSlug  = repoParts[1];
                saved += syncPrCommits(accountId, repoFullName, workspace, repoSlug, prId, headBranch, token);
                saved += syncPrActivity(accountId, repoFullName, workspace, repoSlug, prId, title, token);
            }
        }

        return saved;
    }

    @SuppressWarnings("unchecked")
    private int syncPrCommits(String accountId, String repoFullName,
                               String workspace, String repoSlug,
                               int prId, String headBranch, String token) {
        final String url = "https://api.bitbucket.org/2.0/repositories/"
            + workspace + "/" + repoSlug + "/pullrequests/" + prId + "/commits?pagelen=50";

        final List<Map<String, Object>> commits = fetchPaged(url, token, 1);
        int saved = 0;

        for (Map<String, Object> commit : commits) {
            final String hash = strOrEmpty(commit.get("hash"));
            if (hash.isEmpty()) continue;

            // Author filter — only store commits by this user
            final Map<String, Object> authorInfo = (Map<String, Object>) commit.get("author");
            if (authorInfo != null) {
                final Map<String, Object> authorUser = (Map<String, Object>) authorInfo.get("user");
                if (authorUser != null) {
                    final String authorAccountId = strOrEmpty(authorUser.get("account_id"));
                    if (!authorAccountId.isEmpty() && !authorAccountId.equals(accountId)) continue;
                }
            }

            final String message = firstLine(strOrEmpty(commit.get("message")), 200);
            final String dateStr = strOrEmpty(commit.get("date"));
            final Instant ts = dateStr.isEmpty() ? Instant.now() : parseTs(dateStr);
            final String eventId = accountId + "_bitbucket_commit_" + hash;
            final String shortHash = hash.substring(0, Math.min(7, hash.length()));

            final var existing = eventRepository.findByPlatformEventId(eventId);
            if (existing.isPresent()) {
                final GitActivityRawEventEntity ex = existing.get();
                if (!ex.getEventTimestamp().equals(ts)) {
                    ex.setEventTimestamp(ts);
                    eventRepository.save(ex);
                }
            } else {
                final GitActivityRawEventEntity e = new GitActivityRawEventEntity();
                e.setPlatformEventId(eventId);
                e.setPlatformUsername(accountId);
                e.setPlatform(PLATFORM);
                e.setEventType("PushEvent");
                e.setRepoName(repoFullName);
                e.setAnchorType("REPO");
                e.setAnchorId(headBranch);
                e.setAnchorTitle(headBranch);
                e.setEventIcon("📝");
                e.setEventSummary(message.isEmpty() ? shortHash : message);
                e.setEventTimestamp(ts);
                eventRepository.save(e);
                saved++;
            }
        }
        return saved;
    }

    @SuppressWarnings("unchecked")
    private int syncPrActivity(String accountId, String repoFullName,
                                String workspace, String repoSlug,
                                int prId, String prTitle, String token) {
        final String url = "https://api.bitbucket.org/2.0/repositories/"
            + workspace + "/" + repoSlug + "/pullrequests/" + prId + "/activity?pagelen=50";

        final List<Map<String, Object>> activities = fetchPaged(url, token, 1);
        int saved = 0;

        for (Map<String, Object> activity : activities) {
            // Approval
            final Map<String, Object> approval = (Map<String, Object>) activity.get("approval");
            if (approval != null) {
                final Map<String, Object> user = (Map<String, Object>) approval.get("user");
                final String approverAccountId = user != null ? strOrEmpty(user.get("account_id")) : "";
                if (!approverAccountId.isEmpty() && approverAccountId.equals(accountId)) {
                    final String approvalId = accountId + "_bitbucket_pr_"
                        + repoFullName.replace("/", "_") + "_" + prId + "_approval";
                    if (!eventRepository.existsByPlatformEventId(approvalId)) {
                        final String dateStr = strOrEmpty(approval.get("date"));
                        final Instant ts = dateStr.isEmpty() ? Instant.now() : parseTs(dateStr);
                        final GitActivityRawEventEntity e = new GitActivityRawEventEntity();
                        e.setPlatformEventId(approvalId);
                        e.setPlatformUsername(accountId);
                        e.setPlatform(PLATFORM);
                        e.setEventType("PullRequestReviewEvent");
                        e.setRepoName(repoFullName);
                        e.setAnchorType("PR");
                        e.setAnchorId(String.valueOf(prId));
                        e.setAnchorTitle(prTitle);
                        e.setEventIcon("👁");
                        e.setEventSummary("Approved PR #" + prId
                            + (prTitle.isEmpty() ? "" : ": " + prTitle));
                        e.setEventTimestamp(ts);
                        eventRepository.save(e);
                        saved++;
                    }
                }
            }

            // PR-level comment
            final Map<String, Object> comment = (Map<String, Object>) activity.get("comment");
            if (comment != null) {
                final Map<String, Object> commentUser = (Map<String, Object>) comment.get("user");
                final String commenterAccountId = commentUser != null ? strOrEmpty(commentUser.get("account_id")) : "";
                if (!commenterAccountId.isEmpty() && commenterAccountId.equals(accountId)) {
                    final int commentId = toInt(comment.get("id"));
                    if (commentId != 0) {
                        final String commentEventId = accountId + "_bitbucket_pr_"
                            + repoFullName.replace("/", "_") + "_" + prId + "_comment_" + commentId;
                        if (!eventRepository.existsByPlatformEventId(commentEventId)) {
                            final String dateStr = strOrEmpty(comment.get("created_on"));
                            final Instant ts = dateStr.isEmpty() ? Instant.now() : parseTs(dateStr);
                            final Map<String, Object> content = (Map<String, Object>) comment.get("content");
                            final String body = content != null
                                ? firstLine(strOrEmpty(content.get("raw")), 120) : "";
                            final GitActivityRawEventEntity e = new GitActivityRawEventEntity();
                            e.setPlatformEventId(commentEventId);
                            e.setPlatformUsername(accountId);
                            e.setPlatform(PLATFORM);
                            e.setEventType("PullRequestReviewCommentEvent");
                            e.setRepoName(repoFullName);
                            e.setAnchorType("PR");
                            e.setAnchorId(String.valueOf(prId));
                            e.setAnchorTitle(prTitle);
                            e.setEventIcon("💬");
                            e.setEventSummary("Commented on PR #" + prId
                                + (prTitle.isEmpty() ? "" : ": " + prTitle)
                                + (body.isEmpty() ? "" : " — " + body));
                            e.setEventTimestamp(ts);
                            eventRepository.save(e);
                            saved++;
                        }
                    }
                }
            }
        }
        return saved;
    }

    // ── Pagination ──────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchPaged(String firstUrl, String token, int maxPages) {
        final List<Map<String, Object>> result = new ArrayList<>();
        String nextUrl = firstUrl;
        int page = 0;

        while (nextUrl != null && page < maxPages) {
            try {
                final Map<String, Object> response = restClient.get()
                    .uri(nextUrl)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

                if (response == null) break;

                final Object values = response.get("values");
                if (values instanceof List<?> list) {
                    for (Object item : list) {
                        if (item instanceof Map<?, ?> m) result.add((Map<String, Object>) m);
                    }
                }

                nextUrl = (String) response.get("next");
                page++;
            } catch (Exception e) {
                LOG.warn("Bitbucket API call failed for {}: {}", nextUrl, e.getMessage());
                break;
            }
        }

        return result;
    }

    // ── Utilities ───────────────────────────────────────────────────────────

    private static Instant parseTs(String s) {
        try {
            // Bitbucket returns ISO 8601 with timezone offset, e.g. "2026-06-04T14:32:00.000000+00:00"
            return Instant.parse(s.replace(" ", "T").replaceAll("\\+00:00$", "Z").replaceAll("(\\+\\d{2}:\\d{2})$", "Z"));
        } catch (Exception e) {
            return Instant.now();
        }
    }

    private static int toInt(Object o) {
        if (o instanceof Number n) return n.intValue();
        return 0;
    }

    private static String strOrEmpty(Object o) {
        return o instanceof String s ? s : "";
    }

    private static String firstLine(String s, int maxLen) {
        if (s == null || s.isEmpty()) return "";
        final String first = s.split("\n")[0].trim();
        return first.length() > maxLen ? first.substring(0, maxLen) : first;
    }
}
