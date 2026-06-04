package de.focusshift.zeiterfassung.githubactivity;

import de.focus_shift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.activitytype.ActivityTypeService;
import de.focusshift.zeiterfassung.project.ProjectService;
import de.focusshift.zeiterfassung.timeentry.TimeEntryLockService;
import de.focusshift.zeiterfassung.search.HasUserSearch;
import de.focusshift.zeiterfassung.search.UserSearchViewHelper;
import de.focusshift.zeiterfassung.security.CurrentUser;
import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.timeclock.HasTimeClock;
import de.focusshift.zeiterfassung.timeentry.TimeEntryService;
import de.focusshift.zeiterfassung.user.UserSettings;
import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import de.focusshift.zeiterfassung.user.UserSettingsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@Controller
@RequestMapping("/github-activity")
class GitHubActivityController implements HasTimeClock, HasLaunchpad, HasUserSearch {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter OPENED_DATE_FMT = DateTimeFormatter.ofPattern("MMM d");
    private static final long MIN_SUGGESTED_MINUTES = 15;
    private static final Set<String> REVIEW_EVENT_TYPES = Set.of(
        "PullRequestReviewEvent", "PullRequestReviewCommentEvent",
        "IssueCommentEvent"); // IssueCommentEvent on PRs is routed to anchorType=PR by the sync service

    private final UserSettingsService userSettingsService;
    private final UserSettingsProvider userSettingsProvider;
    private final TimeEntryService timeEntryService;
    private final UserSearchViewHelper userSearchViewHelper;
    private final ProjectService projectService;
    private final ActivityTypeService activityTypeService;
    private final TimeEntryLockService timeEntryLockService;
    private final GitHubRawEventRepository eventRepository;
    private final GitHubSyncService syncService;

    GitHubActivityController(UserSettingsService userSettingsService,
                              UserSettingsProvider userSettingsProvider,
                              TimeEntryService timeEntryService,
                              UserSearchViewHelper userSearchViewHelper,
                              ProjectService projectService,
                              ActivityTypeService activityTypeService,
                              TimeEntryLockService timeEntryLockService,
                              GitHubRawEventRepository eventRepository,
                              GitHubSyncService syncService) {
        this.userSettingsService = userSettingsService;
        this.userSettingsProvider = userSettingsProvider;
        this.timeEntryService = timeEntryService;
        this.userSearchViewHelper = userSearchViewHelper;
        this.projectService = projectService;
        this.activityTypeService = activityTypeService;
        this.timeEntryLockService = timeEntryLockService;
        this.eventRepository = eventRepository;
        this.syncService = syncService;
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
        final ZoneId zone = userSettingsProvider.zoneId();

        final Instant dayStart = selectedDate.atStartOfDay(zone).toInstant();
        final Instant dayEnd = selectedDate.plusDays(1).atStartOfDay(zone).toInstant();
        final List<GitHubRawEventEntity> rawEvents =
            eventRepository.findByGithubUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(login, dayStart, dayEnd);

        final List<ActivityAnchor> allAnchors = toAnchors(rawEvents, zone, login, selectedDate);
        final Long userLocalId = currentUser.getUserIdComposite().localId().value();

        final List<ActivityAnchor> prAnchors = allAnchors.stream()
            .filter(a -> "PR".equals(a.anchorType()) && a.prStatus() != null)
            .toList();
        final List<ActivityAnchor> reviewAnchors = allAnchors.stream()
            .filter(a -> "PR".equals(a.anchorType()) && a.reviewOutcome() != null)
            .toList();
        final List<ActivityAnchor> issueAnchors = allAnchors.stream()
            .filter(a -> "ISSUE".equals(a.anchorType()))
            .toList();

        // Scope to PRs opened on or before the selected day — prevents retroactively hiding
        // standalone commits that were pushed before a PR was opened for that branch.
        final Set<String> prHeadKeys = eventRepository
            .findDistinctRepoAndHeadBranchesByUsernameUpToDate(login, dayEnd);

        final List<ActivityAnchor> standaloneAnchors = allAnchors.stream()
            .filter(a -> "REPO".equals(a.anchorType()))
            // Exclude commits on branches that are the head of a known PR
            .filter(a -> a.anchorId() == null || !prHeadKeys.contains(a.repoName() + "|" + a.anchorId()))
            // Exclude groups that contain only branch create/delete events
            .filter(a -> a.events().stream().anyMatch(
                e -> !e.summary().startsWith("Created ") && !e.summary().startsWith("Deleted ")))
            .toList();

        // Synthesize PR anchors for days where only commits were pushed (no PullRequestEvent).
        // Without this, a day with push-only activity on a PR branch would show nothing at all.
        final Set<String> existingPrKeys = prAnchors.stream()
            .map(a -> a.repoName() + "|" + a.anchorId())
            .collect(java.util.stream.Collectors.toSet());
        final List<ActivityAnchor> syntheticPrAnchors = allAnchors.stream()
            .filter(a -> "REPO".equals(a.anchorType()))
            .filter(a -> a.anchorId() != null && prHeadKeys.contains(a.repoName() + "|" + a.anchorId()))
            .filter(a -> a.events().stream().anyMatch(
                e -> !e.summary().startsWith("Created ") && !e.summary().startsWith("Deleted ")))
            .flatMap(repoAnchor -> buildSyntheticPrAnchor(repoAnchor, existingPrKeys, login, zone, selectedDate))
            .toList();
        final List<ActivityAnchor> allPrAnchors = Stream.concat(
            prAnchors.stream(), syntheticPrAnchors.stream()).toList();

        model.addAttribute("date", selectedDate);
        model.addAttribute("prevDate", selectedDate.minusDays(1));
        model.addAttribute("nextDate", selectedDate.plusDays(1));
        model.addAttribute("prAnchors", allPrAnchors);
        model.addAttribute("reviewAnchors", reviewAnchors);
        model.addAttribute("issueAnchors", issueAnchors);
        model.addAttribute("standaloneAnchors", standaloneAnchors);
        model.addAttribute("hasActivity",
            !allPrAnchors.isEmpty() || !reviewAnchors.isEmpty()
            || !issueAnchors.isEmpty() || !standaloneAnchors.isEmpty());
        model.addAttribute("userLocalId", userLocalId);
        model.addAttribute("isLocked", timeEntryLockService.isLocked(selectedDate));
        model.addAttribute("syncConfigured", syncService.isConfigured());
        model.addAttribute("syncMissingConfig", syncService.missingConfig());
        final Instant lastSync = syncService.getLastSyncTime(login);
        model.addAttribute("lastSyncedAt", lastSync != null ? lastSync.atZone(zone) : null);

        return "github-activity/index";
    }

    @PostMapping("/sync")
    String syncNow(@CurrentUser CurrentOidcUser currentUser,
                   @RequestParam(required = false) LocalDate date,
                   RedirectAttributes redirectAttributes) {

        final UserSettings userSettings = userSettingsService.getUserSettings(currentUser.getUserIdComposite());
        if (userSettings.githubLoginVerified() && userSettings.githubLogin().isPresent()) {
            syncService.syncNow(userSettings.githubLogin().get());
        }

        final String redirectDate = (date != null ? date : LocalDate.now()).toString();
        return "redirect:/github-activity?date=" + redirectDate;
    }

    @PostMapping("/dismiss")
    String dismiss(@RequestParam String eventId,
                   @RequestParam(required = false) LocalDate date) {
        eventRepository.findByGithubEventId(eventId).ifPresent(e -> {
            e.setDismissed(true);
            eventRepository.save(e);
        });
        final String redirectDate = (date != null ? date : LocalDate.now()).toString();
        return "redirect:/github-activity?date=" + redirectDate;
    }

    @PostMapping("/mark-logged")
    org.springframework.http.ResponseEntity<Void> markLogged(@RequestParam String eventId) {
        eventRepository.findByGithubEventId(eventId).ifPresent(e -> {
            e.setLoggedAt(java.time.Instant.now());
            eventRepository.save(e);
        });
        return org.springframework.http.ResponseEntity.ok().build();
    }

    @PostMapping("/mark-anchor-logged")
    org.springframework.http.ResponseEntity<Void> markAnchorLogged(
            @RequestParam String repoName,
            @RequestParam String anchorType,
            @RequestParam String anchorId,
            @RequestParam LocalDate date,
            @CurrentUser CurrentOidcUser currentUser) {
        final UserSettings userSettings = userSettingsService.getUserSettings(currentUser.getUserIdComposite());
        final String login = userSettings.githubLogin().orElse(null);
        if (login == null) return org.springframework.http.ResponseEntity.ok().build();
        final ZoneId zone = userSettingsProvider.zoneId();
        final Instant from = date.atStartOfDay(zone).toInstant();
        final Instant to = date.plusDays(1).atStartOfDay(zone).toInstant();
        eventRepository.markAnchorLogged(login, repoName, anchorType, anchorId, from, to, Instant.now());
        return org.springframework.http.ResponseEntity.ok().build();
    }

    @GetMapping("/inline-form")
    String inlineForm(@RequestParam String comment,
                      @RequestParam String date,
                      @RequestParam(required = false) String startTime,
                      @RequestParam(required = false) String duration,
                      @RequestParam Long userLocalId,
                      @RequestParam(required = false) String frameId,
                      Model model) {

        model.addAttribute("comment", comment);
        model.addAttribute("date", date);
        model.addAttribute("startTime", startTime != null ? startTime : "");
        model.addAttribute("duration", duration != null ? duration : "");
        model.addAttribute("userLocalId", userLocalId);
        model.addAttribute("frameId", frameId != null ? frameId : "inline-form-frame");

        try {
            model.addAttribute("projects", projectService.findAllActive());
        } catch (Exception e) {
            model.addAttribute("projects", List.of());
        }
        try {
            model.addAttribute("activityTypes", activityTypeService.findAllActive());
        } catch (Exception e) {
            model.addAttribute("activityTypes", List.of());
        }

        return "github-activity/inline-form";
    }

    private List<ActivityAnchor> toAnchors(List<GitHubRawEventEntity> entities, ZoneId zone,
                                           String login, LocalDate selectedDate) {
        if (entities.isEmpty()) return List.of();

        final Map<String, List<GitHubRawEventEntity>> grouped = new LinkedHashMap<>();
        for (GitHubRawEventEntity e : entities) {
            final String key = e.getRepoName() + "|" + e.getAnchorType() + "|"
                + (e.getAnchorId() != null ? e.getAnchorId() : "");
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(e);
        }

        return grouped.values().stream().<ActivityAnchor>map(group -> {
            final GitHubRawEventEntity first = group.get(0);

            final Instant minTs = group.stream().map(GitHubRawEventEntity::getEventTimestamp)
                .min(Comparator.naturalOrder()).orElseThrow();
            final Instant maxTs = group.stream().map(GitHubRawEventEntity::getEventTimestamp)
                .max(Comparator.naturalOrder()).orElseThrow();

            final String windowStart = TIME_FMT.withZone(zone).format(minTs);
            final String windowEnd = minTs.equals(maxTs) ? null : TIME_FMT.withZone(zone).format(maxTs);

            final long windowMinutes = Duration.between(minTs, maxTs).toMinutes();
            final long suggested = Math.max(MIN_SUGGESTED_MINUTES, windowMinutes);
            final String suggestedDuration = String.format("%02d:%02d", suggested / 60, suggested % 60);

            final boolean isRepo = "REPO".equals(first.getAnchorType());
            final List<AnchorEvent> events = group.stream()
                // Deduplicate by summary: same commit message from multiple push events
                // (e.g. old-format event IDs before the login_commit_sha change)
                .collect(java.util.stream.Collectors.toMap(
                    GitHubRawEventEntity::getEventSummary,
                    e -> e,
                    (a, b) -> a,           // keep first occurrence
                    java.util.LinkedHashMap::new))
                .values().stream()
                .map(e -> new AnchorEvent(
                    e.getEventIcon(),
                    e.getEventSummary(),
                    TIME_FMT.withZone(zone).format(e.getEventTimestamp()),
                    buildEventComment(e),
                    e.getGithubEventId(),
                    e.getLoggedAt() != null,
                    isRepo ? buildCommitUrl(e) : null))
                .toList();

            // anchorTitle may be blank when the GitHub App token strips PR payload fields;
            // fall back to extracting the title from the stored event summary (e.g. "Merged PR #X: Title")
            final String anchorTitle = resolveAnchorTitle(first, group);
            final String anchorLabel = !anchorTitle.isBlank()
                ? anchorTitle
                : (first.getAnchorId() != null ? first.getAnchorId() : "");
            final String anchorRef = buildAnchorRef(first);
            final String prefilledComment = first.getRepoName() + (anchorRef.isEmpty() ? "" : " " + anchorRef)
                + (anchorLabel.isEmpty() ? "" : ": " + anchorLabel);

            // Categorise PR anchors into own PRs vs. reviews
            final boolean isOwnPr = "PR".equals(first.getAnchorType())
                && group.stream().anyMatch(e -> "PullRequestEvent".equals(e.getEventType()));
            final boolean isReview = "PR".equals(first.getAnchorType()) && !isOwnPr
                && group.stream().anyMatch(e -> REVIEW_EVENT_TYPES.contains(e.getEventType()));

            final String prStatus = isOwnPr ? derivePrStatus(group) : null;
            final String openedDate = isOwnPr ? derivePrOpenedDate(login, first, zone, selectedDate) : null;
            final String reviewOutcome = isReview ? deriveReviewOutcome(group) : null;
            final String issueAction = "ISSUE".equals(first.getAnchorType()) ? deriveIssueAction(group) : null;

            final boolean anchorLogged = events.stream().anyMatch(AnchorEvent::logged);

            return new ActivityAnchor(
                first.getRepoName(),
                first.getAnchorType(),
                first.getAnchorId(),
                anchorTitle,
                events,
                windowStart,
                windowEnd,
                suggestedDuration,
                prefilledComment,
                openedDate,
                prStatus,
                reviewOutcome,
                issueAction,
                anchorLogged
            );
        }).toList();
    }

    /**
     * For a REPO anchor (commits on a PR branch) that has no PullRequestEvent on the selected day,
     * look up the PR entity and produce a synthetic PR anchor so the PR appears in the PR section.
     * Returns an empty stream if the PR is already present or cannot be found.
     */
    private Stream<ActivityAnchor> buildSyntheticPrAnchor(ActivityAnchor repoAnchor,
                                                           Set<String> existingPrKeys,
                                                           String login, ZoneId zone,
                                                           LocalDate selectedDate) {
        return eventRepository
            .findFirstByGithubUsernameAndRepoNameAndHeadBranchOrderByEventTimestampDesc(
                login, repoAnchor.repoName(), repoAnchor.anchorId())
            .filter(pr -> !existingPrKeys.contains(pr.getRepoName() + "|" + pr.getAnchorId()))
            .map(pr -> {
                final String anchorTitle = resolveAnchorTitle(pr, List.of(pr));
                final String anchorRef = "PR #" + pr.getAnchorId();
                final String prefilledComment = pr.getRepoName() + " " + anchorRef
                    + (anchorTitle.isBlank() ? "" : ": " + anchorTitle);
                return new ActivityAnchor(
                    pr.getRepoName(),
                    "PR",
                    pr.getAnchorId(),
                    anchorTitle,
                    repoAnchor.events(),           // commit events → drive the time window
                    repoAnchor.windowStart(),
                    repoAnchor.windowEnd(),
                    repoAnchor.suggestedDuration(),
                    prefilledComment,
                    derivePrOpenedDate(login, pr, zone, selectedDate),
                    derivePrStatus(List.of(pr)),   // Open / Merged / Closed from the PR entity
                    null,
                    null,
                    repoAnchor.logged()
                );
            })
            .stream();
    }

    private static String resolveAnchorTitle(GitHubRawEventEntity first, List<GitHubRawEventEntity> group) {
        if (first.getAnchorTitle() != null && !first.getAnchorTitle().isBlank()) {
            return first.getAnchorTitle();
        }
        // Fall back: extract title from "Verb PR/Issue #N: Title" stored in eventSummary
        for (GitHubRawEventEntity e : group) {
            final int colonIdx = e.getEventSummary().indexOf(": ");
            if (colonIdx >= 0) {
                final String candidate = e.getEventSummary().substring(colonIdx + 2).trim();
                if (!candidate.isBlank()) return candidate;
            }
        }
        return first.getAnchorTitle() != null ? first.getAnchorTitle() : "";
    }

    private String derivePrStatus(List<GitHubRawEventEntity> group) {
        for (GitHubRawEventEntity e : group) {
            if (e.getEventSummary().startsWith("Merged")) return "Merged";
            if (e.getEventSummary().startsWith("Closed")) return "Closed";
        }
        return "Open";
    }

    private String derivePrOpenedDate(String login, GitHubRawEventEntity first, ZoneId zone, LocalDate selectedDate) {
        return eventRepository
            .findFirstByGithubUsernameAndRepoNameAndAnchorTypeAndAnchorIdOrderByEventTimestampAsc(
                login, first.getRepoName(), "PR", first.getAnchorId())
            .map(oldest -> {
                final LocalDate openedDay = oldest.getEventTimestamp().atZone(zone).toLocalDate();
                return openedDay.isBefore(selectedDate)
                    ? OPENED_DATE_FMT.withZone(zone).format(oldest.getEventTimestamp())
                    : null;
            })
            .orElse(null);
    }

    private String deriveReviewOutcome(List<GitHubRawEventEntity> group) {
        boolean approved = false, changesRequested = false;
        for (GitHubRawEventEntity e : group) {
            if (e.getEventSummary().startsWith("Approved")) approved = true;
            if (e.getEventSummary().startsWith("Requested changes")) changesRequested = true;
        }
        return approved ? "Approved" : changesRequested ? "Changes requested" : "Commented";
    }

    private String deriveIssueAction(List<GitHubRawEventEntity> group) {
        boolean closed = false, opened = false, commented = false;
        for (GitHubRawEventEntity e : group) {
            if (e.getEventSummary().startsWith("Closed")) closed = true;
            else if (e.getEventSummary().startsWith("Opened")) opened = true;
            else if (e.getEventSummary().startsWith("Commented")) commented = true;
        }
        return closed ? "Closed" : opened ? "Opened" : commented ? "Commented" : "Updated";
    }

    private static String buildCommitUrl(GitHubRawEventEntity e) {
        // Event ID format: {login}_commit_{fullSha}
        final String id = e.getGithubEventId();
        final int shaStart = id.lastIndexOf('_') + 1;
        if (shaStart <= 0 || shaStart >= id.length()) return null;
        return "https://github.com/" + e.getRepoName() + "/commit/" + id.substring(shaStart);
    }

    private static String buildEventComment(GitHubRawEventEntity e) {
        final String shortRepo = e.getRepoName().contains("/")
            ? e.getRepoName().substring(e.getRepoName().lastIndexOf('/') + 1)
            : e.getRepoName();
        return shortRepo + ": " + e.getEventSummary();
    }

    private static String buildAnchorRef(GitHubRawEventEntity e) {
        return switch (e.getAnchorType()) {
            case "PR" -> e.getAnchorId() != null ? "PR #" + e.getAnchorId() : "";
            case "ISSUE" -> e.getAnchorId() != null ? "Issue #" + e.getAnchorId() : "";
            default -> "";
        };
    }

}
