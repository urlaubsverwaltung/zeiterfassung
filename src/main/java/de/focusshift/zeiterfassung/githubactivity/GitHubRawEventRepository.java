package de.focusshift.zeiterfassung.githubactivity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface GitHubRawEventRepository extends JpaRepository<GitHubRawEventEntity, Long> {

    boolean existsByGithubEventId(String githubEventId);

    List<GitHubRawEventEntity> findByGithubUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
        String githubUsername, Instant from, Instant to);

    java.util.Optional<GitHubRawEventEntity> findByGithubEventId(String githubEventId);

    java.util.Optional<GitHubRawEventEntity> findFirstByGithubUsernameAndRepoNameAndAnchorTypeAndAnchorIdOrderByEventTimestampAsc(
        String githubUsername, String repoName, String anchorType, String anchorId);

    /**
     * Returns "repo|branch" keys for all PR head branches whose PullRequestEvent
     * was stored on or before {@code upTo}. For cross-fork PRs the fork repo is used
     * (COALESCE falls back to repoName for same-repo PRs where headRepoName is null).
     * Used to scope the PR-branch filter to the selected day, preventing retroactive
     * hiding of standalone commits that were pushed before the PR was opened.
     */
    @org.springframework.data.jpa.repository.Query(
        "SELECT DISTINCT COALESCE(e.headRepoName, e.repoName) || '|' || e.headBranch FROM GitHubRawEventEntity e " +
        "WHERE e.githubUsername = :username AND e.headBranch IS NOT NULL AND e.headBranch <> '' " +
        "AND e.eventTimestamp <= :upTo")
    java.util.Set<String> findDistinctRepoAndHeadBranchesByUsernameUpToDate(
        @org.springframework.data.repository.query.Param("username") String username,
        @org.springframework.data.repository.query.Param("upTo") java.time.Instant upTo);

    /**
     * Returns the most recent PR entity for the given user/repo/branch combination.
     * Used to synthesize a PR anchor on days where only commits were pushed (no PullRequestEvent).
     */
    java.util.Optional<GitHubRawEventEntity> findFirstByGithubUsernameAndRepoNameAndHeadBranchOrderByEventTimestampDesc(
        String githubUsername, String repoName, String headBranch);

    /**
     * Cross-fork variant: finds the PR entity by the fork repo (headRepoName) and branch.
     * Used as a fallback in synthetic PR anchor building when commits live in a fork.
     */
    java.util.Optional<GitHubRawEventEntity> findFirstByGithubUsernameAndHeadRepoNameAndHeadBranchOrderByEventTimestampDesc(
        String githubUsername, String headRepoName, String headBranch);

    /** Returns all PullRequestEvent entities for a user — used to determine which PRs are currently open. */
    java.util.List<GitHubRawEventEntity> findByGithubUsernameAndAnchorTypeAndEventType(
        String githubUsername, String anchorType, String eventType);

    @org.springframework.data.jpa.repository.Query(
        "SELECT e FROM GitHubRawEventEntity e " +
        "WHERE e.githubUsername = :username " +
        "AND e.dismissed = false " +
        "AND e.eventTimestamp >= :from " +
        "AND e.eventTimestamp < :to " +
        "AND (LOWER(e.repoName) LIKE LOWER(CONCAT('%', :query, '%')) " +
        "  OR LOWER(COALESCE(e.anchorTitle, '')) LIKE LOWER(CONCAT('%', :query, '%')) " +
        "  OR LOWER(e.eventSummary) LIKE LOWER(CONCAT('%', :query, '%'))) " +
        "ORDER BY e.eventTimestamp DESC")
    List<GitHubRawEventEntity> searchEvents(
        @org.springframework.data.repository.query.Param("username") String username,
        @org.springframework.data.repository.query.Param("query") String query,
        @org.springframework.data.repository.query.Param("from") java.time.Instant from,
        @org.springframework.data.repository.query.Param("to") java.time.Instant to);

    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(
        "DELETE FROM GitHubRawEventEntity e WHERE e.githubUsername = :username " +
        "AND e.eventType = 'PushEvent' AND e.githubEventId NOT LIKE :newPrefix")
    int deleteOldFormatCommits(
        @org.springframework.data.repository.query.Param("username") String username,
        @org.springframework.data.repository.query.Param("newPrefix") String newPrefix);

    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(
        "UPDATE GitHubRawEventEntity e SET e.loggedAt = :now " +
        "WHERE e.githubUsername = :username AND e.repoName = :repoName " +
        "AND e.anchorType = :anchorType AND e.anchorId = :anchorId " +
        "AND e.eventTimestamp BETWEEN :from AND :to AND e.loggedAt IS NULL")
    void markAnchorLogged(
        @org.springframework.data.repository.query.Param("username") String username,
        @org.springframework.data.repository.query.Param("repoName") String repoName,
        @org.springframework.data.repository.query.Param("anchorType") String anchorType,
        @org.springframework.data.repository.query.Param("anchorId") String anchorId,
        @org.springframework.data.repository.query.Param("from") java.time.Instant from,
        @org.springframework.data.repository.query.Param("to") java.time.Instant to,
        @org.springframework.data.repository.query.Param("now") java.time.Instant now);
}
