package de.focusshift.zeiterfassung.githubactivity;

import de.focus_shift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.activitytype.ActivityTypeService;
import de.focusshift.zeiterfassung.project.ProjectService;
import de.focusshift.zeiterfassung.search.HasUserSearch;
import de.focusshift.zeiterfassung.search.UserSearchViewHelper;
import de.focusshift.zeiterfassung.security.CurrentUser;
import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.timeclock.HasTimeClock;
import de.focusshift.zeiterfassung.timeentry.TimeEntryService;
import de.focusshift.zeiterfassung.user.UserSettings;
import de.focusshift.zeiterfassung.user.UserSettingsService;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.view.RedirectView;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


@Controller
@RequestMapping("/github-activity")
class GitHubActivityController implements HasTimeClock, HasLaunchpad, HasUserSearch {

    /** Cache GitHub events per username for 5 minutes to avoid hitting the 60 req/hr unauthenticated rate limit. */
    private record CacheEntry(List<Map<String, Object>> events, Instant fetchedAt) {}
    private static final long CACHE_TTL_SECONDS = 300; // 5 minutes
    private final ConcurrentHashMap<String, CacheEntry> eventCache = new ConcurrentHashMap<>();

    private final UserSettingsService userSettingsService;
    private final TimeEntryService timeEntryService;
    private final UserSearchViewHelper userSearchViewHelper;
    private final ProjectService projectService;
    private final ActivityTypeService activityTypeService;
    private final RestClient restClient;

    GitHubActivityController(UserSettingsService userSettingsService,
                              TimeEntryService timeEntryService,
                              UserSearchViewHelper userSearchViewHelper,
                              ProjectService projectService,
                              ActivityTypeService activityTypeService) {
        this.userSettingsService = userSettingsService;
        this.timeEntryService = timeEntryService;
        this.userSearchViewHelper = userSearchViewHelper;
        this.projectService = projectService;
        this.activityTypeService = activityTypeService;
        this.restClient = RestClient.builder()
            .defaultHeader("User-Agent", "zeiterfassung")
            .defaultHeader("Accept", "application/vnd.github+json")
            .build();
    }

    @GetMapping
    String index(@RequestParam(required = false) LocalDate date,
                 @CurrentUser CurrentOidcUser currentUser,
                 Model model) {

        final LocalDate selectedDate = date != null ? date : LocalDate.now();
        final UserSettings userSettings = userSettingsService.getUserSettings(currentUser.getUserIdComposite());

        if (!userSettings.githubLoginVerified() || userSettings.githubLogin().isEmpty()) {
            model.addAttribute("accountUrl", "/account");
            return "github-activity/no-github-login";
        }

        final String login = userSettings.githubLogin().get();
        final String token = userSettings.githubToken().orElse(null);
        final List<Map<String, Object>> events = fetchGitHubEvents(login, token);
        final List<GitHubActivityGroup> groups = parseAndGroupEvents(events, selectedDate);

        final Long userLocalId = currentUser.getUserIdComposite().localId().value();
        model.addAttribute("date", selectedDate);
        model.addAttribute("prevDate", selectedDate.minusDays(1));
        model.addAttribute("nextDate", selectedDate.plusDays(1));
        model.addAttribute("groups", groups);
        model.addAttribute("userLocalId", userLocalId);

        return "github-activity/index";
    }

    @GetMapping("/inline-form")
    String inlineForm(@RequestParam String comment,
                      @RequestParam String date,
                      @RequestParam(required = false) String startTime,
                      @RequestParam Long userLocalId,
                      @RequestParam(required = false) String frameId,
                      Model model) {

        model.addAttribute("comment", comment);
        model.addAttribute("date", date);
        model.addAttribute("startTime", startTime != null ? startTime : "");
        model.addAttribute("userLocalId", userLocalId);
        model.addAttribute("formAction", "/timeentries");
        model.addAttribute("frameId", frameId != null ? frameId : "inline-form-frame");

        // Load projects / activity types defensively — if the table query fails the inline form still works.
        try {
            model.addAttribute("projects", projectService.findAllActive());
        } catch (Exception e) {
            java.util.logging.Logger.getLogger(getClass().getName()).warning("Could not load projects: " + e.getMessage());
            model.addAttribute("projects", java.util.List.of());
        }
        try {
            model.addAttribute("activityTypes", activityTypeService.findAllActive());
        } catch (Exception e) {
            java.util.logging.Logger.getLogger(getClass().getName()).warning("Could not load activityTypes: " + e.getMessage());
            model.addAttribute("activityTypes", java.util.List.of());
        }

        return "github-activity/inline-form";
    }

    @PostMapping("/create-entries")
    RedirectView createEntries(@RequestParam List<String> comment,
                               @RequestParam(required = false) List<String> startTime,
                               @RequestParam String date,
                               @RequestParam Long userLocalId,
                               @CurrentUser CurrentOidcUser currentUser) {

        final LocalDate entryDate = LocalDate.parse(date);
        final UserLocalId currentUserLocalId = currentUser.getUserIdComposite().localId();
        final ZoneId zone = ZoneId.systemDefault();

        for (int i = 0; i < comment.size(); i++) {
            final String c = comment.get(i);
            if (c == null || c.isBlank()) continue;

            ZonedDateTime start;
            if (startTime != null && i < startTime.size() && startTime.get(i) != null && !startTime.get(i).isBlank()) {
                try {
                    final LocalTime lt = LocalTime.parse(startTime.get(i), DateTimeFormatter.ofPattern("HH:mm"));
                    start = ZonedDateTime.of(entryDate, lt, zone);
                } catch (Exception e) {
                    start = ZonedDateTime.of(entryDate, LocalTime.NOON, zone);
                }
            } else {
                start = ZonedDateTime.of(entryDate, LocalTime.NOON, zone);
            }
            final ZonedDateTime end = start.plusMinutes(30);
            timeEntryService.createTimeEntry(currentUserLocalId, c, start, end, false, null, null);
        }

        return new RedirectView("/github-activity?date=" + date);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchGitHubEvents(String login, @jakarta.annotation.Nullable String token) {
        final String cacheKey = login + (token != null ? ":auth" : ":pub");
        final CacheEntry cached = eventCache.get(cacheKey);
        if (cached != null && Instant.now().minusSeconds(CACHE_TTL_SECONDS).isBefore(cached.fetchedAt())) {
            return cached.events();
        }
        try {
            final var request = restClient.get()
                .uri("https://api.github.com/users/{login}/events?per_page=100", login);
            if (token != null && !token.isBlank()) {
                request.header("Authorization", "Bearer " + token);
            }
            final List<Map<String, Object>> events = request.retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            final List<Map<String, Object>> result = events != null ? events : List.of();
            eventCache.put(cacheKey, new CacheEntry(result, Instant.now()));
            return result;
        } catch (Exception e) {
            return cached != null ? cached.events() : List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<GitHubActivityGroup> parseAndGroupEvents(List<Map<String, Object>> rawEvents, LocalDate date) {
        if (rawEvents == null) return List.of();

        final ZoneId zone = ZoneId.systemDefault();
        final Map<String, List<GitHubEvent>> byRepo = new LinkedHashMap<>();

        for (Map<String, Object> raw : rawEvents) {
            final String createdAt = (String) raw.get("created_at");
            if (createdAt == null) continue;

            final Instant instant = Instant.parse(createdAt);
            final LocalDate eventDate = instant.atZone(zone).toLocalDate();
            if (!eventDate.equals(date)) continue;

            final String startTime = instant.atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm"));
            final String type = (String) raw.get("type");
            final Map<String, Object> repoMap = (Map<String, Object>) raw.get("repo");
            final String repoName = repoMap != null ? (String) repoMap.get("name") : "unknown";
            final Map<String, Object> payload = (Map<String, Object>) raw.get("payload");

            final GitHubEvent event = parseEvent(type, repoName, payload, startTime);
            if (event != null) {
                byRepo.computeIfAbsent(repoName, k -> new ArrayList<>()).add(event);
            }
        }

        return byRepo.entrySet().stream().map(entry -> {
            final String repoName = entry.getKey();
            final List<GitHubEvent> events = entry.getValue();
            final String combined = repoName + ": " + events.stream()
                .map(GitHubEvent::title)
                .collect(Collectors.joining("; "));
            return new GitHubActivityGroup(repoName, events, combined);
        }).toList();
    }

    @SuppressWarnings("unchecked")
    private GitHubEvent parseEvent(String type, String repoName, Map<String, Object> payload, String startTime) {
        if (payload == null) payload = Map.of();

        return switch (type != null ? type : "") {
            case "PushEvent" -> {
                // payload.size is the authoritative commit count — commits[] is capped at 20
                // and is empty for bot/merge pushes even when actual commits were pushed.
                // payload.distinct_size excludes merge commits; prefer it when > 0.
                final int size = toInt(payload.get("size"));
                final int distinctSize = toInt(payload.get("distinct_size"));
                final int count = distinctSize > 0 ? distinctSize : size;

                // Skip empty pushes (bot commits, force-pushes with no new content, etc.)
                if (count == 0) yield null;

                final List<Map<String, Object>> commits = (List<Map<String, Object>>) payload.get("commits");
                final boolean hasCommitDetails = commits != null && !commits.isEmpty();

                final String title = "Pushed " + count + " commit" + (count != 1 ? "s" : "");
                final String firstMsg = hasCommitDetails ? firstLine((String) commits.get(0).get("message"), 72) : "";
                final String detail = hasCommitDetails
                    ? commits.stream().limit(2).map(c -> firstLine((String) c.get("message"), 60)).collect(Collectors.joining(" · "))
                    : "";
                final String prefilledComment = hasCommitDetails
                    ? repoName + ": " + firstMsg + (count > 1 ? " (+" + (count - 1) + " more)" : "")
                    : repoName + ": " + title;
                yield new GitHubEvent(type, "📝", title, detail, prefilledComment, startTime);
            }
            case "PullRequestEvent" -> {
                final String action = (String) payload.get("action");
                final Map<String, Object> pr = (Map<String, Object>) payload.get("pull_request");
                final int number = pr != null ? toInt(pr.get("number")) : 0;
                final String prTitle = pr != null ? (String) pr.get("title") : "";
                final boolean merged = pr != null && Boolean.TRUE.equals(pr.get("merged"));
                final String displayAction = merged ? "Merged" : capitalize(action);
                final String title = displayAction + " PR #" + number;
                final String prefilledComment = displayAction + " PR #" + number + ": " + prTitle + " (" + repoName + ")";
                yield new GitHubEvent(type, "🔀", title, prTitle, prefilledComment, startTime);
            }
            case "PullRequestReviewEvent" -> {
                final Map<String, Object> pr = (Map<String, Object>) payload.get("pull_request");
                final int number = pr != null ? toInt(pr.get("number")) : 0;
                final String prTitle = pr != null ? (String) pr.get("title") : "";
                final String title = "Reviewed PR #" + number;
                final String prefilledComment = "Reviewed PR #" + number + ": " + prTitle + " (" + repoName + ")";
                yield new GitHubEvent(type, "👁", title, prTitle, prefilledComment, startTime);
            }
            case "IssuesEvent" -> {
                final String action = (String) payload.get("action");
                final Map<String, Object> issue = (Map<String, Object>) payload.get("issue");
                final int number = issue != null ? toInt(issue.get("number")) : 0;
                final String issueTitle = issue != null ? (String) issue.get("title") : "";
                final String title = capitalize(action) + " issue #" + number;
                final String prefilledComment = capitalize(action) + " issue #" + number + ": " + issueTitle + " (" + repoName + ")";
                yield new GitHubEvent(type, "🐛", title, issueTitle, prefilledComment, startTime);
            }
            case "IssueCommentEvent" -> {
                final Map<String, Object> issue = (Map<String, Object>) payload.get("issue");
                final Map<String, Object> comment = (Map<String, Object>) payload.get("comment");
                final int number = issue != null ? toInt(issue.get("number")) : 0;
                final String issueTitle = issue != null ? (String) issue.get("title") : "";
                final String body = comment != null ? (String) comment.get("body") : "";
                final String detail = body != null && body.length() > 100 ? body.substring(0, 100) : body;
                final String prefilledComment = "Commented on #" + number + ": " + issueTitle + " (" + repoName + ")";
                yield new GitHubEvent(type, "💬", "Commented on #" + number, detail != null ? detail : "", prefilledComment, startTime);
            }
            case "CreateEvent" -> {
                final String refType = (String) payload.get("ref_type"); // "branch", "tag", "repository"
                final String ref = (String) payload.get("ref");
                final String what = ref != null && !ref.isEmpty() ? refType + " " + ref : refType;
                final String title = "Created " + what;
                yield new GitHubEvent(type, "🌿", title, "", repoName + ": " + title, startTime);
            }
            case "DeleteEvent" -> {
                final String refType = (String) payload.get("ref_type");
                final String ref = (String) payload.get("ref");
                final String what = ref != null && !ref.isEmpty() ? refType + " " + ref : refType;
                final String title = "Deleted " + what;
                yield new GitHubEvent(type, "🗑", title, "", repoName + ": " + title, startTime);
            }
            case "ForkEvent" -> {
                final Map<String, Object> forkee = (Map<String, Object>) payload.get("forkee");
                final String forkName = forkee != null ? (String) forkee.get("full_name") : repoName;
                final String title = "Forked to " + forkName;
                yield new GitHubEvent(type, "🍴", title, "", title, startTime);
            }
            case "WatchEvent" -> {
                final String title = "Starred " + repoName;
                yield new GitHubEvent(type, "⭐", title, "", title, startTime);
            }
            case "ReleaseEvent" -> {
                final Map<String, Object> release = (Map<String, Object>) payload.get("release");
                final String tagName = release != null ? (String) release.get("tag_name") : "";
                final String releaseName = release != null && release.get("name") != null ? (String) release.get("name") : tagName;
                final String action = capitalize((String) payload.get("action"));
                final String title = action + " release " + tagName;
                yield new GitHubEvent(type, "🚀", title, releaseName, repoName + ": " + title, startTime);
            }
            case "CommitCommentEvent" -> {
                final Map<String, Object> comment = (Map<String, Object>) payload.get("comment");
                final String body = comment != null ? firstLine((String) comment.get("body"), 80) : "";
                final String title = "Commented on commit";
                yield new GitHubEvent(type, "💬", title, body, repoName + ": " + title, startTime);
            }
            default -> {
                final String displayType = type != null ? type.replaceAll("Event$", "") : "Unknown";
                final String prefilledComment = repoName + ": " + displayType;
                yield new GitHubEvent(type, "⚡", displayType, "", prefilledComment, startTime);
            }
        };
    }

    private static String firstLine(String s, int maxLen) {
        if (s == null) return "";
        final String first = s.split("\n")[0].trim();
        return first.length() > maxLen ? first.substring(0, maxLen) : first;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static int toInt(Object o) {
        if (o instanceof Number n) return n.intValue();
        return 0;
    }
}
