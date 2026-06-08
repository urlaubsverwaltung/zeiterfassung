package de.focusshift.zeiterfassung.gitactivity;

import de.focus_shift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.activitytype.ActivityTypeService;
import de.focusshift.zeiterfassung.project.ProjectService;
import de.focusshift.zeiterfassung.settings.CategorisationSettingsService;
import de.focusshift.zeiterfassung.timeentry.TimeEntry;
import de.focusshift.zeiterfassung.timeentry.TimeEntryLockService;
import de.focusshift.zeiterfassung.search.HasUserSearch;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.search.UserSearchViewHelper;
import de.focusshift.zeiterfassung.security.CurrentUser;
import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.timeclock.HasTimeClock;
import de.focusshift.zeiterfassung.timeentry.TimeEntryService;
import de.focusshift.zeiterfassung.settings.WorkingTimeSettings;
import de.focusshift.zeiterfassung.settings.WorkingTimeSettingsService;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
@RequestMapping("/github-activity")
class GitActivityController implements HasTimeClock, HasLaunchpad, HasUserSearch {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter OPENED_DATE_FMT = DateTimeFormatter.ofPattern("MMM d");
    private static final Set<String> REVIEW_EVENT_TYPES = Set.of(
        "PullRequestReviewEvent", "PullRequestReviewCommentEvent",
        "IssueCommentEvent");

    private final UserSettingsService userSettingsService;
    private final UserSettingsProvider userSettingsProvider;
    private final TimeEntryService timeEntryService;
    private final UserSearchViewHelper userSearchViewHelper;
    private final ProjectService projectService;
    private final ActivityTypeService activityTypeService;
    private final TimeEntryLockService timeEntryLockService;
    private final GitActivityRawEventRepository eventRepository;
    private final GitOAuthTokenRepository oAuthTokenRepository;
    private final GitHubActivityProvider gitHubProvider;
    private final WorkingTimeSettingsService workingTimeSettingsService;
    private final CategorisationSettingsService categorisationSettingsService;

    GitActivityController(UserSettingsService userSettingsService,
                          UserSettingsProvider userSettingsProvider,
                          TimeEntryService timeEntryService,
                          UserSearchViewHelper userSearchViewHelper,
                          ProjectService projectService,
                          ActivityTypeService activityTypeService,
                          TimeEntryLockService timeEntryLockService,
                          GitActivityRawEventRepository eventRepository,
                          GitOAuthTokenRepository oAuthTokenRepository,
                          GitHubActivityProvider gitHubProvider,
                          WorkingTimeSettingsService workingTimeSettingsService,
                          CategorisationSettingsService categorisationSettingsService) {
        this.userSettingsService = userSettingsService;
        this.userSettingsProvider = userSettingsProvider;
        this.timeEntryService = timeEntryService;
        this.userSearchViewHelper = userSearchViewHelper;
        this.projectService = projectService;
        this.activityTypeService = activityTypeService;
        this.timeEntryLockService = timeEntryLockService;
        this.eventRepository = eventRepository;
        this.oAuthTokenRepository = oAuthTokenRepository;
        this.gitHubProvider = gitHubProvider;
        this.workingTimeSettingsService = workingTimeSettingsService;
        this.categorisationSettingsService = categorisationSettingsService;
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
        final List<GitActivityRawEventEntity> rawEvents =
            eventRepository.findByPlatformUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(login, dayStart, dayEnd);

        final Set<String> prAnchorIds = rawEvents.stream()
            .filter(e -> "PR".equals(e.getAnchorType()) && e.getAnchorId() != null)
            .map(GitActivityRawEventEntity::getAnchorId)
            .collect(Collectors.toSet());
        final Map<String, Instant> earliestPrTimestamps = prAnchorIds.isEmpty() ? Map.of()
            : eventRepository.findEarliestPrTimestamps(login, prAnchorIds).stream()
                .collect(Collectors.toMap(
                    row -> (String) row[0] + "|" + (String) row[1],
                    row -> toInstant(row[2])));

        final List<ActivityAnchor> allAnchors = toAnchors(rawEvents, zone, login, selectedDate, earliestPrTimestamps);
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

        final Set<String> prHeadKeys = eventRepository
            .findDistinctRepoAndHeadBranchesByUsernameUpToDate(login, dayEnd);

        final List<ActivityAnchor> standaloneAnchors = allAnchors.stream()
            .filter(a -> "REPO".equals(a.anchorType()))
            .filter(a -> a.anchorId() == null || !prHeadKeys.contains(a.repoName() + "|" + a.anchorId()))
            .filter(a -> a.events().stream().anyMatch(
                e -> !e.summary().startsWith("Created ") && !e.summary().startsWith("Deleted ")))
            .toList();

        final Set<String> existingPrKeys = prAnchors.stream()
            .map(a -> a.repoName() + "|" + a.anchorId())
            .collect(java.util.stream.Collectors.toSet());
        final List<ActivityAnchor> syntheticPrAnchors = allAnchors.stream()
            .filter(a -> "REPO".equals(a.anchorType()))
            .filter(a -> a.anchorId() != null && prHeadKeys.contains(a.repoName() + "|" + a.anchorId()))
            .filter(a -> a.events().stream().anyMatch(
                e -> !e.summary().startsWith("Created ") && !e.summary().startsWith("Deleted ")))
            .flatMap(repoAnchor -> buildSyntheticPrAnchor(repoAnchor, existingPrKeys, login, zone, selectedDate, earliestPrTimestamps))
            .toList();
        final List<ActivityAnchor> allPrAnchors = Stream.concat(
            prAnchors.stream(), syntheticPrAnchors.stream()).toList();

        model.addAttribute("date", selectedDate);
        model.addAttribute("prevDate", selectedDate.minusDays(1));
        model.addAttribute("nextDate", selectedDate.plusDays(1));
        model.addAttribute("isToday", selectedDate.equals(LocalDate.now(zone)));
        model.addAttribute("prAnchors", allPrAnchors);
        model.addAttribute("reviewAnchors", reviewAnchors);
        model.addAttribute("issueAnchors", issueAnchors);
        final boolean showStandaloneCommits = userSettings.showStandaloneCommits();
        model.addAttribute("standaloneAnchors", standaloneAnchors);
        model.addAttribute("showStandaloneCommits", showStandaloneCommits);
        model.addAttribute("hasActivity",
            !allPrAnchors.isEmpty() || !reviewAnchors.isEmpty()
            || !issueAnchors.isEmpty() || (showStandaloneCommits && !standaloneAnchors.isEmpty()));
        model.addAttribute("userLocalId", userLocalId);
        final boolean isLocked = timeEntryLockService.isLocked(selectedDate);
        model.addAttribute("isLocked", isLocked);
        final boolean syncConfigured = gitHubProvider.isConfigured();
        model.addAttribute("syncConfigured", syncConfigured);
        model.addAttribute("syncMissingConfig", gitHubProvider.missingConfig());
        final Instant lastSync = gitHubProvider.getLastSyncTime(login);
        model.addAttribute("lastSyncedAt", lastSync != null ? lastSync.atZone(zone) : null);
        final boolean rateLimitSafe = gitHubProvider.isRateLimitSafe();
        final int rateLimitPercent = gitHubProvider.getRateLimitPercent();
        model.addAttribute("rateLimitSafe", rateLimitSafe);
        model.addAttribute("rateLimitPercent", rateLimitPercent);
        model.addAttribute("rateLimitRemaining", gitHubProvider.getRateLimitRemaining());
        model.addAttribute("rateLimitTotal", gitHubProvider.getRateLimitTotal());
        model.addAttribute("rateLimitFillColor",
            rateLimitPercent > 60 ? "#22c55e" : rateLimitPercent > 30 ? "#f59e0b" : "#ef4444");
        final Instant rateLimitReset = gitHubProvider.getRateLimitReset();
        if (!rateLimitSafe && rateLimitReset.isAfter(Instant.MIN)) {
            model.addAttribute("rateLimitResetAt",
                rateLimitReset.atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm")));
            model.addAttribute("rateLimitResetEpoch", rateLimitReset.getEpochSecond());
        }

        final Duration loggedTotal = timeEntryService
            .getEntries(selectedDate, selectedDate.plusDays(1), new UserLocalId(userLocalId))
            .stream()
            .map(TimeEntry::durationInMinutes)
            .reduce(Duration.ZERO, Duration::plus);
        final long totalMinutes = loggedTotal.toMinutes();
        if (totalMinutes > 0) {
            final long hours = totalMinutes / 60;
            final long minutes = totalMinutes % 60;
            final String formatted = hours > 0
                ? (minutes > 0 ? hours + "h " + minutes + "m" : hours + "h")
                : minutes + "m";
            model.addAttribute("loggedDuration", formatted);
        }

        final boolean isToday = selectedDate.equals(LocalDate.now(zone));
        final LocalDate lastSyncDay = lastSync != null ? lastSync.atZone(zone).toLocalDate() : null;
        final boolean autoSync = syncConfigured && rateLimitSafe && isToday && !isLocked
            && (lastSyncDay == null || lastSyncDay.isBefore(selectedDate));
        model.addAttribute("autoSync", autoSync);

        return "github-activity/index";
    }

    @PostMapping("/sync")
    String syncNow(@CurrentUser CurrentOidcUser currentUser,
                   @RequestParam(required = false) LocalDate date,
                   RedirectAttributes redirectAttributes) {

        final UserSettings userSettings = userSettingsService.getUserSettings(currentUser.getUserIdComposite());
        if (userSettings.githubLoginVerified() && userSettings.githubLogin().isPresent()) {
            gitHubProvider.syncUser(userSettings.githubLogin().get());
        }

        final String redirectDate = (date != null ? date : LocalDate.now()).toString();
        return "redirect:/github-activity?date=" + redirectDate;
    }

    @PostMapping("/dismiss")
    String dismiss(@RequestParam String eventId,
                   @RequestParam(required = false) LocalDate date,
                   @CurrentUser CurrentOidcUser currentUser) {
        final Set<String> usernames = resolvePlatformUsernames(currentUser);
        eventRepository.findByPlatformEventId(eventId).ifPresent(e -> {
            if (usernames.contains(e.getPlatformUsername())) {
                e.setDismissed(true);
                eventRepository.save(e);
            }
        });
        final String redirectDate = (date != null ? date : LocalDate.now()).toString();
        return "redirect:/github-activity?date=" + redirectDate;
    }

    @PostMapping("/mark-logged")
    org.springframework.http.ResponseEntity<Void> markLogged(@RequestParam String eventId,
                                                              @CurrentUser CurrentOidcUser currentUser) {
        final Set<String> usernames = resolvePlatformUsernames(currentUser);
        eventRepository.findByPlatformEventId(eventId).ifPresent(e -> {
            if (usernames.contains(e.getPlatformUsername())) {
                e.setLoggedAt(java.time.Instant.now());
                eventRepository.save(e);
            }
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

        model.addAttribute("projects", projectService.findAllActive());
        model.addAttribute("activityTypes", activityTypeService.findAllActive());
        model.addAttribute("categorisationSettings", categorisationSettingsService.getCategorisationSettings());

        return "github-activity/inline-form";
    }

    private List<ActivityAnchor> toAnchors(List<GitActivityRawEventEntity> entities, ZoneId zone,
                                           String login, LocalDate selectedDate,
                                           Map<String, Instant> earliestPrTimestamps) {
        if (entities.isEmpty()) return List.of();

        final Map<String, List<GitActivityRawEventEntity>> grouped = new LinkedHashMap<>();
        for (GitActivityRawEventEntity e : entities) {
            final String key = e.getRepoName() + "|" + e.getAnchorType() + "|"
                + (e.getAnchorId() != null ? e.getAnchorId() : "");
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(e);
        }

        return grouped.values().stream().<ActivityAnchor>map(group -> {
            final GitActivityRawEventEntity first = group.get(0);

            final Instant minTs = group.stream().map(GitActivityRawEventEntity::getEventTimestamp)
                .min(Comparator.naturalOrder()).orElseThrow();
            final Instant maxTs = group.stream().map(GitActivityRawEventEntity::getEventTimestamp)
                .max(Comparator.naturalOrder()).orElseThrow();

            final String windowStart = TIME_FMT.withZone(zone).format(minTs);
            final String windowEnd = minTs.equals(maxTs) ? null : TIME_FMT.withZone(zone).format(maxTs);

            final WorkingTimeSettings wtSettings = workingTimeSettingsService.getWorkingTimeSettings();
            final long rounding = wtSettings.timeRoundingMinutes();
            final long minSuggested = wtSettings.minSuggestedMinutes();
            final long windowMinutes = Duration.between(minTs, maxTs).toMinutes();
            final long rounded = ((Math.max(1, windowMinutes) + rounding - 1) / rounding) * rounding;
            final long suggested = Math.max(minSuggested, rounded);
            final String suggestedDuration = String.format("%02d:%02d", suggested / 60, suggested % 60);

            final boolean isRepo = "REPO".equals(first.getAnchorType());
            final List<AnchorEvent> events = group.stream()
                .collect(java.util.stream.Collectors.toMap(
                    GitActivityRawEventEntity::getEventSummary,
                    e -> e,
                    (a, b) -> a,
                    java.util.LinkedHashMap::new))
                .values().stream()
                .map(e -> new AnchorEvent(
                    e.getEventIcon(),
                    e.getEventSummary(),
                    TIME_FMT.withZone(zone).format(e.getEventTimestamp()),
                    buildEventComment(e),
                    e.getPlatformEventId(),
                    e.getLoggedAt() != null,
                    isRepo ? buildCommitUrl(e) : null))
                .toList();

            final String anchorTitle = resolveAnchorTitle(first, group);
            final String anchorLabel = !anchorTitle.isBlank()
                ? anchorTitle
                : (first.getAnchorId() != null ? first.getAnchorId() : "");
            final String anchorRef = buildAnchorRef(first);
            final String prefilledComment = first.getRepoName() + (anchorRef.isEmpty() ? "" : " " + anchorRef)
                + (anchorLabel.isEmpty() ? "" : ": " + anchorLabel);

            final boolean isOwnPr = "PR".equals(first.getAnchorType())
                && group.stream().anyMatch(e -> "PullRequestEvent".equals(e.getEventType()));
            final boolean isReview = "PR".equals(first.getAnchorType()) && !isOwnPr
                && group.stream().anyMatch(e -> REVIEW_EVENT_TYPES.contains(e.getEventType()));

            final String prStatus = isOwnPr ? derivePrStatus(group) : null;
            final String openedDate = isOwnPr ? derivePrOpenedDate(earliestPrTimestamps, first, zone, selectedDate) : null;
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

    private Stream<ActivityAnchor> buildSyntheticPrAnchor(ActivityAnchor repoAnchor,
                                                           Set<String> existingPrKeys,
                                                           String login, ZoneId zone,
                                                           LocalDate selectedDate,
                                                           Map<String, Instant> earliestPrTimestamps) {
        java.util.Optional<GitActivityRawEventEntity> prOpt = eventRepository
            .findFirstByPlatformUsernameAndRepoNameAndHeadBranchOrderByEventTimestampDesc(
                login, repoAnchor.repoName(), repoAnchor.anchorId());
        if (prOpt.isEmpty()) {
            prOpt = eventRepository
                .findFirstByPlatformUsernameAndHeadRepoNameAndHeadBranchOrderByEventTimestampDesc(
                    login, repoAnchor.repoName(), repoAnchor.anchorId());
        }
        return prOpt
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
                    repoAnchor.events(),
                    repoAnchor.windowStart(),
                    repoAnchor.windowEnd(),
                    repoAnchor.suggestedDuration(),
                    prefilledComment,
                    derivePrOpenedDate(earliestPrTimestamps, pr, zone, selectedDate),
                    derivePrStatus(List.of(pr)),
                    null,
                    null,
                    repoAnchor.logged()
                );
            })
            .stream();
    }

    private static String resolveAnchorTitle(GitActivityRawEventEntity first, List<GitActivityRawEventEntity> group) {
        if (first.getAnchorTitle() != null && !first.getAnchorTitle().isBlank()) {
            return first.getAnchorTitle();
        }
        for (GitActivityRawEventEntity e : group) {
            final int colonIdx = e.getEventSummary().indexOf(": ");
            if (colonIdx >= 0) {
                final String candidate = e.getEventSummary().substring(colonIdx + 2).trim();
                if (!candidate.isBlank()) return candidate;
            }
        }
        return first.getAnchorTitle() != null ? first.getAnchorTitle() : "";
    }

    private String derivePrStatus(List<GitActivityRawEventEntity> group) {
        for (GitActivityRawEventEntity e : group) {
            if (e.getEventSummary().startsWith("Merged")) return "Merged";
            if (e.getEventSummary().startsWith("Closed")) return "Closed";
        }
        return "Open";
    }

    private String derivePrOpenedDate(Map<String, Instant> earliestPrTimestamps,
                                       GitActivityRawEventEntity first,
                                       ZoneId zone, LocalDate selectedDate) {
        Instant oldest = earliestPrTimestamps.get(first.getRepoName() + "|" + first.getAnchorId());
        if (oldest == null) {
            // Fallback for synthetic PR anchors whose event wasn't in the current day's raw events
            oldest = eventRepository
                .findFirstByPlatformUsernameAndRepoNameAndAnchorTypeAndAnchorIdOrderByEventTimestampAsc(
                    first.getPlatformUsername(), first.getRepoName(), "PR", first.getAnchorId())
                .map(GitActivityRawEventEntity::getEventTimestamp)
                .orElse(null);
        }
        if (oldest == null) return null;
        final LocalDate openedDay = oldest.atZone(zone).toLocalDate();
        return openedDay.isBefore(selectedDate) ? OPENED_DATE_FMT.withZone(zone).format(oldest) : null;
    }

    private static Instant toInstant(Object o) {
        if (o instanceof Instant i) return i;
        if (o instanceof java.sql.Timestamp t) return t.toInstant();
        return Instant.EPOCH;
    }

    private String deriveReviewOutcome(List<GitActivityRawEventEntity> group) {
        boolean approved = false, changesRequested = false;
        for (GitActivityRawEventEntity e : group) {
            if (e.getEventSummary().startsWith("Approved")) approved = true;
            if (e.getEventSummary().startsWith("Requested changes")) changesRequested = true;
        }
        return approved ? "Approved" : changesRequested ? "Changes requested" : "Commented";
    }

    private String deriveIssueAction(List<GitActivityRawEventEntity> group) {
        boolean closed = false, opened = false, commented = false;
        for (GitActivityRawEventEntity e : group) {
            if (e.getEventSummary().startsWith("Closed")) closed = true;
            else if (e.getEventSummary().startsWith("Opened")) opened = true;
            else if (e.getEventSummary().startsWith("Commented")) commented = true;
        }
        return closed ? "Closed" : opened ? "Opened" : commented ? "Commented" : "Updated";
    }

    private static String buildCommitUrl(GitActivityRawEventEntity e) {
        final String id = e.getPlatformEventId();
        final int shaStart = id.lastIndexOf('_') + 1;
        if (shaStart <= 0 || shaStart >= id.length()) return null;
        final String sha = id.substring(shaStart);

        return switch (e.getPlatform() != null ? e.getPlatform() : "GITHUB") {
            case "BITBUCKET" -> "https://bitbucket.org/" + e.getRepoName() + "/commits/" + sha;
            case "GITLAB"    -> "https://gitlab.com/" + e.getRepoName() + "/-/commit/" + sha;
            default          -> "https://github.com/" + e.getRepoName() + "/commit/" + sha;
        };
    }

    /** Returns all platform usernames owned by the current user (GitHub login + OAuth account IDs). */
    private Set<String> resolvePlatformUsernames(CurrentOidcUser currentUser) {
        final Set<String> usernames = new java.util.HashSet<>();
        userSettingsService.getUserSettings(currentUser.getUserIdComposite())
            .githubLogin().ifPresent(usernames::add);
        final Long userLocalId = currentUser.getUserIdComposite().localId().value();
        oAuthTokenRepository.findByUserLocalId(userLocalId)
            .forEach(t -> usernames.add(t.getPlatformAccountId()));
        return usernames;
    }

    private static String buildEventComment(GitActivityRawEventEntity e) {
        final String shortRepo = e.getRepoName().contains("/")
            ? e.getRepoName().substring(e.getRepoName().lastIndexOf('/') + 1)
            : e.getRepoName();
        return shortRepo + ": " + e.getEventSummary();
    }

    private static String buildAnchorRef(GitActivityRawEventEntity e) {
        return switch (e.getAnchorType()) {
            case "PR" -> e.getAnchorId() != null ? "PR #" + e.getAnchorId() : "";
            case "ISSUE" -> e.getAnchorId() != null ? "Issue #" + e.getAnchorId() : "";
            default -> "";
        };
    }

    @GetMapping("/search")
    String search(@RequestParam(required = false) String q,
                  @RequestParam(required = false) LocalDate from,
                  @RequestParam(required = false) LocalDate to,
                  @CurrentUser CurrentOidcUser currentUser,
                  Model model) {

        final String query = q != null ? q.strip() : "";
        model.addAttribute("searchQuery", query);

        if (query.isBlank()) {
            model.addAttribute("searchResultGroups", List.of());
            return "github-activity/search-results";
        }

        final UserSettings userSettings = userSettingsService.getUserSettings(currentUser.getUserIdComposite());
        if (!userSettings.githubLoginVerified() || userSettings.githubLogin().isEmpty()) {
            model.addAttribute("searchResultGroups", List.of());
            return "github-activity/search-results";
        }

        final String login = userSettings.githubLogin().get();
        final ZoneId zone = userSettingsProvider.zoneId();

        final LocalDate effectiveTo = to != null ? to : LocalDate.now();
        final LocalDate effectiveFrom = from != null ? from : effectiveTo.minusDays(30);

        final Instant fromInstant = effectiveFrom.atStartOfDay(zone).toInstant();
        final Instant toInstant = effectiveTo.plusDays(1).atStartOfDay(zone).toInstant();

        final List<GitActivityRawEventEntity> raw = eventRepository.searchEvents(login, query, fromInstant, toInstant);

        final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("EEE d MMM yyyy");
        final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

        final Map<LocalDate, List<GitActivityRawEventEntity>> byDate = new LinkedHashMap<>();
        for (GitActivityRawEventEntity e : raw) {
            byDate.computeIfAbsent(e.getEventTimestamp().atZone(zone).toLocalDate(),
                k -> new ArrayList<>()).add(e);
        }

        final List<GitSearchResultGroup> groups = byDate.entrySet().stream()
            .map(entry -> new GitSearchResultGroup(
                dateFmt.withZone(zone).format(entry.getKey().atStartOfDay(zone).toInstant()),
                entry.getKey(),
                entry.getValue().stream()
                    .filter(e -> !"Commit".equals(deriveSearchType(e)))
                    .map(e -> new GitSearchResultItem(
                        deriveSearchType(e),
                        e.getRepoName(),
                        e.getAnchorId(),
                        resolveAnchorTitle(e, List.of(e)),
                        timeFmt.withZone(zone).format(e.getEventTimestamp()),
                        e.getLoggedAt() != null))
                    .toList()))
            .filter(g -> !g.items().isEmpty())
            .toList();

        model.addAttribute("searchResultGroups", groups);
        return "github-activity/search-results";
    }

    private String deriveSearchType(GitActivityRawEventEntity e) {
        if ("ISSUE".equals(e.getAnchorType())) return "Issue";
        if ("PR".equals(e.getAnchorType()) && REVIEW_EVENT_TYPES.contains(e.getEventType())) return "Review";
        if ("PR".equals(e.getAnchorType())) return "PR";
        return "Commit";
    }

    record GitSearchResultItem(
        String type, String repoName, String anchorId,
        String title, String time, boolean logged) {}

    record GitSearchResultGroup(
        String dateLabel, LocalDate date, List<GitSearchResultItem> items) {}
}
