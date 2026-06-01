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

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
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
class GitHubSyncService {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    @Value("${github.app.id:}")
    private String appId;

    @Value("${github.app.private-key-path:}")
    private String privateKeyPath;

    @Value("${github.organization:}")
    private String orgName;

    private final GitHubRawEventRepository repository;
    private final UserSettingsService userSettingsService;
    private final RestClient restClient;

    private volatile String cachedInstallationToken;
    private volatile Instant tokenExpiry = Instant.MIN;

    GitHubSyncService(GitHubRawEventRepository repository, UserSettingsService userSettingsService) {
        this.repository = repository;
        this.userSettingsService = userSettingsService;
        this.restClient = RestClient.builder()
            .defaultHeader("User-Agent", "zeiterfassung-github-sync")
            .defaultHeader("Accept", "application/vnd.github+json")
            .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
            .build();
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

    boolean isConfigured() {
        return !appId.isBlank() && !privateKeyPath.isBlank() && !orgName.isBlank();
    }

    @SuppressWarnings("unchecked")
    private void syncUser(String login, String token) {
        final List<Map<String, Object>> events = fetchEvents(login, token);
        int saved = 0;
        for (Map<String, Object> raw : events) {
            final String eventId = (String) raw.get("id");
            if (eventId == null || repository.existsByGithubEventId(eventId)) {
                continue;
            }
            final GitHubRawEventEntity entity = parseToEntity(raw, login);
            if (entity != null) {
                repository.save(entity);
                saved++;
            }
        }
        if (saved > 0) {
            LOG.info("Synced {} new GitHub event(s) for user {}", saved, login);
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
        entity.setEventTimestamp(Instant.parse(createdAt));
        return entity;
    }

    /**
     * Returns [anchorType, anchorId, anchorTitle, icon, summary], or null to skip the event.
     */
    @SuppressWarnings("unchecked")
    private String[] parseAnchorInfo(String type, Map<String, Object> payload) {
        return switch (type) {
            case "PushEvent" -> {
                final int size = toInt(payload.get("size"));
                final int distinctSize = toInt(payload.get("distinct_size"));
                final int count = distinctSize > 0 ? distinctSize : size;
                if (count == 0) yield null;
                final String ref = (String) payload.get("ref");
                final String branch = ref != null ? ref.replaceFirst("^refs/heads/", "") : "unknown";
                final String summary = "Pushed " + count + " commit" + (count != 1 ? "s" : "") + " to " + branch;
                yield new String[]{"REPO", branch, branch, "📝", summary};
            }
            case "PullRequestEvent" -> {
                final Map<String, Object> pr = (Map<String, Object>) payload.get("pull_request");
                final String action = (String) payload.get("action");
                final int number = pr != null ? toInt(pr.get("number")) : 0;
                final String title = pr != null ? strOrEmpty(pr.get("title")) : "";
                final boolean merged = pr != null && Boolean.TRUE.equals(pr.get("merged"));
                final String verb = merged ? "Merged" : capitalize(action);
                yield new String[]{"PR", String.valueOf(number), title, "🔀", verb + " PR #" + number};
            }
            case "PullRequestReviewEvent" -> {
                final Map<String, Object> pr = (Map<String, Object>) payload.get("pull_request");
                final Map<String, Object> review = (Map<String, Object>) payload.get("review");
                final int number = pr != null ? toInt(pr.get("number")) : 0;
                final String title = pr != null ? strOrEmpty(pr.get("title")) : "";
                final String state = review != null ? strOrEmpty(review.get("state")) : "";
                final String verb = switch (state) {
                    case "approved" -> "Approved";
                    case "changes_requested" -> "Requested changes on";
                    default -> "Commented on";
                };
                yield new String[]{"PR", String.valueOf(number), title, "👁", verb + " PR #" + number};
            }
            case "PullRequestReviewCommentEvent" -> {
                final Map<String, Object> pr = (Map<String, Object>) payload.get("pull_request");
                final int number = pr != null ? toInt(pr.get("number")) : 0;
                final String title = pr != null ? strOrEmpty(pr.get("title")) : "";
                yield new String[]{"PR", String.valueOf(number), title, "💬", "Commented on PR #" + number};
            }
            case "IssuesEvent" -> {
                final Map<String, Object> issue = (Map<String, Object>) payload.get("issue");
                final String action = (String) payload.get("action");
                final int number = issue != null ? toInt(issue.get("number")) : 0;
                final String title = issue != null ? strOrEmpty(issue.get("title")) : "";
                yield new String[]{"ISSUE", String.valueOf(number), title, "🐛", capitalize(action) + " issue #" + number};
            }
            case "IssueCommentEvent" -> {
                final Map<String, Object> issue = (Map<String, Object>) payload.get("issue");
                final int number = issue != null ? toInt(issue.get("number")) : 0;
                final String title = issue != null ? strOrEmpty(issue.get("title")) : "";
                yield new String[]{"ISSUE", String.valueOf(number), title, "💬", "Commented on issue #" + number};
            }
            case "CreateEvent" -> {
                final String refType = strOrEmpty(payload.get("ref_type"));
                final String ref = strOrEmpty(payload.get("ref"));
                final String label = !ref.isEmpty() ? ref : refType;
                yield new String[]{"REPO", label, label, "🌿", "Created " + refType + " " + label};
            }
            case "DeleteEvent" -> {
                final String refType = strOrEmpty(payload.get("ref_type"));
                final String ref = strOrEmpty(payload.get("ref"));
                final String label = !ref.isEmpty() ? ref : refType;
                yield new String[]{"REPO", null, null, "🗑", "Deleted " + refType + " " + label};
            }
            case "ReleaseEvent" -> {
                final Map<String, Object> release = (Map<String, Object>) payload.get("release");
                final String tag = release != null ? strOrEmpty(release.get("tag_name")) : "";
                final String name = release != null && release.get("name") != null ? strOrEmpty(release.get("name")) : tag;
                yield new String[]{"REPO", tag, name, "🚀", "Released " + tag};
            }
            case "CommitCommentEvent" -> {
                final Map<String, Object> comment = (Map<String, Object>) payload.get("comment");
                final String body = comment != null ? firstLine(strOrEmpty(comment.get("body")), 80) : "";
                yield new String[]{"REPO", null, null, "💬", "Commented on commit: " + body};
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
