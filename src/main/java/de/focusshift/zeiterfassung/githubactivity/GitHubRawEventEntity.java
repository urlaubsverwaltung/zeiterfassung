package de.focusshift.zeiterfassung.githubactivity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "github_raw_event", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"github_event_id"})
})
public class GitHubRawEventEntity {

    @Id
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    @SequenceGenerator(name = "github_raw_event_seq", sequenceName = "github_raw_event_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "github_raw_event_seq")
    private Long id;

    @Column(name = "github_event_id", nullable = false)
    private String githubEventId;

    @Column(name = "github_username", nullable = false)
    private String githubUsername;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "repo_name", nullable = false)
    private String repoName;

    /** PR, ISSUE, or REPO */
    @Column(name = "anchor_type", nullable = false)
    private String anchorType;

    /** PR number, issue number, branch name, tag name — null for repo-level events without a ref */
    @Column(name = "anchor_id")
    private String anchorId;

    /** PR title, issue title, or branch/tag name captured at sync time */
    @Column(name = "anchor_title")
    private String anchorTitle;

    /** For PullRequestEvent: the head branch (pull_request.head.ref). Null for all other event types. */
    @Column(name = "head_branch")
    private String headBranch;

    @Column(name = "event_icon", nullable = false)
    private String eventIcon;

    @Column(name = "event_summary", nullable = false)
    private String eventSummary;

    @Column(name = "event_timestamp", nullable = false)
    private Instant eventTimestamp;

    @Column(name = "dismissed", nullable = false)
    private boolean dismissed = false;

    @Column(name = "logged_at")
    private Instant loggedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getGithubEventId() { return githubEventId; }
    public void setGithubEventId(String githubEventId) { this.githubEventId = githubEventId; }

    public String getGithubUsername() { return githubUsername; }
    public void setGithubUsername(String githubUsername) { this.githubUsername = githubUsername; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getRepoName() { return repoName; }
    public void setRepoName(String repoName) { this.repoName = repoName; }

    public String getAnchorType() { return anchorType; }
    public void setAnchorType(String anchorType) { this.anchorType = anchorType; }

    public String getAnchorId() { return anchorId; }
    public void setAnchorId(String anchorId) { this.anchorId = anchorId; }

    public String getAnchorTitle() { return anchorTitle; }
    public void setAnchorTitle(String anchorTitle) { this.anchorTitle = anchorTitle; }

    public String getEventIcon() { return eventIcon; }
    public void setEventIcon(String eventIcon) { this.eventIcon = eventIcon; }

    public String getEventSummary() { return eventSummary; }
    public void setEventSummary(String eventSummary) { this.eventSummary = eventSummary; }

    public Instant getEventTimestamp() { return eventTimestamp; }
    public void setEventTimestamp(Instant eventTimestamp) { this.eventTimestamp = eventTimestamp; }

    public boolean isDismissed() { return dismissed; }
    public void setDismissed(boolean dismissed) { this.dismissed = dismissed; }

    public String getHeadBranch() { return headBranch; }
    public void setHeadBranch(String headBranch) { this.headBranch = headBranch; }

    public Instant getLoggedAt() { return loggedAt; }
    public void setLoggedAt(Instant loggedAt) { this.loggedAt = loggedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GitHubRawEventEntity that = (GitHubRawEventEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
