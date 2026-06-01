package de.focusshift.zeiterfassung.githubactivity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
interface GitHubRawEventRepository extends JpaRepository<GitHubRawEventEntity, Long> {

    boolean existsByGithubEventId(String githubEventId);

    List<GitHubRawEventEntity> findByGithubUsernameAndEventTimestampBetweenOrderByEventTimestampAsc(
        String githubUsername, Instant from, Instant to);
}
