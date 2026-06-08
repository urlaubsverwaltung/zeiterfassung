package de.focusshift.zeiterfassung.gitactivity;

import java.util.List;

public record ActivityAnchor(
    String repoName,
    String anchorType,       // PR, ISSUE, REPO
    String anchorId,         // PR/issue number or branch name; null for repo-level
    String anchorTitle,      // PR title, issue title, or branch name
    List<AnchorEvent> events,
    String windowStart,      // HH:mm of earliest event
    String windowEnd,        // HH:mm of latest event; null when only one event
    String suggestedDuration,// HH:mm, floored to 15 min
    String prefilledComment,
    String openedDate,       // PRs: "Jun 2" if opened before the current day; null otherwise
    String prStatus,         // PRs: "Open", "Merged", "Closed"
    String reviewOutcome,    // Reviews: "Approved", "Changes requested", "Commented"
    String issueAction,      // Issues: "Opened", "Closed", "Commented", "Updated"
    boolean logged           // true when all events in this anchor have been logged
) {}
