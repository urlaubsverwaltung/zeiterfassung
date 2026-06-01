package de.focusshift.zeiterfassung.githubactivity;

public record GitHubEvent(
    String type,
    String icon,
    String title,
    String detail,
    String prefilledComment,
    String startTime
) {}
