package de.focusshift.zeiterfassung.githubactivity;

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
    String prefilledComment
) {}
