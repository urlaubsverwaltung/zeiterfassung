package de.focusshift.zeiterfassung.user;

import de.focusshift.zeiterfassung.tenancy.tenant.AbstractTenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;

import static jakarta.persistence.EnumType.STRING;

@Entity
@Table(name = "user_settings")
public class UserSettingsEntity extends AbstractTenantAwareEntity {

    @Id
    @Column(name = "tenant_user_local_id")
    private Long tenantUserLocalId;

    @Column(name = "theme", nullable = false)
    @NotNull
    @Enumerated(STRING)
    private Theme theme;

    @Column(name = "locale")
    private Locale locale;

    @Column(name = "locale_browser_specific")
    private Locale localeBrowserSpecific;

    @Column(name = "github_login")
    @Size(max = 255)
    @Nullable
    private String githubLogin;

    @Column(name = "github_login_verified", nullable = false)
    private boolean githubLoginVerified = false;

    @Column(name = "github_token")
    @Size(max = 255)
    @Nullable
    private String githubToken;

    @Column(name = "notifications_enabled", nullable = false)
    private boolean notificationsEnabled = true;

    /**
     * GitHub App installation ID for the user's personal account.
     * Set when the user installs the GitHub App in their own account to grant
     * access to customer repos. Null means no personal installation connected.
     */
    @Column(name = "github_installation_id")
    @Nullable
    private Long githubInstallationId;

    @Column(name = "show_standalone_commits", nullable = false)
    private boolean showStandaloneCommits = false;

    protected UserSettingsEntity() {
        super(null);
    }

    protected UserSettingsEntity(String tenantId, Long tenantUserLocalId, Theme theme, @Nullable Locale locale, @Nullable Locale localeBrowserSpecific) {
        super(tenantId);
        this.tenantUserLocalId = tenantUserLocalId;
        this.theme = theme;
        this.locale = locale;
        this.localeBrowserSpecific = localeBrowserSpecific;
    }

    public Long getTenantUserLocalId() {
        return tenantUserLocalId;
    }

    public void setTenantUserLocalId(Long tenantUserLocalId) {
        this.tenantUserLocalId = tenantUserLocalId;
    }

    public Theme getTheme() {
        return theme;
    }

    public void setTheme(Theme theme) {
        this.theme = theme;
    }

    public @Nullable Locale getLocale() {
        return locale;
    }

    public void setLocale(@Nullable Locale locale) {
        this.locale = locale;
    }

    public @Nullable Locale getLocaleBrowserSpecific() {
        return localeBrowserSpecific;
    }

    public void setLocaleBrowserSpecific(@Nullable Locale localeBrowserSpecific) {
        this.localeBrowserSpecific = localeBrowserSpecific;
    }

    public @Nullable String getGithubLogin() {
        return githubLogin;
    }

    public void setGithubLogin(@Nullable String githubLogin) {
        this.githubLogin = githubLogin;
    }

    public boolean isGithubLoginVerified() {
        return githubLoginVerified;
    }

    public void setGithubLoginVerified(boolean githubLoginVerified) {
        this.githubLoginVerified = githubLoginVerified;
    }

    public @Nullable String getGithubToken() {
        return githubToken;
    }

    public void setGithubToken(@Nullable String githubToken) {
        this.githubToken = githubToken;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public @Nullable Long getGithubInstallationId() {
        return githubInstallationId;
    }

    public void setGithubInstallationId(@Nullable Long githubInstallationId) {
        this.githubInstallationId = githubInstallationId;
    }

    public boolean isShowStandaloneCommits() {
        return showStandaloneCommits;
    }

    public void setShowStandaloneCommits(boolean showStandaloneCommits) {
        this.showStandaloneCommits = showStandaloneCommits;
    }

    @Override
    public String toString() {
        return "UserSettingsEntity{" +
            "tenantUserLocalId=" + tenantUserLocalId +
            ", theme=" + theme +
            ", locale=" + locale +
            ", localeBrowserSpecific=" + localeBrowserSpecific +
            ", githubLogin=" + githubLogin +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserSettingsEntity that = (UserSettingsEntity) o;
        return Objects.equals(tenantUserLocalId, that.tenantUserLocalId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantUserLocalId);
    }
}
