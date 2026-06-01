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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

@Controller
@RequestMapping("/github-activity")
class GitHubActivityController implements HasTimeClock, HasLaunchpad, HasUserSearch {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final long MIN_SUGGESTED_MINUTES = 15;

    @Value("${github.app.id:}")
    private String githubAppId;

    private final UserSettingsService userSettingsService;
    private final UserSettingsProvider userSettingsProvider;
    private final TimeEntryService timeEntryService;
    private final UserSearchViewHelper userSearchViewHelper;
    private final ProjectService projectService;
    private final ActivityTypeService activityTypeService;
    private final TimeEntryLockService timeEntryLockService;
    private final GitHubRawEventRepository eventRepository;

    GitHubActivityController(UserSettingsService userSettingsService,
                              UserSettingsProvider userSettingsProvider,
                              TimeEntryService timeEntryService,
                              UserSearchViewHelper userSearchViewHelper,
                              ProjectService projectService,
                              ActivityTypeService activityTypeService,
                              TimeEntryLockService timeEntryLockService,
                              GitHubRawEventRepository eventRepository) {
        this.userSettingsService = userSettingsService;
        this.userSettingsProvider = userSettingsProvider;
        this.timeEntryService = timeEntryService;
        this.userSearchViewHelper = userSearchViewHelper;
        this.projectService = projectService;
        this.activityTypeService = activityTypeService;
        this.timeEntryLockService = timeEntryLockService;
        this.eventRepository = eventRepository;
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
            eventRepository.findByGithubUsernameAndEventTimestampBetweenOrderByEventTimestampAsc(login, dayStart, dayEnd);

        final List<ActivityAnchor> anchors = toAnchors(rawEvents, zone);
        final Long userLocalId = currentUser.getUserIdComposite().localId().value();

        model.addAttribute("date", selectedDate);
        model.addAttribute("prevDate", selectedDate.minusDays(1));
        model.addAttribute("nextDate", selectedDate.plusDays(1));
        model.addAttribute("anchors", anchors);
        model.addAttribute("userLocalId", userLocalId);
        model.addAttribute("isLocked", timeEntryLockService.isLocked(selectedDate));
        model.addAttribute("syncConfigured", isSyncConfigured());

        return "github-activity/index";
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

    private List<ActivityAnchor> toAnchors(List<GitHubRawEventEntity> entities, ZoneId zone) {
        if (entities.isEmpty()) return List.of();

        // Group by repo + anchorType + anchorId — preserving chronological order of first event
        final Map<String, List<GitHubRawEventEntity>> grouped = new LinkedHashMap<>();
        for (GitHubRawEventEntity e : entities) {
            final String key = e.getRepoName() + "|" + e.getAnchorType() + "|"
                + (e.getAnchorId() != null ? e.getAnchorId() : "");
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(e);
        }

        return grouped.values().stream().map(group -> {
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

            final List<AnchorEvent> events = group.stream()
                .map(e -> new AnchorEvent(
                    e.getEventIcon(),
                    e.getEventSummary(),
                    TIME_FMT.withZone(zone).format(e.getEventTimestamp())))
                .toList();

            final String anchorLabel = first.getAnchorTitle() != null && !first.getAnchorTitle().isBlank()
                ? first.getAnchorTitle()
                : (first.getAnchorId() != null ? first.getAnchorId() : "");
            final String anchorRef = buildAnchorRef(first);
            final String prefilledComment = first.getRepoName() + (anchorRef.isEmpty() ? "" : " " + anchorRef)
                + (anchorLabel.isEmpty() ? "" : ": " + anchorLabel);

            return new ActivityAnchor(
                first.getRepoName(),
                first.getAnchorType(),
                first.getAnchorId(),
                first.getAnchorTitle(),
                events,
                windowStart,
                windowEnd,
                suggestedDuration,
                prefilledComment
            );
        }).toList();
    }

    private static String buildAnchorRef(GitHubRawEventEntity e) {
        return switch (e.getAnchorType()) {
            case "PR" -> e.getAnchorId() != null ? "PR #" + e.getAnchorId() : "";
            case "ISSUE" -> e.getAnchorId() != null ? "Issue #" + e.getAnchorId() : "";
            default -> "";
        };
    }

    private boolean isSyncConfigured() {
        return !githubAppId.isBlank();
    }
}
