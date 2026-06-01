package de.focusshift.zeiterfassung.githubactivity;

import java.util.List;

public record GitHubActivityGroup(
    String repoName,
    List<GitHubEvent> events,
    String combinedComment
) {}
