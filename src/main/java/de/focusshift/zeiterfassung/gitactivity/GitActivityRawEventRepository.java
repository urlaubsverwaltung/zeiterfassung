package de.focusshift.zeiterfassung.gitactivity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface GitActivityRawEventRepository extends JpaRepository<GitActivityRawEventEntity, Long> {

    boolean existsByPlatformEventId(String platformEventId);

    Optional<GitActivityRawEventEntity> findByPlatformEventId(String platformEventId);

    List<GitActivityRawEventEntity> findByPlatformUsernameAndEventTimestampBetweenAndDismissedFalseOrderByEventTimestampAsc(
        String platformUsername, Instant from, Instant to);

    /**
     * Returns the earliest event timestamp per (repoName, anchorId) pair for the given PR anchor IDs.
     * Each row is [repoName (String), anchorId (String), minTimestamp (Instant)].
     * Used to batch-load PR opened-dates in a single query instead of one query per PR.
     */
    @org.springframework.data.jpa.repository.Query(
        "SELECT e.repoName, e.anchorId, MIN(e.eventTimestamp) " +
        "FROM GitActivityRawEventEntity e " +
        "WHERE e.platformUsername = :username AND e.anchorType = 'PR' AND e.anchorId IN :anchorIds " +
        "GROUP BY e.repoName, e.anchorId")
    List<Object[]> findEarliestPrTimestamps(
        @org.springframework.data.repository.query.Param("username") String username,
        @org.springframework.data.repository.query.Param("anchorIds") Collection<String> anchorIds);

    Optional<GitActivityRawEventEntity> findFirstByPlatformUsernameAndRepoNameAndAnchorTypeAndAnchorIdOrderByEventTimestampAsc(
        String platformUsername, String repoName, String anchorType, String anchorId);

    /**
     * Returns "repo|branch" keys for all PR head branches whose PullRequestEvent
     * was stored on or before {@code upTo}. For cross-fork PRs the fork repo is used
     * (COALESCE falls back to repoName for same-repo PRs where headRepoName is null).
     * Used to scope the PR-branch filter to the selected day, preventing retroactive
     * hiding of standalone commits that were pushed before the PR was opened.
     */
    @org.springframework.data.jpa.repository.Query(
        "SELECT DISTINCT COALESCE(e.headRepoName, e.repoName) || '|' || e.headBranch FROM GitActivityRawEventEntity e " +
        "WHERE e.platformUsername = :username AND e.headBranch IS NOT NULL AND e.headBranch <> '' " +
        "AND e.eventTimestamp <= :upTo")
    Set<String> findDistinctRepoAndHeadBranchesByUsernameUpToDate(
        @org.springframework.data.repository.query.Param("username") String username,
        @org.springframework.data.repository.query.Param("upTo") Instant upTo);

    /**
     * Returns the most recent PR entity for the given user/repo/branch combination.
     * Used to synthesize a PR anchor on days where only commits were pushed (no PullRequestEvent).
     */
    Optional<GitActivityRawEventEntity> findFirstByPlatformUsernameAndRepoNameAndHeadBranchOrderByEventTimestampDesc(
        String platformUsername, String repoName, String headBranch);

    /**
     * Cross-fork variant: finds the PR entity by the fork repo (headRepoName) and branch.
     * Used as a fallback in synthetic PR anchor building when commits live in a fork.
     */
    Optional<GitActivityRawEventEntity> findFirstByPlatformUsernameAndHeadRepoNameAndHeadBranchOrderByEventTimestampDesc(
        String platformUsername, String headRepoName, String headBranch);

    /** Returns all PullRequestEvent entities for a user — used to determine which PRs are currently open. */
    List<GitActivityRawEventEntity> findByPlatformUsernameAndAnchorTypeAndEventType(
        String platformUsername, String anchorType, String eventType);

    @org.springframework.data.jpa.repository.Query(
        "SELECT e FROM GitActivityRawEventEntity e " +
        "WHERE e.platformUsername = :username " +
        "AND e.dismissed = false " +
        "AND e.eventTimestamp >= :from " +
        "AND e.eventTimestamp < :to " +
        "AND (LOWER(e.repoName) LIKE LOWER(CONCAT('%', :query, '%')) " +
        "  OR LOWER(COALESCE(e.anchorTitle, '')) LIKE LOWER(CONCAT('%', :query, '%')) " +
        "  OR LOWER(e.eventSummary) LIKE LOWER(CONCAT('%', :query, '%'))) " +
        "ORDER BY e.eventTimestamp DESC")
    List<GitActivityRawEventEntity> searchEvents(
        @org.springframework.data.repository.query.Param("username") String username,
        @org.springframework.data.repository.query.Param("query") String query,
        @org.springframework.data.repository.query.Param("from") Instant from,
        @org.springframework.data.repository.query.Param("to") Instant to);

    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(
        "UPDATE GitActivityRawEventEntity e SET e.loggedAt = :now " +
        "WHERE e.platformUsername = :username AND e.repoName = :repoName " +
        "AND e.anchorType = :anchorType AND e.anchorId = :anchorId " +
        "AND e.eventTimestamp BETWEEN :from AND :to AND e.loggedAt IS NULL")
    void markAnchorLogged(
        @org.springframework.data.repository.query.Param("username") String username,
        @org.springframework.data.repository.query.Param("repoName") String repoName,
        @org.springframework.data.repository.query.Param("anchorType") String anchorType,
        @org.springframework.data.repository.query.Param("anchorId") String anchorId,
        @org.springframework.data.repository.query.Param("from") Instant from,
        @org.springframework.data.repository.query.Param("to") Instant to,
        @org.springframework.data.repository.query.Param("now") Instant now);
}
